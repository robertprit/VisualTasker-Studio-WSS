package com.visualtasker.wss.workspace.ui.dnd

import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.WssDragPayload
import com.visualtasker.wss.workspace.model.WssDragPayloadKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WssDndEnvelopeTest {
    @Test
    fun `adapter preserves WSS payload identity`() {
        val payload = WssDragPayload(
            id = "command.wait",
            kind = WssDragPayloadKind.Command,
            label = "Wait",
            sourcePanelId = "palette",
            sourcePanelType = PanelType.BlockEditor,
        )

        val envelope = WssDndEnvelope(key = "drag:${payload.id}", payload = payload)

        assertEquals("drag:command.wait", envelope.key)
        assertEquals(payload, envelope.payload)
    }
}
