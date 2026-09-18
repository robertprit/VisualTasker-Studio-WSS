package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockDefinition
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

enum class ToolboxSetMode(val displayName: String) {
    Standard("Standard"),
    Custom("Custom"),
    Mixed("Mixed"),
    JavaScript("JavaScript"),
    Plugins("Plugins"),
}

fun ToolboxSetMode.accepts(definition: BlockDefinition): Boolean {
    val command = VisualTaskerCommandCatalog.findByBlockType(definition.id)
    val pluginOwner = command?.pluginOwner ?: definition.metadata["pluginOwner"]
    val custom = definition.category == BlockCategories.CUSTOM || definition.id.startsWith("custom.")
    val javascript = definition.metadata["language"].equals("javascript", ignoreCase = true) ||
        definition.id.startsWith("javascript.") || definition.id.startsWith("js.")
    val plugin = !pluginOwner.isNullOrBlank() && pluginOwner != "visualtasker.core"
    return when (this) {
        ToolboxSetMode.Standard -> !custom && !javascript && !plugin
        ToolboxSetMode.Custom -> custom
        ToolboxSetMode.Mixed -> true
        ToolboxSetMode.JavaScript -> javascript
        ToolboxSetMode.Plugins -> plugin
    }
}
