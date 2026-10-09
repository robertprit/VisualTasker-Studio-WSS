package com.visualtasker.wss.emscript.runtime

class DeterministicExecutionRunner(
    private val planner: WorkflowExecutionPlanner = WorkflowExecutionPlanner(),
    private val providers: ExecutionProviderRegistry,
    private val maxSteps: Int = 10_000,
    private val maxLoopIterations: Int = 1_000,
) {
    fun execute(
        request: ExecutionRequest,
        cancellationToken: CancellationToken = MutableCancellationToken(),
        traceObserver: (ExecutionTraceEvent) -> Unit = {},
    ): ExecutionResult {
        val lifecycle = ExecutionLifecycle()
        val trace = mutableListOf<ExecutionTraceEvent>()
        val diagnostics = mutableListOf<ExecutionDiagnostic>()
        val context = ExecutionContext(
            requestId = request.id,
            workflowId = request.workflowId,
            variables = request.initialVariables.toMutableMap(),
            cancellationToken = cancellationToken,
        )
        fun event(
            kind: ExecutionTraceEventKind,
            entityId: String? = null,
            relationId: String? = null,
            capability: ExecutionCapability? = null,
            providerId: String? = null,
            value: ExecutionValue? = null,
            diagnostic: ExecutionDiagnostic? = null,
        ) {
            val traceEvent = ExecutionTraceEvent(
                index = trace.size + 1,
                kind = kind,
                state = lifecycle.state,
                entityId = entityId,
                relationId = relationId,
                capability = capability,
                providerId = providerId,
                value = value,
                diagnostic = diagnostic,
            )
            trace += traceEvent
            traceObserver(traceEvent)
        }
        fun transition(next: ExecutionState) {
            lifecycle.transitionTo(next)
            event(ExecutionTraceEventKind.STATE_CHANGED)
        }

        transition(ExecutionState.VALIDATING)
        val plan = when (val result = planner.plan(request)) {
            is ExecutionPlanResult.Ready -> result.plan
            is ExecutionPlanResult.Rejected -> {
                diagnostics += result.diagnostics
                transition(ExecutionState.REJECTED)
                result.diagnostics.forEach { event(ExecutionTraceEventKind.DIAGNOSTIC, entityId = it.entityId, diagnostic = it) }
                return result(request, lifecycle, context, diagnostics, trace)
            }
        }
        transition(ExecutionState.READY)
        if (cancellationToken.isCancellationRequested) {
            transition(ExecutionState.CANCELLED)
            event(ExecutionTraceEventKind.CANCELLED, entityId = plan.entryPointEntityId)
            return result(request, lifecycle, context, diagnostics, trace)
        }

        transition(ExecutionState.RUNNING)
        event(ExecutionTraceEventKind.EXECUTION_STARTED, entityId = plan.entryPointEntityId)
        val machine = Machine(
            context = context,
            providers = providers,
            trace = trace,
            diagnostics = diagnostics,
            lifecycle = lifecycle,
            maxSteps = maxSteps,
            maxLoopIterations = maxLoopIterations,
            traceObserver = traceObserver,
        )
        return try {
            machine.execute(plan.steps)
            if (cancellationToken.isCancellationRequested) {
                transition(ExecutionState.CANCELLED)
                event(ExecutionTraceEventKind.CANCELLED, entityId = machine.currentEntityId)
            } else {
                transition(ExecutionState.COMPLETED)
                event(ExecutionTraceEventKind.EXECUTION_FINISHED, entityId = plan.entryPointEntityId, value = ExecutionValue.VoidValue)
            }
            result(request, lifecycle, context, diagnostics, trace)
        } catch (cancelled: ExecutionCancelled) {
            transition(ExecutionState.CANCELLED)
            event(ExecutionTraceEventKind.CANCELLED, entityId = cancelled.entityId)
            result(request, lifecycle, context, diagnostics, trace)
        } catch (failed: ExecutionFailed) {
            diagnostics += failed.diagnostic
            transition(ExecutionState.FAILED)
            event(ExecutionTraceEventKind.DIAGNOSTIC, entityId = failed.diagnostic.entityId, diagnostic = failed.diagnostic)
            event(ExecutionTraceEventKind.EXECUTION_FINISHED, entityId = plan.entryPointEntityId)
            result(request, lifecycle, context, diagnostics, trace)
        }
    }

    private fun result(
        request: ExecutionRequest,
        lifecycle: ExecutionLifecycle,
        context: ExecutionContext,
        diagnostics: List<ExecutionDiagnostic>,
        trace: List<ExecutionTraceEvent>,
    ): ExecutionResult = ExecutionResult(
        requestId = request.id,
        state = lifecycle.state,
        value = if (lifecycle.state == ExecutionState.COMPLETED) ExecutionValue.VoidValue else ExecutionValue.NullValue,
        variables = context.variables.toMap(),
        diagnostics = diagnostics.toList(),
        trace = trace.toList(),
    )
}

