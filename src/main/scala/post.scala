package nineclue.md2blogger

import org.markdown4j.{Markdown4jProcessor, Plugin}
import play.api.libs.json.Json.toJson

case class Stored(id:String, url:String, selfLink:String)

case class Post(title:String, content:String, added:Option[Stored]=None) {
  def json(blogId:String):String = {
    val blogJson = toJson(Map("id" -> blogId))
    var postMap = Map("kind" -> "blogger#post", "title" -> title, "content" -> Post.md2html(content))
    if (added.nonEmpty) {
      val Stored(id, url, selfLink) = added.get
      postMap = postMap + (("id", id), ("url", url), ("selfLink", selfLink))
    }
    toJson(postMap.mapValues(toJson(_)) + ("blog" -> blogJson)).toString
  }
}

object Post {
  private val mdcode =
    new util.matching.Regex("""(?ms)^```\s*(\w{2,})\s*$(.+?)^```""", "lang", "code")

  def md2html(md:String):String = {
    val converted = mdcode.replaceAllIn(md,
      m => """%%%code lang=$1$2%%%"""
    )
    new Markdown4jProcessor().registerPlugins(new CodeHighlight()).process(converted)
  }

  def toPost(md:MDInfo):Post = {
    val lines = io.Source.fromFile(md.path).getLines.toList
    val title = lines.head
    val content = md2html(lines.tail.mkString("\n"))
    Post(title, content, md.exists)
  }
}