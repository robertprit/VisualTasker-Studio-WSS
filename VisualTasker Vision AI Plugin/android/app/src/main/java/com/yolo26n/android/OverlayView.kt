package com.yolo26n.android
import com.visualtasker.vision.yolo.FrameResult
import com.visualtasker.vision.yolo.Detection
import com.visualtasker.vision.yolo.YoloPredictor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.max

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.4f)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = sp(12f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val kptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
    }
    private val overlayPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val classPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(18f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private var result: FrameResult? = null
    private val mapRect = RectF()

    fun setResult(value: FrameResult) {
        result = value
        postInvalidateOnAnimation()
    }

    fun clear() {
        result = null
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frame = result ?: return
        if (frame.imageWidth <= 0 || frame.imageHeight <= 0) return
        prepareMapping(frame.imageWidth, frame.imageHeight)

        frame.overlay?.let {
            canvas.drawBitmap(it, null, mapRect, overlayPaint)
        }

        for (detection in frame.detections) {
            val color = solidColor(detection.classId)
            boxPaint.color = color
            fillPaint.color = color
            if (detection.polygon.size >= 4) {
                val path = Path()
                detection.polygon.forEachIndexed { index, point ->
                    val (x, y) = map(point.x, point.y)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, boxPaint)
            } else {
                val box = mappedBox(detection.box)
                canvas.drawRect(box, boxPaint)
                drawLabel(canvas, detection, box.left, box.top, color)
            }
            if (detection.polygon.size >= 4) {
                val origin = detection.polygon.first()
                val (x, y) = map(origin.x, origin.y)
                drawLabel(canvas, detection, x, y, color)
            }
            if (detection.keypoints.isNotEmpty()) {
                drawPose(canvas, detection)
            }
        }

        if (frame.topClasses.isNotEmpty() && frame.detections.isEmpty()) {
            var y = mapRect.top + dp(36f)
            for (item in frame.topClasses) {
                val line = "${item.label}  ${"%.0f".format(item.score * 100)}%"
                canvas.drawText(line, mapRect.left + dp(16f), y, classPaint)
                y += dp(24f)
            }
        }
    }

    private fun drawPose(canvas: Canvas, detection: Detection) {
        val kpts = detection.keypoints
        bonePaint.color = Color.CYAN
        for ((a, b) in SKELETON) {
            if (a >= kpts.size || b >= kpts.size) continue
            if (kpts[a].confidence < 0.3f || kpts[b].confidence < 0.3f) continue
            val (x1, y1) = map(kpts[a].x, kpts[a].y)
            val (x2, y2) = map(kpts[b].x, kpts[b].y)
            canvas.drawLine(x1, y1, x2, y2, bonePaint)
        }
        kpts.forEachIndexed { index, keypoint ->
            if (keypoint.confidence < 0.3f) return@forEachIndexed
            kptPaint.color = if (index == 0) Color.YELLOW else solidColor(index)
            val (x, y) = map(keypoint.x, keypoint.y)
            canvas.drawCircle(x, y, dp(4.5f), kptPaint)
        }
    }

    private fun drawLabel(canvas: Canvas, detection: Detection, left: Float, top: Float, color: Int) {
        val label = "${detection.label} ${"%.0f".format(detection.confidence * 100)}%"
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.descent() - textPaint.ascent()
        val labelTop = (top - textHeight - dp(6f)).coerceAtLeast(0f)
        fillPaint.color = color
        canvas.drawRect(left, labelTop, left + textWidth + dp(10f), labelTop + textHeight + dp(6f), fillPaint)
        canvas.drawText(label, left + dp(5f), labelTop + dp(3f) - textPaint.ascent(), textPaint)
    }

    private fun prepareMapping(imageWidth: Int, imageHeight: Int) {
        val scale = max(width / imageWidth.toFloat(), height / imageHeight.toFloat())
        val drawnW = imageWidth * scale
        val drawnH = imageHeight * scale
        val dx = (width - drawnW) / 2f
        val dy = (height - drawnH) / 2f
        mapRect.set(dx, dy, dx + drawnW, dy + drawnH)
    }

    private fun map(x: Float, y: Float): Pair<Float, Float> {
        val scaleX = mapRect.width() / (result?.imageWidth?.toFloat() ?: 1f)
        val scaleY = mapRect.height() / (result?.imageHeight?.toFloat() ?: 1f)
        return x * scaleX + mapRect.left to y * scaleY + mapRect.top
    }

    private fun mappedBox(box: RectF): RectF {
        val (l, t) = map(box.left, box.top)
        val (r, b) = map(box.right, box.bottom)
        return RectF(l, t, r, b)
    }

    private fun solidColor(classId: Int): Int = YoloPredictor.colorWithAlpha(classId, 255)

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    companion object {
        private val SKELETON = listOf(
            0 to 1, 0 to 2, 1 to 3, 2 to 4,
            5 to 6, 5 to 7, 7 to 9, 6 to 8, 8 to 10,
            5 to 11, 6 to 12, 11 to 12,
            11 to 13, 13 to 15, 12 to 14, 14 to 16,
        )
    }
}
