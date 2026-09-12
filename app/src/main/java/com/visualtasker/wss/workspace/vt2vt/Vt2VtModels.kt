package com.visualtasker.wss.workspace.vt2vt

const val VT2VT_MESSAGE_FORMAT = "application/vnd.visualtasker.vt2vt+kv"
const val VT2VT_PROTOCOL_VERSION = 1

enum class Vt2VtRole {
    Primary,
    Secondary,
    Observer,
    CoEditor
}

enum class Vt2VtTransport {
    Loopback,
    LanTcp,
    LanWebSocket,
    UsbAdbBridge
}

enum class Vt2VtConnectionState {
    Offline,
    Pairing,
    Connected,
    Observing,
    Syncing,
    Error
}

enum class Vt2VtMessageType {
    Hello,
    WorkspaceState,
    SelectionChanged,
    RuntimeStepChanged,
    LogEntryAdded,
    ScreenshotCaptured,
    MarkerCreated,
    CapabilityChanged,
    Heartbeat
}

data class Vt2VtPeer(
    val id: String,
    val label: String,
    val role: Vt2VtRole,
    val transport: Vt2VtTransport,
    val state: Vt2VtConnectionState
) {
    init {
        require(id.isNotBlank() && id == id.trim()) { "Peer id must be nonblank and trimmed." }
        require(label.isNotBlank() && label == label.trim()) { "Peer label must be nonblank and trimmed." }
    }
}

data class Vt2VtMessage(
    val id: String,
    val protocolVersion: Int = VT2VT_PROTOCOL_VERSION,
    val type: Vt2VtMessageType,
    val sourcePeerId: String,
    val targetPeerId: String? = null,
    val timestampMs: Long,
    val revision: Long? = null,
    val payload: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank() && id == id.trim()) { "Message id must be nonblank and trimmed." }
        require(sourcePeerId.isNotBlank() && sourcePeerId == sourcePeerId.trim()) {
            "Message sourcePeerId must be nonblank and trimmed."
        }
        require(targetPeerId == null || targetPeerId.isNotBlank()) {
            "Message targetPeerId must be null or nonblank."
        }
        require(protocolVersion == VT2VT_PROTOCOL_VERSION) {
            "Unsupported VT2VT protocol version: $protocolVersion."
        }
    }
}

data class Vt2VtSessionState(
    val localPeer: Vt2VtPeer,
    val remotePeers: List<Vt2VtPeer> = emptyList(),
    val connectionState: Vt2VtConnectionState = Vt2VtConnectionState.Offline,
    val pairingCode: String = defaultPairingCode(localPeer.id),
    val outbound: List<Vt2VtMessage> = emptyList(),
    val inbound: List<Vt2VtMessage> = emptyList(),
    val lastError: String? = null
) {
    val eventCount: Int get() = outbound.size + inbound.size
}

data class Vt2VtUsbBridgeConfig(
    val endpoint: Vt2VtLanEndpoint = Vt2VtLanEndpoint(VT2VT_ADB_BRIDGE_HOST, VT2VT_ADB_BRIDGE_PORT),
    val autoConnect: Boolean = true,
    val reverseCommand: String = "adb reverse tcp:$VT2VT_ADB_BRIDGE_PORT tcp:$VT2VT_ADB_BRIDGE_PORT"
) {
    init {
        require(reverseCommand.isNotBlank() && reverseCommand == reverseCommand.trim()) {
            "VT2VT USB bridge command must be nonblank and trimmed."
        }
    }
}

object Vt2VtMessageCodec {
    fun encode(message: Vt2VtMessage): String =
        buildList {
            add("format=${VT2VT_MESSAGE_FORMAT.escapeField()}")
            add("protocolVersion=${message.protocolVersion}")
            add("id=${message.id.escapeField()}")
            add("type=${message.type.name}")
            add("sourcePeerId=${message.sourcePeerId.escapeField()}")
            message.targetPeerId?.let { add("targetPeerId=${it.escapeField()}") }
            add("timestampMs=${message.timestampMs}")
            message.revision?.let { add("revision=$it") }
            message.payload.entries.sortedBy { it.key }.forEach { (key, value) ->
                add("payload.${key.escapeField()}=${value.escapeField()}")
            }
        }.joinToString("\n")

