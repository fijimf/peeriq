# PeerIQ Engineering Challenge

## Overview

This project's structure is loosely divided into two parts which correlate to the two requirements of the challenge.  

It's implemented as a scala project and has been tested on my personal AWS account.

To run it's necessary to have AWS credentials either set up as environment variables (`AWS_SECRET_ACCESS_KEY`, `AWS_ACCESS_KEY_ID`) or have credentials set in an AWS profile.  The credentials should be for an IAM role with S3FullAccess and EMRFullAccess permissions. 

The first step is to run `sbt assembly` which will generate `/tmp/peeriq-<version>-assembly.jar` This is an assembly uberjar of the project classes and their dependencies.

The second step is use the AWS CLI to run `aws s3 cp /tmp/peeriq-<version>-assembly.jar s3://fijimf-loans/peeriq.jar`.  This moves a copy of the assembly jar to a pre-existing S3 bucket which will be available to the processes on the Spark cluster.

Finally we run the `main` method in `com.fijimf.ClusterManager`.  This spins up an ephemeral EMR cluster with Spark installed which will auto terminate when all steps are run.  It sets up a spark-submit task with the `com.fijimf.data.LoadLoans` class which will start as soon as the cluster is ready.  Upon completion the cluster will terminate.

## The Code

### LoadLoans

LoadLoans is a straightforward Spark application.  It reads a CSV file from an S3 bucket into a dataframe.  The file name and bucket are passed as parameters to the main method.

The `process` function transforms the dataframe in accordance with the requirements.  The process method here is very simple and straightforward.  In a production system, there would be substantially more data quality checks and those checks, the data transformations, and aggregations would probably be grouped together thematically into functions, or traits which might be shared among applications.

Finally the transformed dataframe is written to a Postgres database.  In this case I have chosen to write to the database in 'overwrite'  That is not the only way one could go, but I have found in practice that 'overwrite' often works more smoothly than 'append'.  
The database write is an all or nothing thing, and any constraints you might have that would cause an insert to fail, will fail the whole write task.  Postgres has a first class UPSERT, and so I've found it useful to consider the table written by Spark as a staging table for the final table for the data, and to handle that last step on the database.

  
### ClusterManager

ClusterManager is kind of an ugly piece of code, in that the Java SDK for EMR is a pretty thin wrapper over the AWS CLI.  Basically it builds a *cluster*, with *applications* and then adds *steps*.
The API has a fluent builder interface, but ultimately it's spin up a container and run spark-submit.  Logging is set to got to a pre-existing S3 bucket.  I wrote some convenience methods to monitor the cluster after submitting the job, but I don't feel they're particularly robust right now, so I run the job as a fire and forget kick off and just monitor the cluster in the console.
Note that LoadLoans takes the bucket and filename as arguments to main, while ClusterManager pulls them as environment variables, and feeds them as arguments to the LoadLoans spark-submit step.


### Testing

The bulk of my time was spent debugging the ClusterManager and the cluster spin up, and classpath issues on the worker nodes (ask me about a dumb mistake that cost me _hours_.  Sigh.)  That aspect of the project would be very difficult to unit test.  You're pretty much forced to do manual runs and smoke tests.

LoadLoans, however can be unit tested in a relatively clear and starightforward way.  I've included a couple of test cases off the sample data provided.  In a production system I would probably write many more.



