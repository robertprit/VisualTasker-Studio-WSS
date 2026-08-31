package com.visualtasker.wss.workspace.plugin

data class WorkspaceShellPanelBinding(
    val pluginId: ShellPluginId,
    val panelId: ShellPanelId,
    val studioPanelTypeName: String,
    val shellPanelTypeNames: Set<String>,
    val viewSurface: WorkflowViewSurface
) {
    init {
        require(studioPanelTypeName.isNotBlank() && studioPanelTypeName == studioPanelTypeName.trim()) {
            "studioPanelTypeName must be nonblank and trimmed."
        }
        require(shellPanelTypeNames.isNotEmpty()) {
            "A shared panel binding needs at least one Shell panel type name."
        }
        require(shellPanelTypeNames.all { it.isNotBlank() && it == it.trim() }) {
            "Shell panel type names must be nonblank and trimmed."
        }
    }
}

object WorkspaceShellPluginCatalog {
    val bindings: List<WorkspaceShellPanelBinding> = listOf(
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("blockeditor"),
            panelId = ShellPanelId("blockeditor-panel"),
            studioPanelTypeName = "NATIVE_BLOCK_EDITOR",
            shellPanelTypeNames = setOf("BLOCKEDITOR", "BlockEditor"),
            viewSurface = WorkflowViewSurface.BLOCK_EDITOR
        ),
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("flowchart"),
            panelId = ShellPanelId("flowchart-panel"),
            studioPanelTypeName = "CANVAS",
            shellPanelTypeNames = setOf("FLOWCHART", "Flowchart"),
            viewSurface = WorkflowViewSurface.FLOWCHART
        ),
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("emscript"),
            panelId = ShellPanelId("emscript-panel"),
            studioPanelTypeName = "TEXT_EDITOR",
            shellPanelTypeNames = setOf("EDITOR", "EMSCRIPT", "Emscript", "TextEditor"),
            viewSurface = WorkflowViewSurface.EMSCRIPT
        ),
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("log-console"),
            panelId = ShellPanelId("log-console-panel"),
            studioPanelTypeName = "LOG_CONSOLE",
            shellPanelTypeNames = setOf("LOG_CONSOLE", "RuntimeLog", "LogConsole"),
            viewSurface = WorkflowViewSurface.STEP_EDITOR
        ),
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("debug-info"),
            panelId = ShellPanelId("debug-info-panel"),
            studioPanelTypeName = "DEBUG_INFO",
            shellPanelTypeNames = setOf("DebugInfo"),
            viewSurface = WorkflowViewSurface.STEP_EDITOR
        ),
        WorkspaceShellPanelBinding(
            pluginId = ShellPluginId("step-editor"),
            panelId = ShellPanelId("step-editor-panel"),
            studioPanelTypeName = "RECORDING_STEPS",
            shellPanelTypeNames = setOf("LIST_TEST", "RecorderSteps"),
            viewSurface = WorkflowViewSurface.STEP_EDITOR
        )
    )

    fun bindingForShellPanelType(typeName: String): WorkspaceShellPanelBinding? =
        bindings.firstOrNull { typeName in it.shellPanelTypeNames }

    fun bindingForStudioPanelType(typeName: String): WorkspaceShellPanelBinding? =
        bindings.firstOrNull { it.studioPanelTypeName == typeName }

    fun bindingForPlugin(pluginId: ShellPluginId): WorkspaceShellPanelBinding? =
        bindings.firstOrNull { it.pluginId == pluginId }

    fun bindingForMainPanelType(panelType: com.visualtasker.wss.data.PanelType): WorkspaceShellPanelBinding? =
        bindingForShellPanelType(panelType.name)

    fun bindingForWorkspacePanelType(
        panelType: com.visualtasker.wss.workspace.model.PanelType
    ): WorkspaceShellPanelBinding? =
        bindingForShellPanelType(panelType.name)
}

object WorkflowProjectionPolicy {
    const val WORKFLOW_DOCUMENT_SOURCE = "workflow-document"

    fun requireWorkflowDocumentSource(sourceId: String) {
        require(sourceId == WORKFLOW_DOCUMENT_SOURCE) {
            "Shell panels may project only from $WORKFLOW_DOCUMENT_SOURCE, not from '$sourceId'."
        }
    }
}