    fun decode(raw: String): Vt2VtMessage {
        val fields = raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator < 0) null else line.substring(0, separator).unescapeField() to
                    line.substring(separator + 1).unescapeField()
            }
            .toMap()
        require(fields["format"] == VT2VT_MESSAGE_FORMAT) {
            "Unsupported VT2VT message format: ${fields["format"]}"
        }
        val payload = fields.entries
            .filter { it.key.startsWith("payload.") }
            .associate { it.key.removePrefix("payload.") to it.value }
        return Vt2VtMessage(
            id = fields.getValue("id"),
            protocolVersion = fields.getValue("protocolVersion").toInt(),
            type = enumValueOf(fields.getValue("type")),
            sourcePeerId = fields.getValue("sourcePeerId"),
            targetPeerId = fields["targetPeerId"]?.takeIf { it.isNotBlank() },
            timestampMs = fields.getValue("timestampMs").toLong(),
            revision = fields["revision"]?.toLong(),
            payload = payload
        )
    }

    fun encodeBatch(messages: List<Vt2VtMessage>): String =
        messages.joinToString("\n---vt2vt-message---\n", transform = ::encode)
}

private fun String.escapeField(): String =
    replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("=", "\\e")

private fun String.unescapeField(): String {
    val out = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == '\\' && index + 1 < length) {
            when (this[index + 1]) {
                '\\' -> out.append('\\')
                'n' -> out.append('\n')
                'e' -> out.append('=')
                else -> out.append(this[index + 1])
            }
            index += 2
        } else {
            out.append(char)
            index += 1
        }
    }
    return out.toString()
}

fun defaultVt2VtSession(
    deviceLabel: String,
    timestampMs: Long = System.currentTimeMillis()
): Vt2VtSessionState {
    val peerId = stablePeerId(deviceLabel, timestampMs)
    return Vt2VtSessionState(
        localPeer = Vt2VtPeer(
            id = peerId,
            label = deviceLabel.ifBlank { "VT Studio WSS" },
            role = Vt2VtRole.Primary,
            transport = Vt2VtTransport.Loopback,
            state = Vt2VtConnectionState.Offline
        )
    )
}

fun Vt2VtSessionState.withLoopbackHello(timestampMs: Long = System.currentTimeMillis()): Vt2VtSessionState {
    val message = Vt2VtMessage(
        id = "vt2vt-${outbound.size + inbound.size + 1}",
        type = Vt2VtMessageType.Hello,
        sourcePeerId = localPeer.id,
        timestampMs = timestampMs,
        payload = mapOf(
            "label" to localPeer.label,
            "role" to localPeer.role.name,
            "transport" to Vt2VtTransport.Loopback.name,
        )
    )
    val remote = Vt2VtPeer(
        id = "${localPeer.id}-loopback",
        label = "${localPeer.label} Loopback",
        role = Vt2VtRole.Observer,
        transport = Vt2VtTransport.Loopback,
        state = Vt2VtConnectionState.Observing
    )
    return copy(
        remotePeers = listOf(remote),
        connectionState = Vt2VtConnectionState.Observing,
        outbound = (outbound + message).takeLast(64),
        inbound = (inbound + message.copy(sourcePeerId = remote.id, targetPeerId = localPeer.id)).takeLast(64),
        lastError = null
    )
}

private fun stablePeerId(label: String, timestampMs: Long): String {
    val normalized = label.lowercase().filter { it.isLetterOrDigit() }.take(18).ifBlank { "visualtasker" }
    return "$normalized-${timestampMs.toString().takeLast(6)}"
}

private fun defaultPairingCode(peerId: String): String =
    peerId.filter { it.isLetterOrDigit() }.takeLast(6).uppercase().padStart(6, '0')
