package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.BlockId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicExecutionRunnerTest {
    @Test
    fun linearWorkflowExecutesSeveralStepsInCanonicalOrder() {
        val fixture = fixture(
            """
            log("one")
            log("two")
            log("three")
            """.trimIndent(),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals(listOf("one", "two", "three"), fixture.provider.invocations.map { it.arguments.single().stringValue() })
    }

    @Test
    fun ifTrueExecutesOnlyThenBranch() {
        val fixture = fixture(
            """
            IF true
              log("then")
            ELSE
              log("else")
            END IF
            """.trimIndent(),
        )

        fixture.run()

        assertEquals(listOf("then"), fixture.provider.invocations.map { it.arguments.single().stringValue() })
    }

    @Test
    fun ifFalseExecutesOnlyElseBranch() {
        val fixture = fixture(
            """
            IF false
              log("then")
            ELSE
              log("else")
            END IF
            """.trimIndent(),
        )

        fixture.run()

        assertEquals(listOf("else"), fixture.provider.invocations.map { it.arguments.single().stringValue() })
    }

    @Test
    fun elseifSelectsFirstMatchingAlternative() {
        val fixture = fixture(
            """
            LET score = 2
            IF score < 1
              log("low")
            ELSEIF score == 2
              log("middle")
            ELSE
              log("high")
            END IF
            """.trimIndent(),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals(listOf("middle"), fixture.provider.invocations.map { it.arguments.single().stringValue() })
        assertTrue(result.trace.any { it.kind == ExecutionTraceEventKind.BRANCH_SELECTED && it.relationId?.contains("ELIF") == true })
    }

    @Test
    fun nestedExpressionsUseExistingOperatorSemantics() {
        val fixture = fixture(
            """
            LET value = 2 + 3 * 4
            IF value == 14
              log("correct")
            ELSE
              log("wrong")
            END IF
            """.trimIndent(),
        )

        val result = fixture.run()

        assertEquals(ExecutionValue.NumberValue(14.0), result.variables.values.single())
        assertEquals("correct", fixture.provider.invocations.single().arguments.single().stringValue())
    }

    @Test
    fun variablesCanBeDeclaredReadAndWritten() {
        val fixture = fixture(
            """
            LET count = 1
            SET count = count + 2
            log(count)
            """.trimIndent(),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals(ExecutionValue.NumberValue(3.0), result.variables.values.single())
        assertEquals(ExecutionValue.NumberValue(3.0), fixture.provider.invocations.single().arguments.single())
    }

    @Test
    fun executionOrderIsDeterministic() {
        val fixture = fixture("log(\"a\")\nlog(\"b\")")

        val result = fixture.run()
        val stepIds = result.trace.filter { it.kind == ExecutionTraceEventKind.STEP_STARTED }.mapNotNull { it.entityId }
        val planIds = fixture.plan.steps.map { it.entityId }

        assertEquals(planIds, stepIds)
    }

    @Test
    fun missingCapabilityFailsWithTypedDiagnostic() {
        val imported = import("log(\"missing\")")
        val result = DeterministicExecutionRunner(
            providers = ExecutionProviderRegistry(emptyList()),
        ).execute(request(imported))

        assertEquals(ExecutionState.FAILED, result.state)
        assertEquals("CAPABILITY_PROVIDER_MISSING", result.diagnostics.single().code)
    }

    @Test
    fun fakeProviderReturnsTypedValueForExpression() {
        val fixture = fixture(
            """
            LET text:String = Clipboard.get()
            log(text)
            """.trimIndent(),
            responses = mapOf("clipboard.get" to ProviderResult.Success(ExecutionValue.StringValue("payload"))),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertTrue(result.variables.values.contains(ExecutionValue.StringValue("payload")))
        assertTrue(result.trace.any {
            it.kind == ExecutionTraceEventKind.PROVIDER_FINISHED &&
                it.value == ExecutionValue.StringValue("payload")
        })
    }

    @Test
    fun catalogBoundReporterWithoutCommandPrefixUsesProviderContract() {
        val fixture = fixture(
            """
            IF screenContains("Login")
              log("visible")
            ELSE
              log("missing")
            END IF
            """.trimIndent(),
            responses = mapOf(
                "logic.screenContains" to ProviderResult.Success(ExecutionValue.BooleanValue(true)),
            ),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals(listOf("logic.screenContains", "debug.log"), fixture.provider.invocations.map { it.commandId })
        assertEquals("visible", fixture.provider.invocations.last().arguments.single().stringValue())
    }

    @Test
    fun providerFailureBecomesDefinedRuntimeFailure() {
        val fixture = fixture(
            "log(\"failure\")",
            defaultResult = ProviderResult.Failure("FAKE_PROVIDER_FAILURE", "expected failure"),
        )

        val result = fixture.run()

        assertEquals(ExecutionState.FAILED, result.state)
        assertEquals("FAKE_PROVIDER_FAILURE", result.diagnostics.single().code)
    }

    @Test
    fun cancellationBeforeExecutionProducesNoProviderCall() {
        val fixture = fixture("log(\"never\")")
        val token = MutableCancellationToken().apply { cancel() }

        val result = fixture.run(token)

        assertEquals(ExecutionState.CANCELLED, result.state)
        assertTrue(fixture.provider.invocations.isEmpty())
    }

    @Test
    fun cancellationDuringProviderCallStopsAtNextCheckpoint() {
        val token = MutableCancellationToken()
        val fixture = fixture("log(\"first\")\nlog(\"second\")", onInvoke = { token.cancel() })

        val result = fixture.run(token)

        assertEquals(ExecutionState.CANCELLED, result.state)
        assertEquals(1, fixture.provider.invocations.size)
        assertEquals("first", fixture.provider.invocations.single().arguments.single().stringValue())
    }

    @Test
    fun invalidWorkflowIsRejectedBeforeExecution() {
        val imported = import("log(\"invalid\")")
        val brokenId = imported.blocks.keys.first { imported.blocks.getValue(it).type != "event.start" }
        val broken = imported.copy(
            blocks = imported.blocks + (brokenId to imported.blocks.getValue(brokenId).copy(type = "unknown.block")),
        )

        val result = DeterministicExecutionRunner(
            providers = ExecutionProviderRegistry(emptyList()),
        ).execute(request(broken))

        assertEquals(ExecutionState.REJECTED, result.state)
        assertTrue(result.diagnostics.any { it.code == "WORKFLOW_INVALID" })
    }

    @Test
    fun knownButUnsupportedConstructIsRejectedWithoutImplicitExecution() {
        val imported = import("log(\"opaque\")")
        val logId = imported.blocks.keys.first { imported.blocks.getValue(it).type != "event.start" }
        val custom = imported.copy(
            blocks = imported.blocks + (logId to imported.blocks.getValue(logId).copy(type = "custom.unsupported")),
        )

        val plan = WorkflowExecutionPlanner().plan(request(custom))

        assertTrue(plan is ExecutionPlanResult.Rejected)
        val diagnostics = (plan as ExecutionPlanResult.Rejected).diagnostics
        assertTrue(diagnostics.any { it.code in setOf("WORKFLOW_INVALID", "UNSUPPORTED_WORKFLOW_CONSTRUCT") })
    }

    @Test
    fun traceReferencesOnlyStableCanonicalEntityIds() {
        val fixture = fixture("log(\"trace\")")

        val result = fixture.run()
        val canonicalIds = fixture.document.blocks.keys.map(BlockId::value).toSet()
        val tracedIds = result.trace.mapNotNull { it.entityId }.toSet()

        assertTrue(tracedIds.isNotEmpty())
        assertTrue(tracedIds.all { it in canonicalIds })
        assertTrue(result.trace.filter { it.kind == ExecutionTraceEventKind.PROVIDER_STARTED }.all { it.providerId == "fake" })
    }

    @Test
    fun repeatedExecutionWithSameInputsIsIdentical() {
        val fixture = fixture("LET value = 1\nSET value = value + 1\nlog(value)")

        val firstResult = fixture.run()
        val firstInvocations = fixture.provider.invocations.toList()
        fixture.provider.invocations.clear()
        val secondResult = fixture.run()

        assertEquals(firstResult.state, secondResult.state)
        assertEquals(firstResult.variables, secondResult.variables)
        assertEquals(firstResult.trace, secondResult.trace)
        assertEquals(firstInvocations, fixture.provider.invocations)
    }

    @Test
    fun lifecycleRejectsInvalidStateTransitions() {
        val lifecycle = ExecutionLifecycle()

        val failure = runCatching { lifecycle.transitionTo(ExecutionState.RUNNING) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(ExecutionState.CREATED, lifecycle.state)
    }

    @Test
    fun nullVoidAndFalseRemainDistinctRuntimeValues() {
        assertNotEquals(ExecutionValue.NullValue, ExecutionValue.VoidValue)
        assertNotEquals(ExecutionValue.NullValue, ExecutionValue.BooleanValue(false))
        assertNotEquals(ExecutionValue.VoidValue, ExecutionValue.BooleanValue(false))
    }

    private fun fixture(
        script: String,
        responses: Map<String, ProviderResult> = emptyMap(),
        defaultResult: ProviderResult = ProviderResult.Success(),
        onInvoke: () -> Unit = {},
    ): Fixture {
        val document = import(script)
        val request = request(document)
        val plan = (WorkflowExecutionPlanner().plan(request) as ExecutionPlanResult.Ready).plan
        val provider = FakeProvider(
            capabilities = plan.requiredCapabilities,
            responses = responses,
            defaultResult = defaultResult,
            onInvoke = onInvoke,
        )
        return Fixture(
            document = document,
            request = request,
            plan = plan,
            provider = provider,
            runner = DeterministicExecutionRunner(
                providers = ExecutionProviderRegistry(listOf(provider)),
            ),
        )
    }

    private fun import(script: String) = EmscriptWorkspaceImporter().import(
        script = script,
        workspaceId = "m5-runtime-test",
    ).let { result ->
        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        requireNotNull(result.document)
    }

    private fun request(document: de.visualtasker.workflow.core.WorkspaceDocument) = ExecutionRequest(
        id = "request:m5",
        workflowId = document.id,
        document = document,
    )

    private data class Fixture(
        val document: de.visualtasker.workflow.core.WorkspaceDocument,
        val request: ExecutionRequest,
        val plan: ExecutionPlan,
        val provider: FakeProvider,
        val runner: DeterministicExecutionRunner,
    ) {
        fun run(token: CancellationToken = MutableCancellationToken()): ExecutionResult = runner.execute(request, token)
    }

    private class FakeProvider(
        override val capabilities: Set<ExecutionCapability>,
        private val responses: Map<String, ProviderResult>,
        private val defaultResult: ProviderResult,
        private val onInvoke: () -> Unit,
    ) : ExecutionProvider {
        override val id: String = "fake"
        val invocations = mutableListOf<ProviderInvocation>()

        override fun invoke(invocation: ProviderInvocation, context: ExecutionContext): ProviderResult {
            invocations += invocation
            onInvoke()
            return responses[invocation.commandId] ?: defaultResult
        }
    }

    private fun ExecutionValue.stringValue(): String = (this as ExecutionValue.StringValue).value
}
