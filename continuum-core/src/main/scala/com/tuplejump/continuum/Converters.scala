package com.tuplejump.continuum

import java.util.Properties

import scala.collection.JavaConverters._

trait JConverters {

  def toProperties(config: Map[String,String]): Properties = {
    val props = new Properties()
    props.putAll(config.asJava)
    props
  }

  def fromProperties(props: Properties): Map[String,String] = {
    val x: Map[String,String] = props.asScala.toMap.map {
      case (k,v) => k.asInstanceOf[String] -> v.asInstanceOf[String]
    }

    x
  }

   /* props.entrySet.asScala.toMap.map {
      case (k,v) => k.toString -> v.toString
    }
*/
}
