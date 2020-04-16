package com.fijimf.data

import java.util.Properties

import org.apache.spark.sql.{DataFrame, SaveMode}

case class JdbcDataWriter(url: String, tableName: String, connectionProperties: Properties) {

  def writeDataFrame(frame: DataFrame): Unit = {
    frame
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(url, tableName, connectionProperties)
  }

}
