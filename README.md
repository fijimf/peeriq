# PeerIQ Engineering Challenge

## Overview

This project comprises two parts which correlate to the two requirements of the challenge.  

It's implemented as a scala project and has been tested on my personal AWS account.

To run it's necessary to have AWS credentials either set up as environment variables (`AWS_SECRET_ACCESS_KEY`, `AWS_ACCESS_KEY_ID`) or have credentials set in an AWS profile.  The credentials should be for an IAM role with S3FullAccess and EMRFullAccess permissions. with 

The first step is to run `sbt assembly` which will generate `/tmp/peeriq-<version>-assembly.jar` This is an assembly uberjar of the project classes and their dependencies.

The second step is use the AWS CLI to run `aws s3 cp /tmp/peeriq-<version>-assembly.jar s3://fijimf-loans/peeriq.jar`.  This moves a copy of the assembly jar to a pre-existing S3 bucket which will be available to the processes on the Spark cluster.

Finally we run the `main` method in `com.fijimf.ClusterManager`.  This spins up an ephemeral EMR cluster with Spark installed which will auto terminate when all steps are run.  It sets up a spark-submit task with the `com.fijimf.data.LoadLoans` class which will start as soon as the cluster is ready.  Upon completion the cluster will terminate.

## The Code

### LoadLoans

### ClusterManager
 
