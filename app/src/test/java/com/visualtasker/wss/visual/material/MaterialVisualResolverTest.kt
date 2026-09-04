package com.visualtasker.wss.visual.material

import androidx.compose.ui.graphics.Color
import com.visualtasker.wss.visual.descriptor.Emphasis
import com.visualtasker.wss.visual.descriptor.MotionRole
import com.visualtasker.wss.visual.descriptor.OpacityRole
import com.visualtasker.wss.visual.descriptor.OutlineRole
import com.visualtasker.wss.visual.descriptor.ShapeRole
import com.visualtasker.wss.visual.descriptor.SurfaceRole
import com.visualtasker.wss.visual.descriptor.VisualDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialVisualResolverTest {
    @Test
    fun resolvesDescriptorWithoutChangingSemanticRoles() {
        val style = MaterialVisualResolver.resolve(
            VisualDescriptor(
                shapeRole = ShapeRole.Diamond,
                surfaceRole = SurfaceRole.Condition,
                outlineRole = OutlineRole.RuntimeActive,
                motionRole = MotionRole.RuntimeFlow,
                emphasis = Emphasis.High,
            )
        )

        assertTrue(style.fill != Color.Transparent)
        assertTrue(style.outline != Color.Transparent)
        assertEquals(2.4f, style.outlineWidthDp)
        assertEquals(4f, style.cornerRadiusDp)
        assertEquals(MotionRole.RuntimeFlow, style.motionRole)
    }

    @Test
    fun keepsDisabledAndCriticalStatesExplicit() {
        val style = MaterialVisualResolver.resolve(
            VisualDescriptor(
                shapeRole = ShapeRole.RoundedRect,
                surfaceRole = SurfaceRole.Action,
                outlineRole = OutlineRole.Invalid,
                opacityRole = OpacityRole.Disabled,
                emphasis = Emphasis.Critical,
            )
        )

        assertEquals(3f, style.outlineWidthDp)
        assertEquals(0.42f, style.alpha)
    }
}
