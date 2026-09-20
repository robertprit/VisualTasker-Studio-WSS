package com.visualtasker.ime.ime.handwriting

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea

class DigitalInkHandwritingRecognizer(
    languageTag: String = "de-DE"
) : AutoCloseable {
    private val modelIdentifier = requireNotNull(
        DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
    ) { "Kein Handschriftmodell für $languageTag" }
    private val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    private val recognizer: DigitalInkRecognizer = DigitalInkRecognition.getClient(
        DigitalInkRecognizerOptions.builder(model).build()
    )
    private val modelManager = RemoteModelManager.getInstance()

    fun prepare(onReady: () -> Unit, onFailure: (Exception) -> Unit) {
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    onReady()
                } else {
                    modelManager.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener { onReady() }
                        .addOnFailureListener(onFailure)
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun recognize(
        ink: Ink,
        width: Float,
        height: Float,
        preContext: String,
        onResult: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        prepare(
            onReady = {
                val context = RecognitionContext.builder()
                    .setWritingArea(WritingArea(width, height))
                    .setPreContext(preContext.takeLast(20))
                    .build()
                recognizer.recognize(ink, context)
                    .addOnSuccessListener { result ->
                        onResult(result.candidates.take(3).map { it.text })
                    }
                    .addOnFailureListener(onFailure)
            },
            onFailure = onFailure
        )
    }

    override fun close() {
        recognizer.close()
    }
}
