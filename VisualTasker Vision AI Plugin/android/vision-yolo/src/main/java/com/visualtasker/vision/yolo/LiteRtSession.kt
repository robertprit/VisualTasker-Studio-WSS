package com.visualtasker.vision.yolo

import android.content.Context
import android.util.Log
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LiteRtSession(
    context: Context,
    assetName: String,
) : AutoCloseable {
    data class TensorInfo(val name: String, val shape: IntArray)

    val accelerator: String
    val nchw: Boolean
    val inputWidth: Int
    val inputHeight: Int
    val inputShape: IntArray
    val outputShapes: List<IntArray>
    val labels: List<String>
    val metadataTask: String

    private val model: CompiledModel
    private val inputBuffers: List<TensorBuffer>
    private val outputBuffers: List<TensorBuffer>
    @Volatile private var closed = false

    init {
        val modelFile = copyAsset(context, assetName)
        val meta = readMetadata(modelFile)
        labels = meta.labels
        metadataTask = meta.task
        val prepared = compileWithFallback(context, modelFile)
        model = prepared.model
        inputBuffers = prepared.inputs
        outputBuffers = prepared.outputs
        accelerator = prepared.accelerator
        inputShape = prepared.inputShape
        nchw = prepared.nchw
        inputHeight = prepared.inputHeight
        inputWidth = prepared.inputWidth
        outputShapes = prepared.outputShapes
        Log.i(TAG, "$assetName auf $accelerator input=${inputShape.toList()} outputs=${outputShapes.map { it.toList() }} nchw=$nchw")
    }

    fun run(input: FloatArray): List<FloatArray> {
        check(!closed) { "LiteRT-Session ist geschlossen" }
        inputBuffers[0].writeFloat(input)
        model.run(inputBuffers, outputBuffers)
        return outputBuffers.map { it.readFloat() }
    }

    override fun close() {
        if (closed) return
        closed = true
        inputBuffers.forEach { runCatching { it.close() } }
        outputBuffers.forEach { runCatching { it.close() } }
        runCatching { model.close() }
    }

    private data class Prepared(
        val model: CompiledModel,
        val inputs: List<TensorBuffer>,
        val outputs: List<TensorBuffer>,
        val accelerator: String,
        val inputShape: IntArray,
        val nchw: Boolean,
        val inputWidth: Int,
        val inputHeight: Int,
        val outputShapes: List<IntArray>,
    )

    private data class Meta(val task: String, val labels: List<String>)

    private fun compileWithFallback(context: Context, modelFile: File): Prepared {
        var last: Throwable? = null
        for ((acc, name) in listOf(Accelerator.GPU to "GPU", Accelerator.CPU to "CPU")) {
            try {
                return compile(context, modelFile, acc, name)
            } catch (error: Throwable) {
                last = error
                Log.w(TAG, "$name fehlgeschlagen: ${error.message}")
            }
        }
        throw IllegalStateException("Modell ${modelFile.name} nicht ladbar", last)
    }

    private fun compile(context: Context, modelFile: File, accelerator: Accelerator, name: String): Prepared {
        val options = CompiledModel.Options(accelerator)
        if (accelerator == Accelerator.GPU) {
            options.gpuOptions = CompiledModel.GpuOptions(
                serializationDir = context.codeCacheDir.absolutePath,
                modelCacheKey = "${modelFile.name}_${modelFile.length()}",
                serializeProgramCache = true,
            )
        } else {
            options.cpuOptions = CompiledModel.CpuOptions(
                numThreads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
            )
        }
        val compiled = CompiledModel.create(modelFile.absolutePath, options)
        val inputs: List<TensorBuffer>
        val outputs: List<TensorBuffer>
        try {
            inputs = compiled.createInputBuffers()
            outputs = compiled.createOutputBuffers()
        } catch (error: Throwable) {
            runCatching { compiled.close() }
            throw error
        }
        try {
            val nativeDims = sequenceOf("args_0", "images", "input", "input_1").firstNotNullOfOrNull { tensorName ->
                runCatching {
                    compiled.getInputTensorType(inputName = tensorName).layout?.dimensions?.toIntArray()?.takeIf { it.isNotEmpty() }
                }.getOrNull()
            } ?: inferSquareRgb(inputs[0])
            val isNchw = nativeDims.size >= 4 && nativeDims[1] == 3 && nativeDims.last() != 3
            val height = if (isNchw) nativeDims[2] else nativeDims.getOrElse(1) { 640 }
            val width = if (isNchw) nativeDims[3] else nativeDims.getOrElse(2) { 640 }
            inputs[0].writeFloat(FloatArray((width * height * 3).coerceAtLeast(1)))
            compiled.run(inputs, outputs)
            val shapes = List(outputs.size) { index ->
                val legacy = if (index == 0) "Identity" else "Identity_$index"
                sequenceOf("output_$index", legacy).firstNotNullOfOrNull { tensorName ->
                    runCatching {
                        compiled.getOutputTensorType(outputName = tensorName).layout?.dimensions?.toIntArray()?.takeIf { it.isNotEmpty() }
                    }.getOrNull()
                } ?: intArrayOf(outputs[index].readFloat().size)
            }
            return Prepared(compiled, inputs, outputs, name, nativeDims, isNchw, width, height, shapes)
        } catch (error: Throwable) {
            inputs.forEach { runCatching { it.close() } }
            outputs.forEach { runCatching { it.close() } }
            runCatching { compiled.close() }
            throw error
        }
    }

    private fun inferSquareRgb(buffer: TensorBuffer): IntArray {
        val count = buffer.readFloat().size
        val side = if (count > 0 && count % 3 == 0) sqrt((count / 3.0)).roundToInt() else 0
        require(side > 0 && side * side * 3 == count) { "Eingabe unbekannt ($count)" }
        return intArrayOf(1, 3, side, side)
    }

    private fun readMetadata(file: File): Meta {
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("metadata.json") ?: zip.getEntry("TFLITE_ULTRALYTICS_METADATA.json")
                if (entry == null) return Meta("", emptyList())
                val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                val names = json.optJSONObject("names")
                val labels = if (names != null) {
                    (0 until names.length()).map { names.optString(it.toString(), "cls$it") }
                } else emptyList()
                Meta(json.optString("task"), labels)
            }
        } catch (_: Throwable) {
            Meta("", emptyList())
        }
    }

    private fun copyAsset(context: Context, assetName: String): File {
        val dest = File(context.filesDir, assetName)
        val assetSize = runCatching { context.assets.openFd(assetName).use { it.length } }.getOrNull()
        if (dest.exists() && assetSize != null && dest.length() == assetSize) return dest
        context.assets.open(assetName).use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest
    }

    companion object {
        private const val TAG = "LiteRtSession"
    }
}
