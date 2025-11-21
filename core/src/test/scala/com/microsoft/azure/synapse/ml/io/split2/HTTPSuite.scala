// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.azure.synapse.ml.io.split2

import com.microsoft.azure.synapse.ml.core.test.base.TestBase
import com.microsoft.azure.synapse.ml.io.http.HTTPSchema.string_to_response
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.spark.sql.execution.streaming.{HTTPSinkProvider, HTTPSourceProvider}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.StringType

import java.io.File

class HTTPSuite extends TestBase with HTTPTestUtils {

  test("stream from HTTP") {
    val q1 = spark.readStream.format(classOf[HTTPSourceProvider].getName)
      .option("host", host)
      .option("port", port.toString)
      .option("path", apiPath)
      .load()
      .withColumn("contentLength", col("request.entity.contentLength"))
      .withColumn("reply", string_to_response(col("contentLength").cast(StringType)))
      .writeStream
      .format(classOf[HTTPSinkProvider].getName)
      .option("name", "foo")
      .queryName("foo")
      .option("replyCol", "reply")
      .option("checkpointLocation", new File(tmpDir.toFile, "checkpoints").toString)
      .start()

    using(q1) {
      waitForServer(q1)
      val client = HttpClientBuilder.create().build()
      try {
        val posts = List(
          sendJsonRequest(Map("foo" -> 1, "bar" -> "here"), url),
          sendJsonRequest(Map("foo" -> 1, "bar" -> "heree"), url),
          sendJsonRequest(Map("foo" -> 1, "bar" -> "hereee"), url),
          sendJsonRequest(Map("foo" -> 1, "bar" -> "hereeee"), url))
        val correctResponses = List(22, 23, 24, 25).map(_.toString)
        posts.zip(correctResponses).foreach { case (resp, expected) => assert(resp === expected) }
      } finally {
        client.close()
      }
    }
  }

}
