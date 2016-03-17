package sssg.renderer

import java.io.{File, FileWriter}

import com.typesafe.scalalogging.LazyLogging
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.JadeTemplate
import sssg.Configuration

import scala.collection.JavaConversions._

/**
  * sssg.renderer
  * User: nk
  * Date: 2016-03-14 13:42
  */
trait JadeRenderer extends Renderer with LazyLogging {
  this: Configuration =>

  val jadeConfig: JadeConfiguration = new JadeConfiguration()
  jadeConfig.setPrettyPrint(true)

  override def render(template: String, file: File, context: scala.collection.mutable.Map[String, AnyRef]): Unit = {
    val writer: FileWriter = new FileWriter(file)
    val jadeTemplate: JadeTemplate = jadeConfig.getTemplate(TEMPLATE_PATH + template + ".jade")
    jadeConfig.renderTemplate(jadeTemplate, context, writer)
    logger.debug(s"Rendered ${file.getCanonicalPath}")
    writer.flush()
    writer.close()
  }
}
