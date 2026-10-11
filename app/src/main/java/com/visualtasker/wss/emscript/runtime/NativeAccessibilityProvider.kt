package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import com.visualtasker.wss.targeting.AccessibilityTargetResolver
import com.visualtasker.wss.targeting.AccessibilityTreeSnapshot
import com.visualtasker.wss.targeting.ElementQuery
import com.visualtasker.wss.targeting.ElementRef
import com.visualtasker.wss.targeting.TargetActionSubmission
import com.visualtasker.wss.targeting.TargetResolution
import com.visualtasker.wss.targeting.TargetResolutionPolicy
import com.visualtasker.wss.targeting.TargetResolutionStatus
import de.visualtasker.workflow.semantics.CommandCapability

enum class AccessibilityTargetAvailability {
    AVAILABLE,
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
}

interface AccessibilityTargetGateway {
    fun availability(): AccessibilityTargetAvailability
    fun captureTree(displayId: Int): AccessibilityTreeSnapshot?
    fun performClick(target: ElementRef): TargetActionSubmission
}

class AndroidAccessibilityTargetGateway(
    private val serviceProvider: () -> VisualTaskerAccessibilityService? = VisualTaskerAccessibilityService::current,
) : AccessibilityTargetGateway {
    override fun availability(): AccessibilityTargetAvailability =
        if (serviceProvider() == null) AccessibilityTargetAvailability.SERVICE_UNAVAILABLE
        else AccessibilityTargetAvailability.AVAILABLE

    override fun captureTree(displayId: Int): AccessibilityTreeSnapshot? =
        serviceProvider()?.captureTargetTree(displayId)

    override fun performClick(target: ElementRef): TargetActionSubmission =
        serviceProvider()?.performTargetClick(target) ?: TargetActionSubmission.STALE
}

