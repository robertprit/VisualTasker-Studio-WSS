package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class VisualAssetBridgeInboxTest {
    @Test
    fun pendingImportIsConsumedOnlyByMatchingToken() {
        VisualAssetBridgeInbox.offer("{\"format\":\"emscript_motion_asset\"}")
        val pending = VisualAssetBridgeInbox.pending.value
        assertNotNull(pending)
        assertEquals("{\"format\":\"emscript_motion_asset\"}", pending?.rawEma)

        VisualAssetBridgeInbox.consume((pending?.token ?: 0L) + 1L)
        assertNotNull(VisualAssetBridgeInbox.pending.value)
        VisualAssetBridgeInbox.consume(pending!!.token)
        assertNull(VisualAssetBridgeInbox.pending.value)
    }
}
