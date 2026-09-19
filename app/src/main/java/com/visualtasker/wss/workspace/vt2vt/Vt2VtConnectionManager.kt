package com.visualtasker.wss.workspace.vt2vt

import android.content.Context
import android.os.Build
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class Vt2VtConnectionSettings(
    val remoteHost: String = "",
    val port: Int = 47272,
    val remotePairingCode: String = "",
    val role: Vt2VtRole = Vt2VtRole.Primary,
    val liveMirrorEnabled: Boolean = false,
    val listenOnLaunch: Boolean = false,
    val autoReconnect: Boolean = true,
    val preferUsbBridge: Boolean = true,
    val connectionEnabled: Boolean = false,
) {
    fun encode(): String = JSONObject()
        .put("remoteHost", remoteHost)
        .put("port", port)
        .put("remotePairingCode", remotePairingCode)
        .put("role", role.name)
        .put("liveMirrorEnabled", liveMirrorEnabled)
        .put("listenOnLaunch", listenOnLaunch)
        .put("autoReconnect", autoReconnect)
        .put("preferUsbBridge", preferUsbBridge)
        .put("connectionEnabled", connectionEnabled)
        .toString()

    companion object {
        fun decode(raw: String?): Vt2VtConnectionSettings = runCatching {
            if (raw.isNullOrBlank()) return@runCatching Vt2VtConnectionSettings()
            val json = JSONObject(raw)
            Vt2VtConnectionSettings(
                remoteHost = json.optString("remoteHost").trim(),
                port = json.optInt("port", 47272).coerceIn(1, 65535),
                remotePairingCode = json.optString("remotePairingCode")
                    .filter(Char::isLetterOrDigit).uppercase().take(6),
                role = Vt2VtRole.entries.firstOrNull { it.name == json.optString("role") }
                    ?: Vt2VtRole.Primary,
                liveMirrorEnabled = json.optBoolean("liveMirrorEnabled", false),
                listenOnLaunch = json.optBoolean("listenOnLaunch", false),
                autoReconnect = json.optBoolean("autoReconnect", true),
                preferUsbBridge = json.optBoolean("preferUsbBridge", true),
                connectionEnabled = json.optBoolean("connectionEnabled", false),
            )
        }.getOrDefault(Vt2VtConnectionSettings())
    }
}

data class Vt2VtConnectionSnapshot(
    val settings: Vt2VtConnectionSettings = Vt2VtConnectionSettings(),
    val session: Vt2VtSessionState,
    val status: String = "VT2VT bereit",
    val listenerActive: Boolean = false,
    val reconnectAttempt: Int = 0,
    val localAddresses: List<String> = emptyList(),
    val usbBridgeStatus: Vt2VtUsbAdbBridgeStatus,
    val remoteWorkspaceMirror: Vt2VtWorkspaceMirror? = null,
    val remoteRuntimeMirror: Vt2VtRuntimeMirror? = null,
    val ignoredInbound: String? = null,
    val lastInboundType: Vt2VtMessageType? = null,
    val lastOutboundType: Vt2VtMessageType? = null,
    val lastAcknowledgedType: Vt2VtMessageType? = null,
    val lastSuccessfulExchangeAtMs: Long? = null,
    val consecutiveFailures: Int = 0,
    val lastFailure: String? = null,
)

