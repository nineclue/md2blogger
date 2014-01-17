package nineclue.md2blogger

import java.nio.file.{Path, Paths, Files, LinkOption}
import java.util.Date

object Dir {
  import collection.JavaConversions.{asScalaIterator, asScalaBuffer}
  type Ignore = Path => Boolean
  implicit def defaultIgnore:Ignore =
    { f => Files.isHidden(f) || { val fCh = f.getFileName.toString.apply(0); fCh == '.' || fCh == '_'} }

  // 프로그램 실행되는 현재 디렉토리를 반환
  def currentDir = Paths.get("").toAbsolutePath

  // 처음 실행되는 Path p에 대해 각각의 file에서 code를 실행한다
  def doDirJob(p:Path)(code: => Path=>Unit)(implicit ignore:Ignore):Unit = {
    val stream = Files.newDirectoryStream(p)
    stream.iterator.filter(!ignore(_)).foreach(f =>
      if (Files.isDirectory(f)) doDirJob(f)(code)(ignore)
      else code(f))
    stream.close
  }

  // Path p에 대해 파일 이름, 크기, 정보를 출력
  def printInfo(p:Path) = {
    val name = p.getFileName.toString
    val size = Files.size(p)
    val mTime = Files.getLastModifiedTime(p, LinkOption.NOFOLLOW_LINKS)
    println(s"$name ($size) : $mTime")
  }

  // Path p에 대해 하부 디렉토리를 포함한 파일 정보를 반환
  // 상부와 하부 디렉토리에 위치한 파일간의 차이는 없음 (즉, 디렉토리는 분류의 편리함을 위한 것일뿐)
  // 같은 이름의 파일이 존재하면 에러 발생
  //
  def fileMap(m:collection.mutable.Map[String, (Path, Long, Date)])(p:Path) = {
    val namePath = p.getFileName
    val name = namePath.toString
    val size = Files.size(p)
    val mTime = new Date(Files.getLastModifiedTime(p, LinkOption.NOFOLLOW_LINKS).toMillis)
    if (m.contains(name))
      println("dulpicates found : " + name)
      // throw new Exception(s"Duplicate file name : $name")
    else m += ((name, (p, size, mTime)))
  }

  // Path p의 내용에 대한 checksum 계산
  def pseudoCS(p:Path):Long = {
    val lines = Files.readAllLines(p, java.nio.charset.Charset.forName("UTF-8"))
    lines.foldLeft(0L)((cs, l) => l.foldLeft(cs)((cs2, c) => (cs2 << 2) + c.toLong))
  }

  def apply(code: => Path=>Unit) = doDirJob(currentDir)(code)
  def apply() = doDirJob(currentDir)(printInfo)
  def summary() = {
    val s = collection.mutable.Map.empty[String, (Path, Long, Date)]
    doDirJob(currentDir)(fileMap(s))
    s
  }
  def printsummary() = {
    summary.values.foreach {
      case (path, size, date) =>
        val cs = pseudoCS(path)
        println(s"${path.toString} ($size) : $cs")
    }
  }
}