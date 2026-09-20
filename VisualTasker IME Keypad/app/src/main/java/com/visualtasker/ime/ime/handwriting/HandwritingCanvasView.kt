package com.visualtasker.ime.ime.handwriting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.recognition.Ink

class HandwritingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5E566B.toInt()
        strokeWidth = dp(1f)
    }
    private val paths = mutableListOf<Path>()
    private var activePath: Path? = null
    private var inkBuilder = Ink.builder()
    private var strokeBuilder: Ink.Stroke.Builder? = null

    val hasInk: Boolean
        get() = paths.isNotEmpty() || activePath != null

    init {
        setBackgroundColor(0xFF151219.toInt())
        minimumHeight = dp(150f).toInt()
        contentDescription = "Handschrift-Zeichenfläche"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val baseline = height * 0.72f
        canvas.drawLine(dp(12f), baseline, width - dp(12f), baseline, guidePaint)
        paths.forEach { canvas.drawPath(it, strokePaint) }
        activePath?.let { canvas.drawPath(it, strokePaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePath = Path().apply { moveTo(event.x, event.y) }
                strokeBuilder = Ink.Stroke.builder().apply {
                    addPoint(Ink.Point.create(event.x, event.y, event.eventTime))
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val path = activePath ?: return false
                val stroke = strokeBuilder ?: return false
                for (index in 0 until event.historySize) {
                    val x = event.getHistoricalX(index)
                    val y = event.getHistoricalY(index)
                    path.lineTo(x, y)
                    stroke.addPoint(Ink.Point.create(x, y, event.getHistoricalEventTime(index)))
                }
                path.lineTo(event.x, event.y)
                stroke.addPoint(Ink.Point.create(event.x, event.y, event.eventTime))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                activePath?.lineTo(event.x, event.y)
                strokeBuilder?.addPoint(Ink.Point.create(event.x, event.y, event.eventTime))
                activePath?.let(paths::add)
                strokeBuilder?.build()?.let(inkBuilder::addStroke)
                activePath = null
                strokeBuilder = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePath = null
                strokeBuilder = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun snapshotInk(): Ink = inkBuilder.build()

    fun clearInk() {
        paths.clear()
        activePath = null
        strokeBuilder = null
        inkBuilder = Ink.builder()
        invalidate()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
