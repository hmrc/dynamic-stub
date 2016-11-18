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

import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsNull, JsUndefined, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ReadJSONObjectSpec extends UnitSpec with BeforeAndAfterEach with JsonFormats{


  override def endpoint: EndPoint = ???

  val singleKey = SingleConfigKey("bla")
  val singleKey2 = SingleConfigKey("ble")

  "reading object" should {
    "read object with a single field" in {
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(singleKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"obj":
          | {"value":{
          |    "bla":{"value":"aValue"}
          | }}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        ObjectConfigKey(
          "obj",
          List(SingleConfigKey("bla"))
        ) ->
        ObjectValue(Map(
          singleKey -> StringValue("aValue")
        )))
    }

    "read object with multiple fields" in {
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(singleKey, singleKey2))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"obj":
          | {"value":{
          |    "bla":{"value":"aValue"},
          |    "ble":{"value":"aValue2"},
          |    "ble2":{"value":"aValue2"}
          | }}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        ObjectConfigKey(
          "obj",
          List(SingleConfigKey("bla"), SingleConfigKey("ble"))
        ) ->
          ObjectValue(Map(
            singleKey -> StringValue("aValue"),
            singleKey2 -> StringValue("aValue2")
          )))
    }

    "read recursive objects" in {
      val innerCollectionKey = ObjectConfigKey("innerObject", Seq(singleKey))
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(innerCollectionKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"obj":
          | {"value":{"innerObject":{"value":{"bla":{"value":"aValue"}}}}}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(ObjectConfigKey("obj",
        List(ObjectConfigKey("innerObject",List(SingleConfigKey("bla"))))) ->
        ObjectValue(Map(ObjectConfigKey("innerObject", List(SingleConfigKey("bla")))
          -> ObjectValue(Map(SingleConfigKey("bla") -> StringValue("aValue"))))))
    }

    "read list within an object" in {
      val innerCollectionKey = MultiConfigKey("innerCollection", Seq(singleKey))
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(innerCollectionKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"obj":
          | {"value":{"innerCollection":{"values":[{"bla":{"value":"aValue"}}]}}}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(ObjectConfigKey("obj",
        List(MultiConfigKey("innerCollection",List(SingleConfigKey("bla"))))) ->
        ObjectValue(Map(MultiConfigKey("innerCollection", List(SingleConfigKey("bla")))
          -> ListValue(List(Map(SingleConfigKey("bla") -> StringValue("aValue")))))))
    }

    "when declared value is not provided" in {
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(singleKey, singleKey2))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"obj":
          | {"value":{
          | "bla":{"value":"aValue"}
          |}}}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        ObjectConfigKey("obj", List(SingleConfigKey("bla"), SingleConfigKey("ble"))) ->
        ObjectValue(Map(
          singleKey -> StringValue("aValue")
        )))
    }

    "when JSON is undefined" in {
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(singleKey))))
      val result = readList.reads(JsNull)

      result.isSuccess shouldBe true

      result.get shouldBe Map.empty
    }

    "when JSON is invalid" in {
      val readList = mapReads(Seq(ObjectConfigKey("obj", Seq(singleKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"value": "notAnObject"}}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map.empty
    }
  }
}
