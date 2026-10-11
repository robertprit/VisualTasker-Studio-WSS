package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.targeting.AccessibilityTreeSnapshot
import com.visualtasker.wss.targeting.ElementBoundsPx
import com.visualtasker.wss.targeting.ElementRef
import com.visualtasker.wss.targeting.ObservedAccessibilityElement
import com.visualtasker.wss.targeting.TargetActionSubmission
import com.visualtasker.wss.targeting.TargetResolutionPolicy
import java.util.ArrayDeque
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAccessibilityProviderTest {
    @Test
    fun uniqueTargetUsesNativeActionAndStructuredTrace() {
        val before = snapshot(node("node-0.0", "Login"))
        val after = snapshot(node("node-0.0", "Logged in"))
        val gateway = FakeTargetGateway(ArrayDeque(listOf(before, before, after)))

        val result = provider(gateway).invoke(invocation("Login"), context())

        assertTrue(result is ProviderResult.Success)
        result as ProviderResult.Success
        assertEquals("ACCESSIBILITY_ACTION_CLICK", result.targetTrace?.action)
        assertEquals("CHANGED", result.targetTrace?.observationStatus)
        assertFalse(result.targetTrace?.fallbackUsed ?: true)
        assertEquals("node-0.0", gateway.clicked.single().snapshotNodeId)
    }

    @Test
    fun postActionObservationWaitsForDelayedTreeChange() {
        val before = snapshot(node("node-0.0", "Login"))
        val after = snapshot(node("node-0.0", "Logged in"))
        val gateway = FakeTargetGateway(ArrayDeque(listOf(before, before, before, before, after)))
        val sleeps = mutableListOf<Long>()
        val provider = NativeAccessibilityProvider(
            targetGateway = gateway,
            gestureProvider = FakeGestureProvider(),
            postActionObservationAttempts = 4,
            postActionObservationIntervalMs = 25L,
            observationSleeper = sleeps::add,
        )

        val result = provider.invoke(invocation("Login"), context()) as ProviderResult.Success

        assertEquals("CHANGED", result.targetTrace?.observationStatus)
        assertEquals(listOf(25L, 25L), sleeps)
    }

    @Test
    fun ambiguousAndMissingTargetsReturnDeterministicDiagnostics() {
        val ambiguousGateway = FakeTargetGateway(
            ArrayDeque(listOf(snapshot(node("node-0.0", "Login"), node("node-0.1", "Login", 1)))),
        )
        val ambiguous = provider(ambiguousGateway).invoke(invocation("Login"), context())
        assertEquals("ACCESSIBILITY_TARGET_AMBIGUOUS", (ambiguous as ProviderResult.Failure).code)
        assertEquals(2, ambiguous.targetTrace?.candidateCount)

        val missing = provider(FakeTargetGateway(ArrayDeque(listOf(snapshot()))))
            .invoke(invocation("Login"), context())
        assertEquals("ACCESSIBILITY_TARGET_NOT_FOUND", (missing as ProviderResult.Failure).code)
    }

    @Test
    fun staleTreeAndServiceLossAreNotConvertedToCoordinateClicks() {
        val staleGateway = FakeTargetGateway(
            ArrayDeque(listOf(snapshot(node("node-0.0", "Login")), snapshot())),
        )
        val gesture = FakeGestureProvider()
        val stale = provider(staleGateway, gesture).invoke(invocation("Login"), context())
        assertEquals("ACCESSIBILITY_TARGET_STALE", (stale as ProviderResult.Failure).code)
        assertEquals(0, gesture.calls)

        val unavailable = provider(
            FakeTargetGateway(ArrayDeque(), AccessibilityTargetAvailability.SERVICE_UNAVAILABLE),
        ).invoke(invocation("Login"), context())
        assertEquals("ACCESSIBILITY_SERVICE_UNAVAILABLE", (unavailable as ProviderResult.Failure).code)
    }

    @Test
    fun cancellationBeforeAndDuringResolutionStopsBeforeAction() {
        val beforeToken = MutableCancellationToken().apply { cancel() }
        val beforeGateway = FakeTargetGateway(ArrayDeque(listOf(snapshot(node("node-0.0", "Login")))))
        assertTrue(provider(beforeGateway).invoke(invocation("Login"), context(beforeToken)) is ProviderResult.Cancelled)
        assertTrue(beforeGateway.clicked.isEmpty())

        val duringToken = MutableCancellationToken()
        val duringGateway = FakeTargetGateway(
            snapshots = ArrayDeque(listOf(snapshot(node("node-0.0", "Login")))),
            onCapture = { duringToken.cancel() },
        )
        assertTrue(provider(duringGateway).invoke(invocation("Login"), context(duringToken)) is ProviderResult.Cancelled)
        assertTrue(duringGateway.clicked.isEmpty())
    }

    @Test
    fun coordinateFallbackRequiresExplicitPolicy() {
        val tree = snapshot(node("node-0.0", "Login"))
        val forbiddenGesture = FakeGestureProvider()
        val forbiddenGateway = FakeTargetGateway(
            ArrayDeque(listOf(tree, tree)),
            actionSubmission = TargetActionSubmission.REJECTED,
        )
        val forbidden = provider(forbiddenGateway, forbiddenGesture)
            .invoke(invocation("Login"), context())
        assertEquals("ACCESSIBILITY_COORDINATE_FALLBACK_FORBIDDEN", (forbidden as ProviderResult.Failure).code)
        assertEquals(0, forbiddenGesture.calls)

        val allowedGesture = FakeGestureProvider()
        val allowedGateway = FakeTargetGateway(
            ArrayDeque(listOf(tree, tree)),
            actionSubmission = TargetActionSubmission.REJECTED,
        )
        val allowed = provider(allowedGateway, allowedGesture, allowFallback = true)
            .invoke(invocation("Login"), context())
        assertTrue(allowed is ProviderResult.Success)
        assertEquals(1, allowedGesture.calls)
        assertTrue((allowed as ProviderResult.Success).targetTrace?.fallbackUsed == true)
    }

    @Test
    fun canonicalTextQueryRunsThroughPlanWithoutWorkspaceMutation() {
        val imported = EmscriptWorkspaceImporter().import(
            script = "click(\"Login\")",
            workspaceId = "m5-7-text-query",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = requireNotNull(imported.document)
        val before = WorkflowSerializer.serialize(document)
        val tree = snapshot(node("node-0.0", "Login"))
        val gateway = FakeTargetGateway(ArrayDeque(listOf(tree, tree, tree)))
        val runner = DeterministicExecutionRunner(
            providers = ExecutionProviderRegistry(listOf(provider(gateway))),
        )

        val result = runner.execute(ExecutionRequest("request", document.id, document))

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals("node-0.0", gateway.clicked.single().snapshotNodeId)
        assertEquals(before, WorkflowSerializer.serialize(document))
        assertTrue(result.trace.any { it.targetTrace?.resolutionStatus == "FOUND" })
        val rail = AndroidRuntimeSnapshot(
            state = result.state,
            requestId = result.requestId,
            workflowId = document.id,
            trace = result.trace,
            result = result,
        ).toExecutionTrace()?.toRailTraceSteps().orEmpty()
        assertTrue(rail.any { it.properties["targetResolutionStatus"] == "FOUND" })
        assertTrue(rail.none { it.detail.orEmpty().contains("Login") })
    }

    @Test
    fun variableBoundQueryIsExplicitlyRejectedByCurrentLanguageContract() {
        val imported = EmscriptWorkspaceImporter().import(
            script = "LET target:String = \"Login\"\nclick(target)",
            workspaceId = "m5-7-variable-query-gap",
        )

        assertFalse(imported.isSuccess)
        assertTrue(imported.issues.any { it.message.contains("erwartet Text-String") })
    }

    private fun provider(
        gateway: AccessibilityTargetGateway,
        gesture: ExecutionProvider = FakeGestureProvider(),
        allowFallback: Boolean = false,
    ) = NativeAccessibilityProvider(
        targetGateway = gateway,
        gestureProvider = gesture,
        policyFactory = { displayId ->
            TargetResolutionPolicy(displayId, allowClickableAncestor = true, allowCoordinateFallback = allowFallback)
        },
    )

    private fun invocation(text: String) = ProviderInvocation(
        requestId = "request",
        workflowId = "workflow",
        entityId = "click-entity",
        commandId = "action.clickText",
        capability = NativeAccessibilityProvider.A11Y_CAPABILITY,
        arguments = listOf(ExecutionValue.StringValue(text)),
    )

    private fun context(token: CancellationToken = MutableCancellationToken()) = ExecutionContext(
        requestId = "request",
        workflowId = "workflow",
        variables = mutableMapOf(),
        cancellationToken = token,
    )

    private fun snapshot(vararg children: ObservedAccessibilityElement): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            snapshotId = "snapshot-${children.joinToString { it.ref.snapshotNodeId }}",
            capturedAtEpochMs = 1L,
            displayId = 0,
            windowId = "window",
            packageName = "app",
            root = ObservedAccessibilityElement(
                ref = ElementRef("node-0", "app", "Root", null, ElementBoundsPx(0, 0, 500, 500), "window", 0),
                parentNodeId = null,
                childOrder = 0,
                text = null,
                contentDescription = null,
                clickable = false,
                enabled = true,
                visible = true,
                children = children.toList(),
            ),
        )

    private fun node(id: String, text: String, order: Int = 0) = ObservedAccessibilityElement(
        ref = ElementRef(id, "app", "Button", "app:id/$id", ElementBoundsPx(10, 20 + order * 80, 110, 70 + order * 80), "window", 0),
        parentNodeId = "node-0",
        childOrder = order,
        text = text,
        contentDescription = null,
        clickable = true,
        enabled = true,
        visible = true,
    )

    private class FakeTargetGateway(
        private val snapshots: ArrayDeque<AccessibilityTreeSnapshot>,
        private val targetAvailability: AccessibilityTargetAvailability = AccessibilityTargetAvailability.AVAILABLE,
        private val actionSubmission: TargetActionSubmission = TargetActionSubmission.ACCEPTED,
        private val onCapture: () -> Unit = {},
    ) : AccessibilityTargetGateway {
        val clicked = mutableListOf<ElementRef>()
        override fun availability() = targetAvailability
        override fun captureTree(displayId: Int): AccessibilityTreeSnapshot? {
            val snapshot = if (snapshots.size > 1) snapshots.removeFirst() else snapshots.firstOrNull()
            onCapture()
            return snapshot
        }
        override fun performClick(target: ElementRef): TargetActionSubmission {
            clicked += target
            return actionSubmission
        }
    }

    private class FakeGestureProvider : ExecutionProvider {
        var calls = 0
        override val id = "fake.gesture"
        override val capabilities = setOf(NativeAccessibilityProvider.A11Y_CAPABILITY)
        override fun invoke(invocation: ProviderInvocation, context: ExecutionContext): ProviderResult {
            calls += 1
            return ProviderResult.Success()
        }
    }
}
