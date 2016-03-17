package sssg.renderer

import java.io.File

/**
  * sssg.renderer
  * User: nk
  * Date: 2016-03-14 13:42
  */
trait Renderer {


  def render(template: String, file: File, context: scala.collection.mutable.Map[String, AnyRef])


}
