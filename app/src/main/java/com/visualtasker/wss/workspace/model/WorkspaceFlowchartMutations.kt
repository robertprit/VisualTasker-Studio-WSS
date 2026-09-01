package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory

const val WORKFLOW_SOURCE_FLOWCHART_PREFIX = "flowchart:"

fun addFlowchartNodeToWorkspace(
    document: WorkspaceDocument,
    definitionId: String,
): WorkspaceDocument {
    val x = 96f
    val y = 120f + (document.rootBlocks.size * 32f)
    return WorkspaceReducer.reduce(
        document,
        WorkspaceAction.InstantiateBlock(definitionId, x, y),
        DefaultBlockRegistry.asFactory(),
    )
}
