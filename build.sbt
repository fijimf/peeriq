import Dependencies._

ThisBuild / scalaVersion := "2.11.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.fijimf"
ThisBuild / organizationName := "peeriqData"

lazy val root = (project in file("."))

  .settings(
      name := "peeriq",
      libraryDependencies += scalaTest % Test,
      libraryDependencies += "log4j" % "log4j" % "1.2.17",
      libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.5" % Provided,
      libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.5" % Provided,
      libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.7.3" % Provided,
      libraryDependencies += "com.amazonaws" % "aws-java-sdk-bundle" % "1.11.762" % Provided,
      libraryDependencies += "com.amazonaws" % "aws-java-sdk-emr" % "1.11.762",
      libraryDependencies += "org.postgresql" % "postgresql" % "42.2.12"
  )
test in assembly := {}

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
