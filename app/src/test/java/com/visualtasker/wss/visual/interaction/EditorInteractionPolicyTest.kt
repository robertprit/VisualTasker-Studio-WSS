package com.visualtasker.wss.visual.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInteractionPolicyTest {
    @Test
    fun keepsSharedEditorActionsInTheSameOrderForBlockEditorAndFlowchart() {
        val blockActions = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.BlockEditor)
            .filter { it.shared }
            .map { it.id }
        val flowActions = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.Flowchart)
            .filter { it.shared }
            .map { it.id }

        assertEquals(blockActions, flowActions)
        assertEquals(
            listOf(
                EditorActionId.Save,
                EditorActionId.Undo,
                EditorActionId.Redo,
                EditorActionId.ZoomIn,
                EditorActionId.ZoomOut,
                EditorActionId.FitViewport,
                EditorActionId.AutoArrange,
                EditorActionId.DeleteSelection,
                EditorActionId.OpenPalette,
            ),
            blockActions,
        )
    }

    @Test
    fun keepsProjectionSpecificActionsOutOfTheOtherEditor() {
        val blockActionIds = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.BlockEditor).map { it.id }.toSet()
        val flowActionIds = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.Flowchart).map { it.id }.toSet()

        assertTrue(EditorActionId.ToggleCollapse in blockActionIds)
        assertTrue(EditorActionId.OpenBlockDesigner in blockActionIds)
        assertTrue(EditorActionId.RunDry !in blockActionIds)
        assertTrue(EditorActionId.ToggleDataFlow !in blockActionIds)

        assertTrue(EditorActionId.RunDry in flowActionIds)
        assertTrue(EditorActionId.ToggleDataFlow in flowActionIds)
        assertTrue(EditorActionId.ToggleCollapse !in flowActionIds)
        assertTrue(EditorActionId.OpenBlockDesigner !in flowActionIds)
    }

    @Test
    fun mapsEditingEventsToSharedFeedbackPolicy() {
        val dock = DefaultEditorInteractionPolicy.feedbackFor(EditorFeedbackEvent.Dock)
        val select = DefaultEditorInteractionPolicy.feedbackFor(EditorFeedbackEvent.Select)
        val delete = DefaultEditorInteractionPolicy.feedbackFor(EditorFeedbackEvent.Delete)

        assertEquals(EditorFeedbackEvent.Dock, dock.event)
        assertTrue(dock.haptic)
        assertTrue(dock.sound)
        assertTrue(select.haptic)
        assertTrue(!select.sound)
        assertTrue(delete.haptic)
        assertTrue(delete.sound)
    }
}
