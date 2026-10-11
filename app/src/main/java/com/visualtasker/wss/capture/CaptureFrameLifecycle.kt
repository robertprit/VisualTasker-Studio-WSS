package com.visualtasker.wss.capture

import com.visualtasker.wss.emscript.runtime.ProviderSelectionPolicy
import de.visualtasker.emscript.contract.ProviderId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

fun interface CaptureFrameIdGenerator {
    fun nextId(): CaptureFrameId
}

object UuidCaptureFrameIdGenerator : CaptureFrameIdGenerator {
    override fun nextId(): CaptureFrameId = CaptureFrameId("capture-${UUID.randomUUID()}")
}

class CaptureFrameLease internal constructor(
    val frame: CaptureFrame,
    private val store: CaptureFrameStore,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    fun readBytes(): ByteArray {
        check(!closed.get()) { "Capture frame lease is closed" }
        return store.read(frame.id) ?: error("Capture frame data was released")
    }

    fun retain(): CaptureFrameLease {
        check(!closed.get()) { "Capture frame lease is closed" }
        return checkNotNull(store.acquire(frame.id)) { "Capture frame data was released" }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) store.release(frame.id)
    }
}

class CaptureFrameStore(
    private val maxBytes: Long = 32L * 1024L * 1024L,
) {
    private data class Entry(
        val frame: CaptureFrame,
        val bytes: ByteArray,
        var references: Int,
    )

    private val entries = linkedMapOf<CaptureFrameId, Entry>()
    private var storedBytes = 0L

    @Synchronized
    fun publish(frame: CaptureFrame, bytes: ByteArray): CaptureFrameLease? {
        if (bytes.isEmpty() || bytes.size.toLong() > maxBytes || frame.validity != CaptureValidity.VALID) return null
        if (entries.containsKey(frame.id)) return null
        evictReleasedUntil(bytes.size.toLong())
        if (storedBytes + bytes.size > maxBytes) return null
        entries[frame.id] = Entry(frame, bytes.copyOf(), references = 1)
        storedBytes += bytes.size
        return CaptureFrameLease(frame, this)
    }

    @Synchronized
    fun acquire(frameId: CaptureFrameId): CaptureFrameLease? {
        val entry = entries[frameId] ?: return null
        entry.references += 1
        return CaptureFrameLease(entry.frame, this)
    }

    @Synchronized
    internal fun read(frameId: CaptureFrameId): ByteArray? = entries[frameId]?.bytes?.copyOf()

    @Synchronized
    internal fun release(frameId: CaptureFrameId) {
        val entry = entries[frameId] ?: return
        entry.references = (entry.references - 1).coerceAtLeast(0)
    }

    @Synchronized
    fun discard(frameId: CaptureFrameId): Boolean {
        val entry = entries[frameId] ?: return false
        if (entry.references > 0) return false
        entries.remove(frameId)
        storedBytes -= entry.bytes.size
        return true
    }

    @Synchronized
    fun clearReleased() {
        entries.filterValues { it.references == 0 }.keys.toList().forEach(::remove)
    }

    @Synchronized
    fun activeReferenceCount(frameId: CaptureFrameId): Int = entries[frameId]?.references ?: 0

    @Synchronized
    fun byteCount(): Long = storedBytes

    private fun evictReleasedUntil(requiredBytes: Long) {
        val iterator = entries.iterator()
        while (storedBytes + requiredBytes > maxBytes && iterator.hasNext()) {
            val (_, entry) = iterator.next()
            if (entry.references == 0) {
                storedBytes -= entry.bytes.size
                iterator.remove()
            }
        }
    }

    private fun remove(frameId: CaptureFrameId) {
        entries.remove(frameId)?.let { storedBytes -= it.bytes.size }
    }
}

class CaptureProviderRegistry(
    providers: List<CaptureFrameProvider>,
) {
    private val providersById = providers.associateBy { it.descriptor.id }

    fun resolve(request: CaptureRequest): CaptureFrameProvider? {
        val policy = request.providerPolicy
        policy.explicitProviderId?.let { explicit ->
            return providersById[explicit]?.takeIf { it.availability(request).isExecutable }
        }
        val candidates = providersById.values.filter { provider ->
            provider.descriptor.id !in policy.excludedProviderIds && provider.availability(request).isExecutable
        }
        policy.preferredProviderIds.forEach { preferred ->
            candidates.firstOrNull { it.descriptor.id == preferred }?.let { return it }
        }
        return candidates.sortedWith(
            compareByDescending<CaptureFrameProvider> { it.descriptor.priority }
                .thenBy { it.descriptor.id.value }
        ).firstOrNull()
    }

    fun failure(request: CaptureRequest): CaptureFailure {
        val policy = request.providerPolicy
        val requested = policy.explicitProviderId?.let(providersById::get)
            ?: providersById.values
                .filterNot { it.descriptor.id in policy.excludedProviderIds }
                .maxWithOrNull(compareBy<CaptureFrameProvider> { it.descriptor.priority }.thenBy { it.descriptor.id.value })
        return requested?.availability(request)?.toCaptureFailure(requested.descriptor.id, request.displayContext.displayId)
            ?: CaptureFailure(
                kind = CaptureFailureKind.PROVIDER_UNAVAILABLE,
                code = "CAPTURE_PROVIDER_NOT_REGISTERED",
                message = "No capture provider is registered for the request.",
                requestedDisplayId = request.displayContext.displayId,
            )
    }
}