class Vt2VtConnectionManager internal constructor(
    context: Context,
    private val transport: Vt2VtTransportGateway = DefaultVt2VtTransportGateway,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ioMutex = Mutex()
    private val sequence = AtomicLong(System.currentTimeMillis())
    private var listenerJob: Job? = null
    private var keepAliveJob: Job? = null

    private val _state = MutableStateFlow(initialSnapshot())
    val state: StateFlow<Vt2VtConnectionSnapshot> = _state.asStateFlow()

    init {
        if (_state.value.settings.listenOnLaunch || _state.value.settings.connectionEnabled) startListener()
        startKeepAlive()
    }

    fun updateSettings(transform: (Vt2VtConnectionSettings) -> Vt2VtConnectionSettings) {
        val previous = _state.value.settings
        val next = transform(previous).sanitize()
        prefs.edit().putString(KEY_SETTINGS, next.encode()).apply()
        _state.value = _state.value.copy(
            settings = next,
            session = _state.value.session.copy(
                localPeer = _state.value.session.localPeer.copy(role = next.role),
            ),
        )
        val listenerRequired = next.listenOnLaunch || next.connectionEnabled
        if (listenerJob != null && previous.port != next.port) {
            stopListener()
            if (listenerRequired) startListener()
        } else if (listenerRequired && listenerJob == null) {
            startListener()
        } else if (!listenerRequired && listenerJob != null) {
            stopListener()
        }
    }

    fun refreshEnvironment() {
        _state.value = _state.value.copy(
            localAddresses = runCatching { detectVt2VtLanAddresses() }.getOrDefault(emptyList()),
            usbBridgeStatus = Vt2VtUsbAdbBridge.detect(appContext),
        )
    }

    fun useUsbBridge() {
        val usb = Vt2VtUsbBridgeConfig()
        refreshEnvironment()
        updateSettings {
            it.copy(
                remoteHost = usb.endpoint.host,
                port = usb.endpoint.port,
                preferUsbBridge = true,
                connectionEnabled = true,
            )
        }
        _state.value = _state.value.copy(
            status = "USB/ADB Bridge gewählt: ${usb.reverseCommand}",
            session = _state.value.session.copy(
                connectionState = Vt2VtConnectionState.Pairing,
                localPeer = _state.value.session.localPeer.copy(transport = Vt2VtTransport.UsbAdbBridge),
                lastError = null,
            ),
        )
    }

    fun connect() {
        val snapshot = _state.value
        if (snapshot.settings.remoteHost.isBlank()) {
            _state.value = snapshot.copy(status = "Remote-IP fehlt")
            return
        }
        updateSettings { it.copy(connectionEnabled = true) }
        send(newMessage(Vt2VtMessageType.Hello))
    }

    fun disconnect() {
        updateSettings { it.copy(connectionEnabled = false) }
        _state.value = _state.value.copy(
            status = "Verbindung getrennt; Zieladresse bleibt gespeichert",
            reconnectAttempt = 0,
            session = _state.value.session.copy(connectionState = Vt2VtConnectionState.Offline),
        )
    }

    fun startListener() {
        if (listenerJob?.isActive == true) return
        val port = _state.value.settings.port
        listenerJob = scope.launch {
            _state.value = _state.value.copy(
                listenerActive = true,
                status = "Listener aktiv auf Port $port",
                session = _state.value.session.copy(connectionState = Vt2VtConnectionState.Pairing),
            )
            while (isActive) {
                try {
                    transport.listen(
                        port = port,
                        localPeerId = _state.value.session.localPeer.id,
                        responsePayload = { mapOf("pairingCode" to _state.value.session.pairingCode) },
                    ) { exchange ->
                        ioMutex.withLock {
                            acceptInbound(
                                message = exchange.inbound,
                                remoteLabel = exchange.remoteAddress,
                                transportKind = Vt2VtTransport.LanTcp,
                                outboundAck = exchange.outbound,
                                expectedPairingCode = expectedInboundPairingCode(),
                            )
                        }
                        if (_state.value.settings.remoteHost.isBlank()) {
                            updateSettings { it.copy(remoteHost = exchange.remoteAddress) }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _state.value = _state.value.copy(status = "Listener startet neu: ${error.message ?: error::class.java.simpleName}")
                    delay(1_000)
                }
            }
        }
    }

    fun stopListener() {
        listenerJob?.cancel()
        listenerJob = null
        _state.value = _state.value.copy(listenerActive = false, status = "Listener gestoppt")
    }

    fun receiveOnce() {
        scope.launch {
            val snapshot = _state.value
            runCatching {
                transport.receive(
                    port = snapshot.settings.port,
                    localPeerId = snapshot.session.localPeer.id,
                    timeoutMs = 30_000,
                    responsePayload = mapOf("pairingCode" to snapshot.session.pairingCode),
                )
            }.onSuccess { exchange ->
                ioMutex.withLock {
                    acceptInbound(
                        message = exchange.inbound,
                        remoteLabel = exchange.remoteAddress,
                        transportKind = Vt2VtTransport.LanTcp,
                        outboundAck = exchange.outbound,
                        expectedPairingCode = expectedInboundPairingCode(),
                    )
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(status = "Empfang fehlgeschlagen: ${error.message ?: error::class.java.simpleName}")
            }
        }
    }

    fun send(message: Vt2VtMessage) {
        scope.launch { sendWithRetry(message) }
    }

    fun loopback(message: Vt2VtMessage) {
        val local = _state.value.session.localPeer
        val mirrored = Vt2VtMessageCodec.decode(Vt2VtMessageCodec.encode(message)).copy(
            id = "${message.id}-loopback",
            sourcePeerId = "${local.id}-loopback",
            targetPeerId = local.id,
        )
        _state.value = _state.value.copy(session = _state.value.session.copy(outbound = (_state.value.session.outbound + message).takeLast(64)))
        acceptInbound(
            message = mirrored,
            remoteLabel = "${local.label} Loopback",
            transportKind = Vt2VtTransport.Loopback,
            outboundAck = null,
            expectedPairingCode = null,
        )
    }

    fun newMessage(
        type: Vt2VtMessageType,
        revision: Long? = null,
        payload: Map<String, String> = emptyMap(),
    ): Vt2VtMessage {
        val snapshot = _state.value
        return Vt2VtMessage(
            id = "${snapshot.session.localPeer.id}-${sequence.incrementAndGet()}",
            type = type,
            sourcePeerId = snapshot.session.localPeer.id,
            timestampMs = System.currentTimeMillis(),
            revision = revision,
            payload = payload + mapOf(
                "pairingCode" to snapshot.settings.remotePairingCode,
                "role" to snapshot.session.localPeer.role.name,
            ),
        )
    }

    private suspend fun sendWithRetry(message: Vt2VtMessage) = ioMutex.withLock {
        val settings = _state.value.settings
        if (settings.remoteHost.isBlank()) return@withLock
        val stateBeforeSend = _state.value.session.connectionState
        val endpoint = Vt2VtLanEndpoint(settings.remoteHost, settings.port)
        val usb = Vt2VtUsbBridgeConfig()
        val transportKind = if (
            endpoint == usb.endpoint && _state.value.usbBridgeStatus.bridgeReady
        ) Vt2VtTransport.UsbAdbBridge else Vt2VtTransport.LanTcp
        _state.value = _state.value.copy(
            status = "Sende ${message.type.name} an ${endpoint.host}:${endpoint.port}",
            lastOutboundType = message.type,
            session = _state.value.session.copy(
                connectionState = Vt2VtConnectionState.Syncing,
                localPeer = _state.value.session.localPeer.copy(transport = transportKind),
                outbound = (_state.value.session.outbound + message).takeLast(64),
                lastError = null,
            ),
        )
        var lastError: Throwable? = null
        val delays = if (settings.autoReconnect) listOf(0L, 500L, 1_500L) else listOf(0L)
        for ((attempt, retryDelay) in delays.withIndex()) {
            if (retryDelay > 0) delay(retryDelay)
            try {
                val ack = transport.send(endpoint, message)
                acceptInbound(
                    message = ack,
                    remoteLabel = endpoint.host,
                    transportKind = transportKind,
                    outboundAck = null,
                    expectedPairingCode = settings.remotePairingCode.takeIf(String::isNotBlank),
                )
                _state.value = _state.value.copy(
                    reconnectAttempt = 0,
                    status = "Verbunden mit ${endpoint.host}:${endpoint.port}",
                    consecutiveFailures = 0,
                    lastFailure = null,
                    lastSuccessfulExchangeAtMs = System.currentTimeMillis(),
                )
                return@withLock
            } catch (error: Throwable) {
                lastError = error
                _state.value = _state.value.copy(
                    reconnectAttempt = attempt + 1,
                    status = "Wiederverbinden ${attempt + 1}/${delays.size}: ${error.message ?: error::class.java.simpleName}",
                )
            }
        }
        val failures = _state.value.consecutiveFailures + 1
        val connectionLost = failures >= FAILURE_THRESHOLD
        val detail = lastError?.message ?: lastError?.javaClass?.simpleName ?: "unbekannter Fehler"
        _state.value = _state.value.copy(
            status = if (connectionLost) {
                "Verbindung nicht erreichbar; Wiederaufnahme läuft"
            } else {
                "${message.type.name} nicht bestätigt ($failures/$FAILURE_THRESHOLD); Verbindung bleibt aktiv"
            },
            consecutiveFailures = failures,
            lastFailure = detail,
            session = _state.value.session.copy(
                connectionState = if (connectionLost) Vt2VtConnectionState.Error else stateBeforeSend,
                lastError = if (connectionLost) detail else null,
            ),
        )
    }

    private fun acceptInbound(
        message: Vt2VtMessage,
        remoteLabel: String,
        transportKind: Vt2VtTransport,
        outboundAck: Vt2VtMessage?,
        expectedPairingCode: String?,
    ) {
        val current = _state.value
        when (val result = Vt2VtSessionReducer.receive(
            state = current.session,
            message = message,
            expectedPairingCode = expectedPairingCode,
            transport = transportKind,
            remoteLabel = remoteLabel,
        )) {
            is Vt2VtInboundResult.Accepted -> {
                var workspace = current.remoteWorkspaceMirror
                var runtime = current.remoteRuntimeMirror
                runCatching {
                    when (message.type) {
                        Vt2VtMessageType.WorkspaceState -> workspace = decodeWorkspacePayload(message.payload).copy(
                            sourcePeerId = message.sourcePeerId,
                            sourceRole = message.payload["role"]
                                ?.let { role -> Vt2VtRole.entries.firstOrNull { it.name.equals(role, ignoreCase = true) } }
                                ?: Vt2VtRole.Secondary,
                            sentAtEpochMs = message.timestampMs,
                        )
                        Vt2VtMessageType.RuntimeStepChanged -> runtime = decodeRuntimePayload(message.payload)
                        else -> Unit
                    }
                }
                _state.value = current.copy(
                    session = result.state.copy(
                        localPeer = result.state.localPeer.copy(transport = transportKind),
                        outbound = outboundAck?.let { (result.state.outbound + it).takeLast(64) } ?: result.state.outbound,
                    ),
                    status = "${message.type.name} von $remoteLabel",
                    remoteWorkspaceMirror = workspace,
                    remoteRuntimeMirror = runtime,
                    ignoredInbound = null,
                    lastInboundType = message.type,
                    lastAcknowledgedType = message.payload["receivedType"]
                        ?.let { type -> Vt2VtMessageType.entries.firstOrNull { it.name == type } }
                        ?: current.lastAcknowledgedType,
                    lastSuccessfulExchangeAtMs = System.currentTimeMillis(),
                    consecutiveFailures = 0,
                    lastFailure = null,
                )
            }
            is Vt2VtInboundResult.Ignored -> {
                _state.value = current.copy(session = result.state, ignoredInbound = result.reason.name, status = "Paket ignoriert: ${result.reason.name}")
            }
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(KEEP_ALIVE_INTERVAL_MS)
                refreshEnvironment()
                val snapshot = _state.value
                if (
                    snapshot.settings.autoReconnect &&
                    snapshot.settings.connectionEnabled &&
                    snapshot.settings.remoteHost.isNotBlank() &&
                    snapshot.session.connectionState != Vt2VtConnectionState.Syncing
                ) {
                    sendWithRetry(newMessage(Vt2VtMessageType.Heartbeat))
                }
            }
        }
    }

    private fun initialSnapshot(): Vt2VtConnectionSnapshot {
        val settings = Vt2VtConnectionSettings.decode(prefs.getString(KEY_SETTINGS, null)).sanitize()
        val localId = prefs.getString(KEY_LOCAL_PEER_ID, null)
            ?.takeIf(String::isNotBlank)
            ?: "vt-${UUID.randomUUID().toString().replace("-", "").take(18)}".also {
                prefs.edit().putString(KEY_LOCAL_PEER_ID, it).apply()
            }
        val label = listOf(Build.MANUFACTURER, Build.MODEL).filter(String::isNotBlank).joinToString(" ").ifBlank { "VT Studio WSS" }
        val peer = Vt2VtPeer(localId, label, settings.role, Vt2VtTransport.LanTcp, Vt2VtConnectionState.Offline)
        return Vt2VtConnectionSnapshot(
            settings = settings,
            session = Vt2VtSessionState(localPeer = peer),
            localAddresses = runCatching { detectVt2VtLanAddresses() }.getOrDefault(emptyList()),
            usbBridgeStatus = Vt2VtUsbAdbBridge.detect(appContext),
        )
    }

    private fun Vt2VtConnectionSettings.sanitize() = copy(
        remoteHost = remoteHost.trim(),
        port = port.coerceIn(1, 65535),
        remotePairingCode = remotePairingCode.filter(Char::isLetterOrDigit).uppercase().take(6),
    )

    private fun expectedInboundPairingCode(): String? =
        _state.value.session.pairingCode.takeIf {
            _state.value.settings.remotePairingCode.isNotBlank()
        }

    companion object {
        private const val PREFS_NAME = "vt2vt_settings"
        private const val KEY_SETTINGS = "connection_settings"
        private const val KEY_LOCAL_PEER_ID = "local_peer_id"
        private const val FAILURE_THRESHOLD = 3
        private const val KEEP_ALIVE_INTERVAL_MS = 15_000L
    }
}

interface Vt2VtTransportGateway {
    suspend fun send(endpoint: Vt2VtLanEndpoint, message: Vt2VtMessage): Vt2VtMessage
    suspend fun receive(port: Int, localPeerId: String, timeoutMs: Int, responsePayload: Map<String, String>): Vt2VtLanExchange
    suspend fun listen(
        port: Int,
        localPeerId: String,
        responsePayload: () -> Map<String, String>,
        onExchange: suspend (Vt2VtLanExchange) -> Unit,
    )
}

private object DefaultVt2VtTransportGateway : Vt2VtTransportGateway {
    override suspend fun send(endpoint: Vt2VtLanEndpoint, message: Vt2VtMessage) = Vt2VtLanTransport.send(endpoint, message)
    override suspend fun receive(port: Int, localPeerId: String, timeoutMs: Int, responsePayload: Map<String, String>) =
        Vt2VtLanTransport.receiveOnce(port, localPeerId, timeoutMs, responsePayload)
    override suspend fun listen(
        port: Int,
        localPeerId: String,
        responsePayload: () -> Map<String, String>,
        onExchange: suspend (Vt2VtLanExchange) -> Unit,
    ) = Vt2VtLanTransport.listen(port, localPeerId, responsePayload, onExchange)
}

object Vt2VtRuntime {
    @Volatile private var manager: Vt2VtConnectionManager? = null
    fun manager(context: Context): Vt2VtConnectionManager = manager ?: synchronized(this) {
        manager ?: Vt2VtConnectionManager(context).also { manager = it }
    }
}
