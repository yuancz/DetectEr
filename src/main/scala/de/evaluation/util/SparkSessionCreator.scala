package de.evaluation.util

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession

/**
  * Created by visenger on 30/11/16.
  */
object SparkSessionCreator {
  private val conf: Config = ConfigFactory.load()

  def createSession(appName: String) = {
    SparkSession
      .builder()
      .master("local[4]")
      .appName(appName)
      .config("spark.local.ip",
        conf.getString("spark.config.local.ip.value"))
      .config("spark.driver.memory", "10g")
      .config("spark.executor.memory", "8g")
      .getOrCreate()
  }

}