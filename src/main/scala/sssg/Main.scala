package sssg

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.util.concurrent.Executors

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import fi.iki.elonen.SimpleWebServer
import scopt.OptionParser

import scala.collection.JavaConversions._

case class Arguments(out: String = null, server: Boolean = false)

object Main extends LazyLogging {

  def main(args: Array[String]) {
    val parser = new OptionParser[Arguments]("scala static site generator") {
      head("scala static site generator", "1.0")
      opt[String]('o', "out") valueName "<path>" action { (x, c) =>
        c.copy(out = x)
      } text "the output directory"
      arg[Unit]("server") optional() action { (_, c) =>
        c.copy(server = true)
      } text "serve static files from output"
      help("help") text "prints this usage text"
    }

    parser.parse(args, Arguments()) match {
      case Some(arguments) =>
        val sssg: SSSG = new SSSG with JadeRenderer {
          override def config: Config = ConfigFactory.load()
        }
        if (arguments.server) {
          sssg.build()
          startServer(new File(sssg.OUTPUT_PATH))
          logger.trace("Server initialized")
          val watchService: WatchService = FileSystems.getDefault.newWatchService()

          val pathStaticFiles: Path = Paths.get(sssg.STATIC_FILES_PATH)
          pathStaticFiles.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)
          logger.trace(s"Watching ${pathStaticFiles}")

          val templateFiles: Path = Paths.get(sssg.TEMPLATE_PATH)
          templateFiles.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)
          logger.trace(s"Watching ${templateFiles}")

          val pathArticles: Path = Paths.get(sssg.ARTICLES_PATH)
          pathArticles.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)
          logger.trace(s"Watching ${pathArticles}")

          val pathPages: Path = Paths.get(sssg.PAGES_PATH)
          pathPages.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE)
          logger.trace(s"Watching ${pathPages}")

          while (true) {
            logger.trace("Running watch")
            val take: WatchKey = watchService.take()
            logger.trace("Event received")
            take.pollEvents().listIterator().foreach(e => {
              logger.debug(e.kind().toString)
              val kind: WatchEvent[Path] = e.asInstanceOf[WatchEvent[Path]]
              logger.debug(s"${kind.context().toString} changed")
            })
            take.reset()
            sssg.build()
          }
        } else {
          sssg.build()
        }

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }

  val executor = Executors.newFixedThreadPool(1)

  private def startServer(root: File): Unit = {
    executor.submit(new Runnable {
      override def run(): Unit = {
        val server: SimpleWebServer = new SimpleWebServer("localhost", 8000, root, true)
        server.start()
        while (server.isAlive) {

        }
      }
    })
  }

}
