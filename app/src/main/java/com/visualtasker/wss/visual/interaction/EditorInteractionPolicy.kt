package com.visualtasker.wss.visual.interaction

enum class EditorProjection {
    BlockEditor,
    Flowchart,
}

enum class EditorActionId {
    Save,
    Undo,
    Redo,
    ZoomIn,
    ZoomOut,
    FitViewport,
    AutoArrange,
    ToggleCollapse,
    ToggleDataFlow,
    ToggleRuntime,
    ToggleDiagnostics,
    RunDry,
    RunLive,
    StepBack,
    StepForward,
    DeleteSelection,
    OpenBlockDesigner,
    ClearWorkspace,
    OpenPalette,
}

enum class EditorFeedbackEvent {
    Select,
    Drop,
    Dock,
    Undock,
    Connect,
    Disconnect,
    Delete,
    Collapse,
    Expand,
    AutoArrange,
    Error,
}

data class EditorActionDescriptor(
    val id: EditorActionId,
    val label: String,
    val order: Int,
    val shared: Boolean,
    val feedbackEvent: EditorFeedbackEvent? = null,
)

data class EditorFeedbackPolicy(
    val event: EditorFeedbackEvent,
    val haptic: Boolean,
    val sound: Boolean,
)

object DefaultEditorInteractionPolicy {
    private val sharedActions = listOf(
        EditorActionDescriptor(EditorActionId.Save, "Speichern", 10, shared = true),
        EditorActionDescriptor(EditorActionId.Undo, "Undo", 20, shared = true),
        EditorActionDescriptor(EditorActionId.Redo, "Redo", 30, shared = true),
        EditorActionDescriptor(EditorActionId.ZoomIn, "Zoom +", 40, shared = true),
        EditorActionDescriptor(EditorActionId.ZoomOut, "Zoom -", 50, shared = true),
        EditorActionDescriptor(EditorActionId.FitViewport, "Zentrieren", 60, shared = true),
        EditorActionDescriptor(EditorActionId.AutoArrange, "Auto anordnen", 70, shared = true, feedbackEvent = EditorFeedbackEvent.AutoArrange),
        EditorActionDescriptor(EditorActionId.DeleteSelection, "Auswahl loeschen", 900, shared = true, feedbackEvent = EditorFeedbackEvent.Delete),
        EditorActionDescriptor(EditorActionId.OpenPalette, "Palette", 1000, shared = true),
    )

    private val blockEditorActions = listOf(
        EditorActionDescriptor(EditorActionId.ToggleCollapse, "Ein-/ausklappen", 100, shared = false),
        EditorActionDescriptor(EditorActionId.OpenBlockDesigner, "Blockdesigner", 910, shared = false),
        EditorActionDescriptor(EditorActionId.ClearWorkspace, "Workspace leeren", 920, shared = false, feedbackEvent = EditorFeedbackEvent.Delete),
    )

    private val flowchartActions = listOf(
        EditorActionDescriptor(EditorActionId.ToggleDataFlow, "Dataflow", 100, shared = false),
        EditorActionDescriptor(EditorActionId.ToggleRuntime, "Runtime", 110, shared = false),
        EditorActionDescriptor(EditorActionId.ToggleDiagnostics, "Diagnose", 120, shared = false),
    )

    fun actionsFor(projection: EditorProjection): List<EditorActionDescriptor> =
        when (projection) {
            EditorProjection.BlockEditor -> sharedActions + blockEditorActions
            EditorProjection.Flowchart -> sharedActions + flowchartActions
        }.sortedBy { it.order }

    fun feedbackFor(event: EditorFeedbackEvent): EditorFeedbackPolicy =
        when (event) {
            EditorFeedbackEvent.Select -> EditorFeedbackPolicy(event, haptic = true, sound = false)
            EditorFeedbackEvent.Drop,
            EditorFeedbackEvent.Dock,
            EditorFeedbackEvent.Undock,
            EditorFeedbackEvent.Connect,
            EditorFeedbackEvent.Disconnect,
            EditorFeedbackEvent.Collapse,
            EditorFeedbackEvent.Expand,
            EditorFeedbackEvent.AutoArrange -> EditorFeedbackPolicy(event, haptic = true, sound = true)
            EditorFeedbackEvent.Delete,
            EditorFeedbackEvent.Error -> EditorFeedbackPolicy(event, haptic = true, sound = true)
        }
}
