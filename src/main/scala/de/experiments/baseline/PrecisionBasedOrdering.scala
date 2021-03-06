package de.experiments.baseline

import de.evaluation.f1.{F1, FullResult}
import de.evaluation.util.{DataSetCreator, SparkLOAN}
import de.experiments.ExperimentsCommonConfig
import de.model.util.FormatUtil
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Created by visenger on 11/04/17.
  */
class PrecisionBasedOrdering {

}

object PrecisionBasedOrderingRunner extends ExperimentsCommonConfig {

  def maxPrec(e1: (String, Double), e2: (String, Double)): (String, Double) = {
    val maxP = Math.max(e1._2, e2._2)
    e1._2 == maxP match {
      case true => e1
      case false => e2
    }
  }

  private val detected = "detected"

  def main(args: Array[String]): Unit = {

    SparkLOAN.withSparkSession("PRECISION-BASED-ORDERING") {
      session => {
        process_data {
          data => {
            val datasetName = data._1
            println(s"precision based ordering for $datasetName")
            val trainFile = data._2._1
            // val testFile = data._2._2

            val trainDF: DataFrame = DataSetCreator.createFrame(session, trainFile, FullResult.schema: _*)

            val Array(limitedSample, _) = trainDF.randomSplit(Array(0.1, 0.9), 123L)
            val sample: DataFrame = limitedSample.toDF(FullResult.schema: _*)

            // val testDF = DataSetCreator.createFrame(session, testFile, FullResult.schema: _*)

            Array(0.1, 0.2, 0.3, 0.4, 0.5).foreach(threshold => {
              println(s"threshold: $threshold")
              val allTools = FullResult.tools

              val precisionBasedOrdering: DataFrame = getBestTool(session, sample, threshold, allTools)

              //              val precisionBasedOrdering: DataFrame = getBestTool(session, trainDF, threshold, allTools)
              val pbo: DataFrame = precisionBasedOrdering
                .select(FullResult.label, detected)
                .toDF(FullResult.label, detected)

              val predictionAndLabel: RDD[(Double, Double)] = FormatUtil.getStringPredictionAndLabel(pbo, detected)

              val precisionBasedEval = F1.evalPredictionAndLabels(predictionAndLabel)
              precisionBasedEval.printLatexString(s"PBO $$\\delta$$=$threshold  ")

            })
          }
        }
      }
    }

  }

  def getBestTool(session: SparkSession, trainDF: DataFrame, threshold: Double, allTools: Seq[String]): DataFrame = {
    val toolToPrecision = allTools.map(t => {

      val toolPredictAndLabel = FormatUtil.getStringPredictionAndLabel(trainDF, t)
      val evalTool = F1.evalPredictionAndLabels(toolPredictAndLabel)
      t -> evalTool.precision
    }).toMap

    val qualifiedTools = toolToPrecision.filter(entry => {
      entry._2 >= threshold
    })
    if (qualifiedTools.nonEmpty) {
      val bestToolPrecision: (String, Double) = qualifiedTools.reduce((e1, e2) => maxPrec(e1, e2))
      println(s"best system: ${getName(bestToolPrecision._1)}, precision: ${bestToolPrecision._2}")

      val remainingDataset: DataFrame = trainDF
        .where(trainDF(bestToolPrecision._1) === "0")
      //.toDF(FullResult.schema: _*) <- bug!!!

      val bestDF = trainDF
        .select(FullResult.label, bestToolPrecision._1)
        .where(trainDF(bestToolPrecision._1) === "1")
        .withColumnRenamed(bestToolPrecision._1, detected)
        .toDF(FullResult.label, detected)

      // bestDF.printSchema()

      val remainingTools: Seq[String] = allTools.diff(Seq(bestToolPrecision._1))
      // println(s"remaining tools: ${remainingTools.mkString(", ")}")
      bestDF
        .union(getBestTool(session, remainingDataset, threshold, remainingTools))
      //.repartition(1)
    } else {
      //there are no tools left, so we pass the remaining tools

      val df = trainDF.select(FullResult.label).withColumn(detected, lit("0"))
      val dataFrame = df.select(FullResult.label, detected)
        // .repartition(1)
        .toDF(FullResult.label, detected)
      //      dataFrame.printSchema()
      //      dataFrame.show()
      dataFrame
    }

  }
}
