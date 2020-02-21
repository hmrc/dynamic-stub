/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.stub.dynamic.repository

import java.net.URI

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, Writes, _}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONArray, BSONDateTime, BSONDocument, BSONInteger, BSONLong, BSONObjectID, BSONString, BSONValue}
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.BSONFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, BSONBuilderHelpers, DatabaseUpdate, ReactiveRepository}
import uk.gov.hmrc.time.DateTimeUtils

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class ExpectationMongo(testId: String,
                            template: String,
                            delay: Option[Long],
                            resultCode: Option[Int],
                            timeToLive: Option[Long]) {

  import scala.concurrent.duration._

  def toLive = timeToLive.fold(5 minutes)(_ millis)
}

case class ExpectationSave(id: BSONObjectID, uri: Seq[URI], expectation: ExpectationMongo)

object ExpectationSave {

  val mongoFormats: Format[ExpectationSave] = ReactiveMongoFormats.mongoEntity(
    {
      implicit val oidFormat = ReactiveMongoFormats.objectIdFormats

      implicit val expectationReads = Json.reads[ExpectationMongo]

      implicit val expectationSaveReads: Reads[ExpectationSave] =
        ((__ \ "_id").read[BSONObjectID] and
          (__ \ "uri").read[Seq[String]] and
          (__ \ "expectation").read[ExpectationMongo]
          ) {
          (id, uri, expectation) => {
            ExpectationSave(id, uri.map(new URI(_)), expectation)
          }
        }

      implicit val urlWrites = Writes[URI] {
        url => JsString(url.toString)
      }
      implicit val writes = Json.writes[ExpectationMongo]
      implicit val writesExpectation = Json.writes[ExpectationSave]

      Format(expectationSaveReads, writesExpectation)
    })
}

object DynamicRepository extends MongoDbConnection {
  lazy val mongo = new DynamicRepositoryTestDataMongoRepository

  def apply(): DynamicTestDataRepository = mongo
}

class DynamicRepositoryTestDataMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[ExpectationSave, BSONObjectID]("dynamic", mongo, ExpectationSave.mongoFormats, ReactiveMongoFormats.objectIdFormats)
    with AtomicUpdate[ExpectationSave]
    with DynamicTestDataRepository
    with BSONBuilderHelpers {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[scala.Seq[Boolean]] = {
    // set to zero for per document TTL using 'expiry' attribute to define the actual expiry time.
    val expireAfterSeconds = 0
    Future.sequence(
      Seq(
        collection.indexesManager.ensure(
          Index(Seq("expectation.testId" -> IndexType.Ascending), name = Some("testIdUnique"), unique = true)),
        collection.indexesManager.ensure(
          Index(Seq("uri" -> IndexType.Ascending), name = Some("uriUnique"), unique = false)),
        collection.indexesManager.ensure(
          Index(
            key = Seq("expiry" -> IndexType.Ascending),
            options = BSONDocument("expireAfterSeconds" -> expireAfterSeconds)))
      )
    )
  }

  override def isInsertion(suppliedId: BSONObjectID, returned: ExpectationSave): Boolean =
    suppliedId.equals(returned.id)

  protected def findByTestId(testId: String) = BSONDocument("expectation.testId" -> BSONString(testId))

  protected def bsonJson[T](entity: T)(implicit writes: Writes[T]) = BSONFormats.toBSON(Json.toJson(entity)).get

  private def modifierForInsert(uriSeq: Seq[URI], expectation: ExpectationMongo): BSONDocument = {
    val delay = optionalValues(expectation.delay, "expectation.delay", BSONLong)
    val resultCode = optionalValues(expectation.resultCode, "expectation.resultCode", BSONInteger)
    BSONDocument(
      "$setOnInsert" -> BSONDocument("expiry" -> BSONDateTime(DateTimeUtils.now.getMillis + expectation.toLive.toMillis)),
      "$setOnInsert" -> BSONDocument("expectation.testId" -> expectation.testId),
      "$set" -> BSONDocument("expectation.template" -> expectation.template),
      "$set" -> BSONDocument("uri" -> BSONArray(uriSeq.map(uri => BSONString(uri.toASCIIString))))
    ) ++ delay ++ resultCode
  }

  override def add(uri: Seq[URI], expectation: ExpectationMongo): Future[DatabaseUpdate[ExpectationSave]] = {
    atomicUpsert(findByTestId(expectation.testId), modifierForInsert(uri, expectation))
  }

  override def findByIdAndUri(id: String, uri: String): Future[Option[ExpectationSave]] = {
    collection.find(and(BSONDocument("expectation.testId" -> id) ++ BSONDocument("uri" -> uri))).one[ExpectationSave]
  }

  override def removeById(id: String)(implicit ec: ExecutionContext): Future[WriteResult] = {
    removeById(BSONObjectID(id.getBytes))
  }

  private def unset(key: String): BSONDocument = BSONDocument("$unset" -> BSONDocument(key -> 0L))

  private def set[T](value: T, key: String, bsonObject: T => BSONValue): BSONDocument = BSONDocument("$set" -> BSONDocument(key -> bsonObject(value)))

  private def optionalValues[T](option: Option[T], key: String, bsonObject: T => BSONValue): BSONDocument = {
    option.fold(unset(key)) {
      value => set(value, key, bsonObject)
    }
  }
}

trait DynamicTestDataRepository {
  def removeById(id: String)(implicit ec: ExecutionContext): Future[WriteResult]

  def add(uri: Seq[URI], expectation: ExpectationMongo): Future[DatabaseUpdate[ExpectationSave]]

  def findByIdAndUri(id: String, uri: String): Future[Option[ExpectationSave]]
}
