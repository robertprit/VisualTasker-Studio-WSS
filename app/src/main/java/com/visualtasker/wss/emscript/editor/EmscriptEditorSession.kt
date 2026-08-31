package com.visualtasker.wss.emscript.editor

import java.security.MessageDigest

enum class EmscriptEditorTabKind {
    MANUAL,
    GENERATED_FROM_BLOCKS,
    IMPORTED,
}

enum class EmscriptSourceKind {
    MANUAL,
    GENERATED_BLOCKS,
    AI_DRAFT,
    IMPORTED,
}

enum class EmscriptEditorDiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
}

data class EmscriptEditorDiagnostic(
    val severity: EmscriptEditorDiagnosticSeverity,
    val message: String,
    val code: String? = null,
)

data class EmscriptEditorTab(
    val id: String,
    val title: String,
    val kind: EmscriptEditorTabKind,
    val content: String,
    val readOnly: Boolean,
    val executionLocked: Boolean,
    val dirty: Boolean = false,
    val diagnostics: List<EmscriptEditorDiagnostic> = emptyList(),
    val updatedAt: Long? = null,
) {
    val hasErrorDiagnostics: Boolean
        get() = diagnostics.any { it.severity == EmscriptEditorDiagnosticSeverity.ERROR }
}

data class ActiveEmscriptSource(
    val id: String,
    val title: String,
    val sourceKind: EmscriptSourceKind,
    val content: String,
    val executionLocked: Boolean,
    val dirty: Boolean,
) {
    val tabId: String
        get() = id

    val contentHash: String
        get() = emscriptContentHash(content)
}

