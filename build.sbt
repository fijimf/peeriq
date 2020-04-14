import Dependencies._

ThisBuild / scalaVersion := "2.11.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "peeriq",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.5",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.5",
    libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.7.2",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.2.12"
  )
test in assembly := {}
mainClass in assembly := Some("play.core.server.ProdServerStart")
assemblyMergeStrategy in assembly := {
    case PathList(ps @ _*) if ps.last endsWith ".conf" => MergeStrategy.concat
    case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.concat
    case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.last
    case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.last
    case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last endsWith "messages" => MergeStrategy.concat
    case PathList(ps @ _*) if ps.last endsWith "types" => MergeStrategy.last
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter {_.data.getName.startsWith("org.apache.spark")}
}
assemblyOutputPath in assembly :=  file(s"${sys.env.getOrElse("DEPLOY_DIR", "/tmp")}/${name.value}-${version.value}-assembly.jar")
