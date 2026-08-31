package com.visualtasker.wss.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconMotionEngine {
    MATERIAL,
    RIVE
}

object IconMotionConfig {
    var engine: IconMotionEngine = IconMotionEngine.MATERIAL
}

@Composable
fun AnimatedPanelIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    riveArtboard: String? = null,
    riveStateMachine: String? = null
) {
    // Rive-Vorbereitung: gleicher Aufrufpunkt für spätere Engine-Umstellung.
    // Solange keine Rive Runtime integriert ist, rendern wir Material Icons.
    when (IconMotionConfig.engine) {
        IconMotionEngine.MATERIAL -> Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
        IconMotionEngine.RIVE -> Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
    }
    @Suppress("UNUSED_VARIABLE")
    val _rivePlaceholders = riveArtboard to riveStateMachine
}

