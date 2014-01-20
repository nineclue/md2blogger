package nineclue.md2blogger

import org.markdown4j.{Markdown4jProcessor, Plugin}
import io.Source._
import java.nio.file.Files

object Md2Blogger {
  def main(args:Array[String]) {
    /* if (args.isEmpty) throw new Exception("Missing file name")
    val (name, ext) = Dir.splitFileName(args(0))
    val fname = if (ext.size == 0) s"$name.md" else s"$name.$ext"
    val file = java.nio.file.FileSystems.getDefault.getPath(fname)
    if (!Files.exists(file)) throw new Exception(s"Cannot find ${file.toString}")
    val htmlContent = markdown2html(io.Source.fromFile(file.toFile).mkString)
    writeToFile(s"$name.html", htmlContent) */
    // println(Dir.summary())
    // Dir.printsummary
    // println(Dir.loadFromFile(".mdinfo"))
    BlogAPI.load
  }
}