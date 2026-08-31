package com.visualtasker.wss.grid

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

object GridSystem {
    const val GRID_SIZE_DP_SMALL = 24
    const val GRID_SIZE_DP_LARGE = 48

    fun snapToGrid(value: Float, gridSizeDp: Int): Float {
        return (value / gridSizeDp).roundToInt() * gridSizeDp.toFloat()
    }

    fun snapPosition(position: Offset, gridSizeDp: Int): Offset {
        return Offset(
            x = snapToGrid(position.x, gridSizeDp).coerceAtLeast(0f),
            y = snapToGrid(position.y, gridSizeDp).coerceAtLeast(0f)
        )
    }

    fun autoArrangePositions(
        panelCount: Int,
        screenWidth: Int,
        screenHeight: Int,
        gridSizeDp: Int,
        panelWidth: Int = 288,
        panelHeight: Int = 288
    ): List<Offset> {
        val effectivePanelHeight = panelHeight.coerceAtMost((screenHeight - 48).coerceAtLeast(panelHeight))
        val cols = ((screenWidth - 48) / (panelWidth + gridSizeDp)).coerceAtLeast(1)
        val positions = mutableListOf<Offset>()

        for (i in 0 until panelCount) {
            val col = i % cols
            val row = i / cols
            positions.add(
                Offset(
                    x = (24f + col * (panelWidth + gridSizeDp)),
                    y = (24f + row * (effectivePanelHeight + gridSizeDp))
                )
            )
        }
        return positions
    }
}
