package sssg

import java.io.{FileWriter, File}
import java.nio.file._
import java.util

import com.typesafe.config.{Config, ConfigFactory}
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.JadeTemplate
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.pegdown.PegDownProcessor
import scopt.OptionParser
import sssg.ConfigKeys._
import scala.collection.JavaConversions._

import scala.io.{BufferedSource, Source}

case class Content(markdown: String, meta: Map[String, String]) {
  def html(): String = {
    new PegDownProcessor().markdownToHtml(markdown)
  }
}


case class Error(msg: String)

case class SSSG(config: Config) {

  val PAGE_TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/page.jade"
  val STATIC_FILES_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/static/"
  val OUTPUT_PATH = s"${config.getString(outputPath)}"

  def build(): Unit = {
    // get content for pages
    // get template
    // copy static files
    // render html to file
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
      if(!directory.exists()){
        directory.mkdirs()
      }
      val pageFile = new File(directory, FilenameUtils.getName(file))
      val writer: FileWriter = new FileWriter(pageFile)
      jadeConfig.renderTemplate(template, p.meta,  writer)
      println(s"Processed ${pageFile}")
      writer.flush()
      writer.close()
    })
  }

  def parsePages(): (Iterable[Content], Iterable[Error]) = {
    val file: File = new File(s"${config.getString(contentPath)}/${config.getString(pagesPath)}")
    getContent(file)
  }

  def parseArticles(): (Iterable[Content], Iterable[Error]) = {
    val file: File = new File(s"${config.getString(contentPath)}/${config.getString(articlesPath)}")
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
        case Right(x) => (l._1 :+ x, l._2)
        case Left(x) => (l._1, l._2 :+ x)
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
        Right(Content(strings(1), meta))
    }
  }

}

case class Arguments(out: String = null)

object SSSG {

  def apply(): SSSG = {
    val config: Config = ConfigFactory.load()
    SSSG(config)
  }

}
