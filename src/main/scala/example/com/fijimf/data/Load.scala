package example.com.fijimf.data

import java.util.Properties

import org.apache.spark.sql.types.DecimalType
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

trait DataLoader {
  def session: SparkSession

  def readDataFrame(session: SparkSession): DataFrame

  def writeDataFrame(frame: DataFrame):Unit

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



object LocalFileCsvRunner extends DataLoader {

  override lazy val session: SparkSession =
    SparkSession.builder
      .config("spark.master", "local")
      .appName("Simple Application")
      .getOrCreate()

  override def readDataFrame(session: SparkSession): DataFrame =
    session
      .read
      .option("header", "true")
      .csv("src/test/resources/sample.csv")

  def main(args: Array[String]) {
    val df = process(readDataFrame(session))
    println(df.count())
    df.show(20)
    writeDataFrame(df)
    session.stop()
  }

  override def writeDataFrame(frame: DataFrame): Unit = {
    val properties = new Properties()
    properties.setProperty("user","fijuser")
    properties.setProperty("password","mut()mb()")
    JdbcDataWriter("jdbc:postgresql://localhost:5432/deepfijdb","data_test", properties).writeDataFrame(frame)
  }
}


object LocalS3CsvRunner extends DataLoader {

  override lazy val session: SparkSession =
    SparkSession.builder
      .config("spark.master", "local")
      .appName("Simple Application")
      .getOrCreate()

  override def readDataFrame(session: SparkSession): DataFrame =
    session
      .read
      .option("header", "true")
      .csv("s3a://fijimf-loans/sample.csv")

  def main(args: Array[String]) {
    val df = process(readDataFrame(session))
    println(df.count())
    df.show(20)
    writeDataFrame(df)
    session.stop()
  }

  override def writeDataFrame(frame: DataFrame): Unit = {
    val properties = new Properties()
    properties.setProperty("user","fijuser")
    properties.setProperty("password","mut()mb()")
    JdbcDataWriter("jdbc:postgresql://localhost:5432/deepfijdb","data_test", properties).writeDataFrame(frame)
  }
}


object SubmitFileCsvRunner extends DataLoader {

  override lazy val session: SparkSession =
    SparkSession.builder
      .appName("Simple Application")
      .getOrCreate()

  override def readDataFrame(session: SparkSession): DataFrame =
    session
      .read
      .option("header", "true")
      .csv("/Users/jfrohnhofer/peeriq/src/test/resources/sample.csv")

  def main(args: Array[String]) {
   // session.sparkContext.addFile("/Users/jfrohnhofer/peeriq/src/test/resources/sample.csv")
    val df = process(readDataFrame(session))
    println(df.count())
    df.show(20)
    writeDataFrame(df)
    session.stop()
  }

  override def writeDataFrame(frame: DataFrame): Unit = {
    val properties = new Properties()
    properties.setProperty("user","fijuser")
    properties.setProperty("password","mut()mb()")
    JdbcDataWriter("jdbc:postgresql://localhost:5432/deepfijdb","data_test", properties).writeDataFrame(frame)
  }
}

