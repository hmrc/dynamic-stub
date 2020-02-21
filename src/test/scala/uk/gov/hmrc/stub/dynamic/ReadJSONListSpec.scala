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

import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsNull, JsUndefined, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ReadJSONListSpec extends UnitSpec with BeforeAndAfterEach with JsonFormats {


  override def endpoint: EndPoint = ???

  val singleKey = SingleConfigKey("bla")
  val singleKey2 = SingleConfigKey("ble")

  "reading list" should {
    "read list with a single element" in {
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(singleKey, singleKey2))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values":[{
          |    "bla":{"value":"aValue"},
          |    "ble":{"value":"aValue2"},
          |    "ble2":{"value":"aValue2"}
          | }]}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        MultiConfigKey(
          "collection",
          List(SingleConfigKey("bla"), SingleConfigKey("ble"))
        ) ->
        ListValue(Seq(Map(
          singleKey -> StringValue("aValue"),
          singleKey2 -> StringValue("aValue2")
        ))))
    }

    "read list with multiple elements" in {
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(singleKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values":[{"bla":{"value":"aValue"}}, {"bla":{"value":"aValue2"}}]}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(MultiConfigKey("collection", List(SingleConfigKey("bla"))) ->
        ListValue(Seq(
        Map(singleKey -> StringValue("aValue")),
        Map(singleKey -> StringValue("aValue2")))))
    }

    "read recursive list" in {
      val innerCollectionKey = MultiConfigKey("innerCollection", Seq(singleKey))
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(innerCollectionKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values":[{"innerCollection":{"values":[{"bla":{"value":"aValue"}}]}}, {"bla":{"value":"aValue2"}}]}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(MultiConfigKey("collection",
        List(MultiConfigKey("innerCollection",List(SingleConfigKey("bla"))))) ->
        ListValue(List(Map(MultiConfigKey("innerCollection",
          List(SingleConfigKey("bla"))) -> ListValue(List(Map(SingleConfigKey("bla") -> StringValue("aValue"))))),
          Map())))
    }

    "when declared value is not provided" in {
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(singleKey, singleKey2))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values":[{
          | "bla":{"value":"aValue"}
          |}]}}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        MultiConfigKey("collection", List(SingleConfigKey("bla"), SingleConfigKey("ble"))) ->
        ListValue(Seq(Map(
          singleKey -> StringValue("aValue")
        ))))
    }

    "read object within a list" in {
      val innerObjectKey = ObjectConfigKey("innerObject", Seq(singleKey))
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(innerObjectKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values":[{"innerObject":{"value":{"bla":{"value":"aValue"}}}}]}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(MultiConfigKey("collection",
        List(ObjectConfigKey("innerObject",List(SingleConfigKey("bla"))))) ->
        ListValue(List(Map(ObjectConfigKey("innerObject", List(SingleConfigKey("bla")))
          -> ObjectValue(Map(SingleConfigKey("bla") -> StringValue("aValue")))))))
    }


    "when JSON is undefined" in {
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(singleKey))))
      val result = readList.reads(JsNull)

      result.isSuccess shouldBe true

      result.get shouldBe Map.empty
    }

    "when JSON is invalid" in {
      val readList = mapReads(Seq(MultiConfigKey("collection", Seq(singleKey))))
      val result = readList.reads(Json.parse(
        """
          |{
          |"collection":
          | {"values": "notAnArray"}}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map.empty
    }
  }
}
