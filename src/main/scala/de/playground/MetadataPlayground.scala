package de.playground

import de.evaluation.data.metadata.MetadataCreator
import de.evaluation.util.SparkLOAN
import org.apache.spark.sql.DataFrame

object MetadataPlayground {
  //val metadataPath = "/Users/visenger/deepdive_notebooks/hosp-cleaning/dirty-data/SCDP-1.1-SNAPSHOT.jar2018-04-27T143644_stats"
  //val metadataPath = "/Users/visenger/research/datasets/craft-beers/craft-cans/metadata/SCDP-1.1-SNAPSHOT.jar2018-05-02T110514_stats"
  val metadataPath = "/Users/visenger/research/datasets/craft-beers/craft-cans/metadata/dirtySCDP-1.1-SNAPSHOT.jar2018-05-18T125830_stats"

  def main(args: Array[String]): Unit = {
    SparkLOAN.withSparkSession("metadata reader") {
      session => {
        val creator = MetadataCreator.apply()

        val metadataTop10: DataFrame = creator
          .extractTop10Values(session, metadataPath)
        metadataTop10.show()

        val fullMetadataDF: DataFrame = creator.getFullMetadata(session, metadataPath)
        fullMetadataDF.printSchema()

        /**
          * root
          * |-- nulls count: long (nullable = true)
          * |-- % of nulls: long (nullable = true)
          * |-- % of distinct vals: long (nullable = true)
          * |-- top10: array (nullable = true)
          * |    |-- element: string (containsNull = true)
          * |-- freqTop10: array (nullable = true)
          * |    |-- element: long (containsNull = true)
          * |-- histogram: array (nullable = true)
          * |    |-- element: string (containsNull = true)
          * |-- attrName: string (nullable = true)
          */
        fullMetadataDF.show(false)

      }
    }
  }
}