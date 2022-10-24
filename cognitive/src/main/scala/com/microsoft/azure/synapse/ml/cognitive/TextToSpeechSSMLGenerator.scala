// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.azure.synapse.ml.cognitive

import com.microsoft.azure.synapse.ml.cognitive.TextToSpeechSSMLFormat.TextToSpeechSSMLResponseFormat
import com.microsoft.azure.synapse.ml.logging.BasicLogging
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{AbstractHttpEntity, StringEntity}
import org.apache.spark.ml.{ComplexParamsReadable, NamespaceInjections, PipelineModel, Transformer}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{StringType, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import spray.json._

object TextToSpeechSSMLGenerator extends ComplexParamsReadable[TextToSpeechSSMLGenerator] with Serializable
class TextToSpeechSSMLGenerator(override val uid: String) extends CognitiveServicesBase(uid)
  with HasSSMLGeneratorParams with HasCognitiveServiceInput
  with HasInternalStringOutputParser
  with BasicLogging {
  logClass()

  def this() = this(Identifiable.randomUID(classOf[TextToSpeechSSMLGenerator].getSimpleName))

  setDefault(errorCol -> (uid + "_errors"))
  setDefault(locale -> Left("en-US"))
  setDefault(voice -> Left("en-US-SaraNeural"))

  def urlPath: String = "cognitiveservices/v1"

  protected val additionalHeaders: Map[String, String] = Map[String, String](
    ("X-Microsoft-OutputFormat", "textanalytics-json"),
    ("Content-Type", "application/ssml+xml")
  )

  override def inputFunc(schema: StructType): Row => Option[HttpRequestBase] = super.inputFunc(schema).andThen(r => {
    r.map(req => {
      additionalHeaders.foreach(header => req.setHeader(header._1, header._2))
      req
    })
  })

  override protected def prepareEntity: Row => Option[AbstractHttpEntity] = { row =>
    val localeValue = getValue(row, locale)
    val zhCNVoiceName = "Microsoft Server Speech Text to Speech Voice (zh-CN, XiaomoNeural)"
    val enUSVoiceName = "Microsoft Server Speech Text to Speech Voice (en-US, JennyNeural)"
    val voiceName = if (localeValue == "zh-CN") zhCNVoiceName else enUSVoiceName
    val textValue = getValue(row, text)
    val body: String =
      s"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis'" +
        s" xmlns:mstts='https://www.w3.org/2001/mstts'" +
        s" xml:lang='${localeValue}'><voice xml:lang='${localeValue}' xml:gender='Female' name='${voiceName}'>" +
        s"<mstts:task name ='RoleStyle'/>${textValue}</voice></speak>"
    Some(new StringEntity(body))
  }

  def formatSSML(row: Row, response: TextToSpeechSSMLResponse): String = {
    val ssmlFormat: String = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
      "xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='%s'>".format(getValue(row, locale)) +
      "%s</speak>"
    val voiceFormat = "<voice name='%s'>".format(getValue(row, voice)) + "%s</voice>"
    val expressAsFormat = "<mstts:express-as role='%s' style='%s'>%s</mstts:express-as>"
    val builder = new StringBuilder()
    val fullText = "%s".format(getValue(row, text))
    var lastEnd = 0
    response.Conversations.foreach(c => {
      val content = c.Content
      val role = c.Role.toLowerCase()
      val style = c.Style.toLowerCase()
      val begin = c.Begin
      val end = c.End

      val ssmlTurnRoleStyleStr = expressAsFormat.format(role, style, content)
      val preStr = fullText.substring(lastEnd, begin - lastEnd)

      if (preStr.length > 0) {
        builder.append(preStr)
      }
      builder.append(ssmlTurnRoleStyleStr)
      lastEnd = end
    })

    val endStr = fullText.substring(lastEnd)
    if (endStr.length > 0) {
      builder.append(endStr)
    }
    val outSsmlStr = ssmlFormat.format(voiceFormat.format(builder.toString())) + "\n"
    outSsmlStr
  }

  val postprocessingTransformer: Transformer = new Transformer {
    def transform(dataset: Dataset[_]): DataFrame = dataset.toDF().map { row =>
      val response = row.getAs[String](getOutputCol).parseJson.convertTo[TextToSpeechSSMLResponse]
      val result = formatSSML(row, response)
      Row.fromSeq(row.toSeq ++ Seq(result))
    }(RowEncoder(transformSchema(dataset.schema)))

    override val uid: String = Identifiable.randomUID("TTSSSMLInternalPostProcessor")

    override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

    override def transformSchema(schema: StructType): StructType = schema.add(getSSMLOutputCol, StringType)
  }

  override def getInternalTransformer(schema: StructType): PipelineModel = {
    NamespaceInjections.pipelineModel(stages=Array[Transformer](
      super.getInternalTransformer(schema),
      postprocessingTransformer
    ))
  }
}
