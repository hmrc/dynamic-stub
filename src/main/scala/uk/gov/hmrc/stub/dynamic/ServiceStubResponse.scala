/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URI

import play.api.Logger
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.stub.dynamic.repository.{DynamicTestDataRepository, ExpectationMongo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ServiceStubResponse {

  def cache: DynamicTestDataRepository

  // Save a stub definition to the cache.
  protected def saveToCache(expectation: Expectation): Future[Option[(Seq[URI], BSONObjectID)]] = {
    // Build a list of URI's that are generated from the URI templates using the input keys.
    val resources: scala.Seq[URI] = for {
      urlTemplate <- expectation.endpoint.urlTemplates
      if urlTemplate.isConfiguredFor(expectation)
      uri = urlTemplate(new DataSupplier(expectation.data))
    } yield uri

    if (resources.isEmpty) {
      Logger.error("Failed to record resource URI's from input request and template definition! Check the keys configured for the request!")
      Future.successful(None)
    } else {
      val mongoExpectation = ExpectationMongo(expectation.testId,
        expectation.endpoint.bodyTemplate(new DataSupplier(expectation.endpoint.defaults ++ expectation.data)).body,
        expectation.delay,
        expectation.resultCode,
        expectation.timeToLive)

      cache.add(resources, mongoExpectation).map(
        update => {
          Some((resources, update.updateType.savedValue.id))
        })
    }
  }

  private def getTestSessionId(implicit request: Request[AnyContent]) = request.headers.get("True-Client-IP")

  protected def stubbedResponse(query: String, endpoint: EndPoint)(func: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    getTestSessionId.fold(func) { key =>
      cache.findByIdAndUri(key, query).flatMap(
        expectation => {
          expectation.fold(func) { item => {
            val result = Results.Status(item.expectation.resultCode.getOrElse(200))(item.expectation.template)
            TimedEvent.delayedSuccess(item.expectation.delay.getOrElse(0), result)
          }
          }
        })
    }
  }

}
