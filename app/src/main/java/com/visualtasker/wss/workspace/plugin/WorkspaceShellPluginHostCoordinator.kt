package com.visualtasker.wss.workspace.plugin

class WorkspaceShellPluginHostCoordinator(
    private val hostServices: ShellPluginHostServices = RecordingShellPluginHostAdapter(),
    private val pluginLookup: (ShellPluginId) -> ShellEditorPlugin? = { null }
) {
    fun bindingFor(shellPanelTypeName: String): WorkspaceShellPanelBinding? =
        WorkspaceShellPluginCatalog.bindingForShellPanelType(shellPanelTypeName)

    fun openEditor(
        shellPanelTypeName: String,
        input: ShellEditorInput,
        workflowSourceId: String = WorkflowProjectionPolicy.WORKFLOW_DOCUMENT_SOURCE
    ): BoundWorkspaceShellEditor {
        WorkflowProjectionPolicy.requireWorkflowDocumentSource(workflowSourceId)
        val binding = bindingFor(shellPanelTypeName)
            ?: error("Unknown Workspace Shell panel type: $shellPanelTypeName")
        val plugin = pluginLookup(binding.pluginId)
            ?: error("Plugin '${binding.pluginId.value}' is not registered for Workspace Shell.")
        require(input.formatId in plugin.supportedFormatIds) {
            "Plugin '${binding.pluginId.value}' does not support '${input.formatId}'."
        }
        val session = plugin.createEditorSession(input, hostServices)
        session.onActivated()
        return BoundWorkspaceShellEditor(
            binding = binding,
            session = session,
            hostServices = hostServices
        )
    }
}

data class BoundWorkspaceShellEditor(
    val binding: WorkspaceShellPanelBinding,
    val session: ShellEditorSession,
    val hostServices: ShellPluginHostServices
) {
    fun close() {
        session.onDeactivated()
        session.dispose()
    }
}
