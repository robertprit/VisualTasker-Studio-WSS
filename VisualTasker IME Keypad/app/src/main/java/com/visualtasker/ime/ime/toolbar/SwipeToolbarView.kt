package com.visualtasker.ime.ime.toolbar

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import kotlin.math.abs

class SwipeToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(3), dp(2), dp(3), dp(6))
    }
    private val dots = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER
    }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var pageIndex = 0
    private var pages: List<ToolbarPage> = emptyList()
    private var actionListener: ((ToolbarAction) -> Unit)? = null

    init {
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, dp(46)))
        addView(
            dots,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(6), android.view.Gravity.BOTTOM)
        )
        isClickable = true
    }

    fun configure(
        pages: List<ToolbarPage>,
        onAction: (ToolbarAction) -> Unit
    ) {
        this.pages = pages
        actionListener = onAction
        pageIndex = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        renderPage()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.2f) return true
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                if (abs(dx) >= dp(42)) {
                    showPage(pageIndex + if (dx < 0f) 1 else -1, dx < 0f)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    private fun showPage(index: Int, movingForward: Boolean) {
        val next = index.coerceIn(0, pages.lastIndex)
        if (next == pageIndex) return
        pageIndex = next
        content.animate()
            .alpha(0f)
            .translationX(if (movingForward) -dp(24).toFloat() else dp(24).toFloat())
            .setDuration(90)
            .withEndAction {
                renderPage()
                content.translationX = if (movingForward) dp(24).toFloat() else -dp(24).toFloat()
                content.animate().alpha(1f).translationX(0f).setDuration(110).start()
            }
            .start()
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun renderPage() {
        content.removeAllViews()
        val page = pages.getOrNull(pageIndex) ?: return
        page.items.forEach { item ->
            val button = createButton(item)
            content.addView(button, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        content.contentDescription = "${page.name}, Seite ${pageIndex + 1} von ${pages.size}"
        renderDots()
    }

    private fun createButton(item: ToolbarItem): View {
        val clickListener = View.OnClickListener { actionListener?.invoke(item.action) }
        return if (item.androidIcon != null) {
            ImageButton(context).apply {
                setImageResource(item.androidIcon)
                setColorFilter(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                contentDescription = item.contentDescription
                setPadding(dp(9), dp(7), dp(9), dp(7))
                setOnClickListener(clickListener)
                tooltipText = item.contentDescription
            }
        } else {
            AppCompatButton(context).apply {
                text = item.label
                textSize = if (pages.getOrNull(pageIndex)?.items?.size ?: 0 > 10) 10f else 11f
                setTextColor(Color.WHITE)
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(1), 0, dp(1), 0)
                setBackgroundColor(Color.TRANSPARENT)
                contentDescription = item.contentDescription
                setOnClickListener(clickListener)
                tooltipText = item.contentDescription
            }
        }
    }

    private fun renderDots() {
        dots.removeAllViews()
        pages.indices.forEach { index ->
            dots.addView(View(context).apply {
                setBackgroundColor(if (index == pageIndex) 0xFF9B82F3.toInt() else 0xFF625C6E.toInt())
            }, LinearLayout.LayoutParams(dp(if (index == pageIndex) 14 else 6), dp(2)).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
