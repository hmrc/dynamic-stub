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

package uk.gov.hmrc.stub.dynamic

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{BaseController, Call, ControllerComponents}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.stub.dynamic.repository.DynamicTestDataRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DynamicStubDataController @Inject() (removeUrl:    BSONObjectID => Call,
                                           val cache:    DynamicTestDataRepository,
                                           val endpoint: EndPoint)
  (implicit ec: ExecutionContext,
   val controllerComponents: ControllerComponents)
  extends ServiceStubResponse
  with BaseController
  with JsonFormats {

  // Record a stub service definition. Return the URI's which have been stubbed from the request and the associated remove URI in the Location header.
  def recordService = Action.async(parse.json) { implicit request =>
    request.body.validate[Expectation].fold(
      errors => {
        Logger(s"Received error from parsing json $errors")
        Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      update => {
        saveToCache(update).map {
          case Some((uris, id)) => Created(Json.obj("uri" -> Json.toJson(uris.map(_.toString))))
            .withHeaders("Location" -> removeUrl(id).url)
          case value =>
            Logger(s"Did not receive URIs or BSON Ids from insert. saveToCache returned: $value")
            BadRequest
        }
      }
    )
  }

  // Remove the service definition.
  def removeService(id: String) = Action.async { _ =>
    cache.removeById(id).map(_ => NoContent)
  }
}
