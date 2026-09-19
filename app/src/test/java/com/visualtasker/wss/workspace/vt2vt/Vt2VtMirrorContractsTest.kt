package com.visualtasker.wss.workspace.vt2vt

import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowRunId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowSourceSessionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Vt2VtMirrorContractsTest {
    @Test
    fun workspaceMirrorCarriesCanonicalDocumentAndChecksum() {
        val state = WorkspaceWorkflowState.fromDocument(
            document = WorkspaceBootstrap.starter(),
            mutationSource = "vt2vt-test",
        )

        val mirror = decodeWorkspacePayload(workspacePayload(state))

        assertEquals(state.revision, mirror.revision)
        assertEquals(state.serializedJson, mirror.workspaceJson)
        assertEquals(state.emscriptProjection.getOrNull()?.takeIf { it.isNotBlank() }, mirror.emscript)
        assertEquals(64, mirror.checksum.length)
    }

    @Test
    fun runtimeMirrorCarriesNodeStatesEdgesAndDiagnosticsSurface() {
        val workflow = WorkspaceWorkflowState.fromDocument(WorkspaceBootstrap.starter(), "vt2vt-test")
        val snapshot = FlowRuntimeSnapshot(
            runId = FlowRunId("run-1"),
            sourceSessionId = FlowSourceSessionId("session-1"),
            documentId = FlowDocumentId("document-1"),
            documentRevision = FlowDocumentRevision("3"),
            sequence = 8L,
            capturedAtEpochMs = 100L,
            activeNodeId = FlowNodeId("node-1"),
            nodeStates = mapOf(FlowNodeId("node-1") to FlowRuntimeNodeState.RUNNING),
        )

        val mirror = decodeRuntimePayload(
            runtimePayload(workflow, snapshot, 4, FlowNodeId("node-1"), null),
        )

        assertEquals("run-1", mirror.runId)
        assertEquals(8L, mirror.sequence)
        assertEquals(4, mirror.activeStep)
        assertEquals(FlowRuntimeNodeState.RUNNING, mirror.nodeStates["node-1"])
    }

    @Test
    fun observerReducerRejectsDuplicatesWrongTargetsAndStaleRuntime() {
        val base = defaultVt2VtSession("Observer", timestampMs = 1L).copy(
            localPeer = defaultVt2VtSession("Observer", timestampMs = 1L).localPeer.copy(role = Vt2VtRole.Observer),
        )
        val fresh = message(id = "runtime-2", target = base.localPeer.id, sequence = 2L)
        val accepted = Vt2VtSessionReducer.receive(base, fresh) as Vt2VtInboundResult.Accepted

        assertEquals(Vt2VtConnectionState.Observing, accepted.state.connectionState)
        assertTrue(Vt2VtSessionReducer.receive(accepted.state, fresh) is Vt2VtInboundResult.Ignored)
        val stale = Vt2VtSessionReducer.receive(
            accepted.state,
            message(id = "runtime-1", target = base.localPeer.id, sequence = 1L),
        ) as Vt2VtInboundResult.Ignored
        assertEquals(Vt2VtInboundIgnoreReason.StaleRuntimeSequence, stale.reason)
        val wrongTarget = Vt2VtSessionReducer.receive(
            accepted.state,
            message(id = "runtime-3", target = "other", sequence = 3L),
        ) as Vt2VtInboundResult.Ignored
        assertEquals(Vt2VtInboundIgnoreReason.WrongTarget, wrongTarget.reason)
    }

    @Test
    fun observerReducerEnforcesConfiguredPairingCode() {
        val base = defaultVt2VtSession("Observer", timestampMs = 2L)
        val rejected = Vt2VtSessionReducer.receive(
            state = base,
            message = message(id = "runtime-pair", target = base.localPeer.id, sequence = 1L)
                .copy(payload = mapOf("runtimeSequence" to "1", "pairingCode" to "WRONG1")),
            expectedPairingCode = base.pairingCode,
        ) as Vt2VtInboundResult.Ignored

        assertEquals(Vt2VtInboundIgnoreReason.PairingMismatch, rejected.reason)
        assertTrue(base.inbound.isEmpty())
    }

    @Test
    fun observerReducerAcceptsPacketWhenPairingIsNotConfigured() {
        val base = defaultVt2VtSession("Observer", timestampMs = 3L)
        val accepted = Vt2VtSessionReducer.receive(
            state = base,
            message = message(id = "runtime-open", target = base.localPeer.id, sequence = 1L),
            expectedPairingCode = null,
        )

        assertTrue(accepted is Vt2VtInboundResult.Accepted)
    }

    @Test
    fun observerReducerTreatsWorkspaceRevisionAsOpaqueFingerprint() {
        val base = defaultVt2VtSession("Observer", timestampMs = 4L)
        val previous = Vt2VtMessage(
            id = "workspace-high-hash",
            type = Vt2VtMessageType.WorkspaceState,
            sourcePeerId = "primary-1",
            timestampMs = 100L,
            revision = 2_000_000_000L,
        )
        val acceptedPrevious = Vt2VtSessionReducer.receive(base, previous) as Vt2VtInboundResult.Accepted
        val newerWithLowerHash = previous.copy(
            id = "workspace-low-hash",
            timestampMs = 200L,
            revision = -2_000_000_000L,
        )

        assertTrue(
            Vt2VtSessionReducer.receive(acceptedPrevious.state, newerWithLowerHash) is
                Vt2VtInboundResult.Accepted,
        )
    }

    @Test
    fun observerReducerStillRejectsDelayedRevisionedPacket() {
        val base = defaultVt2VtSession("Observer", timestampMs = 5L)
        val current = Vt2VtMessage(
            id = "workspace-current",
            type = Vt2VtMessageType.WorkspaceState,
            sourcePeerId = "primary-1",
            timestampMs = 200L,
            revision = 10L,
        )
        val acceptedCurrent = Vt2VtSessionReducer.receive(base, current) as Vt2VtInboundResult.Accepted
        val delayed = current.copy(
            id = "workspace-delayed",
            timestampMs = 100L,
            revision = 20L,
        )
        val result = Vt2VtSessionReducer.receive(acceptedCurrent.state, delayed) as Vt2VtInboundResult.Ignored

        assertEquals(Vt2VtInboundIgnoreReason.StaleRevision, result.reason)
    }

    @Test
    fun workspaceRoleMatrixSeparatesAuthorityAndObservation() {
        assertTrue(Vt2VtRole.Primary.acceptsWorkspaceFrom(Vt2VtRole.Secondary))
        assertTrue(Vt2VtRole.Secondary.acceptsWorkspaceFrom(Vt2VtRole.Primary))
        assertTrue(Vt2VtRole.CoEditor.acceptsWorkspaceFrom(Vt2VtRole.Primary))
        assertTrue(Vt2VtRole.Observer.acceptsWorkspaceFrom(Vt2VtRole.Primary))
        assertTrue(!Vt2VtRole.Primary.acceptsWorkspaceFrom(Vt2VtRole.Primary))
        assertTrue(!Vt2VtRole.CoEditor.acceptsWorkspaceFrom(Vt2VtRole.Observer))
    }

    private fun message(id: String, target: String, sequence: Long): Vt2VtMessage =
        Vt2VtMessage(
            id = id,
            type = Vt2VtMessageType.RuntimeStepChanged,
            sourcePeerId = "primary-1",
            targetPeerId = target,
            timestampMs = sequence,
            revision = 3L,
            payload = mapOf("runtimeSequence" to sequence.toString()),
        )
}