class NativeAccessibilityProvider(
    private val targetGateway: AccessibilityTargetGateway = AndroidAccessibilityTargetGateway(),
    private val gestureProvider: ExecutionProvider = NativeGestureProvider(),
    private val policyFactory: (Int) -> TargetResolutionPolicy = { displayId ->
        TargetResolutionPolicy(
            displayId = displayId,
            allowClickableAncestor = true,
            allowCoordinateFallback = false,
        )
    },
    private val postActionObservationAttempts: Int = 5,
    private val postActionObservationIntervalMs: Long = 30L,
    private val observationSleeper: (Long) -> Unit = Thread::sleep,
) : ExecutionProvider {
    override val id: String = PROVIDER_ID
    override val capabilities: Set<ExecutionCapability> = setOf(A11Y_CAPABILITY)

    override fun invoke(invocation: ProviderInvocation, context: ExecutionContext): ProviderResult {
        if (invocation.commandId !in SEMANTIC_CLICK_COMMAND_IDS) {
            return gestureProvider.invoke(invocation, context)
        }
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled(
                message = "Accessibility target resolution cancelled before capture.",
                targetTrace = trace("TEXT", 0, "CANCELLED", DEFAULT_DISPLAY_ID),
            )
        }
        val queryText = (invocation.arguments.singleOrNull() as? ExecutionValue.StringValue)
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return failure(
                status = TargetResolutionStatus.NOT_FOUND,
                queryType = "TEXT",
                candidateCount = 0,
                displayId = DEFAULT_DISPLAY_ID,
                code = "ACCESSIBILITY_TEXT_QUERY_REQUIRED",
                message = "clickText requires exactly one non-empty String value.",
            )
        when (targetGateway.availability()) {
            AccessibilityTargetAvailability.PERMISSION_DENIED -> return failure(
                TargetResolutionStatus.PERMISSION_DENIED,
                "TEXT",
                0,
                DEFAULT_DISPLAY_ID,
                "ACCESSIBILITY_PERMISSION_DENIED",
                "Accessibility permission is not granted.",
            )
            AccessibilityTargetAvailability.SERVICE_UNAVAILABLE -> return failure(
                TargetResolutionStatus.SERVICE_UNAVAILABLE,
                "TEXT",
                0,
                DEFAULT_DISPLAY_ID,
                "ACCESSIBILITY_SERVICE_UNAVAILABLE",
                "VisualTasker AccessibilityService is not connected.",
            )
            AccessibilityTargetAvailability.AVAILABLE -> Unit
        }

        val query = ElementQuery(
            text = queryText,
            displayId = DEFAULT_DISPLAY_ID,
            matchTextOrDescription = true,
        )
        val policy = policyFactory(DEFAULT_DISPLAY_ID)
        val firstSnapshot = targetGateway.captureTree(DEFAULT_DISPLAY_ID)
            ?: return unavailableTree(query, policy)
        val firstResolution = AccessibilityTargetResolver.resolve(firstSnapshot, query, policy)
        if (firstResolution.status != TargetResolutionStatus.FOUND) return firstResolution.toFailure()
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled(
                message = "Accessibility target resolution cancelled before validation.",
                targetTrace = firstResolution.toTrace(resolutionStatus = "CANCELLED"),
            )
        }

        val validatedSnapshot = targetGateway.captureTree(DEFAULT_DISPLAY_ID)
            ?: return firstResolution.staleFailure("Accessibility tree disappeared before action.")
        val validatedResolution = AccessibilityTargetResolver.resolve(validatedSnapshot, query, policy)
        if (validatedResolution.status != TargetResolutionStatus.FOUND) {
            return firstResolution.staleFailure(
                "Accessibility target changed before action (${validatedResolution.status}).",
            )
        }
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled(
                message = "Accessibility target resolution cancelled before action.",
                targetTrace = validatedResolution.toTrace(resolutionStatus = "CANCELLED"),
            )
        }

        val target = requireNotNull(validatedResolution.match).actionTarget
        return when (targetGateway.performClick(target)) {
            TargetActionSubmission.ACCEPTED -> {
                val observation = observePostAction(validatedSnapshot, context)
                if (observation == "CANCELLED") {
                    return ProviderResult.Cancelled(
                        message = "Accessibility action observation cancelled.",
                        targetTrace = validatedResolution.toTrace(
                            action = "ACCESSIBILITY_ACTION_CLICK",
                            observationStatus = observation,
                        ),
                    )
                }
                ProviderResult.Success(
                    targetTrace = validatedResolution.toTrace(
                        action = "ACCESSIBILITY_ACTION_CLICK",
                        observationStatus = observation,
                    ),
                )
            }
            TargetActionSubmission.STALE -> validatedResolution.staleFailure(
                "Accessibility target became stale during native action dispatch.",
            )
            TargetActionSubmission.REJECTED -> {
                if (!policy.allowCoordinateFallback) {
                    return ProviderResult.Failure(
                        code = "ACCESSIBILITY_COORDINATE_FALLBACK_FORBIDDEN",
                        message = "Native ACTION_CLICK was rejected and coordinate fallback is disabled.",
                        targetTrace = validatedResolution.toTrace(
                            action = "ACCESSIBILITY_ACTION_CLICK",
                            observationStatus = "REJECTED",
                        ),
                    )
                }
                val fallbackInvocation = invocation.copy(
                    commandId = "input.clickPoint",
                    arguments = listOf(
                        ExecutionValue.NumberValue(target.bounds.centerX.toDouble()),
                        ExecutionValue.NumberValue(target.bounds.centerY.toDouble()),
                    ),
                )
                when (val fallback = gestureProvider.invoke(fallbackInvocation, context)) {
                    is ProviderResult.Success -> fallback.copy(
                        targetTrace = validatedResolution.toTrace(
                            action = "COORDINATE_FALLBACK",
                            fallbackUsed = true,
                        ),
                    )
                    is ProviderResult.Failure -> fallback.copy(
                        targetTrace = validatedResolution.toTrace(
                            action = "COORDINATE_FALLBACK",
                            fallbackUsed = true,
                            observationStatus = "FAILED",
                        ),
                    )
                    is ProviderResult.Cancelled -> fallback.copy(
                        targetTrace = validatedResolution.toTrace(
                            action = "COORDINATE_FALLBACK",
                            fallbackUsed = true,
                            observationStatus = "CANCELLED",
                        ),
                    )
                }
            }
        }
    }

    private fun observePostAction(
        before: AccessibilityTreeSnapshot,
        context: ExecutionContext,
    ): String {
        var captured = false
        repeat(postActionObservationAttempts.coerceAtLeast(1)) { attempt ->
            if (context.cancellationToken.isCancellationRequested) return "CANCELLED"
            targetGateway.captureTree(DEFAULT_DISPLAY_ID)?.let { after ->
                captured = true
                if (after.root != before.root) return "CHANGED"
            }
            if (attempt < postActionObservationAttempts - 1 && postActionObservationIntervalMs > 0L) {
                observationSleeper(postActionObservationIntervalMs)
            }
        }
        return if (captured) "UNCHANGED" else "UNAVAILABLE"
    }

    private fun unavailableTree(query: ElementQuery, policy: TargetResolutionPolicy): ProviderResult =
        if (query.displayId != policy.displayId || query.displayId != DEFAULT_DISPLAY_ID) {
            failure(
                TargetResolutionStatus.DISPLAY_UNSUPPORTED,
                query.queryType,
                0,
                query.displayId,
                "ACCESSIBILITY_DISPLAY_UNSUPPORTED",
                "Accessibility target display ${query.displayId} is not supported by this provider.",
            )
        } else {
            failure(
                TargetResolutionStatus.SERVICE_UNAVAILABLE,
                query.queryType,
                0,
                query.displayId,
                "ACCESSIBILITY_TREE_UNAVAILABLE",
                "Accessibility tree is unavailable for the active display.",
            )
        }

    private fun TargetResolution.toFailure(): ProviderResult.Failure {
        val (code, message) = when (status) {
            TargetResolutionStatus.NOT_FOUND -> "ACCESSIBILITY_TARGET_NOT_FOUND" to "No matching accessibility target was found."
            TargetResolutionStatus.AMBIGUOUS -> "ACCESSIBILITY_TARGET_AMBIGUOUS" to "Multiple equivalent accessibility targets matched the query."
            TargetResolutionStatus.NOT_ACTIONABLE -> "ACCESSIBILITY_TARGET_NOT_ACTIONABLE" to "The matching accessibility target is not actionable."
            TargetResolutionStatus.DISPLAY_UNSUPPORTED -> "ACCESSIBILITY_DISPLAY_UNSUPPORTED" to "The requested accessibility display is unsupported."
            else -> "ACCESSIBILITY_TARGET_RESOLUTION_FAILED" to "Accessibility target resolution failed with $status."
        }
        return ProviderResult.Failure(code, message, toTrace())
    }

    private fun TargetResolution.staleFailure(message: String): ProviderResult.Failure =
        ProviderResult.Failure(
            code = "ACCESSIBILITY_TARGET_STALE",
            message = message,
            targetTrace = toTrace(resolutionStatus = TargetResolutionStatus.STALE.name),
        )

    private fun TargetResolution.toTrace(
        resolutionStatus: String = status.name,
        action: String? = null,
        fallbackUsed: Boolean = false,
        observationStatus: String? = null,
    ): TargetResolutionTraceInfo = trace(
        queryType = query.queryType,
        candidateCount = candidateCount,
        resolutionStatus = resolutionStatus,
        displayId = query.displayId,
        action = action,
        fallbackUsed = fallbackUsed,
        observationStatus = observationStatus,
    )

    private fun failure(
        status: TargetResolutionStatus,
        queryType: String,
        candidateCount: Int,
        displayId: Int,
        code: String,
        message: String,
    ): ProviderResult.Failure = ProviderResult.Failure(
        code = code,
        message = message,
        targetTrace = trace(queryType, candidateCount, status.name, displayId),
    )

    companion object {
        const val PROVIDER_ID = "android.native.accessibility"
        const val DEFAULT_DISPLAY_ID = 0
        val A11Y_CAPABILITY = ExecutionCapability(CommandCapability.A11Y.name)
        val SEMANTIC_CLICK_COMMAND_IDS = setOf("action.clickText", "clickText", "click")

        private fun trace(
            queryType: String,
            candidateCount: Int,
            resolutionStatus: String,
            displayId: Int,
            action: String? = null,
            fallbackUsed: Boolean = false,
            observationStatus: String? = null,
        ) = TargetResolutionTraceInfo(
            queryType = queryType,
            candidateCount = candidateCount,
            resolutionStatus = resolutionStatus,
            action = action,
            fallbackUsed = fallbackUsed,
            displayId = displayId,
            observationStatus = observationStatus,
        )
    }
}
