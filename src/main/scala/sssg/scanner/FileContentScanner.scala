package sssg.scanner

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import sssg.domain
import sssg.domain.{Content, Error}

import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Source}

/**
  * sssg
  * User: nk
  * Date: 2016-03-14 13:40
  */
trait FileContentScanner extends ContentScanner with LazyLogging {

  override def getContent(file: File): (Seq[Content], Seq[domain.Error]) = {
    walkTree(file).foldLeft((Seq[Content](), Seq[domain.Error]()))((l, f) => {
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

  private def walkTree(file: File): Seq[File] = {
    val children = new Iterable[File] {
      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
    }
    val files: Seq[File] = file.isDirectory match {
      case true => Nil
      case false => List(file)
    }
    files ++: children.flatMap(walkTree).toSeq
  }

  private def process(file: File): Either[domain.Error, Content] = {
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
        Right(new Content(file.getPath, strings(1), meta))
    }
  }
}
