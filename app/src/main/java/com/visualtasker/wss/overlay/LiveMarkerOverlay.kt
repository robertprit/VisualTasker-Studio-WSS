package com.visualtasker.wss.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

internal enum class LiveMarkerMode {
    Point,
    Region,
    Swipe,
    Spline,
    Path,
}

internal data class LiveMarkerPoint(val x: Float, val y: Float)

internal data class LiveMarkerResult(
    val id: String,
    val mode: LiveMarkerMode,
    val points: List<LiveMarkerPoint>,
    val multiGroupId: String? = null,
    val createdAtEpochMs: Long,
) {
    val left: Float = points.minOfOrNull { it.x } ?: 0f
    val top: Float = points.minOfOrNull { it.y } ?: 0f
    val right: Float = points.maxOfOrNull { it.x } ?: left
    val bottom: Float = points.maxOfOrNull { it.y } ?: top
}

internal class LiveMarkerGestureBuilder(private val mode: LiveMarkerMode) {
    private val points = mutableListOf<LiveMarkerPoint>()

    fun start(x: Float, y: Float) {
        points.clear()
        points += LiveMarkerPoint(x, y)
    }

    fun move(x: Float, y: Float) {
        when (mode) {
            LiveMarkerMode.Point -> if (points.isEmpty()) points += LiveMarkerPoint(x, y)
            LiveMarkerMode.Region,
            LiveMarkerMode.Swipe,
            -> if (points.size == 1) points += LiveMarkerPoint(x, y) else points[1] = LiveMarkerPoint(x, y)
            LiveMarkerMode.Spline,
            LiveMarkerMode.Path,
            -> points += LiveMarkerPoint(x, y)
        }
    }

    fun snapshot(): List<LiveMarkerPoint> = points.toList()

    fun finish(x: Float, y: Float): List<LiveMarkerPoint> {
        move(x, y)
        return when (mode) {
            LiveMarkerMode.Point -> points.take(1)
            else -> points.toList()
        }
    }
}

internal object LiveMarkerStore {
    private const val RELATIVE_PATH = "emscript-runtime/markers/live-markers.jsonl"

    fun append(context: Context, marker: LiveMarkerResult): File {
        val target = File(context.filesDir, RELATIVE_PATH)
        target.parentFile?.mkdirs()
        target.appendText(marker.toJson().toString() + "\n")
        return target
    }

    fun load(context: Context): List<LiveMarkerResult> {
        val target = File(context.filesDir, RELATIVE_PATH)
        if (!target.isFile) return emptyList()
        return target.readLines().mapNotNull { line ->
            runCatching { JSONObject(line).toLiveMarkerResult() }.getOrNull()
        }
    }
}

internal class LiveMarkerOverlayView(
    context: Context,
    private val mode: LiveMarkerMode,
    private val multiGroupId: String?,
    private val onMarker: (LiveMarkerResult) -> Unit,
    private val onCancel: () -> Unit,
) : View(context) {
    private val builder = LiveMarkerGestureBuilder(mode)
    private var preview = emptyList<LiveMarkerPoint>()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(79, 195, 247)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(48, 79, 195, 247)
        style = Paint.Style.FILL
    }
    private val cancelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 35, 35, 42)
        style = Paint.Style.FILL
    }
    private val cancelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.argb(22, 0, 0, 0))
        canvas.drawCircle(width - 54f, 54f, 38f, cancelPaint)
        canvas.drawText("×", width - 54f, 68f, cancelTextPaint)
        if (preview.isEmpty()) return
        when (mode) {
            LiveMarkerMode.Point -> canvas.drawCircle(preview.first().x, preview.first().y, 18f, strokePaint)
            LiveMarkerMode.Region -> {
                val start = preview.first()
                val end = preview.last()
                canvas.drawRect(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y), fillPaint)
                canvas.drawRect(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y), strokePaint)
            }
            LiveMarkerMode.Swipe -> drawPolyline(canvas, preview, arrow = true)
            LiveMarkerMode.Spline,
            LiveMarkerMode.Path,
            -> drawPolyline(canvas, preview, arrow = false)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && event.x >= width - 110f && event.y <= 110f) {
            onCancel()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> builder.start(event.x, event.y)
            MotionEvent.ACTION_MOVE -> builder.move(event.x, event.y)
            MotionEvent.ACTION_UP -> {
                preview = builder.finish(event.x, event.y)
                val now = System.currentTimeMillis()
                onMarker(
                    LiveMarkerResult(
                        id = "live:${mode.name.lowercase()}:$now",
                        mode = mode,
                        points = preview,
                        multiGroupId = multiGroupId,
                        createdAtEpochMs = now,
                    ),
                )
            }
            MotionEvent.ACTION_CANCEL -> preview = emptyList()
        }
        preview = builder.snapshot()
        invalidate()
        return true
    }

    private fun drawPolyline(canvas: Canvas, points: List<LiveMarkerPoint>, arrow: Boolean) {
        if (points.size < 2) return
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, strokePaint)
        if (arrow) canvas.drawCircle(points.last().x, points.last().y, 12f, strokePaint)
    }
}

private fun LiveMarkerResult.toJson(): JSONObject = JSONObject()
    .put("schemaVersion", 1)
    .put("id", id)
    .put("mode", mode.name)
    .put("multiGroupId", multiGroupId ?: "")
    .put("createdAt", createdAtEpochMs)
    .put("points", JSONArray().apply {
        points.forEach { point -> put(JSONObject().put("x", point.x.toDouble()).put("y", point.y.toDouble())) }
    })

private fun JSONObject.toLiveMarkerResult(): LiveMarkerResult {
    val array = getJSONArray("points")
    val points = buildList {
        for (index in 0 until array.length()) {
            val point = array.getJSONObject(index)
            add(LiveMarkerPoint(point.getDouble("x").toFloat(), point.getDouble("y").toFloat()))
        }
    }
    return LiveMarkerResult(
        id = getString("id"),
        mode = LiveMarkerMode.valueOf(getString("mode")),
        points = points,
        multiGroupId = optString("multiGroupId").takeIf { it.isNotBlank() },
        createdAtEpochMs = getLong("createdAt"),
    )
}