data class EmscriptEditorSession(
    val tabs: List<EmscriptEditorTab>,
    val activeTabId: String,
    val lastExecutableTabId: String? = MANUAL_TAB_ID,
) {
    val activeTab: EmscriptEditorTab
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    val activeSource: ActiveEmscriptSource
        get() {
            val tab = activeTab
            return ActiveEmscriptSource(
                id = tab.id,
                title = tab.title,
                sourceKind = tab.toSourceKind(),
                content = tab.content,
                executionLocked = tab.executionLocked || tab.readOnly || tab.hasErrorDiagnostics,
                dirty = tab.dirty,
            )
        }

    fun selectTab(tabId: String): EmscriptEditorSession =
        if (tabs.any { it.id == tabId }) copy(activeTabId = tabId) else this

    fun updateManualContent(text: String): EmscriptEditorSession =
        updateTab(MANUAL_TAB_ID) { tab ->
            tab.copy(
                content = text,
                dirty = true,
                updatedAt = nextUpdatedAt(tab.updatedAt),
            )
        }

    fun updateGeneratedFromBlocks(
        content: String,
        diagnostics: List<EmscriptEditorDiagnostic> = emptyList(),
    ): EmscriptEditorSession {
        val existing = tabs.firstOrNull { it.id == GENERATED_FROM_BLOCKS_TAB_ID }
        val nextTab = (existing ?: generatedFromBlocksTab()).copy(
            content = content,
            readOnly = true,
            executionLocked = true,
            dirty = true,
            diagnostics = diagnostics,
            updatedAt = nextUpdatedAt(existing?.updatedAt),
        )
        val nextTabs = if (existing == null) {
            tabs + nextTab
        } else {
            tabs.map { if (it.id == GENERATED_FROM_BLOCKS_TAB_ID) nextTab else it }
        }
        return copy(tabs = nextTabs.distinctBy { it.id })
    }

    fun updateAiDraft(
        content: String,
        diagnostics: List<EmscriptEditorDiagnostic> = emptyList(),
    ): EmscriptEditorSession {
        val existing = tabs.firstOrNull { it.id == AI_DRAFT_TAB_ID }
        val nextTab = (existing ?: aiDraftTab()).copy(
            content = content,
            readOnly = true,
            executionLocked = true,
            dirty = true,
            diagnostics = diagnostics,
            updatedAt = nextUpdatedAt(existing?.updatedAt),
        )
        val nextTabs = if (existing == null) {
            tabs + nextTab
        } else {
            tabs.map { if (it.id == AI_DRAFT_TAB_ID) nextTab else it }
        }
        return copy(tabs = nextTabs.distinctBy { it.id })
    }

    fun copyGeneratedToManual(): EmscriptEditorSession = acceptDraftIntoManualTab(GENERATED_FROM_BLOCKS_TAB_ID)

    fun acceptDraftIntoManualTab(tabId: String): EmscriptEditorSession {
        val draft = tabs.firstOrNull { it.id == tabId && it.executionLocked } ?: return this
        return updateManualContent(draft.content).copy(activeTabId = MANUAL_TAB_ID)
    }

    fun markTabSynchronized(tabId: String): EmscriptEditorSession =
        updateTab(tabId) { it.copy(dirty = false) }

    fun canExecuteActiveTab(): Boolean {
        val tab = activeTab
        return !tab.executionLocked && !tab.readOnly && !tab.hasErrorDiagnostics
    }

    fun closeTab(tabId: String): EmscriptEditorSession {
        if (tabId == MANUAL_TAB_ID) return this
        if (tabs.none { it.id == tabId }) return this
        val nextTabs = tabs.filterNot { it.id == tabId }
        val fallbackActive = when {
            activeTabId != tabId -> activeTabId
            nextTabs.any { it.id == MANUAL_TAB_ID } -> MANUAL_TAB_ID
            nextTabs.isNotEmpty() -> nextTabs.first().id
            else -> MANUAL_TAB_ID
        }
        return copy(
            tabs = nextTabs.ifEmpty { listOf(generatedFromBlocksTab()) },
            activeTabId = fallbackActive,
        )
    }

    private fun updateTab(
        tabId: String,
        transform: (EmscriptEditorTab) -> EmscriptEditorTab,
    ): EmscriptEditorSession =
        if (tabs.none { it.id == tabId }) {
            this
        } else {
            copy(tabs = tabs.map { tab -> if (tab.id == tabId) transform(tab) else tab })
        }

    companion object {
        const val MANUAL_TAB_ID = "manual"
        const val GENERATED_FROM_BLOCKS_TAB_ID = "generated-from-blocks"
        const val AI_DRAFT_TAB_ID = "ai-draft"
        const val IMPORTED_TAB_ID = "imported"

        fun create(
            manualContent: String,
            manualTitle: String = "Manuell",
            generatedContent: String = "",
        ): EmscriptEditorSession {
            val manual = EmscriptEditorTab(
                id = MANUAL_TAB_ID,
                title = manualTitle,
                kind = EmscriptEditorTabKind.MANUAL,
                content = manualContent,
                readOnly = false,
                executionLocked = false,
                dirty = false,
                diagnostics = emptyList(),
                updatedAt = 0L,
            )
            val generated = generatedFromBlocksTab(generatedContent)
            return EmscriptEditorSession(
                tabs = listOf(manual, generated),
                activeTabId = MANUAL_TAB_ID,
                lastExecutableTabId = MANUAL_TAB_ID,
            )
        }

        fun generatedFromBlocksTab(content: String = ""): EmscriptEditorTab =
            EmscriptEditorTab(
                id = GENERATED_FROM_BLOCKS_TAB_ID,
                title = "Aus Blöcken",
                kind = EmscriptEditorTabKind.GENERATED_FROM_BLOCKS,
                content = content,
                readOnly = true,
                executionLocked = true,
                dirty = false,
                diagnostics = emptyList(),
                updatedAt = 0L,
            )

        fun aiDraftTab(content: String = ""): EmscriptEditorTab =
            EmscriptEditorTab(
                id = AI_DRAFT_TAB_ID,
                title = "KI-Draft",
                kind = EmscriptEditorTabKind.IMPORTED,
                content = content,
                readOnly = true,
                executionLocked = true,
                dirty = false,
                diagnostics = emptyList(),
                updatedAt = 0L,
            )

        private fun nextUpdatedAt(previous: Long?): Long = (previous ?: 0L) + 1L
    }
}

fun emscriptContentHash(content: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(12)

private fun EmscriptEditorTab.toSourceKind(): EmscriptSourceKind = when {
    id == EmscriptEditorSession.AI_DRAFT_TAB_ID -> EmscriptSourceKind.AI_DRAFT
    kind == EmscriptEditorTabKind.MANUAL -> EmscriptSourceKind.MANUAL
    kind == EmscriptEditorTabKind.GENERATED_FROM_BLOCKS -> EmscriptSourceKind.GENERATED_BLOCKS
    else -> EmscriptSourceKind.IMPORTED
}
