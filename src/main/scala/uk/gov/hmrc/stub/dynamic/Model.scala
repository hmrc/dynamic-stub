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

import java.net.URI

import play.twirl.api.TxtFormat

abstract class UrlTemplate(keys: Set[ConfigKey]) {
  def isConfiguredFor(expectation: Expectation) = {
    keys.intersect(expectation.keys) == keys
  }

  def apply(data: DataSupplier): URI

}

trait EndPoint {
  def keys: Seq[ConfigKey]
  def defaults: Map[ConfigKey, ValueType]
  def bodyTemplate: DataSupplier => TxtFormat.Appendable
  def urlTemplates: Seq[UrlTemplate]
}

class DataSupplier(data: Map[ConfigKey, ValueType]) {
  def apply(key: SingleConfigKey): String = data(key).asInstanceOf[StringValue].value
  def apply(key: ObjectConfigKey): DataSupplier = new DataSupplier(data(key).asInstanceOf[ObjectValue].data)
  def apply(key: MultiConfigKey): Seq[DataSupplier] = data(key).asInstanceOf[ListValue].data.map(new DataSupplier(_))
  def isDefinedAt(key: ConfigKey): Boolean = data.isDefinedAt(key)
}

sealed trait ValueType
case class StringValue(value: String) extends ValueType
case class ObjectValue(data: Map[ConfigKey, ValueType]) extends ValueType
case class ListValue(data: Seq[Map[ConfigKey, ValueType]]) extends ValueType

sealed trait ConfigKey
case class SingleConfigKey(name: String) extends ConfigKey
case class ObjectConfigKey(name: String, children: Seq[ConfigKey]) extends ConfigKey
case class MultiConfigKey(name: String, children: Seq[ConfigKey]) extends ConfigKey

case class Expectation(testId:     String,
                       endpoint:   EndPoint,
                       data:       Map[ConfigKey, ValueType],
                       delay:      Option[Long],
                       resultCode: Option[Int],
                       timeToLive: Option[Long]) {
  def keys = data.keys.toSet
}
