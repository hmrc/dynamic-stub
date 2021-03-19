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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
trait JsonFormats {
  def endpoint: EndPoint

  /**
   * converts json like:  {"value": "leaf-value"}
   * to an instance of StringValue which is like a String
   */
  private val stringValueReads = Reads[StringValue] { jv =>
    (jv \ "value").asOpt[String].fold[JsResult[StringValue]](JsError("no value"))(value => JsSuccess(StringValue(value)))
  }

  /**
   * converts json like:
   * {"values" :[
   * {"a-key" : {"value": "leaf-value-1a"}, "b-key": {"value": "leaf-value-1b}},
   * {"a-key" : {"value": "leaf-value-2a"}, "b-key": {"value": "leaf-value-2b}},
   * ...
   * ]}
   * to an instance of ListValue which is like a sequence of maps
   */
  private def listReads(children: Seq[ConfigKey]) = Reads[ListValue] { jv =>
    (jv \ "values").get match {
      case JsArray(elements) => JsSuccess(ListValue(elements.map(extract(children))))
      case _                 => JsError()
    }
  }

  /**
   * converts json like:
   * {"value" :
   * {"a-key" : {"value": "leaf-value-a"}, "b-key": {"value": "leaf-value-b}
   * }
   * to an instance of ObjectValue which is like a map
   */
  private def objectReads(children: Seq[ConfigKey]) = Reads[ObjectValue] { jv =>
    (jv \ "value").get match {
      case obj @ JsObject(_) => JsSuccess(ObjectValue(extract(children)(obj)))
      case _                 => JsError()
    }
  }

  private def extract(children: Seq[ConfigKey])(js: JsValue) =
    js.as[Map[ConfigKey, ValueType]](mapReads(children))

  def mapReads(keys: Seq[ConfigKey]): Reads[Map[ConfigKey, ValueType]] = Reads[Map[ConfigKey, ValueType]] { jv =>

      def readValueFor(key: ConfigKey): Option[ValueType] = {
        key match {
          case SingleConfigKey(name)           => (jv \ name).asOpt[StringValue](stringValueReads)
          case ObjectConfigKey(name, children) => (jv \ name).asOpt[ObjectValue](objectReads(children))
          case MultiConfigKey(name, children)  => (jv \ name).asOpt[ListValue](listReads(children))
        }
      }

    val result = for {
      key <- keys
      value <- readValueFor(key)
    } yield key -> value

    JsSuccess(result.toMap)
  }

  implicit lazy val expectationReads: Reads[Expectation] = {

    implicit val dataReads = mapReads(endpoint.keys)

    ((__ \ "testId").read[String] and
      (__ \ "data").read[Map[ConfigKey, ValueType]] and
      (__ \ "delay").readNullable[Long] and
      (__ \ "resultCode").readNullable[Int] and
      (__ \ "timeToLive").readNullable[Long]
    )((id, data, delay, resultCode, ttl) ⇒ Expectation(id, endpoint, data, delay, resultCode, ttl)
      )
  }
}
