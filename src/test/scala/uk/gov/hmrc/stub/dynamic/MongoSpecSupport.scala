/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.stub.dynamic

import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{DefaultDB, FailoverStrategy}
import reactivemongo.api.collections.bson.BSONCollection
import uk.gov.hmrc.mongo.{Awaiting, MongoConnector}

trait MongoSpecSupport {

  protected val databaseName: String = "test-" + this.getClass.getSimpleName
  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?rm.heartbeatFrequencyMS=4000&rm.failover=default"

  implicit lazy val mongoConnectorForTest: MongoConnector = MongoConnector(mongoUri)
  implicit val mongo: () => DefaultDB = mongoConnectorForTest.db

  private def getCollection(name: String)(
      failoverStrategy: FailoverStrategy = mongoConnectorForTest.helper.db.failoverStrategy): BSONCollection = {
    mongoConnectorForTest.helper.db(name, failoverStrategy)
  }

  def dropTestCollection(collectionName: String): Unit = new Awaiting {
    await {
      getCollection(collectionName)().drop(failIfNotFound = false)
    }
  }

  implicit val reactiveMongoComponent: ReactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }
}
