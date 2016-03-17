package sssg.renderer

import java.io.{File, FileWriter}
import java.util.Locale

import com.typesafe.scalalogging.LazyLogging
import nz.net.ultraq.thymeleaf.LayoutDialect
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.FileTemplateResolver
import sssg.Configuration

/**
  * sssg.renderer
  * User: nk
  * Date: 2016-03-17 00:54
  */
trait ThymeleafRenderer extends Renderer with LazyLogging {

  this: Configuration =>

  lazy val engine: TemplateEngine = new TemplateEngine

  private val templateResolver: FileTemplateResolver = new FileTemplateResolver()

  templateResolver.setTemplateMode("HTML5")
  templateResolver.setSuffix(".html")
  templateResolver.setPrefix(TEMPLATE_PATH)
  templateResolver.setCacheable(false)

  engine.setTemplateResolver(templateResolver)
  engine.addDialect(new LayoutDialect())

  override def render(template: String, file: File, context: scala.collection.mutable.Map[String, AnyRef]): Unit = {
    import scala.collection.JavaConversions._
    if(file.exists()) {
      file.delete()
    }
    val writer: FileWriter = new FileWriter(file)
    val c: Context = new Context(Locale.getDefault, context)
    engine.process(template, c, writer)
    logger.debug(s"Rendered ${file.getCanonicalPath}")
    writer.flush()
    writer.close()
  }

}
