package sssg

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import sssg.ConfigKeys._

class SSSGSpec extends FlatSpec with Matchers{

  it should "should load configuration correctly" in {
    println(new File(".").getAbsolutePath)
    val sssg: SSSG = SSSG()
    sssg.config.getString(contentPath) shouldBe "content"
    sssg.config.getString(themePath) shouldBe "themes"
    sssg.config.getString(pagesPath) shouldBe "testpages"
  }

  it should "build pages correctly" in {
    val sssg: SSSG = SSSG()
    val pages = sssg.parsePages()
    pages._1.size shouldBe 1
    pages._2.size shouldBe 0
    val page: Content = pages._1.head
    page.meta("title") shouldBe "Curriculum vitae"
  }

  it should "build articles correctly" in {
    val sssg: SSSG = SSSG()
    val pages = sssg.parseArticles()
    pages._1.size shouldBe 1
    pages._2.size shouldBe 0
    val page: Content = pages._1.head
    page.meta("title") shouldBe "test article"
  }

  it should "render content correctly" in {
    val sssg: SSSG = SSSG()
    val pages = sssg.parseArticles()
    pages._1.size shouldBe 1
    pages._2.size shouldBe 0
    val page: Content = pages._1.head
    sssg.build()
  }
}
