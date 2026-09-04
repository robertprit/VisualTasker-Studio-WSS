package com.visualtasker.wss.visual.material

import androidx.compose.ui.graphics.Color
import com.visualtasker.wss.ui.theme.M3EColors
import com.visualtasker.wss.visual.descriptor.Emphasis
import com.visualtasker.wss.visual.descriptor.MotionRole
import com.visualtasker.wss.visual.descriptor.OpacityRole
import com.visualtasker.wss.visual.descriptor.OutlineRole
import com.visualtasker.wss.visual.descriptor.ShapeRole
import com.visualtasker.wss.visual.descriptor.SurfaceRole
import com.visualtasker.wss.visual.descriptor.VisualDescriptor

data class MaterialVisualStyle(
    val fill: Color,
    val content: Color,
    val outline: Color,
    val outlineWidthDp: Float,
    val alpha: Float,
    val cornerRadiusDp: Float,
    val motionRole: MotionRole,
)

object MaterialVisualResolver {
    fun resolve(descriptor: VisualDescriptor): MaterialVisualStyle =
        MaterialVisualStyle(
            fill = descriptor.surfaceRole.fillColor(),
            content = contentColorFor(descriptor.surfaceRole),
            outline = descriptor.outlineRole.outlineColor(),
            outlineWidthDp = descriptor.outlineRole.outlineWidth(descriptor.emphasis),
            alpha = descriptor.opacityRole.alpha(),
            cornerRadiusDp = descriptor.shapeRole.cornerRadius(),
            motionRole = descriptor.motionRole,
        )

    private fun SurfaceRole.fillColor(): Color =
        when (this) {
            SurfaceRole.Event -> M3EColors.Gold
            SurfaceRole.Action -> M3EColors.Mint
            SurfaceRole.Condition -> M3EColors.Violet
            SurfaceRole.Loop -> M3EColors.Sky
            SurfaceRole.Value -> M3EColors.Auroraint
            SurfaceRole.WorldEntity -> M3EColors.Oceanneon
            SurfaceRole.Observation -> M3EColors.Limepop
            SurfaceRole.Resource -> M3EColors.Amber
            SurfaceRole.Ambiguity -> M3EColors.Pink
            SurfaceRole.Runtime -> M3EColors.Ultraviolet
            SurfaceRole.Group -> M3EColors.DarkPanelElevated
            SurfaceRole.Neutral -> M3EColors.DarkPanel
        }

    private fun contentColorFor(surfaceRole: SurfaceRole): Color =
        when (surfaceRole) {
            SurfaceRole.Group,
            SurfaceRole.Neutral -> Color(0xFFECE6F3)
            else -> Color(0xFF111016)
        }

    private fun OutlineRole.outlineColor(): Color =
        when (this) {
            OutlineRole.None -> Color.Transparent
            OutlineRole.Focused -> M3EColors.Oceanneon
            OutlineRole.Selected -> Color(0xFF40C4FF)
            OutlineRole.Proposal -> M3EColors.Pink
            OutlineRole.Warning -> M3EColors.Amber
            OutlineRole.Invalid -> M3EColors.Cherryfire
            OutlineRole.RuntimeActive -> M3EColors.Limepop
        }

    private fun OutlineRole.outlineWidth(emphasis: Emphasis): Float =
        when {
            this == OutlineRole.None -> 0f
            emphasis == Emphasis.Critical -> 3f
            emphasis == Emphasis.High -> 2.4f
            else -> 1.6f
        }

    private fun OpacityRole.alpha(): Float =
        when (this) {
            OpacityRole.Normal -> 1f
            OpacityRole.Muted -> 0.68f
            OpacityRole.Disabled -> 0.42f
            OpacityRole.Historical -> 0.56f
        }

    private fun ShapeRole.cornerRadius(): Float =
        when (this) {
            ShapeRole.Capsule,
            ShapeRole.NotchedValue -> 999f
            ShapeRole.Diamond,
            ShapeRole.Hexagon,
            ShapeRole.Connector -> 4f
            ShapeRole.GroupRegion -> 14f
            ShapeRole.RoundedRect,
            ShapeRole.Row -> 8f
        }
}
