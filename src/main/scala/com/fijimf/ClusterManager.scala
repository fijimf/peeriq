package com.fijimf

import java.util.Date

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, Cluster, ClusterState, ClusterStatus, ClusterSummary, DescribeClusterRequest, JobFlowInstancesConfig, ListClustersRequest, ListStepsRequest, RunJobFlowRequest, StepConfig, TerminateJobFlowsRequest}
import com.amazonaws.services.elasticmapreduce.util.StepFactory

object ClusterManager {

  val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com", "us-east-1"))
    .build()

  val enableDebugging: StepConfig = new StepConfig()
    .withName("Enable debugging")
    .withActionOnFailure("TERMINATE_JOB_FLOW")
    .withHadoopJarStep(new StepFactory().newEnableDebuggingStep)

  val spark: Application = new Application().withName("Spark")

  def runSteps(clusterName: String, steps: Array[StepConfig]): String = {

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
    emr.runJobFlow(request).getJobFlowId
  }


  def listActiveClusters(): List[Cluster] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest()).getClusters.map(cs => emr.describeCluster(new DescribeClusterRequest().withClusterId(cs.getId)).getCluster).toList
  }

  def terminateCluster(id: String) = {
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
    val bucket: String = System.getenv("PEERIQ_BUCKET_NAME")
    val file: String = System.getenv("PEERIQ_FILE_NAME")
    require(bucket != null && file != null)
    val resp = runSteps(
      "peerIqCluster",
      Array(
        enableDebugging,
        SparkStepConfig.createStepConfig(
          "loadLoans",
          "com.fijimf.data.LoadLoans",
          Map.empty[String, String],
          Array(bucket, file)
        )
      )
    )

  }

}