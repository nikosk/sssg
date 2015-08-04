package sssg

import scopt.OptionParser

object Main {

  def main(args: Array[String]) {
    val parser = new OptionParser[Arguments]("scopt") {
      head("scala static site generator", "0.0.1")
      opt[String]('o', "out") valueName "<path>" action { (x, c) =>
        c.copy(out = x)
      } text "the output directory"
    }
    parser.parse(args, Arguments()) match {
      case Some(config) =>
        val sssg = SSSG()
        sssg.build()

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }
}
