package com.visualtasker.wss.workspace.model

/** Suppresses the single selection callback caused by applying an external focus. */
class WorkspaceSelectionEchoGuard<T : Any> {
    private var pendingExternalValue: T? = null

    fun expect(value: T) {
        pendingExternalValue = value
    }

    fun consumeIfEcho(value: T?): Boolean {
        val expected = pendingExternalValue
        pendingExternalValue = null
        return expected != null && expected == value
    }

    fun clear() {
        pendingExternalValue = null
    }
}
