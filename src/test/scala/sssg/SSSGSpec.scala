package sssg

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.{FlatSpec, Matchers}
import sssg.ConfigKeys._

class SSSGSpec extends FlatSpec with Matchers {

  type Rendering = (String, File, Map[String, AnyRef])

  trait MockRenderer extends Renderer {

    var rendered: Seq[Rendering] = Seq()

    override def render(template: String, file: File, context: Map[String, AnyRef]): Unit = {
      println(s"Rendering $template to file ${file.getAbsolutePath}")
      rendered = rendered :+(template, file, context)
    }
  }

  def init = new SSSG with MockRenderer {
    override def config: Config = ConfigFactory.load().withValue(ConfigKeys.outputPath, ConfigValueFactory.fromAnyRef("../../../target/tmp/"))
  }

  it should "should load configuration correctly" in {
    println(new File(".").getAbsolutePath)
    val sssg: SSSG = init
    sssg.config.getString(contentPath) shouldBe "content"
    sssg.config.getString(themePath) shouldBe "themes"
    sssg.config.getString(pagesPath) shouldBe "testpages"
  }

  it should "parse pages correctly" in {
    val sssg: SSSG with MockRenderer = init
    sssg.build()
    val res: Seq[Rendering] = sssg.rendered.filter((rendering) => rendering._1.endsWith("page.jade"))
    res.size shouldBe 1
    res.head._3("title") shouldBe "Curriculum vitae"
  }

  it should "parse articles correctly" in {
    val sssg: SSSG with MockRenderer = init
    sssg.build()
    val res: Seq[Rendering] = sssg.rendered.filter((rendering) => rendering._1.endsWith("article.jade"))
    res.size shouldBe 1
    res.head._3.get("article").get.asInstanceOf[Content].meta.get("title").get shouldBe "test article"
  }

  it should "parse categories correctly" in {
    val sssg: SSSG with MockRenderer = init
    sssg.build()
    val res: Seq[Rendering] = sssg.rendered.filter((rendering) => rendering._1.endsWith("category.jade"))
    res.size shouldBe 1
    res.head._3.get("category").get.asInstanceOf[Category].name shouldBe "test"
  }


}
