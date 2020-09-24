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

import javax.inject.{Inject, Singleton}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, Writes, _}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers.{BSONDocumentWrites ⇒ _, _}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{BSONBuilderHelpers, ReactiveRepository}
import uk.gov.hmrc.time.DateTimeUtils

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class ExpectationMongo(testId:     String,
                            template:   String,
                            delay:      Option[Long],
                            resultCode: Option[Int],
                            timeToLive: Option[Long]) {

  import scala.concurrent.duration._

  def toLive = timeToLive.fold(5 minutes)(_ millis)
}

case class ExpectationSave(id: BSONObjectID, uri: Seq[URI], expectation: ExpectationMongo)

object ExpectationSave {

  val mongoFormats: Format[ExpectationSave] = ReactiveMongoFormats.mongoEntity(
    {

      implicit val expectationReads: Reads[ExpectationMongo] = Json.reads[ExpectationMongo]

      implicit val expectationSaveReads: Reads[ExpectationSave] =
        ((__ \ "_id").read[BSONObjectID] and
          (__ \ "uri").read[Seq[String]] and
          (__ \ "expectation").read[ExpectationMongo]
        ) {
            (id, uri, expectation) =>
              {
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

@Singleton
class DynamicTestDataRepository @Inject() ()(implicit mongo: ReactiveMongoComponent)
  extends ReactiveRepository[ExpectationSave, BSONObjectID]("dynamic", mongo.mongoConnector.db, ExpectationSave.mongoFormats, ReactiveMongoFormats.objectIdFormats)
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
            key     = Seq("expiry" -> IndexType.Ascending),
            options = BSONDocument("expireAfterSeconds" -> expireAfterSeconds)))
      )
    )
  }

  protected def findByTestId(testId: String): JsObject = Json.obj("expectation.testId" -> testId)

  private def modifierForInsert(uriSeq: Seq[URI], expectation: ExpectationMongo): JsObject = {
    val delay = optionalValues(expectation.delay, "expectation.delay")
    val resultCode = optionalValues(expectation.resultCode, "expectation.resultCode")
    Json.obj(
      "$setOnInsert" -> Json.obj("expiry" -> (DateTimeUtils.now.getMillis + expectation.toLive.toMillis)),
      "$setOnInsert" -> Json.obj("expectation.testId" -> expectation.testId),
      "$set" -> Json.obj("expectation.template" -> expectation.template),
      "$set" -> Json.obj("uri" -> Json.arr(uriSeq.map(uri => uri.toASCIIString)))
    ) ++ delay ++ resultCode
  }

  def add(uri: Seq[URI], expectation: ExpectationMongo)(implicit ec: ExecutionContext): Future[Option[ExpectationSave]] =
    findAndUpdate(findByTestId(expectation.testId), modifierForInsert(uri, expectation), upsert = true).map(_.result[ExpectationSave])

  def findByIdAndUri(id: String, uri: String)(implicit ec: ExecutionContext): Future[Option[ExpectationSave]] =
    collection.find(Json.obj("expectation.testId" -> id, "uri" -> uri), None).one[ExpectationSave]

  def removeById(id: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    removeById(BSONObjectID(id.getBytes))

  private def unset(key: String): JsObject = Json.obj("$unset" -> Json.obj(key -> 0L))

  private def set[T: Writes](value: T, key: String): JsObject = Json.obj("$set" -> Json.obj(key -> value))

  private def optionalValues[T: Writes](option: Option[T], key: String): JsObject = {
    option.fold(unset(key)) {
      value => set(value, key)
    }
  }
}

