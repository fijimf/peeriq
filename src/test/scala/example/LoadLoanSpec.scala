package example

import com.fijimf.data.LoadLoans
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.FlatSpec

class LoadLoanSpec extends FlatSpec {
  val session: SparkSession =
    SparkSession.builder
      .config("spark.master", "local")
      .appName("Simple Application")
      .getOrCreate()

  val frame: DataFrame =
    session
      .read
      .option("header", "true")
      .csv("src/test/resources/sample.csv")


  "Test data" should " load correctly " in {
    assert(frame.count === 31)
  }

  "LoadLoans" should "exclude loans with purpose 'Other'" in {
    val f2: DataFrame = LoadLoans.process(frame)
    f2.foreach(r=>{
      val purpose: String = r.getAs[String]("purpose")
      assert(purpose != "other")
    })
  }

}
