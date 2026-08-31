package com.visualtasker.wss.workspace.plugin

import com.visualtasker.wss.workspace.plugin.blockeditor.BlockEditorShellPlugin
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPlugin

class WorkspaceShellPluginRegistry(
    plugins: Iterable<ShellEditorPlugin>
) {
    private val editorsById: Map<ShellPluginId, ShellEditorPlugin> =
        plugins.associateBy(ShellEditorPlugin::pluginId)

    fun findEditorPlugin(pluginId: ShellPluginId): ShellEditorPlugin? =
        editorsById[pluginId]
}

fun defaultWorkspaceShellPluginRegistry(): WorkspaceShellPluginRegistry =
    WorkspaceShellPluginRegistry(
        listOf(
            BlockEditorShellPlugin(),
            FlowchartShellPlugin()
        )
    )
