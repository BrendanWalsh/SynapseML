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

  private def fitGeneric[V, L](dataset: Dataset[_])(
      implicit ctV: scala.reflect.ClassTag[V], ctL: scala.reflect.ClassTag[L]): ConditionalKNNModel = {

    val kvlTriples = dataset.toDF().select(getFeaturesCol, getValuesCol, getLabelCol).collect()
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
    // Use Any for type parameters due to path-dependent type ClassTag limitations
    fitGeneric[Any, Any](dataset)
  }

}

trait OptimizedKNNFitting extends KNNParams with SynapseMLLogging {

  private def fitGeneric[V](dataset: Dataset[_])(implicit ctV: scala.reflect.ClassTag[V]): KNNModel = {

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
    // Use Any for type parameters due to path-dependent type ClassTag limitations
    fitGeneric[Any](dataset)
  }

}
