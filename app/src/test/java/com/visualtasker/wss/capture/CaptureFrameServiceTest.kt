package com.visualtasker.wss.capture

import com.visualtasker.wss.emscript.runtime.ProviderAvailability
import com.visualtasker.wss.emscript.runtime.ProviderCapabilityState
import com.visualtasker.wss.emscript.runtime.ProviderExecutionState
import com.visualtasker.wss.emscript.runtime.ProviderPermissionState
import com.visualtasker.wss.emscript.runtime.ProviderSelectionPolicy
import com.visualtasker.wss.window.DisplayContext
import com.visualtasker.wss.window.DisplayId
import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace
import com.visualtasker.wss.window.WindowInsets
import com.visualtasker.wss.window.WindowPoint
import de.visualtasker.emscript.contract.ProviderId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFrameServiceTest {
    @Test
    fun successfulCapturePublishesTypedDisplayAwareFrame() = runBlocking {
        val provider = FakeProvider()
        val service = service(provider)

        val result = service.capture(request()) as CaptureResult.Success

        assertEquals(ProviderId("capture.fake"), result.providerId)
        assertEquals(DisplayId(7), result.lease.frame.metadata.actualDisplayContext.displayId)
        assertEquals(WindowCoordinateSpace.SCREENSHOT_PX, result.lease.frame.metadata.coordinateSpace)
        assertArrayEquals(PNG, result.lease.readBytes())
        result.lease.close()
    }

    @Test
    fun permissionFailureIsTypedAndProviderIsNotCalled() = runBlocking {
        val provider = FakeProvider(
            availability = ProviderAvailability(
                permission = ProviderPermissionState.DENIED,
                execution = ProviderExecutionState.NOT_EXECUTABLE,
                diagnosticCode = "PERMISSION_DENIED",
            )
        )

        val result = service(provider).capture(request()) as CaptureResult.Failure

        assertEquals(CaptureFailureKind.PERMISSION_MISSING, result.failure.kind)
        assertEquals(0, provider.captureCalls)
    }

    @Test
    fun unsupportedDisplayIsTyped() = runBlocking {
        val provider = FakeProvider(
            availability = ProviderAvailability(
                capability = ProviderCapabilityState.UNSUPPORTED,
                execution = ProviderExecutionState.NOT_EXECUTABLE,
                diagnosticCode = "DISPLAY_UNSUPPORTED",
            )
        )
        val result = service(provider).capture(request()) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.DISPLAY_UNSUPPORTED, result.failure.kind)
    }

    @Test
    fun actualDisplayMismatchIsRejectedWithoutPublication() = runBlocking {
        val provider = FakeProvider(actualDisplay = display(id = 8))
        val result = service(provider).capture(request()) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.DISPLAY_MISMATCH, result.failure.kind)
        assertEquals(DisplayId(7), result.failure.requestedDisplayId)
        assertEquals(DisplayId(8), result.failure.actualDisplayId)
    }

    @Test
    fun cancellationBeforeCaptureDoesNotInvokeProvider() = runBlocking {
        val provider = FakeProvider()
        val result = service(provider).capture(
            request().copy(cancellation = CaptureCancellation { true })
        ) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.CANCELLED, result.failure.kind)
        assertEquals(0, provider.captureCalls)
    }

    @Test
    fun timeoutIsTyped() = runBlocking {
        val provider = FakeProvider(delayMillis = 100)
        val result = service(provider).capture(request().copy(timeoutMillis = 10)) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.TIMEOUT, result.failure.kind)
    }

    @Test
    fun providerLossIsPreserved() = runBlocking {
        val provider = FakeProvider(
            resultFailure = CaptureFailure(
                CaptureFailureKind.PROVIDER_LOST,
                "PROVIDER_LOST",
                "lost",
                ProviderId("capture.fake"),
            )
        )
        val result = service(provider).capture(request()) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.PROVIDER_LOST, result.failure.kind)
    }

    @Test
    fun unsupportedProviderCapabilityIsPreserved() = runBlocking {
        val provider = FakeProvider(
            resultFailure = CaptureFailure(
                CaptureFailureKind.CAPABILITY_UNSUPPORTED,
                "CAPTURE_CROP_UNSUPPORTED",
                "crop unsupported",
                ProviderId("capture.fake"),
            )
        )
        val result = service(provider).capture(
            request().copy(
                cropDisplayPx = WindowBounds(
                    10f,
                    20f,
                    50f,
                    100f,
                    WindowCoordinateSpace.DISPLAY_PX,
                )
            )
        ) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.CAPABILITY_UNSUPPORTED, result.failure.kind)
    }

    @Test
    fun emptyProviderPayloadIsRejectedAsInvalidFrame() = runBlocking {
        val result = service(FakeProvider(bytes = byteArrayOf())).capture(request()) as CaptureResult.Failure
        assertEquals(CaptureFailureKind.INVALID_FRAME, result.failure.kind)
    }

    @Test
    fun multipleConsumersShareOneCaptureAndReleaseIndependently() = runBlocking {
        val store = CaptureFrameStore()
        val provider = FakeProvider()
        val service = service(provider, store)
        val first = service.capture(request()) as CaptureResult.Success
        val second = first.lease.retain()

        assertEquals(1, provider.captureCalls)
        assertEquals(2, store.activeReferenceCount(first.lease.frame.id))
        first.lease.close()
        assertArrayEquals(PNG, second.readBytes())
        second.close()
        assertEquals(0, store.activeReferenceCount(first.lease.frame.id))
        assertTrue(store.discard(first.lease.frame.id))
    }

    @Test
    fun requestCanReusePublishedFrameWithoutSecondCapture() = runBlocking {
        val provider = FakeProvider()
        val service = service(provider)
        val first = service.capture(request()) as CaptureResult.Success
        val reused = service.capture(request().copy(reuseFrameId = first.lease.frame.id)) as CaptureResult.Success

        assertTrue(reused.reused)
        assertEquals(1, provider.captureCalls)
        assertEquals(first.lease.frame.id, reused.lease.frame.id)
        first.lease.close()
        reused.lease.close()
    }

    @Test
    fun frameStoreEnforcesMemoryBudgetAndEvictsReleasedFrames() {
        val store = CaptureFrameStore(maxBytes = 6)
        val first = frame("first", byteCount = 4)
        val firstLease = requireNotNull(store.publish(first, byteArrayOf(1, 2, 3, 4)))
        assertEquals(null, store.publish(frame("blocked", 4), byteArrayOf(5, 6, 7, 8)))
        firstLease.close()
        val second = store.publish(frame("second", 4), byteArrayOf(5, 6, 7, 8))
        assertNotNull(second)
        assertEquals(4L, store.byteCount())
        second?.close()
    }

    @Test
    fun cropAndScreenshotScalingAreReversible() {
        val metadata = metadata(
            width = 100,
            height = 200,
            bounds = WindowBounds(10f, 20f, 50f, 100f, WindowCoordinateSpace.DISPLAY_PX),
        )
        val mapper = CaptureCoordinateMapper(metadata)
        val screenshot = mapper.displayToScreenshot(WindowPoint(35f, 70f, WindowCoordinateSpace.DISPLAY_PX))
        assertEquals(50f, screenshot.x, 0.001f)
        assertEquals(100f, screenshot.y, 0.001f)
        val display = mapper.screenshotToDisplay(screenshot)
        assertEquals(35f, display.x, 0.001f)
        assertEquals(70f, display.y, 0.001f)
    }

    @Test
    fun rotationMappingIsReversible() {
        val metadata = metadata(
            width = 200,
            height = 100,
            rotation = 90,
            bounds = WindowBounds(0f, 0f, 100f, 200f, WindowCoordinateSpace.DISPLAY_PX),
        )
        val mapper = CaptureCoordinateMapper(metadata)
        val source = WindowPoint(20f, 40f, WindowCoordinateSpace.DISPLAY_PX)
        val result = mapper.screenshotToDisplay(mapper.displayToScreenshot(source))
        assertEquals(source.x, result.x, 0.001f)
        assertEquals(source.y, result.y, 0.001f)
    }

    @Test
    fun densityInsetsAndRegionLocalRemainExplicit() {
        val metadata = metadata(width = 100, height = 200)
        assertEquals(2f, metadata.actualDisplayContext.density, 0f)
        assertEquals(WindowInsets(1, 2, 3, 4), metadata.actualDisplayContext.insets)
        val local = CaptureCoordinateMapper(metadata).screenshotToRegionLocal(
            WindowPoint(40f, 60f, WindowCoordinateSpace.SCREENSHOT_PX),
            WindowBounds(10f, 20f, 50f, 100f, WindowCoordinateSpace.SCREENSHOT_PX),
        )
        assertEquals(WindowPoint(30f, 40f, WindowCoordinateSpace.LOCAL_WINDOW_PX), local)
    }

    @Test
    fun explicitProviderPolicyUsesRegisteredProvider() = runBlocking {
        val ignored = FakeProvider(id = "capture.ignored", priority = 100)
        val selected = FakeProvider(id = "capture.selected", priority = 1)
        val service = CaptureFrameService(listOf(ignored, selected), idGenerator = ids())
        val result = service.capture(
            request().copy(providerPolicy = ProviderSelectionPolicy(explicitProviderId = ProviderId("capture.selected")))
        ) as CaptureResult.Success
        assertEquals(ProviderId("capture.selected"), result.providerId)
        assertEquals(0, ignored.captureCalls)
        assertEquals(1, selected.captureCalls)
        result.lease.close()
    }

    @Test
    fun equalPriorityProvidersResolveByStableProviderId() = runBlocking {
        val second = FakeProvider(id = "capture.z", priority = 10)
        val first = FakeProvider(id = "capture.a", priority = 10)
        val result = CaptureFrameService(listOf(second, first), idGenerator = ids())
            .capture(request()) as CaptureResult.Success
        assertEquals(ProviderId("capture.a"), result.providerId)
        assertEquals(1, first.captureCalls)
        assertEquals(0, second.captureCalls)
        result.lease.close()
    }

    @Test
    fun captureCoreHasNoWorkspaceMutationDependency() {
        val contractTypes = listOf(
            CaptureFrame::class.java,
            CaptureRequest::class.java,
            Observation::class.java,
            CaptureFrameService::class.java,
        )
        assertFalse(
            contractTypes.flatMap { it.declaredFields.toList() }
                .any { it.type.name.contains("workspace", ignoreCase = true) }
        )
    }

    @Test
    fun releasedLeaseCannotReadFrameData() = runBlocking {
        val result = service(FakeProvider()).capture(request()) as CaptureResult.Success
        result.lease.close()
        assertTrue(runCatching { result.lease.readBytes() }.isFailure)
    }

    private fun service(provider: FakeProvider, store: CaptureFrameStore = CaptureFrameStore()) =
        CaptureFrameService(listOf(provider), store, ids())

    private fun ids(): CaptureFrameIdGenerator {
        var next = 0
        return CaptureFrameIdGenerator { CaptureFrameId("frame-${++next}") }
    }

    private fun request() = CaptureRequest("request-1", display())

    private fun display(id: Int = 7) = DisplayContext(
        DisplayId(id), 100, 200, 2f, 0, WindowInsets(1, 2, 3, 4),
    )

    private fun metadata(
        width: Int,
        height: Int,
        rotation: Int = 0,
        bounds: WindowBounds = WindowBounds(0f, 0f, 100f, 200f, WindowCoordinateSpace.DISPLAY_PX),
    ) = CaptureMetadata(
        requestedDisplayId = DisplayId(7),
        actualDisplayContext = display(),
        capturedAtEpochMs = 1_000,
        capturedAtElapsedRealtimeNanos = 2_000,
        widthPx = width,
        heightPx = height,
        rotation = rotation,
        densityDpi = 320,
        source = CaptureSource.TEST,
        captureBoundsDisplayPx = bounds,
    )

    private fun frame(id: String, byteCount: Long) = CaptureFrame(
        CaptureFrameId(id),
        metadata(100, 200),
        CaptureDataReference(CaptureFrameId(id), "image/png", byteCount),
    )

    private inner class FakeProvider(
        id: String = "capture.fake",
        priority: Int = 10,
        private val availability: ProviderAvailability = ProviderAvailability.available(),
        private val actualDisplay: DisplayContext = display(),
        private val delayMillis: Long = 0,
        private val resultFailure: CaptureFailure? = null,
        private val bytes: ByteArray = PNG,
    ) : CaptureFrameProvider {
        var captureCalls = 0
        override val descriptor = CaptureProviderDescriptor(
            ProviderId(id), id, setOf(CaptureCapability.SCREENSHOT), priority,
        )

        override fun availability(request: CaptureRequest) = availability

        override suspend fun capture(request: CaptureRequest): ProviderCaptureResult {
            captureCalls += 1
            if (delayMillis > 0) delay(delayMillis)
            resultFailure?.let { return ProviderCaptureResult.Failure(it) }
            return ProviderCaptureResult.Success(
                CapturedFramePayload(
                    bytes,
                    metadata(100, 200).copy(
                        requestedDisplayId = request.displayContext.displayId,
                        actualDisplayContext = actualDisplay,
                    ),
                )
            )
        }
    }

    private companion object {
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)
    }
}
