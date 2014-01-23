package nineclue.md2blogger

import org.markdown4j.{Markdown4jProcessor, Plugin}
import io.Source._
import java.nio.file.Files

object Md2Blogger {
  def main(args:Array[String]) {
    Dir.loadOrInit
    val sArgs = args.map(_.toLowerCase)
    if (args.size > 1 && (args(0) == "rm" || args(0) == "del")) {
      val deletions = args.tail.filter(Dir.contains(_))
      println("unmanaging files : " + deletions.mkString(", "))
      Dir.removeFiles(deletions)
    } else {
      BlogAPI.loadOrInit
      val updates = Dir.updates
      Dir.printUpdates(updates)
      if (updates.size > 0) {
        var answer:String = ""
        do {
          print("Proceed to updates blogger (Y/n) : ")
          answer = System.console.readLine.stripLineEnd.toLowerCase
        } while (answer != "" && answer != "y" && answer != "n")
        if (answer == "" || answer == "y")
          BlogAPI.updatePosts(updates)
      }
    }
  }
}