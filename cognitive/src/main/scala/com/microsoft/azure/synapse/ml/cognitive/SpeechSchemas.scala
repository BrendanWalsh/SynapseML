// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.azure.synapse.ml.cognitive

import com.microsoft.azure.synapse.ml.core.contracts.HasOutputCol
import com.microsoft.azure.synapse.ml.core.schema.SparkBindings
import com.microsoft.azure.synapse.ml.io.http.HasErrorCol
import com.microsoft.azure.synapse.ml.param.ServiceParam
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import org.apache.spark.ml.param.{Param, Params}
import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class DetailedSpeechResponse(Confidence: Double,
                                  Lexical: String,
                                  ITN: String,
                                  MaskedITN: String,
                                  Display: String)

trait SharedSpeechFields {
  def RecognitionStatus: String

  def Offset: Long

  def Duration: Long

  def DisplayText: Option[String]

  def NBest: Option[Seq[DetailedSpeechResponse]]
}

case class SpeechResponse(RecognitionStatus: String,
                          Offset: Long,
                          Duration: Long,
                          Id: Option[String],
                          DisplayText: Option[String],
                          NBest: Option[Seq[DetailedSpeechResponse]]
                         ) extends SharedSpeechFields

object SpeechResponse extends SparkBindings[SpeechResponse]

case class TranscriptionResponse(RecognitionStatus: String,
                                 Offset: Long,
                                 Duration: Long,
                                 Id: Option[String],
                                 DisplayText: Option[String],
                                 NBest: Option[Seq[DetailedSpeechResponse]],
                                 SpeakerId: String,
                                 Type: String,
                                 UtteranceId: String) extends SharedSpeechFields

object TranscriptionResponse extends SparkBindings[TranscriptionResponse]

case class TranscriptionParticipant(name: String, language: String, signature: String)

object SpeechFormat extends DefaultJsonProtocol {
  implicit val DetailedSpeechResponseFormat: RootJsonFormat[DetailedSpeechResponse] =
    jsonFormat5(DetailedSpeechResponse.apply)
  implicit val SpeechResponseFormat: RootJsonFormat[SpeechResponse] = jsonFormat6(SpeechResponse.apply)
  implicit val TranscriptionResponseFormat: RootJsonFormat[TranscriptionResponse] =
    jsonFormat9(TranscriptionResponse.apply)
  implicit val TranscriptionParticipantFormat: RootJsonFormat[TranscriptionParticipant] =
    jsonFormat3(TranscriptionParticipant.apply)
}

object SpeechSynthesisError extends SparkBindings[SpeechSynthesisError] {
  def fromSDK(error: SpeechSynthesisCancellationDetails): SpeechSynthesisError = {
    SpeechSynthesisError(error.getErrorCode.name(), error.getErrorDetails, error.getReason.name())
  }
}

case class SpeechSynthesisError(errorCode: String, errorDetails: String, errorReason: String)

trait HasLocaleCol extends HasServiceParams {
  val locale = new ServiceParam[String](this,
    "locale",
    s"The locale of the input text",
    isRequired = true)

  def setLocale(v: String): this.type = setScalarParam(locale, v)
  def setLocaleCol(v: String): this.type = setVectorParam(locale, v)
}

trait HasTextCol extends HasServiceParams {
  val text = new ServiceParam[String](this,
    "text",
    s"The text to synthesize",
    isRequired = true)

  def setText(v: String): this.type = setScalarParam(text, v)
  def setTextCol(v: String): this.type = setVectorParam(text, v)
}

trait HasVoiceCol extends HasServiceParams {
  val voice = new ServiceParam[String](this,
    "voice",
    s"The name of the voice used for synthesis",
    isRequired = true)

  def setVoiceName(v: String): this.type = setScalarParam(voice, v)
  def setVoiceNameCol(v: String): this.type = setVectorParam(voice, v)
}

trait HasSSMLOutputCol extends Params {
  val ssmlOutputCol = new Param[String](this, "ssmlCol", "The name of the SSML column")

  def setSSMLOutputCol(value: String): this.type = set(ssmlOutputCol, value)

  def getSSMLOutputCol: String = $(ssmlOutputCol)
}

trait HasSSMLGeneratorParams extends HasServiceParams
  with HasLocaleCol with HasTextCol with HasVoiceCol
  with HasSSMLOutputCol with HasOutputCol with HasErrorCol

case class TextToSpeechSSMLError(errorCode: String, errorDetails: String)
  object TextToSpeechSSMLError extends SparkBindings[TextToSpeechSSMLError]

case class SSMLConversation(Begin: Int,
                        End: Int,
                        Content: String,
                        Role: String,
                        Style: String)
object SSMLConversation extends SparkBindings[SSMLConversation]

case class TextToSpeechSSMLResponse(IsValid: Boolean, Conversations: Seq[SSMLConversation])
object TextToSpeechSSMLResponse extends SparkBindings[TextToSpeechSSMLResponse]

object TextToSpeechSSMLFormat extends DefaultJsonProtocol {
  implicit val ConversationFormat: RootJsonFormat[SSMLConversation] =
    jsonFormat(SSMLConversation.apply, "Begin", "End", "Content", "Role", "Style")
  implicit val TextToSpeechSSMLResponseFormat: RootJsonFormat[TextToSpeechSSMLResponse] =
    jsonFormat(TextToSpeechSSMLResponse.apply, "IsValid", "Conversations")
  implicit val TextToSpeechSSMLErrorFormat: RootJsonFormat[TextToSpeechSSMLError] =
    jsonFormat(TextToSpeechSSMLError.apply, "ErrorCode", "ErrorDetails")
}