private class Machine(
    private val context: ExecutionContext,
    private val providers: ExecutionProviderRegistry,
    private val trace: MutableList<ExecutionTraceEvent>,
    private val diagnostics: MutableList<ExecutionDiagnostic>,
    private val lifecycle: ExecutionLifecycle,
    private val maxSteps: Int,
    private val maxLoopIterations: Int,
    private val traceObserver: (ExecutionTraceEvent) -> Unit,
) {
    var currentEntityId: String? = null
        private set
    private var stepCount = 0

    fun execute(steps: List<ExecutionStep>) {
        steps.forEach { step ->
            checkpoint(step.entityId)
            currentEntityId = step.entityId
            emit(ExecutionTraceEventKind.STEP_STARTED, entityId = step.entityId)
            val value = when (step) {
                is ExecutionStep.Assign -> evaluate(step.expression).also { value ->
                    context.variables[step.variableId] = value
                }
                is ExecutionStep.Invoke -> invoke(step.entityId, step.commandId, step.capability, step.arguments)
                is ExecutionStep.Branch -> executeBranch(step)
                is ExecutionStep.Repeat -> executeRepeat(step)
                is ExecutionStep.While -> executeWhile(step)
            }
            checkpoint(step.entityId)
            emit(ExecutionTraceEventKind.STEP_FINISHED, entityId = step.entityId, value = value)
        }
    }

    private fun executeBranch(step: ExecutionStep.Branch): ExecutionValue {
        val selected = step.alternatives.firstOrNull { alternative -> evaluate(alternative.condition).asBoolean(step.entityId) }
        if (selected != null) {
            emit(ExecutionTraceEventKind.BRANCH_SELECTED, entityId = step.entityId, relationId = selected.relationId)
            execute(selected.steps)
        } else {
            emit(
                ExecutionTraceEventKind.BRANCH_SELECTED,
                entityId = step.entityId,
                relationId = "branch:${step.entityId}:ELSE",
            )
            execute(step.elseSteps)
        }
        return ExecutionValue.VoidValue
    }

    private fun executeRepeat(step: ExecutionStep.Repeat): ExecutionValue {
        val count = evaluate(step.count).asNumber(step.entityId).toInt()
        requireLoopCount(step.entityId, count)
        repeat(count) {
            checkpoint(step.entityId)
            execute(step.body)
        }
        return ExecutionValue.VoidValue
    }

    private fun executeWhile(step: ExecutionStep.While): ExecutionValue {
        var count = 0
        while (evaluate(step.condition).asBoolean(step.entityId)) {
            count += 1
            requireLoopCount(step.entityId, count)
            checkpoint(step.entityId)
            execute(step.body)
        }
        return ExecutionValue.VoidValue
    }

    private fun requireLoopCount(entityId: String, count: Int) {
        if (count < 0 || count > maxLoopIterations) {
            fail("LOOP_LIMIT_EXCEEDED", "Loop at $entityId exceeded $maxLoopIterations iterations.", entityId)
        }
    }

    private fun evaluate(expression: ExecutionExpression): ExecutionValue = when (expression) {
        is ExecutionExpression.Literal -> expression.value
        is ExecutionExpression.Variable -> context.variables[expression.variableId] ?: ExecutionValue.NullValue
        is ExecutionExpression.Binary -> evaluateBinary(expression)
        is ExecutionExpression.ProviderCall -> invoke(
            expression.entityId,
            expression.commandId,
            expression.capability,
            expression.arguments,
        )
    }

    private fun evaluateBinary(expression: ExecutionExpression.Binary): ExecutionValue {
        val left = evaluate(expression.left)
        val right = evaluate(expression.right)
        return when (expression.operator) {
            "||" -> ExecutionValue.BooleanValue(left.asBoolean(expression.entityId) || right.asBoolean(expression.entityId))
            "&&" -> ExecutionValue.BooleanValue(left.asBoolean(expression.entityId) && right.asBoolean(expression.entityId))
            "+" -> ExecutionValue.NumberValue(left.asNumber(expression.entityId) + right.asNumber(expression.entityId))
            "-" -> ExecutionValue.NumberValue(left.asNumber(expression.entityId) - right.asNumber(expression.entityId))
            "*" -> ExecutionValue.NumberValue(left.asNumber(expression.entityId) * right.asNumber(expression.entityId))
            "/" -> ExecutionValue.NumberValue(left.asNumber(expression.entityId) / right.asNumber(expression.entityId))
            "%" -> ExecutionValue.NumberValue(left.asNumber(expression.entityId) % right.asNumber(expression.entityId))
            "==" -> ExecutionValue.BooleanValue(left == right)
            "!=" -> ExecutionValue.BooleanValue(left != right)
            "<" -> ExecutionValue.BooleanValue(left.asNumber(expression.entityId) < right.asNumber(expression.entityId))
            "<=" -> ExecutionValue.BooleanValue(left.asNumber(expression.entityId) <= right.asNumber(expression.entityId))
            ">" -> ExecutionValue.BooleanValue(left.asNumber(expression.entityId) > right.asNumber(expression.entityId))
            ">=" -> ExecutionValue.BooleanValue(left.asNumber(expression.entityId) >= right.asNumber(expression.entityId))
            else -> fail("UNSUPPORTED_OPERATOR", "Unsupported operator '${expression.operator}'.", expression.entityId)
        }
    }

    private fun invoke(
        entityId: String,
        commandId: String,
        capability: ExecutionCapability,
        arguments: List<ExecutionExpression>,
    ): ExecutionValue {
        checkpoint(entityId)
        val provider = providers.resolve(capability) ?: fail(
            code = "CAPABILITY_PROVIDER_MISSING",
            message = "No provider supplies capability '${capability.value}' for '$commandId'.",
            entityId = entityId,
        )
        val values = arguments.map(::evaluate)
        emit(
            kind = ExecutionTraceEventKind.PROVIDER_STARTED,
            entityId = entityId,
            capability = capability,
            providerId = provider.id,
        )
        return when (val result = provider.invoke(
            ProviderInvocation(
                requestId = context.requestId,
                workflowId = context.workflowId,
                entityId = entityId,
                commandId = commandId,
                capability = capability,
                arguments = values,
            ),
            context,
        )) {
            is ProviderResult.Success -> result.value.also { value ->
                emit(
                    kind = ExecutionTraceEventKind.PROVIDER_FINISHED,
                    entityId = entityId,
                    capability = capability,
                    providerId = provider.id,
                    value = value,
                )
            }
            is ProviderResult.Failure -> fail(result.code, result.message, entityId)
            is ProviderResult.Cancelled -> throw ExecutionCancelled(entityId)
        }
    }

    private fun checkpoint(entityId: String) {
        if (context.cancellationToken.isCancellationRequested) throw ExecutionCancelled(entityId)
        stepCount += 1
        if (stepCount > maxSteps) {
            fail("STEP_LIMIT_EXCEEDED", "Execution exceeded $maxSteps checkpoints.", entityId)
        }
    }

    private fun emit(
        kind: ExecutionTraceEventKind,
        entityId: String? = null,
        relationId: String? = null,
        capability: ExecutionCapability? = null,
        providerId: String? = null,
        value: ExecutionValue? = null,
    ) {
        val traceEvent = ExecutionTraceEvent(
            index = trace.size + 1,
            kind = kind,
            state = lifecycle.state,
            entityId = entityId,
            relationId = relationId,
            capability = capability,
            providerId = providerId,
            value = value,
        )
        trace += traceEvent
        traceObserver(traceEvent)
    }

    private fun fail(code: String, message: String, entityId: String): Nothing =
        throw ExecutionFailed(ExecutionDiagnostic(code, message, entityId))
}

private class ExecutionCancelled(val entityId: String) : RuntimeException()
private class ExecutionFailed(val diagnostic: ExecutionDiagnostic) : RuntimeException(diagnostic.message)

private fun ExecutionValue.asBoolean(entityId: String): Boolean = when (this) {
    is ExecutionValue.BooleanValue -> value
    else -> throw ExecutionFailed(
        ExecutionDiagnostic("BOOLEAN_REQUIRED", "Boolean value required at $entityId, received $this.", entityId),
    )
}

private fun ExecutionValue.asNumber(entityId: String): Double = when (this) {
    is ExecutionValue.NumberValue -> value
    else -> throw ExecutionFailed(
        ExecutionDiagnostic("NUMBER_REQUIRED", "Number value required at $entityId, received $this.", entityId),
    )
}
