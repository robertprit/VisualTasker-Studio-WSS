package com.visualtasker.wss.workspace.plugin.flowchart

import androidx.compose.runtime.Composable
import de.visualtasker.flowchart.compose.FlowchartHost
import de.visualtasker.flowchart.compose.FlowchartHostCallbacks
import de.visualtasker.flowchart.compose.FlowchartUiConfig

@Composable
fun FlowchartShellPanel(
    session: FlowchartShellEditorSession,
    uiConfig: FlowchartUiConfig = FlowchartUiConfig()
) {
    FlowchartHost(
        graphDocument = session.graphDocument,
        viewDocument = session.viewDocument,
        runtimeSnapshot = null,
        controller = session.controller,
        uiConfig = uiConfig,
        callbacks = FlowchartHostCallbacks(
            onViewDocumentChanged = session::onViewDocumentChanged,
            onStatusMessage = session::onStatusMessage
        )
    )
}
