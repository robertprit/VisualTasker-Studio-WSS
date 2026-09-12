package com.visualtasker.wss.workspace.vt2vt

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

class Vt2VtModelsTest {
    @Test
    fun messageCodecRoundtripsRuntimeStepPayload() {
        val message = Vt2VtMessage(
            id = "msg-1",
            type = Vt2VtMessageType.RuntimeStepChanged,
            sourcePeerId = "primary-1",
            targetPeerId = "observer-1",
            timestampMs = 1_000L,
            revision = 42L,
            payload = mapOf(
                "step" to "7",
                "node" to "node-if",
                "phase" to "dryRun"
            )
        )

        val restored = Vt2VtMessageCodec.decode(Vt2VtMessageCodec.encode(message))

        assertEquals(message, restored)
    }

    @Test
    fun loopbackHelloCreatesObservablePeerAndMirrorsMessage() {
        val session = defaultVt2VtSession("SM-S938B", timestampMs = 123456L)
            .withLoopbackHello(timestampMs = 123999L)

        assertEquals(Vt2VtConnectionState.Observing, session.connectionState)
        assertEquals(1, session.remotePeers.size)
        assertEquals(1, session.outbound.size)
        assertEquals(1, session.inbound.size)
        assertEquals(Vt2VtMessageType.Hello, session.inbound.single().type)
        assertTrue(session.pairingCode.isNotBlank())
    }

    @Test
    fun lanTransportReceivesMessageAndReturnsAck() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = async {
            Vt2VtLanTransport.receiveOnce(
                port = port,
                localPeerId = "observer-1",
                timeoutMs = 2_000,
                responsePayload = mapOf("pairingCode" to "ABC123")
            )
        }
        delay(100)

        val ack = Vt2VtLanTransport.send(
            endpoint = Vt2VtLanEndpoint("127.0.0.1", port),
            message = Vt2VtMessage(
                id = "msg-lan-1",
                type = Vt2VtMessageType.WorkspaceState,
                sourcePeerId = "primary-1",
                targetPeerId = "observer-1",
                timestampMs = 1_234L,
                revision = 9L,
                payload = mapOf("nodes" to "3")
            ),
            timeoutMs = 2_000
        )
        val exchange = server.await()

        assertEquals("primary-1", exchange.inbound.sourcePeerId)
        assertEquals(Vt2VtMessageType.WorkspaceState, exchange.inbound.type)
        assertEquals("observer-1", ack.sourcePeerId)
        assertEquals("msg-lan-1", ack.payload["ack"])
        assertEquals("ABC123", ack.payload["pairingCode"])
    }

    @Test
    fun usbBridgeConfigUsesLocalhostAdbReverseConvention() {
        val config = Vt2VtUsbBridgeConfig()

        assertEquals(Vt2VtLanEndpoint("127.0.0.1", 47272), config.endpoint)
        assertTrue(config.autoConnect)
        assertEquals("adb reverse tcp:47272 tcp:47272", config.reverseCommand)
    }
}
