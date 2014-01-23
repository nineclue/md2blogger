package nineclue.md2blogger

import java.nio.file.{Path, Paths, Files, LinkOption}
import java.util.Date

sealed abstract trait Differ
case class Added(md:MDInfo) extends Differ
case class Updated(md:MDInfo) extends Differ

case class MDInfo(name:String, path:String, size:Long, mDate:Date, cs:Long, exists:Option[Stored]) {
  def removeStored = MDInfo(name, path, size, mDate, cs, None)
}

object Dir {
  import collection.JavaConversions.{asScalaIterator, asScalaBuffer}

  type Ignore = Path => Boolean
  type ScanInfo = collection.mutable.Map[String, (String, Long, Date)]
  type DirInfo = collection.mutable.Map[String, MDInfo]

  private val fileName = ".mdInfo"
  private val dirInfo = collection.mutable.Map.empty[String, MDInfo]
  def contains(fn:String) = dirInfo.contains(fn)

  // must be file, hidden or starts with '.' || '_' or extension is not ...
  implicit def defaultIgnore:Ignore =
    { f => {
        val fName = f.getFileName.toString
        val (name, ext) = splitFileName(fName)
        !Files.isDirectory(f) &&
        (Files.isHidden(f) || fName(0) == '.' || fName(0) == '_' ||
          (ext != "md" && ext != "scala" && ext != "java"))
      }
    }

  // 프로그램 실행되는 현재 디렉토리를 반환
  def currentDir = Paths.get("").toAbsolutePath

  // String 파일 이름을 확장자와 분리하여 Tuple 반환
  def splitFileName(name:String):(String, String) = {
    val dot = name.lastIndexOf('.')
    if (dot < 0) (name, "")
    else (name.take(dot), name.drop(dot+1).toLowerCase)
  }

  // 처음 실행되는 Path p에 대해 각각의 file에서 code를 실행한다
  def doDirJob(p:Path)(code: => Path=>Unit)(implicit ignore:Ignore):Unit = {
    val stream = Files.newDirectoryStream(p)
    stream.iterator.filter(!ignore(_)).foreach(f =>
      if (Files.isDirectory(f)) doDirJob(f)(code)(ignore)
      else code(f) )
    stream.close
  }

  // Path p에 대해 하부 디렉토리를 포함한 파일 정보를 반환
  // 상부와 하부 디렉토리에 위치한 파일간의 차이는 없음 (즉, 디렉토리는 분류의 편리함을 위한 것일뿐)
  // 같은 이름의 파일이 존재하면 에러 발생
  def fileMap(m:ScanInfo)(p:Path):ScanInfo = {
    val namePath = p.getFileName
    val name = namePath.toString
    val size = Files.size(p)
    val mTime = new Date(Files.getLastModifiedTime(p, LinkOption.NOFOLLOW_LINKS).toMillis)
    if (m.contains(name))
      throw new Exception(s"Duplicate file name : $name")
    else m += ((name, (p.toString, size, mTime)))
  }

  // Path p의 내용에 대한 checksum 계산
  def pseudoCS(p:String):Long = {
    val lines = Files.readAllLines(Paths.get(p), java.nio.charset.Charset.forName("UTF-8"))
    lines.foldLeft(0L)((cs, l) => cs + l.hashCode.toLong)
  }

  def apply(code: => Path=>Unit) = doDirJob(currentDir)(code)

  // 현재 디렉토리이하의 자료 Map을 반환
  def summaryMap:ScanInfo = {
    val s = collection.mutable.Map.empty[String, (String, Long, Date)]
    doDirJob(currentDir)(fileMap(s))
    s
  }

  // 자료 Map을 지정된 이름으로 저장
  def save = {
    import scala.pickling._
    import json._

    val file = Files.newBufferedWriter(Paths.get(fileName), java.nio.charset.Charset.forName("UTF-8"))
    try
      file.write(dirInfo.pickle.value)
    finally
      file.close
  }

  // 지정된 이름을 불러옴
  def loadOrInit = {
    import scala.pickling._
    import json._

    val f = Paths.get(fileName)
    if (Files.exists(f)) {
      val jsonVal = new String(Files.readAllBytes(f))
      dirInfo ++= jsonVal.unpickle[DirInfo]
    } else {
      println("Directory information not exists, making a new one.")
      summaryMap.foreach {
        case (name, (pathString, size, mTime)) =>
          dirInfo += ((name, MDInfo(name, pathString, size, mTime, pseudoCS(pathString), None)))
      }
      save
    }
  }

  def updates:List[Differ] = {
    val l = scala.collection.mutable.ListBuffer.empty[Differ]
    summaryMap.foreach {
      case (fname, (fpath, size, mDate)) =>
        if (dirInfo.contains(fname)) {
          val oInfo = dirInfo(fname)
          if ((oInfo.size != size) || (oInfo.mDate.compareTo(mDate) != 0)) {
            val cs = pseudoCS(fpath)
            val updatedMD = MDInfo(fname, fpath, size, mDate, cs, dirInfo(fname).exists)
            dirInfo.update(fname, updatedMD)
            if (updatedMD.exists.nonEmpty && cs != oInfo.cs)   // checksum is different, blogger updated needed
              l += Updated(updatedMD)
          }
        } else {
          val newMD = MDInfo(fname, fpath, size, mDate, pseudoCS(fpath), None)
          dirInfo.update(fname, newMD)
          l += Added(newMD)
        }
    }
    // save
    l.toList
  }

  def printUpdates(l:List[Differ]) = {
    println(s"${l.size} updates found.")
    l.foreach {
      case Added(mdInfo) =>
        println(s"New file... ${mdInfo.path}")
      case Updated(mdInfo) =>
        println(s"Modified file... ${mdInfo.path}")
    }
  }

  def updateMDInfo(mdi:MDInfo, stored:Stored) = {
    dirInfo.update(mdi.name, MDInfo(mdi.name, mdi.path, mdi.size, mdi.mDate, mdi.cs, Some(stored)))
    save
  }

  def removeFiles(files:Array[String]) = {
    files.foreach(fn => dirInfo.update(fn, dirInfo(fn).removeStored))
    save
  }
}