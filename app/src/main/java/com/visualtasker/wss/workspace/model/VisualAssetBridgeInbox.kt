package com.visualtasker.wss.workspace.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingVisualAssetImport(
    val token: Long,
    val rawEma: String,
)

object VisualAssetBridgeInbox {
    private val mutablePending = MutableStateFlow<PendingVisualAssetImport?>(null)
    val pending = mutablePending.asStateFlow()

    fun offer(rawEma: String) {
        if (rawEma.isBlank()) return
        mutablePending.value = PendingVisualAssetImport(System.nanoTime(), rawEma)
    }

    fun consume(token: Long) {
        if (mutablePending.value?.token == token) mutablePending.value = null
    }
}
