package com.visualtasker.wss.workspace.plugin.blockeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.visualtasker.blockeditor.compose.host.BlockEditorHost
import de.visualtasker.blockeditor.compose.host.BlockEditorHostUiConfig

@Composable
fun BlockEditorShellPanel(
    session: BlockEditorShellEditorSession,
    modifier: Modifier = Modifier,
    onSave: (() -> Unit)? = null,
    uiConfig: BlockEditorHostUiConfig = BlockEditorHostUiConfig(
        showBottomPanel = true,
        showBlockFactory = true,
        showToolbox = true,
        allowClearWorkspace = true
    )
) {
    BlockEditorHost(
        controller = session.controller,
        uiConfig = uiConfig,
        modifier = modifier,
        onSaveWorkspace = onSave
    )
}
