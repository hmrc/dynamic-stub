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

class ReadJSONStringsSpec extends UnitSpec with BeforeAndAfterEach with JsonFormats {


  override def endpoint: EndPoint = ???

  val singleKey = SingleConfigKey("bla")
  val singleKey2 = SingleConfigKey("ble")

  "reading" should {
    "read single values" in {
      val readList = mapReads(Seq(singleKey, singleKey2))
      val result = readList.reads(Json.parse(
        """
          |{
          |"bla":{"value":"aValue"},
          |"ble":{"value":"aValue2"},
          |"ble2":{"value":"aValue2"}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        singleKey -> StringValue("aValue"),
        singleKey2 -> StringValue("aValue2")
      )
    }

    "not read invalid values" in {
      val readList = mapReads(Seq(singleKey, singleKey2))
      val result = readList.reads(Json.parse(
        """
          |{
          |"bla": "blabla",
          |"ble":{"value":"aValue2"},
          |"ble2":{"value":"aValue2"}
          |}
        """.stripMargin))

      result.isSuccess shouldBe true

      result.get shouldBe Map(
        singleKey2 -> StringValue("aValue2")
      )
    }

    "not read invalid JSON" in {
      val read = mapReads(Seq(singleKey, singleKey2))
      val result = read.reads(JsNull)

      result.isSuccess shouldBe true

      result.get shouldBe Map()
    }
  }
}
