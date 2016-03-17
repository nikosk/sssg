package sssg

import java.util

import com.typesafe.config.Config

/**
  * sssg
  * User: nk
  * Date: 2016-03-14 13:45
  */
trait Configuration {

  import scala.collection.JavaConversions._

  import scala.collection.JavaConverters._

  def config: Config

  lazy val TEMPLATE_PATH = s"${config.getString(ConfigKeys.themePath)}/${config.getString(ConfigKeys.theme)}/templates/"

  def sharedVariables: Map[String, AnyRef] = {
    val map: util.HashMap[String, AnyRef] = new java.util.HashMap[String, AnyRef]();
    config.getConfig("sssg").entrySet().foreach(e => map.put(e.getKey, e.getValue.unwrapped()))
    map.asScala.toMap
  }
}
