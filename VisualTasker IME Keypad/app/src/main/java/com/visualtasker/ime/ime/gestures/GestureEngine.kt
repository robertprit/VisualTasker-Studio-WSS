package com.visualtasker.ime.ime.gestures

data class GestureConfig(
    val doubleTapWindowMs: Long = 400L,
    val longPressEnabled: Boolean = true,
    val swipeEnabled: Boolean = true,
    val stickyModifierEnabled: Boolean = true,
    val oneHandModeEnabled: Boolean = false
)

class GestureEngine(private val config: GestureConfig = GestureConfig()) {
    private var lastTapAt: Long = 0L

    fun registerTap(now: Long = System.currentTimeMillis()): Boolean {
        val isDoubleTap = now - lastTapAt in 1..config.doubleTapWindowMs
        lastTapAt = now
        return isDoubleTap
    }

    fun isSwipeEnabled(): Boolean = config.swipeEnabled
    fun isLongPressEnabled(): Boolean = config.longPressEnabled
    fun isStickyModifierEnabled(): Boolean = config.stickyModifierEnabled
    fun isOneHandModeEnabled(): Boolean = config.oneHandModeEnabled
}
