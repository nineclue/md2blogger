name := "md2blogger"

version := "0.1"

libraryDependencies ++= Seq(
  "org.commonjava.googlecode.markdown4j" % "markdown4j" % "2.2-cj-1.0",
  // "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
  "uk.co.bigbeeconsultants" %% "bee-client" % "0.21.+",
  "uk.co.bigbeeconsultants" %% "bee-config" % "1.1.+",
  "org.slf4j" % "slf4j-api" % "1.7.+",
  "ch.qos.logback" % "logback-core" % "1.0.+",
  "ch.qos.logback" % "logback-classic" % "1.0.+",
  "org.scala-lang" %% "scala-pickling" % "0.8.0-SNAPSHOT"
)

resolvers ++= Seq("Big Bee Consultants" at "http://repo.bigbeeconsultants.co.uk/repo",
  Resolver.sonatypeRepo("snapshots"))
