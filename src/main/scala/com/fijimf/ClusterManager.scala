package com.fijimf

import java.sql.DriverManager
import java.util.Date

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, Cluster, ClusterState, ClusterStatus, ClusterSummary, DescribeClusterRequest, JobFlowInstancesConfig, ListClustersRequest, ListStepsRequest, RunJobFlowRequest, StepConfig, TerminateJobFlowsRequest}
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import com.fijimf.data.SparkStepConfig

object ClusterManager {
  val driver =DriverManager.getDriver("jdbc:postgresql://fijimf.com:5432/postgres")
  println(driver.toString)
  val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com", "us-east-1"))
    .build()

  val enableDebugging: StepConfig = new StepConfig()
    .withName("Enable debugging")
    .withActionOnFailure("TERMINATE_JOB_FLOW")
    .withHadoopJarStep(new StepFactory().newEnableDebuggingStep)

//  val zzz: StepConfig = new StepConfig()
//    .withName("Enable debugging")
//    .withActionOnFailure("TERMINATE_JOB_FLOW")
//    .withHadoopJarStep(new StepFactory().newScriptRunnerStep())
//

  val spark: Application = new Application().withName("Spark")

  def runSteps(clusterName: String, steps: Array[StepConfig]): Unit = {

    val request: RunJobFlowRequest = new RunJobFlowRequest()
      .withName(clusterName)
      .withReleaseLabel("emr-5.13.0")
      .withSteps(
        steps: _*
      ).withApplications(spark).withLogUri("s3://deepfij-emr/logs/")
      .withServiceRole("EMR_DefaultRole")
      .withJobFlowRole("EMR_EC2_DefaultRole")
      .withInstances(
        new JobFlowInstancesConfig()
          .withInstanceCount(3)
          .withKeepJobFlowAliveWhenNoSteps(false)
          .withMasterInstanceType("m3.xlarge")
          .withSlaveInstanceType("m3.xlarge")
      ).withLogUri("s3://fijimf-peeriq-logs")
    emr.runJobFlow(request)
  }


//  def generateSnapshotParquetFiles(): (String, Date) = {
//    val name = "SNAP-" + UUID.randomUUID().toString
//
//    runSteps(
//      name,
//      Array(
//        enableDebugging,
//        GenerateSnapshotParquetFiles.stepConfig(Map.empty[String, String])
//      )
//    )
//    (name, new Date())
//  }
//
//  def generateTeamStatistics(): (String, Date) = {
//    val name = "DF-" + UUID.randomUUID().toString
//
//    runSteps(
//      name,
//      Array(
//        enableDebugging,
//        WonLost.stepConfig(dbOptions),
//        Scoring.stepConfig(dbOptions),
//        MarginRegression.stepConfig(dbOptions)
//      )
//    )
//    (name, new Date())
//  }
//
//  def recreateAll(): (String, Date) = {
//    val name = "ALL-" + UUID.randomUUID().toString
//
//    runSteps(
//      name,
//      Array(
//        enableDebugging,
//        GenerateSnapshotParquetFiles.stepConfig(Map.empty[String, String]),
//        WonLost.stepConfig(dbOptions),
//        Scoring.stepConfig(dbOptions),
//        MarginRegression.stepConfig(dbOptions)
//      )
//    )
//    (name, new Date())
//  }
//
  def listActiveClusters(): List[Cluster] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest()).getClusters.map(cs => emr.describeCluster(new DescribeClusterRequest().withClusterId(cs.getId)).getCluster).toList
  }

  def terminateCluster(id: String): Unit = {
    import scala.collection.JavaConversions._
    val steps = emr.listSteps(new ListStepsRequest().withClusterId(id))
    val keys = steps.getSteps.filter(step => List("PENDING", "RUNNING").contains(step.getStatus.getState)).map(_.getId)
    emr.terminateJobFlows(new TerminateJobFlowsRequest(keys))
  }

  def isClusterRunning(name: String, start: Date): Boolean = {
    getClusterStatus(name, start).map(s => ClusterState.valueOf(s.getState)) match {
      case Some(c) => c == ClusterState.TERMINATED || c == ClusterState.TERMINATED_WITH_ERRORS
      case None => false
    }
  }

  def getClusterStatus(name: String, start: Date): Option[ClusterStatus] = {
    getClusterSummary(name, start).map(_.getStatus)
  }

  def getClusterSummary(name: String, start: Date): Option[ClusterSummary] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest().withCreatedAfter(start)).getClusters.find(_.getName == name)
  }

  def main(args: Array[String]): Unit = {
    runSteps("peerIqCluster", Array(enableDebugging, SparkStepConfig.createStepConfig("main", "com.fijimf.data.LocalS3CsvRunner", Map.empty[String, String])))


  }
}