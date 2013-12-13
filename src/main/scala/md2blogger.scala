import org.markdown4j.{Markdown4jProcessor, Plugin}
import io.Source._
import java.nio.file.{Files, Paths}
import prettify.PrettifyParser
import syntaxhighlight.ParseResult

object Md2Blogger {
  private val mdcode =
    new util.matching.Regex("""(?ms)^```\s*(\w{2,})\s*$(.+?)^```""", "lang", "code")
  def markdown2html(md:String):String = {
    val converted = mdcode.replaceAllIn(md,
      m => """%%%code lang=$1$2%%%"""
    )
    new Markdown4jProcessor().registerPlugins(new CodeHighlight()).process(converted)
  }

  def splitFileName(name:String):(String, String) = {
    val dot = name.lastIndexOf('.')
    if (dot < 0) (name, "")
    else (name.take(dot-1), name.drop(dot))
  }

  def writeToFile(fn:String, content:String):Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(fn))
    try pw.write(content) finally pw.close
  }

  def main(args:Array[String]) {
    if (args.isEmpty) throw new Exception("Missing file name")
    val (name, ext) = splitFileName(args(0))
    val fname = if (ext.size == 0) s"$name.md" else s"$name.$ext"
    val file = java.nio.file.FileSystems.getDefault.getPath(fname)
    if (!Files.exists(file)) throw new Exception(s"Cannot find ${file.toString}")
    val htmlContent = markdown2html(io.Source.fromFile(file.toFile).mkString)
    writeToFile(s"$name.html", htmlContent)
  }
}