package sssg

import java.io.File
import java.nio.file.Paths
import java.util

import com.github.slugify.Slugify
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.{FileUtils, FilenameUtils}
import sssg.ConfigKeys._
import sssg.domain.{Category, Content, Page, Site}
import sssg.renderer.{JadeRenderer, Renderer}
import sssg.scanner.{ContentScanner, FileContentScanner}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

abstract class SSSG() extends LazyLogging with Configuration with Renderer with ContentScanner {

  lazy val PAGE_TEMPLATE_PATH = "page"
  lazy val ARTICLE_TEMPLATE_PATH = "article"
  lazy val CATEGORY_TEMPLATE_PATH = "category"

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

    val pages: (Iterable[Content], Iterable[domain.Error]) = parsePages()
    val categories: (Iterable[Category], Iterable[domain.Error]) = parseArticles()
    (pages._2 ++ categories._2).foreach(error => logger.error(error.msg))


    val site: Site = new Site(sharedVariables, pages._1.asJava, categories._1.asJava)
    renderPages(outputDir, site)
    writeCategories(outputDir, site)
  }

  private def writeCategories(outputDir: File, site: Site) = {
    val slg = new Slugify(false)
    site.categories.foreach(c => {
      val categoryDir = new File(outputDir, slg.slugify(c.name))
      if (!categoryDir.exists()) {
        categoryDir.mkdirs()
      }

      val pager = Pager(ARTICLES_PER_PAGE, c.articles)
      pager.pages.foreach(i => {
        val pageFile = new File(categoryDir, if (i == 0) "index.html" else i + ".html")
        val model: mutable.Map[String, AnyRef] = mutable.Map("page" -> pager.get(i)) +
          (("category", c)) +
          (("pager", pager)) + (("site", site))
        render(CATEGORY_TEMPLATE_PATH, pageFile, model)
        logger.debug(s"Processed ${pageFile.getCanonicalPath}")
      })

      c.articles.foreach(a => {
        val articleFile = new File(categoryDir, a.path)
        val model: mutable.Map[String, AnyRef] = mutable.Map(("category", c)) + (("article", a)) + (("site", site))
        render(ARTICLE_TEMPLATE_PATH, articleFile, model)
      })
    })
  }

  private def renderPages(outputDir: File, site: Site): Unit = {
    site.pages.foreach(page => {
      val path: String = page.path
      val directory: File = new File(outputDir, FilenameUtils.getPath(path))
      if (!directory.exists()) {
        directory.mkdirs()
      }
      val pageFile = new File(directory, FilenameUtils.getName(path))

      val model: mutable.Map[String, Object] = page.meta + (("content", page.html())) + (("site", site))
      render(PAGE_TEMPLATE_PATH, pageFile, model)
      logger.debug(s"Processed ${pageFile.getCanonicalPath}")
    })
  }

  private def parsePages(): (Iterable[Content], Iterable[domain.Error]) = {
    val file: File = new File(PAGES_PATH)
    getContent(file)
  }

  private def parseArticles(): (Iterable[Category], Iterable[domain.Error]) = {
    val file: File = new File(ARTICLES_PATH)
    val categories: mutable.Map[String, Category] = mutable.Map()
    val result: (Iterable[Content], Iterable[domain.Error]) = getContent(file)
    result._1.foreach(a => {
      val name = a.meta.getOrElse("category", "uncategorized")
      categories.update(name, categories.get(name) match {
        case None =>
          val contents: util.ArrayList[Content] = new util.ArrayList[Content]()
          contents.add(a)
          new Category(name, name, contents)
        case Some(c) => c.articles.add(a); c
      })
    })
    (categories.values, result._2)
  }

}

object SSSG {

  def apply(): SSSG = {
    new SSSG() with JadeRenderer with FileContentScanner {
      override def config: Config = ConfigFactory.load()
    }
  }

}

case class Pager(perPage: Int, articles: Seq[Content]) {

  val pages = 0 to articles.size by perPage

  val totalPages = pages.size

  def get(index: Int): Page = {
    val from: Int = index * perPage
    new Page(index, articles.slice(from, from + perPage), index + 1 < totalPages, index - 1 > 0)
  }
}
