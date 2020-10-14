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

import java.net.URI

import org.mockito.ArgumentMatchersSugar
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Call, ControllerComponents, Headers}
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.TxtFormat
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.stub.dynamic.repository.{DynamicTestDataRepository, ExpectationMongo}

import scala.concurrent.ExecutionContext.Implicits.global

class DynamicStubDataControllerSpec
  extends UnitSpec
  with MockitoSugar
  with Matchers
  with ArgumentMatchersSugar
  with MongoSpecSupport
  with BeforeAndAfterEach {

  def generateDeletionCall(id: BSONObjectID): Call = Call("DELETE", "/some/uri/to/delete/" + id.stringify)

  val dynamicStubRepo = new DynamicTestDataRepository()

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropTestCollection("dynamic")
  }

  implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
  object Keys {
    val nino: SingleConfigKey = SingleConfigKey("nino")
  }

  val templates = Seq(
    new UrlTemplate(Set(Keys.nino)) {
      def apply(data: DataSupplier) =
        new URI(s"/individuals/nino/${data(Keys.nino)}/financial-accounts?nino=${data(Keys.nino)}")
    }
  )

  val endpoint: EndPoint = new EndPoint {
    override def keys: Seq[ConfigKey] = Seq(Keys.nino)
    override def defaults: Map[ConfigKey, ValueType] = Map.empty
    override def bodyTemplate: DataSupplier ⇒ TxtFormat.Appendable = _ ⇒ TxtFormat.empty
    override def urlTemplates: Seq[UrlTemplate] = templates
  }

  val controller = new DynamicStubDataController(generateDeletionCall, dynamicStubRepo, endpoint)

  val validPayload: JsValue = Json.parse("""{
    "testId":"1234567",
    "name":"CID",
    "service":"find",
    "data":{
      "nino":{
      "value":"AA000003D"
    },
      "sautr":{
      "value":"1234567890"
    },
      "firstName":{
      "value": "Jim"
    },
      "lastName":{
      "value": "Ferguson"
    },
      "dob":{
      "value": "01011948"
    }
    },
    "resultCode":200,
    "delay":5000,
    "timeToLive":300000 }""")

  val invalidPayload: JsObject = Json.obj("some" → true, "random" → List("yup"), "json" → 42)

  val expectationMongo: ExpectationMongo = ExpectationMongo("testId", "template", None, None, None)

  val fakeUri = new URI("http", "localhost", "/foot/bar", "baz")

  "The DynamicStubDataController" should {

    "return a 400 if the Expectation json payload is incorrect" in {

      val fakeRequest = FakeRequest("DELETE", "someUri", Headers(), invalidPayload)
      val result = await(controller.recordService()(fakeRequest))
      result.header.status shouldBe Status.BAD_REQUEST

    }

    "return a 201 if the json payload is correct and we create the stub, including the remove URL in the Location header" in {

      val fakeRequest = FakeRequest("POST", "someUri", Headers(), validPayload)
      val result = await(controller.recordService()(fakeRequest))

      result.header.status shouldBe Status.CREATED
      result.header.headers(HeaderNames.LOCATION) should startWith("/some/uri/to/delete/")

    }

    "return a 204 if asked to remove a stub" in {

      val fakePost = FakeRequest("POST", "someUri", Headers(), validPayload)
      val result1 = await(controller.recordService()(fakePost))

      result1.header.status shouldBe Status.CREATED
      val deleteLocation = result1.header.headers(HeaderNames.LOCATION)
      deleteLocation should startWith("/some/uri/to/delete/")

      val bsonId = deleteLocation.stripPrefix("/some/uri/to/delete/")

      val fakeDelete = FakeRequest("DELETE", deleteLocation)
      val result2 = await(controller.removeService(bsonId)(fakeDelete))

      result2.header.status shouldBe Status.NO_CONTENT

    }
  }

}
