package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.PanelState
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceCanvasVisibilityTest {
    @Test
    fun `topmost visible panel owns the workspace surface`() {
        val panels = listOf(
            panel("flow", PanelType.Flowchart, zIndex = 5, minimized = true),
            panel("block", PanelType.BlockEditor, zIndex = 6, minimized = true),
            panel("rail", PanelType.RecorderSteps, zIndex = 7, minimized = false),
        )

        assertTrue(workspaceSurfaceOwnerPanelType(panels) == PanelType.RecorderSteps)
    }

    @Test
    fun `marker and canvas panels expose the workspace canvas`() {
        assertTrue(shouldShowWorkspaceCanvas(PanelType.Marker, false, false))
        assertTrue(shouldShowWorkspaceCanvas(PanelType.Screenshot, false, false))
    }

    @Test
    fun `railtrace exposes the workspace canvas only for visual step context`() {
        assertFalse(shouldShowWorkspaceCanvas(PanelType.RecorderSteps, false, true))
        assertFalse(shouldShowWorkspaceCanvas(PanelType.RecorderSteps, true, false))
        assertTrue(shouldShowWorkspaceCanvas(PanelType.RecorderSteps, true, true))
    }

    @Test
    fun `vision and workflow editors retain their dedicated surfaces`() {
        assertFalse(shouldShowWorkspaceCanvas(PanelType.Vision, true, true))
        assertFalse(shouldShowWorkspaceCanvas(PanelType.BlockEditor, true, true))
        assertFalse(shouldShowWorkspaceCanvas(PanelType.Flowchart, true, true))
    }

    private fun panel(
        id: String,
        type: PanelType,
        zIndex: Int,
        minimized: Boolean,
    ): PanelState = PanelState(
        id = id,
        type = type,
        title = id,
        x = 0f,
        y = 0f,
        width = 320f,
        height = 480f,
        zIndex = zIndex,
        minimized = minimized,
        accentColor = Color.Transparent,
    )
}
