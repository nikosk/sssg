package sssg

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.{FlatSpec, Matchers}
import sssg.ConfigKeys._
import sssg.domain.{Category, Content, Error, Site}
import sssg.renderer.Renderer

import scala.collection.JavaConversions._
import scala.collection.immutable.Nil
import scala.collection.mutable

/**
  *
  * User: nk
  * Date: 2016-03-14 14:13
  */
class SSSGSpec extends FlatSpec with Matchers {

  type Rendering = (String, File, mutable.Map[String, AnyRef])

  trait MockRenderer extends Renderer {

    var rendered: Seq[Rendering] = Seq()

    override def render(template: String, file: File, context: mutable.Map[String, AnyRef]): Unit = {
      println(s"Rendering $template to file ${file.getAbsolutePath}")
      rendered = rendered :+(template, file, context)
    }
  }


  "SSSG" should "should load configuration correctly" in {
    val sssg: SSSG = SSSG()
    sssg.config.getString(contentPath) shouldBe "content"
    sssg.config.getString(themePath) shouldBe "themes"
    sssg.config.getString(pagesPath) shouldBe "pages"
  }

  "SSSG" should "render pages correctly" in {

    val sssg = new SSSG with MockRenderer {

      override def config: Config = ConfigFactory.load()
        .withValue(ConfigKeys.outputPath, ConfigValueFactory.fromAnyRef("target/tmp/"))
        .withValue(ConfigKeys.themePath, ConfigValueFactory.fromAnyRef("src/test/resources/themes"))

      override def getContent(file: File): (Iterable[Content], Iterable[Error]) = {
        if (file.getAbsolutePath.endsWith("pages")) (Seq(new Content("index.md", "#test", mutable.Map("title" -> "title"))), Nil)
        else (Nil, Nil)
      }
    }
    sssg.build()
    sssg.rendered.foreach(r => {
      r._1 shouldBe "page"
      r._2.getPath shouldBe "target/tmp/index.html"
      val meta: mutable.Map[String, AnyRef] = r._3
      meta.keys should contain("title")
      meta.values should contain("title")
      meta.keys should contain("content")
      meta.values should contain("<h1>test</h1>")
      val site: Option[Site] = meta.get("site").asInstanceOf[Option[Site]]

      site.get.meta.keys should contain("BASE_URL")
      site.get.meta.keys should contain("AUTHOR")
      site.get.meta.keys should contain ("LANG")
    })
  }

  "SSSG" should "render articles correctly" in {

    val sssg = new SSSG with MockRenderer {

      override def config: Config = ConfigFactory.load()
        .withValue(ConfigKeys.outputPath, ConfigValueFactory.fromAnyRef("target/tmp/"))
        .withValue(ConfigKeys.themePath, ConfigValueFactory.fromAnyRef("src/test/resources/themes"))

      override def getContent(file: File): (Iterable[Content], Iterable[Error]) = {
        if (file.getAbsolutePath.endsWith("articles")) (Seq(new Content("blog/blogpost.md", "#test", Map("title" -> "the title", "category" -> "category name"))), Nil)
        else (Nil, Nil)
      }
    }
    sssg.build()
    val r: (String, File, mutable.Map[String, AnyRef]) = sssg.rendered.head
    r._1 shouldBe "category"
    r._2.getPath shouldBe "target/tmp/category-name/index.html"

    val meta: mutable.Map[String, AnyRef] = r._3
    meta.keys should contain("category")
    meta.keys should contain("page")
    val site: Option[Site] = meta.get("site").asInstanceOf[Option[Site]]

    site.get.meta.keys should contain("BASE_URL")
    site.get.meta.keys should contain("AUTHOR")
    site.get.meta.keys should contain("LANG")

    meta.get("category").get.asInstanceOf[Category].name shouldBe "category name"
    val r2: Rendering = sssg.rendered.tail.head
    r2._1 shouldBe "article"
    r2._2.getPath shouldBe "target/tmp/category-name/blogpost.html"

    val meta2: mutable.Map[String, AnyRef] = r2._3
    meta2.keys should contain("category")
    meta2.keys should contain("article")
    val site2: Option[Site] = meta2.get("site").asInstanceOf[Option[Site]]

    site2.get.meta.keys should contain("BASE_URL")
    site2.get.meta.keys should contain("AUTHOR")
    site2.get.meta.keys should contain("LANG")
  }

}
