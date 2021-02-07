package com.github.qiuxin2012

import com.intel.analytics.bigdl.dataset.Sample
import com.intel.analytics.zoo.pipeline.api.keras.layers._
import com.intel.analytics.zoo.pipeline.api.keras.models._
import com.intel.analytics.bigdl.tensor.Tensor
import org.apache.spark.SparkConf
import Utils._
import com.intel.analytics.bigdl.nn.MSECriterion
import com.intel.analytics.zoo.common.NNContext._
import com.intel.analytics.bigdl.optim.SGD
import com.intel.analytics.bigdl.utils.Shape

object SimpleMlp {

  def main(args: Array[String]): Unit = {
    testParser.parse(args, new TestParams()).map(param => {
      val dimInput = param.dimInput
      val nHidden = param.nHidden
      val recordSize = param.recordSize
      val maxEpoch = param.maxEpoch
      val batchSize = param.batchSize

      //init spark context
      val conf = new SparkConf()
        .setAppName(s"SampleMlp-$dimInput-$nHidden-$recordSize-$maxEpoch-$batchSize")
      val sc = initNNContext(conf)

      // make up some data
      val data = sc.range(0, recordSize, 1).map { _ =>
        val featureTensor = Tensor[Float](dimInput)
        featureTensor.apply1(_ => scala.util.Random.nextFloat())
        val labelTensor = Tensor[Float](1)
        labelTensor(Array(1)) = Math.round(scala.util.Random.nextFloat())
        Sample[Float](featureTensor, labelTensor)
      }

      val model = Sequential[Float]()
      model.add(Dense[Float](nHidden, activation = "relu", inputShape = Shape(dimInput)).setName("fc_1"))
      model.add(Dense[Float](nHidden, activation = "relu").setName("fc_2"))
      model.add(Dense[Float](1).setName("fc_3"))

      println(model)

      model.compile(
        optimizer = new SGD[Float](learningRate = 0.01),
        loss = MSECriterion[Float]()
      )
      model.fit(data, batchSize = param.batchSize, nbEpoch = param.maxEpoch)

    })
  }
}