class CaptureFrameService(
    providers: List<CaptureFrameProvider>,
    private val store: CaptureFrameStore = CaptureFrameStore(),
    private val idGenerator: CaptureFrameIdGenerator = UuidCaptureFrameIdGenerator,
) {
    private val registry = CaptureProviderRegistry(providers)

    suspend fun capture(request: CaptureRequest): CaptureResult {
        if (request.cancellation.isCancellationRequested()) return cancelled(request)
        request.reuseFrameId?.let { frameId ->
            store.acquire(frameId)?.let { lease ->
                if (lease.frame.metadata.actualDisplayContext.displayId == request.displayContext.displayId) {
                    return CaptureResult.Success(lease, ProviderId("capture.frame-store"), reused = true)
                }
                lease.close()
                return CaptureResult.Failure(
                    CaptureFailure(
                        CaptureFailureKind.DISPLAY_MISMATCH,
                        "CAPTURE_REUSE_DISPLAY_MISMATCH",
                        "Reusable frame belongs to a different display.",
                        requestedDisplayId = request.displayContext.displayId,
                        actualDisplayId = lease.frame.metadata.actualDisplayContext.displayId,
                    )
                )
            }
        }
        val provider = registry.resolve(request) ?: return CaptureResult.Failure(registry.failure(request))
        return try {
            when (val result = withTimeout(request.timeoutMillis) { provider.capture(request) }) {
                is ProviderCaptureResult.Failure -> CaptureResult.Failure(result.failure)
                is ProviderCaptureResult.Success -> materialize(request, provider.descriptor.id, result.payload)
            }
        } catch (_: TimeoutCancellationException) {
            CaptureResult.Failure(
                CaptureFailure(
                    CaptureFailureKind.TIMEOUT,
                    "CAPTURE_TIMEOUT",
                    "Capture timed out after ${request.timeoutMillis} ms.",
                    provider.descriptor.id,
                    request.displayContext.displayId,
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            CaptureResult.Failure(
                CaptureFailure(
                    CaptureFailureKind.CAPTURE_FAILED,
                    "CAPTURE_PROVIDER_FAILED",
                    error.message ?: "Capture provider failed.",
                    provider.descriptor.id,
                    request.displayContext.displayId,
                )
            )
        }
    }

    private fun materialize(
        request: CaptureRequest,
        providerId: ProviderId,
        payload: CapturedFramePayload,
    ): CaptureResult {
        if (request.cancellation.isCancellationRequested()) return cancelled(request, providerId)
        val actualDisplayId = payload.metadata.actualDisplayContext.displayId
        if (actualDisplayId != request.displayContext.displayId) {
            return CaptureResult.Failure(
                CaptureFailure(
                    CaptureFailureKind.DISPLAY_MISMATCH,
                    "CAPTURE_DISPLAY_MISMATCH",
                    "Provider captured display ${actualDisplayId.value}, requested ${request.displayContext.displayId.value}.",
                    providerId,
                    request.displayContext.displayId,
                    actualDisplayId,
                )
            )
        }
        if (payload.bytes.isEmpty()) {
            return CaptureResult.Failure(
                CaptureFailure(
                    CaptureFailureKind.INVALID_FRAME,
                    "CAPTURE_FRAME_EMPTY",
                    "Capture provider returned an empty frame.",
                    providerId,
                    request.displayContext.displayId,
                    actualDisplayId,
                )
            )
        }
        val id = idGenerator.nextId()
        val frame = CaptureFrame(
            id = id,
            metadata = payload.metadata,
            dataReference = CaptureDataReference(id, payload.mimeType, payload.bytes.size.toLong()),
        )
        val lease = store.publish(frame, payload.bytes) ?: return CaptureResult.Failure(
            CaptureFailure(
                CaptureFailureKind.RESOURCE_LIMIT,
                "CAPTURE_FRAME_STORE_LIMIT",
                "Capture frame exceeds the configured memory budget.",
                providerId,
                request.displayContext.displayId,
                actualDisplayId,
            )
        )
        return CaptureResult.Success(lease, providerId, reused = false)
    }

    fun acquire(frameId: CaptureFrameId): CaptureFrameLease? = store.acquire(frameId)
    fun releaseUnusedFrames() = store.clearReleased()

    private fun cancelled(request: CaptureRequest, providerId: ProviderId? = null) = CaptureResult.Failure(
        CaptureFailure(
            CaptureFailureKind.CANCELLED,
            "CAPTURE_CANCELLED",
            "Capture was cancelled before publication.",
            providerId,
            request.displayContext.displayId,
        )
    )
}
