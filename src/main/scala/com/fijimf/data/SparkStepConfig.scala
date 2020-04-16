package com.fijimf.data

import com.amazonaws.services.elasticmapreduce.model.{HadoopJarStepConfig, StepConfig}

object SparkStepConfig {
  val assemblyJar = "s3://fijimf-loans/peeriq.jar"

  def createStepConfig(name: String, fqn: String, extraOptions: Map[String, String]): StepConfig = {
    new StepConfig()
      .withName(name)
      .withActionOnFailure("TERMINATE_JOB_FLOW")
      .withHadoopJarStep(
        new HadoopJarStepConfig()
          .withJar("command-runner.jar")
          .withArgs(
            createArgs(name, fqn, extraOptions): _*
          )
      )
  }

  def createArgs(name: String, fqn: String, extraOptions: Map[String, String]): Array[String] = {
    val dOptions = extraOptions.map { case (k: String, v: String) => s"-D$k=$v" }.mkString(" ")
    if (extraOptions.isEmpty)
      Array(
        "spark-submit",
        "--class", fqn,
        "--master", "yarn",
        "--deploy-mode", "client",
        "--executor-memory", "5g",
        "--num-executors", "10",
        "--jars","s3://fijimf-loans/postgresql-42.2.10.jar",
        assemblyJar
      )
    else
      Array(
        "spark-submit",
        "--class", fqn,
        "--conf", s"spark.driver.extraJavaOptions=$dOptions",
        "--conf", s"spark.executor.extraJavaOptions=$dOptions",
        "--master", "yarn",
        "--deploy-mode", "client",
        "--executor-memory", "5g",
        "--num-executors", "10",
        "--jars","s3://fijimf-loans/postgresql-42.2.10.jar",
        assemblyJar
      )

  }

//  def stepConfig(extraProperties:Map[String, String]): StepConfig
}