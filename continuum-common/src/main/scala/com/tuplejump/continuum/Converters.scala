/*
 * Copyright 2016 Tuplejump
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

package com.tuplejump.continuum

/** TODO handle in serializer/des. */
trait StringConverter {

  def to(s: String): Array[Byte] =
    new String(s).getBytes("UTF-8")

  def from(bytes: Array[Byte]): String =
    new String(bytes)
}

trait JPropertiesConverter {
  import java.util.Properties

  import scala.collection.JavaConverters._

  def to(config: Map[String,String]): Properties = {
    val props = new Properties()
    props.putAll(config.asJava)
    props
  }

  def from(props: Properties): Map[String,String] =
    props.asScala.toMap[String,String]

}
