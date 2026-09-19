package com.visualtasker.vision.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class YoloPredictor(
    context: Context,
    val task: YoloTask,
    private val confThreshold: Float = 0.2f,
    private val iouThreshold: Float = 0.45f,
    private val maxDetections: Int = 30,
) : AutoCloseable {
    val session = LiteRtSession(context, task.asset)
    val labels: List<String> = session.labels
    val accelerator: String get() = session.accelerator

    private val inputWidth = session.inputWidth
    private val inputHeight = session.inputHeight
    private val pixels = IntArray(inputWidth * inputHeight)
    private val input = FloatArray(inputWidth * inputHeight * 3)
    private val canvasBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val centerCrop = task == YoloTask.CLASSIFY

    fun predict(bitmap: Bitmap): FrameResult {
        val letterbox = drawLetterbox(bitmap)
        fillInput()
        val outputs = session.run(input)
        return when (task) {
            YoloTask.DETECT -> decodeDetect(outputs[0], letterbox, bitmap)
            YoloTask.POSE -> decodePose(outputs[0], letterbox, bitmap)
            YoloTask.SEGMENT -> decodeSegment(outputs, letterbox, bitmap)
            YoloTask.SEMANTIC -> decodeSemantic(outputs[0], letterbox, bitmap)
            YoloTask.OBB -> decodeObb(outputs[0], letterbox, bitmap)
            YoloTask.CLASSIFY -> decodeClassify(outputs[0], bitmap)
            YoloTask.DEPTH -> decodeDepth(outputs[0], letterbox, bitmap)
        }
    }

    private data class Letterbox(
        val gain: Float,
        val padX: Float,
        val padY: Float,
        val padRight: Float,
        val padBottom: Float,
        val srcW: Int,
        val srcH: Int,
    )

    private fun drawLetterbox(source: Bitmap): Letterbox {
        val scaleX = inputWidth / source.width.toFloat()
        val scaleY = inputHeight / source.height.toFloat()
        val gain = if (centerCrop) max(scaleX, scaleY) else min(scaleX, scaleY)
        val resizedW = (source.width * gain).roundToInt()
        val resizedH = (source.height * gain).roundToInt()
        val padW = inputWidth - resizedW
        val padH = inputHeight - resizedH
        val padX = (padW / 2f - 0.1f).roundToInt().toFloat()
        val padY = (padH / 2f - 0.1f).roundToInt().toFloat()
        val padRight = (padW / 2f + 0.1f).roundToInt().toFloat()
        val padBottom = (padH / 2f + 0.1f).roundToInt().toFloat()
        Canvas(canvasBitmap).apply {
            drawColor(Color.rgb(114, 114, 114))
            save()
            translate(padX, padY)
            scale(gain, gain)
            drawBitmap(source, 0f, 0f, bitmapPaint)
            restore()
        }
        return Letterbox(gain, padX, padY, padRight, padBottom, source.width, source.height)
    }

    private fun fillInput() {
        canvasBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val count = pixels.size
        if (session.nchw) {
            var r = 0
            var g = count
            var b = count * 2
            for (pixel in pixels) {
                input[r++] = ((pixel shr 16) and 0xFF) / 255f
                input[g++] = ((pixel shr 8) and 0xFF) / 255f
                input[b++] = (pixel and 0xFF) / 255f
            }
        } else {
            var i = 0
            for (pixel in pixels) {
                input[i++] = ((pixel shr 16) and 0xFF) / 255f
                input[i++] = ((pixel shr 8) and 0xFF) / 255f
                input[i++] = (pixel and 0xFF) / 255f
            }
        }
    }

    private fun decodeDetect(flat: FloatArray, lb: Letterbox, source: Bitmap): FrameResult {
        val shape = session.outputShapes.first()
        val (features, anchors, featuresFirst) = headLayout(shape, expectedMinFeatures = 6)
        val classCount = (features - 4).coerceAtLeast(1)
        val candidates = ArrayList<Detection>(64)
        var maxScore = 0f
        for (anchor in 0 until anchors) {
            var bestClass = 0
            var best = value(flat, 4, anchor, anchors, featuresFirst)
            for (cls in 1 until classCount) {
                val score = value(flat, 4 + cls, anchor, anchors, featuresFirst)
                if (score > best) {
                    best = score
                    bestClass = cls
                }
            }
            maxScore = max(maxScore, best)
            if (best < confThreshold) continue
            val box = xywhBox(
                value(flat, 0, anchor, anchors, featuresFirst),
                value(flat, 1, anchor, anchors, featuresFirst),
                value(flat, 2, anchor, anchors, featuresFirst),
                value(flat, 3, anchor, anchors, featuresFirst),
            )
            val mapped = mapRect(box, lb) ?: continue
            candidates += Detection(bestClass, label(bestClass), best, mapped)
        }
        val kept = nms(candidates)
        Log.d(TAG, "detect candidates=${candidates.size} kept=${kept.size} maxScore=${"%.3f".format(maxScore)}")
        return FrameResult(task, source.width, source.height, kept, summary = "${kept.size} Objekte")
    }

    private fun decodePose(flat: FloatArray, lb: Letterbox, source: Bitmap): FrameResult {
        val shape = session.outputShapes.first()
        val (features, anchors, featuresFirst) = headLayout(shape, expectedMinFeatures = 8)
        val kptStart = if ((features - 6) % 3 == 0 && features >= 57) 6 else 5
        val kptCount = ((features - kptStart) / 3).coerceAtLeast(0)
        val end2end = !featuresFirst && anchors <= 300
        val candidates = ArrayList<Detection>(32)
        for (anchor in 0 until anchors) {
            val conf = value(flat, 4, anchor, anchors, featuresFirst)
            if (conf < confThreshold) continue
            val box = if (end2end) {
                RectF(
                    value(flat, 0, anchor, anchors, featuresFirst),
                    value(flat, 1, anchor, anchors, featuresFirst),
                    value(flat, 2, anchor, anchors, featuresFirst),
                    value(flat, 3, anchor, anchors, featuresFirst),
                )
            } else {
                xywhBox(
                    value(flat, 0, anchor, anchors, featuresFirst),
                    value(flat, 1, anchor, anchors, featuresFirst),
                    value(flat, 2, anchor, anchors, featuresFirst),
                    value(flat, 3, anchor, anchors, featuresFirst),
                )
            }
            val mapped = mapRect(box, lb) ?: continue
            val keypoints = ArrayList<Keypoint>(kptCount)
            for (k in 0 until kptCount) {
                val kx = value(flat, kptStart + k * 3, anchor, anchors, featuresFirst)
                val ky = value(flat, kptStart + k * 3 + 1, anchor, anchors, featuresFirst)
                val kc = value(flat, kptStart + k * 3 + 2, anchor, anchors, featuresFirst)
                val (x, y) = mapPoint(kx, ky, lb, box)
                keypoints += Keypoint(x, y, kc)
            }
            candidates += Detection(0, label(0), conf, mapped, keypoints)
        }
        val kept = nms(candidates)
        return FrameResult(task, source.width, source.height, kept, summary = "${kept.size} Personen")
    }

    private fun decodeSegment(outputs: List<FloatArray>, lb: Letterbox, source: Bitmap): FrameResult {
        val detIndex = session.outputShapes.indexOfFirst { it.size == 3 }.takeIf { it >= 0 } ?: 0
        val protoIndex = session.outputShapes.indexOfFirst { it.size == 4 }.takeIf { it >= 0 } ?: 1
        val detShape = session.outputShapes[detIndex]
        val protoShape = session.outputShapes.getOrElse(protoIndex) { intArrayOf(1, 32, 160, 160) }
        val (features, anchors, featuresFirst) = headLayout(detShape, expectedMinFeatures = 36)
        val protoNchw = protoShape.size == 4 && protoShape[1] == 32
        val maskC = if (protoNchw) protoShape[1] else protoShape.last()
        val maskH = if (protoNchw) protoShape[2] else protoShape[1]
        val maskW = if (protoNchw) protoShape[3] else protoShape[2]
        val classCount = (features - 4 - maskC).coerceAtLeast(1)
        val det = outputs[detIndex]
        val proto = outputs.getOrElse(protoIndex) { FloatArray(0) }
        val raw = ArrayList<SegCandidate>(32)
        for (anchor in 0 until anchors) {
            var bestClass = 0
            var best = value(det, 4, anchor, anchors, featuresFirst)
            for (cls in 1 until classCount) {
                val score = value(det, 4 + cls, anchor, anchors, featuresFirst)
                if (score > best) {
                    best = score
                    bestClass = cls
                }
            }
            if (best < confThreshold) continue
            val box = xywhBox(
                value(det, 0, anchor, anchors, featuresFirst),
                value(det, 1, anchor, anchors, featuresFirst),
                value(det, 2, anchor, anchors, featuresFirst),
                value(det, 3, anchor, anchors, featuresFirst),
            )
            val mapped = mapRect(box, lb) ?: continue
            val coeffs = FloatArray(maskC) { c -> value(det, 4 + classCount + c, anchor, anchors, featuresFirst) }
            raw += SegCandidate(Detection(bestClass, label(bestClass), best, mapped), coeffs, box)
        }
        val kept = nms(raw.map { it.detection }).take(12)
        val keptRaw = kept.mapNotNull { detBox -> raw.find { it.detection === detBox || (it.detection.box == detBox.box && it.detection.classId == detBox.classId) } }
        val overlay = if (proto.isNotEmpty()) buildInstanceMask(keptRaw, proto, maskC, maskH, maskW, protoNchw, lb) else null
        return FrameResult(task, source.width, source.height, kept, overlay, summary = "${kept.size} Masken")
    }

    private data class SegCandidate(val detection: Detection, val coeffs: FloatArray, val modelBox: RectF)

    private fun buildInstanceMask(
        detections: List<SegCandidate>,
        proto: FloatArray,
        maskC: Int,
        maskH: Int,
        maskW: Int,
        nchw: Boolean,
        lb: Letterbox,
    ): Bitmap? {
        if (detections.isEmpty() || maskW <= 0 || maskH <= 0) return null
        val left = (lb.padX / inputWidth * maskW).roundToInt().coerceIn(0, maskW)
        val top = (lb.padY / inputHeight * maskH).roundToInt().coerceIn(0, maskH)
        val right = (maskW - lb.padRight / inputWidth * maskW).roundToInt().coerceIn(0, maskW)
        val bottom = (maskH - lb.padBottom / inputHeight * maskH).roundToInt().coerceIn(0, maskH)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        val pixelsOut = IntArray(width * height)
        val plane = maskW * maskH
        for (item in detections) {
            val color = colorWithAlpha(item.detection.classId, 140)
            for (y in 0 until height) {
                val py = y + top
                for (x in 0 until width) {
                    val px = x + left
                    var acc = 0f
                    for (c in 0 until maskC) {
                        val protoVal = if (nchw) proto[c * plane + py * maskW + px] else proto[(py * maskW + px) * maskC + c]
                        acc += item.coeffs[c] * protoVal
                    }
                    if (1f / (1f + exp(-acc)) > 0.5f) {
                        pixelsOut[y * width + x] = color
                    }
                }
            }
        }
        return Bitmap.createBitmap(pixelsOut, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun decodeSemantic(flat: FloatArray, lb: Letterbox, source: Bitmap): FrameResult {
        val shape = session.outputShapes.first()
        val nchw = shape.size == 4 && (shape[1] <= shape.getOrElse(3) { 0 } || shape[1] == labels.size)
        val classCount = when {
            shape.size == 3 -> labels.size.coerceAtLeast(2)
            nchw -> shape[1]
            else -> shape.last()
        }
        val maskH = if (shape.size == 3) shape[1] else if (nchw) shape[2] else shape[1]
        val maskW = if (shape.size == 3) shape[2] else if (nchw) shape[3] else shape[2]
        val left = (lb.padX / inputWidth * maskW).roundToInt().coerceIn(0, maskW)
        val top = (lb.padY / inputHeight * maskH).roundToInt().coerceIn(0, maskH)
        val right = (maskW - lb.padRight / inputWidth * maskW).roundToInt().coerceIn(0, maskW)
        val bottom = (maskH - lb.padBottom / inputHeight * maskH).roundToInt().coerceIn(0, maskH)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        val pixelsOut = IntArray(width * height)
        val plane = maskW * maskH
        val counts = IntArray(classCount)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sx = x + left
                val sy = y + top
                var best = 0
                var bestScore = Float.NEGATIVE_INFINITY
                if (shape.size == 3) {
                    best = flat[sy * maskW + sx].toInt().coerceIn(0, classCount - 1)
                } else {
                    for (c in 0 until classCount) {
                        val score = if (nchw) flat[c * plane + sy * maskW + sx] else flat[(sy * maskW + sx) * classCount + c]
                        if (score > bestScore) {
                            bestScore = score
                            best = c
                        }
                    }
                }
                counts[best]++
                pixelsOut[y * width + x] = colorWithAlpha(best, 150)
            }
        }
        val overlay = Bitmap.createBitmap(pixelsOut, width, height, Bitmap.Config.ARGB_8888)
        val topClasses = counts.indices.filter { counts[it] > 0 }
            .sortedByDescending { counts[it] }
            .take(3)
            .map { ClassScore(label(it), counts[it] / (width * height).toFloat()) }
        return FrameResult(task, source.width, source.height, overlay = overlay, topClasses = topClasses, summary = topClasses.joinToString { it.label })
    }

    private fun decodeObb(flat: FloatArray, lb: Letterbox, source: Bitmap): FrameResult {
        val shape = session.outputShapes.first()
        val (features, anchors, featuresFirst) = headLayout(shape, expectedMinFeatures = 6)
        val classCount = (features - 5).coerceAtLeast(1)
        val candidates = ArrayList<Detection>(32)
        for (anchor in 0 until anchors) {
            var bestClass = 0
            var best = value(flat, 4, anchor, anchors, featuresFirst)
            for (cls in 1 until classCount) {
                val score = value(flat, 4 + cls, anchor, anchors, featuresFirst)
                if (score > best) {
                    best = score
                    bestClass = cls
                }
            }
            if (best < confThreshold) continue
            val cx = value(flat, 0, anchor, anchors, featuresFirst)
            val cy = value(flat, 1, anchor, anchors, featuresFirst)
            val w = value(flat, 2, anchor, anchors, featuresFirst)
            val h = value(flat, 3, anchor, anchors, featuresFirst)
            val angle = value(flat, 4 + classCount, anchor, anchors, featuresFirst)
            val polygon = obbPolygon(cx, cy, w, h, angle, lb)
            if (polygon.size < 4) continue
            val box = RectF(
                polygon.minOf { it.x },
                polygon.minOf { it.y },
                polygon.maxOf { it.x },
                polygon.maxOf { it.y },
            )
            candidates += Detection(bestClass, label(bestClass), best, box, polygon = polygon)
        }
        val kept = nms(candidates)
        return FrameResult(task, source.width, source.height, kept, summary = "${kept.size} OBB")
    }

    private fun decodeClassify(scores: FloatArray, source: Bitmap): FrameResult {
        val ranked = scores.indices.sortedByDescending { scores[it] }.take(5).map {
            ClassScore(label(it), scores[it])
        }
        val summary = ranked.firstOrNull()?.let { "${it.label} ${"%.0f".format(it.score * 100)}%" } ?: "—"
        return FrameResult(task, source.width, source.height, topClasses = ranked, summary = summary)
    }

    private fun decodeDepth(flat: FloatArray, lb: Letterbox, source: Bitmap): FrameResult {
        val shape = session.outputShapes.first()
        val channelLast = shape.size == 4 && shape.last() == 1
        val depthH = if (channelLast) shape[1] else shape.getOrElse(shape.size - 2) { inputHeight }
        val depthW = if (channelLast) shape[2] else shape.last()
        val left = (lb.padX / inputWidth * depthW).roundToInt().coerceIn(0, depthW)
        val top = (lb.padY / inputHeight * depthH).roundToInt().coerceIn(0, depthH)
        val right = (depthW - lb.padRight / inputWidth * depthW).roundToInt().coerceIn(0, depthW)
        val bottom = (depthH - lb.padBottom / inputHeight * depthH).roundToInt().coerceIn(0, depthH)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        val values = FloatArray(width * height)
        var i = 0
        for (y in 0 until height) {
            val row = (y + top) * depthW
            for (x in 0 until width) {
                val v = flat[row + left + x]
                values[i++] = v
                if (v < minV) minV = v
                if (v > maxV) maxV = v
            }
        }
        val span = (maxV - minV).takeIf { it > 1e-6f } ?: 1f
        val pixelsOut = IntArray(values.size) { idx ->
            depthColor(((values[idx] - minV) / span).coerceIn(0f, 1f))
        }
        val overlay = Bitmap.createBitmap(pixelsOut, width, height, Bitmap.Config.ARGB_8888)
        return FrameResult(task, source.width, source.height, overlay = overlay, summary = "Tiefe")
    }

    private fun headLayout(shape: IntArray, expectedMinFeatures: Int): Triple<Int, Int, Boolean> {
        val d1 = shape.getOrElse(1) { 0 }
        val d2 = shape.getOrElse(2) { 0 }
        val featuresFirst = d1 >= expectedMinFeatures && d2 > 300
        return if (featuresFirst) Triple(d1, d2, true) else Triple(d2.coerceAtLeast(1), d1.coerceAtLeast(1), false)
    }

    private fun value(flat: FloatArray, feature: Int, anchor: Int, anchors: Int, featuresFirst: Boolean): Float {
        val index = if (featuresFirst) feature * anchors + anchor else anchor * (flat.size / anchors.coerceAtLeast(1)) + feature
        return if (index in flat.indices) flat[index] else 0f
    }

    private fun xywhBox(cx: Float, cy: Float, w: Float, h: Float) =
        RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)

    private fun normalized(rect: RectF): Boolean {
        val maxC = max(max(abs(rect.left), abs(rect.top)), max(abs(rect.right), abs(rect.bottom)))
        return maxC <= 2f
    }

    private fun mapRect(model: RectF, lb: Letterbox): RectF? {
        val scaled = if (normalized(model)) {
            RectF(model.left * inputWidth, model.top * inputHeight, model.right * inputWidth, model.bottom * inputHeight)
        } else model
        val left = (scaled.left - lb.padX) / lb.gain
        val top = (scaled.top - lb.padY) / lb.gain
        val right = (scaled.right - lb.padX) / lb.gain
        val bottom = (scaled.bottom - lb.padY) / lb.gain
        val rect = RectF(min(left, right), min(top, bottom), max(left, right), max(top, bottom))
        return rect.takeIf { it.width() > 1f && it.height() > 1f }
    }

    private fun mapPoint(x: Float, y: Float, lb: Letterbox, sample: RectF): Pair<Float, Float> {
        val norm = max(abs(x), abs(y)) <= 2f || normalized(sample)
        val mx = if (norm) x * inputWidth else x
        val my = if (norm) y * inputHeight else y
        return (mx - lb.padX) / lb.gain to (my - lb.padY) / lb.gain
    }

    private fun obbPolygon(cx: Float, cy: Float, w: Float, h: Float, angle: Float, lb: Letterbox): List<PointF> {
        val sample = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        val norm = normalized(sample)
        val mcx = if (norm) cx * inputWidth else cx
        val mcy = if (norm) cy * inputHeight else cy
        val mw = if (norm) w * inputWidth else w
        val mh = if (norm) h * inputHeight else h
        val cosA = cos(angle)
        val sinA = sin(angle)
        val dx = mw / 2f
        val dy = mh / 2f
        val local = arrayOf(-dx to -dy, dx to -dy, dx to dy, -dx to dy)
        return local.map { (px, py) ->
            val mx = mcx + px * cosA - py * sinA
            val my = mcy + px * sinA + py * cosA
            PointF((mx - lb.padX) / lb.gain, (my - lb.padY) / lb.gain)
        }
    }

    private fun nms(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }
        val kept = ArrayList<Detection>(min(maxDetections, sorted.size))
        val used = BooleanArray(sorted.size)
        for (i in sorted.indices) {
            if (used[i]) continue
            val a = sorted[i]
            kept += a
            if (kept.size >= maxDetections) break
            for (j in i + 1 until sorted.size) {
                if (used[j] || sorted[j].classId != a.classId) continue
                if (iou(a.box, sorted[j].box) > iouThreshold) used[j] = true
            }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val inter = max(0f, right - left) * max(0f, bottom - top)
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun label(index: Int) = labels.getOrElse(index) { "cls$index" }

    override fun close() {
        session.close()
        canvasBitmap.recycle()
    }

    companion object {
        private const val TAG = "YoloPredictor"

        fun colorWithAlpha(classId: Int, alpha: Int): Int {
            val hue = ((classId * 47) % 360).toFloat()
            val rgb = Color.HSVToColor(floatArrayOf(hue, 0.85f, 1f))
            return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        }

        fun depthColor(t: Float): Int {
            val hue = (0.7f - t * 0.7f) * 360f
            return Color.HSVToColor(180, floatArrayOf(hue, 0.9f, 1f))
        }
    }
}
