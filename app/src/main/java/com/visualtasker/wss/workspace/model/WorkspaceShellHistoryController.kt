package com.visualtasker.wss.workspace.model

/**
 * Process-local semantic history used by the workspace shell.
 *
 * Documents remain owned by the shell. This controller only stores accepted before/after snapshots;
 * it does not mutate, validate, deserialize, or publish a workspace.
 */
class WorkspaceShellHistoryController(
    private val maxDepth: Int = 100,
) {
    private val undoEntries = ArrayDeque<WorkspaceHistoryEntry>()
    private val redoEntries = ArrayDeque<WorkspaceHistoryEntry>()
    private var lastMutationSource: String? = null

    val undoDepth: Int get() = undoEntries.size
    val redoDepth: Int get() = redoEntries.size

    fun recordAcceptedTransition(
        current: WorkspaceWorkflowState,
        next: WorkspaceWorkflowState,
        selection: WorkspaceSelectionState,
        source: String,
    ): Boolean {
        if (current.serializedJson == next.serializedJson) return false
        if (current.canonicalDocument == next.canonicalDocument) return false

        if (!WorkspaceMutationSourcePolicy.coalesces(source, lastMutationSource)) {
            undoEntries.addLast(WorkspaceHistoryEntry(current.serializedJson, selection))
            trimToDepth(undoEntries)
        }
        redoEntries.clear()
        lastMutationSource = source
        return true
    }

    fun undo(
        current: WorkspaceWorkflowState,
        selection: WorkspaceSelectionState,
    ): WorkspaceHistoryEntry? {
        val previous = undoEntries.removeLastOrNull() ?: return null
        redoEntries.addLast(WorkspaceHistoryEntry(current.serializedJson, selection))
        trimToDepth(redoEntries)
        lastMutationSource = null
        return previous
    }

    fun redo(
        current: WorkspaceWorkflowState,
        selection: WorkspaceSelectionState,
    ): WorkspaceHistoryEntry? {
        val next = redoEntries.removeLastOrNull() ?: return null
        undoEntries.addLast(WorkspaceHistoryEntry(current.serializedJson, selection))
        trimToDepth(undoEntries)
        lastMutationSource = null
        return next
    }

    fun clear() {
        undoEntries.clear()
        redoEntries.clear()
        lastMutationSource = null
    }

    private fun trimToDepth(entries: ArrayDeque<WorkspaceHistoryEntry>) {
        while (entries.size > maxDepth) entries.removeFirst()
    }
}
