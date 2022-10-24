package com.microsoft.azure.synapse.ml.cognitive.split2

import com.microsoft.azure.synapse.ml.cognitive._
import com.microsoft.azure.synapse.ml.cognitive.TextToSpeechSSMLGenerator
import com.microsoft.azure.synapse.ml.core.test.fuzzing.{TestObject, TransformerFuzzing}
import org.apache.spark.ml.util.MLReadable
import org.apache.spark.sql.DataFrame
import spray.json._
import com.microsoft.azure.synapse.ml.cognitive.TextToSpeechSSMLFormat.TextToSpeechSSMLResponseFormat

class TextToSpeechSSMLGeneratorSuite extends TransformerFuzzing[TextToSpeechSSMLGenerator] with CognitiveKey  {

  import spark.implicits._

  def ssmlGenerator: TextToSpeechSSMLGenerator = new TextToSpeechSSMLGenerator()
    .setUrl("https://eastus.tts.speech.microsoft.com/cognitiveservices/v1")
    .setSubscriptionKey(cognitiveKey)
    .setTextCol("textColName")
    .setOutputCol("outputColName")
    .setSSMLOutputCol("SSMLColName")
    .setErrorCol("errorColName")
    .setLocale("en-US")
    .setVoiceName("JennyNeural")

  val testData: Map[String, (Boolean, String)] = Map[String, (Boolean, String)](
    "\"I'm shouting excitedly!\" she shouted excitedly." ->
      (true, "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
        "xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='en-US'><voice name='JennyNeural'>" +
        "<mstts:express-as role='female' style='cheerful'>\"I'm shouting excitedly!\"</mstts:express-as> she shouted " +
        "excitedly.</voice></speak>\n"),
    "This text has no quotes in it, so isValid should be false" ->
      (false, "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
        "xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='en-US'><voice name='JennyNeural'>" +
        "This text has no quotes in it, so isValid should be false</voice></speak>\n"),
    "\"This is an example of a sentence with unmatched quotes,\" she said.\"" ->
      (false, "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
        "xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='en-US'><voice name='JennyNeural'>" +
        "<mstts:express-as role='female' style='calm'>\"This is an example of a sentence with unmatched quotes,\"" +
        "</mstts:express-as> she said.\"</voice></speak>\n")
  )

  lazy val df: DataFrame = testData.map(e => e._1).toSeq.toDF("textColName")

  test("basic") {
    testData.map(e => {
      val transform = ssmlGenerator.transform(Seq(e._1).toDF("textColName"))
      transform.show(truncate = false)
      val result = transform.collect()
      result.map(row => row.getString(2)).foreach(out =>
        assert(out.parseJson.convertTo[TextToSpeechSSMLResponse].IsValid == e._2._1))
      result.map(row => row.getString(3)).foreach(out =>
        assert(out.trim == e._2._2.trim))
    })
  }

  override def testObjects(): Seq[TestObject[TextToSpeechSSMLGenerator]] =
    Seq(new TestObject(ssmlGenerator, df))

  override def reader: MLReadable[_] = TextToSpeechSSMLGenerator
}
