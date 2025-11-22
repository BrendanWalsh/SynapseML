// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package org.apache.spark.sql.types.injections

import breeze.linalg.{DenseVector => BDV}
import com.microsoft.azure.synapse.ml.logging.SynapseMLLogging
import com.microsoft.azure.synapse.ml.nn._
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.catalyst.types.PhysicalDataType
import org.apache.spark.sql.types._

trait OptimizedCKNNFitting extends ConditionalKNNParams with SynapseMLLogging {

  private def fitGeneric[V: scala.reflect.ClassTag, L: scala.reflect.ClassTag](dataset: Dataset[_]): ConditionalKNNModel = {

    val kvlTriples = dataset.toDF()
      .select(getFeaturesCol, getValuesCol, getLabelCol)
      .collect()
      .map { row =>
        val bdv = new BDV(row.getAs[Vector](getFeaturesCol).toDense.values)
        val value = row.getAs[V](getValuesCol)
        val label = row.getAs[L](getLabelCol)
        (bdv, value, label)
      }
    val ballTree = ConditionalBallTree(
      kvlTriples.map(_._1), kvlTriples.map(_._2), kvlTriples.map(_._3), getLeafSize)
    new ConditionalKNNModel()
      .setFeaturesCol(getFeaturesCol)
      .setValuesCol(getValuesCol)
      .setBallTree(ballTree)
      .setOutputCol(getOutputCol)
      .setLabelCol(getLabelCol)
      .setConditionerCol(getConditionerCol)
      .setK(getK)
  }

  protected def fitOptimized(dataset: Dataset[_]): ConditionalKNNModel = {
    fitGeneric[Any, Any](dataset)
  }

}

trait OptimizedKNNFitting extends KNNParams with SynapseMLLogging {

  private def fitGeneric[V: scala.reflect.ClassTag](dataset: Dataset[_]): KNNModel = {

    val kvlTuples = dataset.toDF().select(getFeaturesCol, getValuesCol).collect()
      .map { row =>
        val bdv = new BDV(row.getAs[Vector](getFeaturesCol).toDense.values)
        val value = row.getAs[V](getValuesCol)
        (bdv, value)
      }
    val ballTree = BallTree(
      kvlTuples.map(_._1), kvlTuples.map(_._2), getLeafSize)
    new KNNModel()
      .setFeaturesCol(getFeaturesCol)
      .setValuesCol(getValuesCol)
      .setBallTree(ballTree)
      .setOutputCol(getOutputCol)
      .setK(getK)
  }

  protected def fitOptimized(dataset: Dataset[_]): KNNModel = {
    fitGeneric[Any](dataset)
  }

}
