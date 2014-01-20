package nineclue.md2blogger

import java.nio.file.{Path, Paths, Files, LinkOption}
import java.util.Date

sealed abstract trait Differ
case class Added(path:String) extends Differ
case class Updated(path:String) extends Differ

case class MDInfo(name:String, path:String, size:Long, mDate:Date, cs:Long, exists:Option[Exists])

object Dir {
  import collection.JavaConversions.{asScalaIterator, asScalaBuffer}

  type Ignore = Path => Boolean
  type ScanInfo = collection.mutable.Map[String, (String, Long, Date)]
  type DirInfo = collection.mutable.Map[String, MDInfo]

  private val fileName = ".mdInfo"

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
    lines.foldLeft(0L)((cs, l) => l.foldLeft(cs)((cs2, c) => (cs2 << 2) + c.toLong))
  }


  // DirInfo m에 대해 각각의 pseudochecksum 계산해 MDInfo 반환
  // 처음 mass update시 호출하고 이후 CS는 필요한 경우에만 계산하도록 하기 위함
  // 혹은 나중에 디렉토리를 처리하기 위함
  def updateCS(m:ScanInfo):DirInfo = {
    val mdMap = collection.mutable.Map.empty[String, MDInfo]
    m.foreach {
      case (name, (pathString, size, mTime)) =>
        mdMap += ((name, MDInfo(name, pathString, size, mTime, pseudoCS(pathString), None)))
    }
    mdMap
  }

  def apply(code: => Path=>Unit) = doDirJob(currentDir)(code)

  // 현재 디렉토리이하의 자료 Map을 반환
  def summaryMap = {
    val s = collection.mutable.Map.empty[String, (String, Long, Date)]
    doDirJob(currentDir)(fileMap(s))
    s
  }

  def printsummary = {
    val sum = summaryMap
    sum.values.foreach {
      case (path, size, date) =>
        val cs = pseudoCS(path)
        println(s"${path.toString} ($size) : $cs")
    }
    // saveToFile(".mdinfo", sum)
  }

  // 자료 Map을 지정된 이름으로 저장
  def saveToFile(name:String, m:DirInfo) = {
    import scala.pickling._
    import json._

    val file = Files.newBufferedWriter(Paths.get(name), java.nio.charset.Charset.forName("UTF-8"))
    try
      file.write(m.pickle.value)
    finally
      file.close
  }

  // 지정된 이름을 불러옴
  def loadFromFile(name:String):DirInfo = {
    import scala.pickling._
    import json._

    val f = Paths.get(name)
    if (Files.exists(f)) {
      val jsonVal = new String(Files.readAllBytes(f))
      jsonVal.unpickle[DirInfo]
    } else
      collection.mutable.Map.empty[String, MDInfo]
  }

  def updates(org:DirInfo, cur:ScanInfo):(List[Differ], DirInfo) = ???
}