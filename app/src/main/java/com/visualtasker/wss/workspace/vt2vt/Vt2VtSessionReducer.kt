package com.visualtasker.wss.workspace.vt2vt

enum class Vt2VtInboundIgnoreReason {
    Duplicate,
    WrongTarget,
    PairingMismatch,
    StaleRevision,
    StaleRuntimeSequence,
}

sealed interface Vt2VtInboundResult {
    val state: Vt2VtSessionState

    data class Accepted(
        override val state: Vt2VtSessionState,
        val message: Vt2VtMessage,
    ) : Vt2VtInboundResult

    data class Ignored(
        override val state: Vt2VtSessionState,
        val message: Vt2VtMessage,
        val reason: Vt2VtInboundIgnoreReason,
    ) : Vt2VtInboundResult
}

/** Applies remote state only to the VT2VT observer session, never to the local workflow. */
object Vt2VtSessionReducer {
    fun receive(
        state: Vt2VtSessionState,
        message: Vt2VtMessage,
        expectedPairingCode: String? = null,
        transport: Vt2VtTransport = Vt2VtTransport.LanTcp,
        remoteLabel: String = message.sourcePeerId,
    ): Vt2VtInboundResult {
        val ignoredReason = when {
            state.inbound.any { it.id == message.id && it.sourcePeerId == message.sourcePeerId } ->
                Vt2VtInboundIgnoreReason.Duplicate
            message.targetPeerId != null && message.targetPeerId != state.localPeer.id ->
                Vt2VtInboundIgnoreReason.WrongTarget
            !expectedPairingCode.isNullOrBlank() && message.payload["pairingCode"] != expectedPairingCode ->
                Vt2VtInboundIgnoreReason.PairingMismatch
            isStaleRuntimeSequence(state, message) -> Vt2VtInboundIgnoreReason.StaleRuntimeSequence
            isStaleRevision(state, message) -> Vt2VtInboundIgnoreReason.StaleRevision
            else -> null
        }
        if (ignoredReason != null) {
            return Vt2VtInboundResult.Ignored(state, message, ignoredReason)
        }
        val declaredRole = message.payload["role"]
            ?.let { value -> Vt2VtRole.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } }
            ?: Vt2VtRole.Secondary
        val remote = Vt2VtPeer(
            id = message.sourcePeerId,
            label = remoteLabel.ifBlank { message.sourcePeerId },
            role = declaredRole,
            transport = transport,
            state = if (state.localPeer.role == Vt2VtRole.Observer) {
                Vt2VtConnectionState.Observing
            } else {
                Vt2VtConnectionState.Connected
            },
        )
        val next = state.copy(
            remotePeers = (state.remotePeers.filterNot { it.id == remote.id } + remote).takeLast(8),
            connectionState = remote.state,
            inbound = (state.inbound + message).takeLast(64),
            lastError = null,
        )
        return Vt2VtInboundResult.Accepted(next, message)
    }

    private fun isStaleRevision(state: Vt2VtSessionState, message: Vt2VtMessage): Boolean {
        message.revision ?: return false
        return state.inbound
            .asSequence()
            .filter { it.sourcePeerId == message.sourcePeerId && it.type == message.type }
            .filter { it.revision != null }
            .map(Vt2VtMessage::timestampMs)
            .maxOrNull()
            ?.let { message.timestampMs < it }
            ?: false
    }

    private fun isStaleRuntimeSequence(state: Vt2VtSessionState, message: Vt2VtMessage): Boolean {
        if (message.type != Vt2VtMessageType.RuntimeStepChanged) return false
        val sequence = message.payload["runtimeSequence"]?.toLongOrNull() ?: return false
        return state.inbound
            .asSequence()
            .filter { it.sourcePeerId == message.sourcePeerId && it.type == message.type }
            .mapNotNull { it.payload["runtimeSequence"]?.toLongOrNull() }
            .maxOrNull()
            ?.let { sequence < it }
            ?: false
    }
}
