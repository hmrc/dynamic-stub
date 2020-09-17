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
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.mvc.{Call, Headers}
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.TxtFormat
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.stub.dynamic.repository.{DynamicTestDataRepository, ExpectationMongo, ExpectationSave}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DynamicStubDataControllerSpec extends UnitSpec with MockitoSugar with Matchers with ArgumentMatchersSugar {

  def generateDeletionCall(id: BSONObjectID) = Call("DELETE", "/some/uri/to/delete/" + id.stringify)
  val mockRepo = mock[DynamicTestDataRepository]
  implicit val cc = Helpers.stubControllerComponents()
  object Keys {
    val nino = SingleConfigKey("nino")
  }

  val templates = Seq(
    new UrlTemplate(Set(Keys.nino)) {
      def apply(data: DataSupplier) =
        new URI(s"/individuals/nino/${data(Keys.nino)}/financial-accounts?nino=${data(Keys.nino)}")
    }
  )

  val endpoint = new EndPoint {
    override def keys: Seq[ConfigKey] = Seq(Keys.nino)
    override def defaults: Map[ConfigKey, ValueType] = Map.empty
    override def bodyTemplate: DataSupplier ⇒ TxtFormat.Appendable = _ ⇒ TxtFormat.empty
    override def urlTemplates: Seq[UrlTemplate] = templates
  }

  val controller = new DynamicStubDataController(generateDeletionCall, mockRepo, endpoint)

  val validPayload = Json.parse("""{
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

  val invalidPayload = Json.obj("some" → true, "random" → List("yup"), "json" → 42)

  val expectationMongo = ExpectationMongo("testId", "template", None, None, None)

  val fakeUri = new URI("http", "localhost", "/foot/bar", "baz")

  val expectationSave = ExpectationSave(BSONObjectID.generate(), Seq(fakeUri), expectationMongo)

  "The DynamicStubDataController" should {
    "return a 400 if the Expectation json payload is incorrect" in {

      val fakeRequest = FakeRequest("DELETE", "someUri", Headers(), invalidPayload)
      val result = await(controller.recordService()(fakeRequest))
      result.header.status shouldBe Status.BAD_REQUEST

    }
    "return a 201 if the json payload is correct and we create the stub, including the remove URL in the Location header" in {

      when(mockRepo.add(any, any)(any)).thenReturn(Future.successful(Some(expectationSave)))

      val fakeRequest = FakeRequest("POST", "someUri", Headers(), validPayload)
      val result = await(controller.recordService()(fakeRequest))

      result.header.status shouldBe Status.CREATED
      result.header.headers(HeaderNames.LOCATION) shouldBe generateDeletionCall(expectationSave.id).url

    }
    "return a 204 if asked to remove a stub" in {
      when(mockRepo.removeById(any)(any)).thenReturn(Future.successful(UpdateWriteResult(true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val fakeRequest = FakeRequest("DELETE", "someUri")
      val result = await(controller.removeService("ik")(fakeRequest))

      result.header.status shouldBe Status.NO_CONTENT
    }
  }

}
