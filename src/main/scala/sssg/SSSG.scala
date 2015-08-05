package sssg

import java.io.{FileWriter, File}
import java.nio.file._
import java.util

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.JadeTemplate
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.pegdown.PegDownProcessor
import sssg.ConfigKeys._
import scala.collection.JavaConversions._

import scala.io.{BufferedSource, Source}

case class Content(markdown: String, meta: Map[String, String]) {
  def html(): String = {
    new PegDownProcessor().markdownToHtml(markdown)
    Some
  }
}


case class Error(msg: String)

case class SSSG(config: Config) extends LazyLogging {

  val TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/"
  val PAGE_TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/page.jade"
  val STATIC_FILES_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/static/"
  val OUTPUT_PATH = s"${config.getString(outputPath)}"
  val PAGES_PATH: String = s"${config.getString(contentPath)}/${config.getString(pagesPath)}"
  val ARTICLES_PATH: String = s"${config.getString(contentPath)}/${config.getString(articlesPath)}"

  def build(): Unit = {
    logger.debug(s"Building with config: ${config.getConfig("sssg")}")
    val pages = parsePages()._1
    val jadeConfig: JadeConfiguration = new JadeConfiguration()
    jadeConfig.setSharedVariables(getSharedVariables)
    jadeConfig.setPrettyPrint(true)
    val template: JadeTemplate = jadeConfig.getTemplate(PAGE_TEMPLATE_PATH)

    val output: File = Paths.get(OUTPUT_PATH).toFile
    if (!output.exists()) {
      output.mkdirs()
    }
    val s: File = new File(STATIC_FILES_PATH)
    FileUtils.copyDirectoryToDirectory(s, output)

    pages.foreach(p => {
      val file: String = p.meta.getOrElse("save_as", "index.html")
      val directory: File = new File(output, FilenameUtils.getPath(file))
      if (!directory.exists()) {
        directory.mkdirs()
      }
      val pageFile = new File(directory, FilenameUtils.getName(file))
      val writer: FileWriter = new FileWriter(pageFile)
      val model: util.Map[String, AnyRef] = p.meta + (("content", p.html())) + (("pages", pages.toArray))
      jadeConfig.renderTemplate(template, model, writer)
      logger.debug(s"Processed ${pageFile.getCanonicalPath}")
      writer.flush()
      writer.close()
    })
  }

  def parsePages(): (Iterable[Content], Iterable[Error]) = {
    val file: File = new File(PAGES_PATH)
    getContent(file)
  }

  def parseArticles(): (Iterable[Content], Iterable[Error]) = {
    val file: File = new File(ARTICLES_PATH)
    getContent(file)
  }

  private def getSharedVariables: util.Map[String, AnyRef] = {
    import scala.collection.JavaConversions._
    val map: util.HashMap[String, AnyRef] = new java.util.HashMap[String, AnyRef]();
    config.getConfig("sssg").entrySet().foreach(e => map.put(e.getKey, e.getValue.unwrapped()))
    map.put("config", config)
    map
  }

  private def getContent(file: File): (Iterable[Content], Iterable[Error]) = {
    walkTree(file).foldLeft((Seq[Content](), Seq[Error]()))((l, f) => {
      process(f) match {
        case Right(x) => {
          (l._1 :+ x, l._2)
        }
        case Left(x) => {
          logger.error(x.msg)
          (l._1, l._2 :+ x)
        }
      }
    })
  }


  private def walkTree(file: File): Iterable[File] = {
    val children = new Iterable[File] {
      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
    }
    val files: Seq[File] = file.isDirectory match {
      case true => Nil
      case false => Seq(file)
    }
    files ++: children.flatMap(walkTree)
  }

  private def process(file: File): Either[Error, Content] = {
    logger.debug(s"Found content: ${file.getAbsolutePath}")
    val source: BufferedSource = Source.fromFile(file)
    val strings: Array[String] = source.mkString.split("-{5,}")
    strings.size match {
      case 1 => Left(Error(s"No metadata found for file ${file.getAbsolutePath}"))
      case 2 =>
        val meta = strings(0).split("\n").foldLeft(Map[String, String]())((m, l) => {
          if (l.contains(":")) {
            val s: Array[String] = l.split(":")
            m + ((s(0).trim, s(1).trim))
          } else {
            m
          }
        })
        logger.debug(s"Parsed content with meta: ${meta.mkString}")
        Right(Content(strings(1), meta))
    }
  }

}


object SSSG {

  def apply(): SSSG = {
    val config: Config = ConfigFactory.load()
    SSSG(config)
  }

}
