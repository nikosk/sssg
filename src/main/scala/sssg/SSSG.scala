package sssg

import java.io.{File, FileWriter}
import java.nio.file.Paths
import java.util

import com.github.slugify.Slugify
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.JadeTemplate
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.pegdown.PegDownProcessor
import sssg.ConfigKeys._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.{BufferedSource, Source}

trait Renderer {

  def config: Config

  def render(template: String, file: File, context: Map[String, AnyRef])

  def getSharedVariables: util.Map[String, AnyRef] = {
    val map: util.HashMap[String, AnyRef] = new java.util.HashMap[String, AnyRef]()
    config.getConfig("sssg").entrySet().foreach(e => map.put(e.getKey, e.getValue.unwrapped()))
    map.put("config", config)
    map
  }
}

trait JadeRenderer extends Renderer with LazyLogging {

  val jadeConfig: JadeConfiguration = new JadeConfiguration()
  jadeConfig.setSharedVariables(getSharedVariables)
  jadeConfig.setPrettyPrint(true)

  override def render(template: String, file: File, context: Map[String, AnyRef]): Unit = {
    val writer: FileWriter = new FileWriter(file)
    val jadeTemplate: JadeTemplate = jadeConfig.getTemplate(template)
    jadeConfig.renderTemplate(jadeTemplate, context, writer)
    logger.debug(s"Rendered ${file.getCanonicalPath}")
    writer.flush()
    writer.close()
  }
}


abstract class SSSG() extends LazyLogging with Renderer {

  lazy val TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/"
  lazy val PAGE_TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/page.jade"
  lazy val ARTICLE_TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/article.jade"
  lazy val CATEGORY_TEMPLATE_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/templates/category.jade"

  lazy val STATIC_FILES_PATH = s"${config.getString(themePath)}/${config.getString(theme)}/static/"
  lazy val OUTPUT_PATH = s"${config.getString(outputPath)}"
  lazy val PAGES_PATH: String = s"${config.getString(contentPath)}/${config.getString(pagesPath)}"
  lazy val ARTICLES_PATH: String = s"${config.getString(contentPath)}/${config.getString(articlesPath)}"
  lazy val ARTICLES_PER_PAGE: Int = config.getInt(articlesPerPage)


  def build(): Unit = {
    val outputDir: File = Paths.get(OUTPUT_PATH).toFile
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }
    val staticFileDir: File = new File(STATIC_FILES_PATH)
    FileUtils.copyDirectoryToDirectory(staticFileDir, outputDir)

    val pages: (Iterable[Content], Iterable[Error]) = parsePages()
    val categories: (Iterable[Category], Iterable[Error]) = parseArticles()
    (pages._2 ++ categories._2).foreach(error => logger.error(error.msg))
    renderPages(outputDir, pages._1)
    writeCategories(outputDir, categories._1)
  }

  private def writeCategories(outputDir: File, categories: Iterable[Category]) = {
    val slg = new Slugify(false)
    categories.foreach(c => {
      val categoryDir = new File(outputDir, slg.slugify(c.name))
      if (!categoryDir.exists()) {
        categoryDir.mkdirs()
      }

      val pager = Pager(ARTICLES_PER_PAGE, c.articles)
      pager.pages.foreach(i => {
        val pageFile = new File(categoryDir, if (i == 1) "index.html" else i + ".html")
        val model: Map[String, AnyRef] = Map("page" -> pager.get(i)) +
          (("category", c)) +
          (("excerpts", pager.get(i).map(c => c.html()))) +
          (("pager", pager))
        render(CATEGORY_TEMPLATE_PATH, pageFile, model)
        logger.debug(s"Processed ${pageFile.getCanonicalPath}")
      })

      c.articles.foreach(a => {
        val articleFile = new File(categoryDir, a.filePath())
        val model: Map[String, AnyRef] = Map(("category", c)) + (("article", a))
        render(ARTICLE_TEMPLATE_PATH, articleFile, model)
      })
    })
  }

  private def renderPages(outputDir: File, pages: Iterable[Content]): Unit = {
    pages.foreach(page => {
      val path: String = page.filePath()
      val directory: File = new File(outputDir, FilenameUtils.getPath(path))
      if (!directory.exists()) {
        directory.mkdirs()
      }
      val pageFile = new File(directory, FilenameUtils.getName(path))
      val model: Map[String, AnyRef] = page.meta + (("content", page.html())) + (("pages", pages.toArray))
      render(PAGE_TEMPLATE_PATH, pageFile, model)
      logger.debug(s"Processed ${pageFile.getCanonicalPath}")
    })
  }

  private def parsePages(): (Iterable[Content], Iterable[Error]) = {
    val file: File = new File(PAGES_PATH)
    getContent(file)
  }

  private def parseArticles(): (Iterable[Category], Iterable[Error]) = {
    val file: File = new File(ARTICLES_PATH)
    val categories: mutable.Map[String, Category] = mutable.Map()
    val result: (Iterable[Content], Iterable[Error]) = getContent(file)
    result._1.foreach(a => {
      val name = a.meta.getOrElse("category", "uncategorized")
      categories.update(name, categories.get(name) match {
        case None => Category(name, name, Seq(a))
        case Some(c) => c.copy(articles = c.articles :+ a)
      })
    })
    (categories.values, result._2)
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
    strings.length match {
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
        logger.debug(s"Parsed content with meta: ${meta.mkString(", ")}")
        Right(Content(file.getPath, strings(1), meta))
    }
  }

}


object SSSG {

  def apply(): SSSG = {
    new SSSG() with JadeRenderer {
      override def config: Config = ConfigFactory.load()
    }
  }

}

case class Pager(perPage: Int, articles: Seq[Content]) {

  val pages = 1 to articles.size by perPage

  val totalPages = pages.size

  def get(index: Int): Seq[Content] = {
    val from: Int = index * perPage
    articles.slice(from, from + perPage)
  }

}

case class Content(originalPath: String, markdown: String, meta: Map[String, String]) {

  def html(): String = {
    new PegDownProcessor().markdownToHtml(markdown)
  }

  def filePath(): String = {
    meta.getOrElse("save_as", s"${FilenameUtils.getBaseName(originalPath)}.html")
  }

}

case class Category(name: String, path: String, articles: Seq[Content])

case class Error(msg: String)


