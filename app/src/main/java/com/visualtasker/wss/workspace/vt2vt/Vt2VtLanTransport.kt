package com.visualtasker.wss.workspace.vt2vt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

private const val VT2VT_DEFAULT_TIMEOUT_MS = 5_000

data class Vt2VtLanEndpoint(
    val host: String,
    val port: Int = 47272
) {
    init {
        require(host.isNotBlank()) { "VT2VT host must not be blank." }
        require(port in 1..65535) { "VT2VT port must be between 1 and 65535." }
    }
}

data class Vt2VtLanExchange(
    val remoteAddress: String,
    val inbound: Vt2VtMessage,
    val outbound: Vt2VtMessage
)

object Vt2VtLanTransport {
    suspend fun send(
        endpoint: Vt2VtLanEndpoint,
        message: Vt2VtMessage,
        timeoutMs: Int = VT2VT_DEFAULT_TIMEOUT_MS
    ): Vt2VtMessage = withContext(Dispatchers.IO) {
        Socket().use { socket ->
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
            val output = DataOutputStream(socket.getOutputStream().buffered())
            writeFrame(output, Vt2VtMessageCodec.encode(message))
            output.flush()
            val input = DataInputStream(socket.getInputStream().buffered())
            Vt2VtMessageCodec.decode(readFrame(input))
        }
    }

    suspend fun receiveOnce(
        port: Int,
        localPeerId: String,
        timeoutMs: Int = VT2VT_DEFAULT_TIMEOUT_MS,
        responsePayload: Map<String, String> = emptyMap()
    ): Vt2VtLanExchange = withContext(Dispatchers.IO) {
        require(port in 1..65535) { "VT2VT port must be between 1 and 65535." }
        ServerSocket(port).use { server ->
            server.soTimeout = timeoutMs
            server.accept().use { socket ->
                socket.soTimeout = timeoutMs
                val input = DataInputStream(socket.getInputStream().buffered())
                val inbound = Vt2VtMessageCodec.decode(readFrame(input))
                val outbound = Vt2VtMessage(
                    id = "${inbound.id}-ack",
                    type = Vt2VtMessageType.Heartbeat,
                    sourcePeerId = localPeerId,
                    targetPeerId = inbound.sourcePeerId,
                    timestampMs = System.currentTimeMillis(),
                    revision = inbound.revision,
                    payload = mapOf(
                        "ack" to inbound.id,
                        "receivedType" to inbound.type.name
                    ) + responsePayload
                )
                val output = DataOutputStream(socket.getOutputStream().buffered())
                writeFrame(output, Vt2VtMessageCodec.encode(outbound))
                output.flush()
                Vt2VtLanExchange(
                    remoteAddress = socket.inetAddress.hostAddress ?: "-",
                    inbound = inbound,
                    outbound = outbound
                )
            }
        }
    }
}

fun detectVt2VtLanAddresses(): List<String> =
    NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { networkInterface ->
            networkInterface.inetAddresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress }
                .map { it.hostAddress }
        }
        .toList()
        .distinct()

private fun writeFrame(output: DataOutputStream, payload: String) {
    val bytes = payload.encodeToByteArray()
    output.writeInt(bytes.size)
    output.write(bytes)
}

private fun readFrame(input: DataInputStream): String {
    val size = input.readInt()
    require(size in 1..262_144) { "Invalid VT2VT frame size: $size." }
    val bytes = ByteArray(size)
    input.readFully(bytes)
    return bytes.decodeToString()
}
