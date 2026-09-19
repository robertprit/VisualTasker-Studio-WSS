package com.visualtasker.wss.workspace.vt2vt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

private const val VT2VT_DEFAULT_TIMEOUT_MS = 5_000
internal const val VT2VT_MAX_FRAME_BYTES = 8 * 1024 * 1024

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
        reusableServerSocket(port).use { server ->
            server.soTimeout = timeoutMs
            server.accept().use { socket -> exchange(socket, localPeerId, timeoutMs, responsePayload) }
        }
    }

    suspend fun listen(
        port: Int,
        localPeerId: String,
        responsePayload: () -> Map<String, String> = { emptyMap() },
        onExchange: suspend (Vt2VtLanExchange) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        require(port in 1..65535) { "VT2VT port must be between 1 and 65535." }
        reusableServerSocket(port).use { server ->
            server.soTimeout = LISTENER_POLL_TIMEOUT_MS
            while (currentCoroutineContext().isActive) {
                val socket = try {
                    server.accept()
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                }
                socket.use {
                    onExchange(exchange(it, localPeerId, VT2VT_DEFAULT_TIMEOUT_MS, responsePayload()))
                }
            }
        }
    }

    private fun reusableServerSocket(port: Int): ServerSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(port))
    }

    private fun exchange(
        socket: Socket,
        localPeerId: String,
        timeoutMs: Int,
        responsePayload: Map<String, String>,
    ): Vt2VtLanExchange {
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
                "receivedType" to inbound.type.name,
            ) + responsePayload,
        )
        val output = DataOutputStream(socket.getOutputStream().buffered())
        writeFrame(output, Vt2VtMessageCodec.encode(outbound))
        output.flush()
        return Vt2VtLanExchange(
            remoteAddress = socket.inetAddress.hostAddress ?: "-",
            inbound = inbound,
            outbound = outbound,
        )
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
    require(bytes.size in 1..VT2VT_MAX_FRAME_BYTES) { "Invalid VT2VT frame size: ${bytes.size}." }
    output.writeInt(bytes.size)
    output.write(bytes)
}

private fun readFrame(input: DataInputStream): String {
    val size = input.readInt()
    require(size in 1..VT2VT_MAX_FRAME_BYTES) { "Invalid VT2VT frame size: $size." }
    val bytes = ByteArray(size)
    input.readFully(bytes)
    return bytes.decodeToString()
}

private const val LISTENER_POLL_TIMEOUT_MS = 1_000
