package com.fijimf.data

import java.util.Properties

import org.apache.spark.sql.types.DecimalType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}


object LoadLoans {
   lazy val session: SparkSession =
    SparkSession.builder
      .appName("Simple Application")
      .getOrCreate()

   def readDataFrame(session: SparkSession, bucket:String, file:String): DataFrame =
    session
      .read
      .option("header", "true")
      .csv(s"s3://$bucket/$file")

  def main(args: Array[String]) {
    val bucket:String=args(0)
    val file:String=args(1)
    val df: DataFrame = process(readDataFrame(session, bucket, file))
    //    println(df.count())
    //    df.show(20)
    writeDataFrame(df)
    session.stop()
  }

  /* In a production system the database details below would not be hardcoded.  The Postgres on fijimf.com:5432
     is a Docker throwaway Postgres instance on a personal EC2 server I have.
   */
  def writeDataFrame(frame: DataFrame): Unit = {
    val properties = new Properties()
    properties.setProperty("user", "dbuser")
    properties.setProperty("password", "password")
    properties.setProperty("driver", "org.postgresql.Driver")
    val url = "jdbc:postgresql://fijimf.com:5432/dbuser"
    val table = "data_test"

    frame
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(url, table, properties)
  }

  def process(frame:DataFrame): DataFrame = {
    frame
      .filter(frame("purpose") =!= "other")
      .filter(frame("loan_status") =!= "Charged Off")
      .filter(frame("fico_range_low") >= 700)
      .withColumn("loan_amnt", frame("loan_amnt").cast(DecimalType(10,2)))
      .withColumn("funded_amnt", frame("funded_amnt").cast(DecimalType(10,2)))
      .withColumn("funded_amnt_inv", frame("funded_amnt_inv").cast(DecimalType(10,2)))
  }
}