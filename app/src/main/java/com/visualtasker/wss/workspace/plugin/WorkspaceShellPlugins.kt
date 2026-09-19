package com.visualtasker.wss.workspace.plugin

import com.visualtasker.integrations.api.VisualTaskerIntegrationCatalog
import com.visualtasker.wss.workspace.plugin.blockeditor.BlockEditorShellPlugin
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPlugin

class WorkspaceShellPluginRegistry(
    plugins: Iterable<ShellEditorPlugin>,
    descriptors: Iterable<ShellPluginDescriptor> = emptyList(),
) {
    private val editorsById: Map<ShellPluginId, ShellEditorPlugin> =
        plugins.associateBy(ShellEditorPlugin::pluginId)
    private val descriptorsById: Map<ShellPluginId, ShellPluginDescriptor> =
        descriptors.associateBy(ShellPluginDescriptor::pluginId)

    fun findEditorPlugin(pluginId: ShellPluginId): ShellEditorPlugin? =
        editorsById[pluginId]

    fun findDescriptor(pluginId: ShellPluginId): ShellPluginDescriptor? =
        descriptorsById[pluginId]

    fun descriptors(): List<ShellPluginDescriptor> =
        descriptorsById.values.sortedBy { it.displayName }
}

fun defaultWorkspaceShellPluginRegistry(): WorkspaceShellPluginRegistry =
    WorkspaceShellPluginRegistry(
        plugins = listOf(
            BlockEditorShellPlugin(),
            FlowchartShellPlugin()
        ),
        descriptors = defaultShellPluginDescriptors(),
    )

fun defaultShellPluginDescriptors(): List<ShellPluginDescriptor> = listOf(
    ShellPluginDescriptor(
        pluginId = ShellPluginId("m3-shapemaker"),
        displayName = "VisualTasker M3ShapeMaker",
        kind = ShellPluginKind.VISUAL_TOOL,
        implementationVersion = "1.0.0",
        capabilities = setOf(
            ShellPluginCapability("visual-asset.design"),
            ShellPluginCapability("visual-asset.ema"),
        ),
        supportedFormatIds = setOf("application/vnd.emscript.motion+json"),
        companionPackageName = "com.m3shapes.editor",
    ),
    ShellPluginDescriptor(
        pluginId = ShellPluginId("chartgraph"),
        displayName = "VisualTasker ChartGraph",
        kind = ShellPluginKind.VISUAL_TOOL,
        implementationVersion = "0.1.0",
        capabilities = setOf(
            ShellPluginCapability("chart.line"),
            ShellPluginCapability("chart.bar"),
            ShellPluginCapability("chart.pie"),
            ShellPluginCapability("chart.candlestick"),
        ),
        supportedFormatIds = setOf("application/vnd.visualtasker.chart+json"),
    ),
    ShellPluginDescriptor(
        pluginId = ShellPluginId("visualtasker-ime"),
        displayName = "VisualTasker IME Keypad",
        kind = ShellPluginKind.COMPANION_APP,
        implementationVersion = "0.1.0",
        capabilities = setOf(
            ShellPluginCapability("ime.input"),
            ShellPluginCapability("ime.visualtasker-bridge"),
        ),
        companionPackageName = "com.visualtasker.ime",
    ),
    ShellPluginDescriptor(
        pluginId = ShellPluginId("vision-ai"),
        displayName = "VisualTasker Vision AI",
        kind = ShellPluginKind.PERCEPTION_PROVIDER,
        implementationVersion = "0.1.0",
        capabilities = setOf(
            ShellPluginCapability("vision.yolo"),
            ShellPluginCapability("vision.mlkit", optional = true),
            ShellPluginCapability("vision.opencv", optional = true),
            ShellPluginCapability("ai.gemma", optional = true),
            ShellPluginCapability("ai.rag", optional = true),
        ),
        supportedFormatIds = setOf("application/vnd.visualtasker.observation+json"),
        companionPackageName = "com.yolo26n.android",
    ),
    ShellPluginDescriptor(
        pluginId = ShellPluginId("integrations"),
        displayName = "VisualTasker Integrations",
        kind = ShellPluginKind.RUNTIME_ADAPTER,
        implementationVersion = "0.1.0",
        capabilities = VisualTaskerIntegrationCatalog.descriptors
            .flatMap { it.capabilities }
            .map { ShellPluginCapability(it.id, it.version) }
            .toSet(),
    ),
)
