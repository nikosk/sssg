package sssg.scanner

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import sssg.domain
import sssg.domain.Content

class FileContentScannerSpec extends FlatSpec with Matchers {


  "FileContentScanner" should "scan articles correctly" in {
    val fcs: Object with FileContentScanner = new Object with FileContentScanner
    val contentAndErrors: (Iterable[Content], Iterable[domain.Error]) = fcs.getContent(new File("src/test/resources/content/articles"))
    val content: Seq[Content] = contentAndErrors._1.toSeq.sortBy(x => x.meta.getOrDefault("title", ""))
    val errors: Iterable[domain.Error] = contentAndErrors._2
    content.size shouldBe 4
    errors.size shouldBe 0
    content.map(c => c.meta.getOrDefault("title", "")) should contain inOrder(
      "another article",
      "article in subdirectory",
      "some article",
      "test article")
    content.map(c => c.meta).foreach(m => {
      m should contain key "title"
    })
    content.map(c => c.originalPath).sorted should contain inOrder(
      "src/test/resources/content/articles/another-article.md",
      "src/test/resources/content/articles/some-article.md",
      "src/test/resources/content/articles/sub/article-in-subdirectory.md",
      "src/test/resources/content/articles/testarticle.md"
      )
  }


  "FileContentScanner" should "scan pages correctly" in {
    val fcs: Object with FileContentScanner = new Object with FileContentScanner
    val contentAndErrors: (Iterable[Content], Iterable[domain.Error]) = fcs.getContent(new File("src/test/resources/content/pages"))
    val content: Seq[Content] = contentAndErrors._1.toSeq.sortBy(x => x.meta.getOrDefault("title", ""))
    val errors: Iterable[domain.Error] = contentAndErrors._2
    content.size shouldBe 2
    errors.size shouldBe 0
    content.map(c => c.meta.getOrDefault("title", "")) should contain inOrder(
      "Curriculum vitae", "Other page"
      )
    content.map(c => c.meta).foreach(m => {
      m should contain key "title"
    })
    content.map(c => c.originalPath).sorted should contain inOrder(
      "src/test/resources/content/pages/sub/otherpage.md",
      "src/test/resources/content/pages/test.md"
      )
  }

}
