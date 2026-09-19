package com.visualtasker.wss.workspace.data

import androidx.compose.ui.graphics.Color
import com.visualtasker.wss.workspace.model.PanelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneInspectorPanelContractTest {
    @Test
    fun sceneInspectorIsPersistableAndHasDedicatedAccent() {
        assertTrue(PanelType.SceneInspector in supportedWorkspacePanelTypes)
        assertEquals(Color(0xFF26C6DA), defaultAccentForPanelType(PanelType.SceneInspector))
    }
}
