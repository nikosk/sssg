package sssg.scanner

import java.io.File

import sssg.domain
import sssg.domain.Content

/**
  * sssg.scanner
  * User: nk
  * Date: 2016-03-14 13:42
  */
trait ContentScanner {

  def getContent(file: File): (Iterable[Content], Iterable[domain.Error])

}
