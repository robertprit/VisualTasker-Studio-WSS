package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectVariable
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.WorkflowRootId
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableRuntimeTest {
    @Test
    fun localAndScriptLetSetRemainSessionScoped() {
        val session = session(bindings = listOf(binding("local", VariableScope.Local), binding("script", VariableScope.Script)))

        assertSuccess(session.write("local", ExecutionValue.NumberValue(1.0), declaration = true))
        assertSuccess(session.write("local", ExecutionValue.NumberValue(2.0), declaration = false))
        assertSuccess(session.write("script", ExecutionValue.StringValue("a"), declaration = true))
        assertSuccess(session.write("script", ExecutionValue.StringValue("b"), declaration = false))

        assertEquals(ExecutionValue.NumberValue(2.0), assertSuccess(session.read("local")).value)
        assertEquals(ExecutionValue.StringValue("b"), assertSuccess(session.read("script")).value)
    }

    @Test
    fun sameLabelsAcrossScopesNeverCollapseIdentity() {
        val session = session(
            bindings = listOf(
                binding("local-id", VariableScope.Local, label = "value"),
                binding("script-id", VariableScope.Script, label = "value"),
            ),
        )

        assertEquals(VariableFailureCode.AMBIGUOUS_LABEL, assertFailed(session.read("value")).failure.code)
        assertSuccess(session.write("local-id", ExecutionValue.NumberValue(1.0), true))
        assertSuccess(session.write("script-id", ExecutionValue.NumberValue(2.0), true))
        assertNotEquals(assertSuccess(session.read("local-id")).value, assertSuccess(session.read("script-id")).value)
    }

    @Test
    fun nullFalseVoidAndMissingRemainDistinct() {
        val session = session(
            bindings = listOf(
                binding("optional", VariableScope.Script, type = "String?"),
                binding("flag", VariableScope.Script, type = "Bool"),
                binding("required", VariableScope.Script, type = "String"),
            ),
        )

        assertEquals(ExecutionValue.NullValue, assertSuccess(session.write("optional", ExecutionValue.NullValue, true)).value)
        assertEquals(ExecutionValue.BooleanValue(false), assertSuccess(session.write("flag", ExecutionValue.BooleanValue(false), true)).value)
        assertEquals(VariableFailureCode.TYPE_MISMATCH, assertFailed(session.write("required", ExecutionValue.NullValue, true)).failure.code)
        assertEquals(VariableFailureCode.TYPE_MISMATCH, assertFailed(session.write("required", ExecutionValue.VoidValue, true)).failure.code)
        assertEquals(VariableFailureCode.MISSING_VARIABLE, assertFailed(session.read("absent")).failure.code)
    }

    @Test
    fun setRequiresExistingValueAndDuplicateLetIsRejected() {
        val session = session(bindings = listOf(binding("count", VariableScope.Script)))

        assertEquals(VariableFailureCode.MISSING_VARIABLE, assertFailed(session.write("count", ExecutionValue.NumberValue(2.0), false)).failure.code)
        assertSuccess(session.write("count", ExecutionValue.NumberValue(1.0), true))
        assertEquals(VariableFailureCode.ALREADY_DECLARED, assertFailed(session.write("count", ExecutionValue.NumberValue(2.0), true)).failure.code)
    }

    @Test
    fun projectValuesArePersistentAndProjectIsolated() {
        val root = Files.createTempDirectory("variable-project-store").toFile()
        try {
            val provider = ProjectVariableProvider(AtomicFileVariableValueStore(root))
            val first = session(listOf(binding("count", VariableScope.Project, projectId = "project:a")), listOf(provider))
            val secondProject = session(listOf(binding("count", VariableScope.Project, projectId = "project:b")), listOf(provider))

            assertSuccess(first.write("count", ExecutionValue.NumberValue(4.0), true))
            assertTrue(secondProject.read("count") is VariableRuntimeResult.Missing)
            assertEquals(ExecutionValue.NumberValue(4.0), assertSuccess(first.read("count")).value)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun appSharedValuesCrossProjectOnlyThroughExplicitBinding() {
        val root = Files.createTempDirectory("variable-shared-store").toFile()
        try {
            val provider = AppSharedVariableProvider(AtomicFileVariableValueStore(root))
            val first = session(listOf(binding("shared", VariableScope.AppShared, type = "String", projectId = "project:a")), listOf(provider))
            val second = session(listOf(binding("shared", VariableScope.AppShared, type = "String", projectId = "project:b")), listOf(provider))

            assertSuccess(first.write("shared", ExecutionValue.StringValue("visible"), true))
            assertEquals(ExecutionValue.StringValue("visible"), assertSuccess(second.read("shared")).value)
            assertEquals(VariableFailureCode.MISSING_VARIABLE, assertFailed(second.read("not-bound")).failure.code)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun processRestartReloadsConfirmedPersistentWrite() {
        val root = Files.createTempDirectory("variable-restart-store").toFile()
        try {
            val definition = binding("count", VariableScope.Project, projectId = "project:restart")
            val beforeRestart = session(listOf(definition), listOf(ProjectVariableProvider(AtomicFileVariableValueStore(root))))
            assertSuccess(beforeRestart.write("count", ExecutionValue.NumberValue(7.0), true))

            val afterRestart = session(listOf(definition), listOf(ProjectVariableProvider(AtomicFileVariableValueStore(root))))
            assertEquals(ExecutionValue.NumberValue(7.0), assertSuccess(afterRestart.read("count")).value)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun crashRecoveryPromotesCompletedTemporaryValue() {
        val root = Files.createTempDirectory("variable-recovery-store").toFile()
        try {
            val store = AtomicFileVariableValueStore(root)
            assertTrue(store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(9.0), null, token()) is VariableWriteResult.Written)
            val committed = root.walkTopDown().single { it.isFile && it.extension == "json" }
            val temporary = committed.parentFile!!.resolve(committed.name + ".tmp")
            assertTrue(committed.renameTo(temporary))

            val recovered = AtomicFileVariableValueStore(root)

            assertEquals(ExecutionValue.NumberValue(9.0), (recovered.read("project:a", "count") as VariableReadResult.Found).stored.value)
            assertFalse(root.walkTopDown().any { it.isFile && it.name.endsWith(".tmp") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun crashRecoveryPrefersConfirmedBackupOverUncommittedTemporaryValue() {
        val root = Files.createTempDirectory("variable-recovery-priority").toFile()
        try {
            val store = AtomicFileVariableValueStore(root)
            assertTrue(store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(1.0), null, token()) is VariableWriteResult.Written)
            val committed = root.walkTopDown().single { it.isFile && it.extension == "json" }
            val backup = committed.parentFile!!.resolve(committed.name + ".bak")
            val temporary = committed.parentFile!!.resolve(committed.name + ".tmp")
            assertTrue(committed.renameTo(backup))
            temporary.writeText(backup.readText().replace("\"value\":1", "\"value\":2"))

            val recovered = AtomicFileVariableValueStore(root)

            assertEquals(ExecutionValue.NumberValue(1.0), (recovered.read("project:a", "count") as VariableReadResult.Found).stored.value)
            assertFalse(temporary.exists())
            assertFalse(backup.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun listAndPointValuesSurviveSaveReload() {
        val root = Files.createTempDirectory("variable-types-store").toFile()
        try {
            val before = AtomicFileVariableValueStore(root)
            val list = ExecutionValue.ListValue(listOf(ExecutionValue.BooleanValue(false), ExecutionValue.NullValue))
            val point = ExecutionValue.PointValue(PointerPathPoint(12.5, 24.0))
            assertTrue(before.compareAndSet("project:a", "list", list, null, token()) is VariableWriteResult.Written)
            assertTrue(before.compareAndSet("project:a", "point", point, null, token()) is VariableWriteResult.Written)
            val after = AtomicFileVariableValueStore(root)

            assertEquals(list, (after.read("project:a", "list") as VariableReadResult.Found).stored.value)
            assertEquals(point, (after.read("project:a", "point") as VariableReadResult.Found).stored.value)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun pathPointerPathAndMultiPathValuesSurviveSaveReload() {
        val root = Files.createTempDirectory("variable-path-store").toFile()
        try {
            val start = PointerPathPoint(10.0, 20.0)
            val line = PointerPathSegment.Line(start, PointerPathPoint(30.0, 40.0))
            val curve = PointerPathSegment.QuadraticBezier(line.to, PointerPathPoint(50.0, 10.0), PointerPathPoint(70.0, 40.0))
            val geometry = PointerPathGeometry(start, listOf(line, curve))
            val firstPointer = PointerPath(0, geometry, PointerPathTiming(segmentDurationsMs = listOf(80L, 120L)), true)
            val secondPointer = PointerPath(1, geometry, PointerPathTiming(durationMs = 200L), true)
            val values = mapOf(
                "segment" to ExecutionValue.PathSegmentValue(curve),
                "path" to ExecutionValue.PathValue(geometry),
                "pointer" to ExecutionValue.PointerPathValue(firstPointer),
                "multi" to ExecutionValue.MultiPathValue(MultiPointerGesture(listOf(firstPointer, secondPointer))),
            )
            val before = AtomicFileVariableValueStore(root)
            values.forEach { (id, value) ->
                assertTrue(before.compareAndSet("project:a", id, value, null, token()) is VariableWriteResult.Written)
            }
            val after = AtomicFileVariableValueStore(root)

            values.forEach { (id, value) ->
                assertEquals(value, (after.read("project:a", id) as VariableReadResult.Found).stored.value)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun projectBindingsPreserveIdentityScopeAndExplicitExternalProvider() {
        val rootId = WorkflowRootId("root:variables")
        val workspace = EmscriptWorkspaceImporter().import("LET local = 1", "binding-project").document!!
        val project = ProjectDocument(
            id = ProjectId("project:variables"),
            manifest = ProjectManifest("Variables", rootId),
            roots = mapOf(rootId to ProjectWorkflowRoot(rootId, "Variables", workspace)),
            variables = mapOf(
                "project" to ProjectVariable(VariableDefinition("project", "Same", "Number", VariableScope.Project)),
                "shared" to ProjectVariable(VariableDefinition("shared", "Same", "String", VariableScope.AppShared)),
                "external" to ProjectVariable(VariableDefinition("external", "%VT", "String", VariableScope.External), TASKER_VARIABLE_PROVIDER_ID),
            ),
        )

        val bindings = ProjectVariableBindings.forExecution(project, rootId)

        assertEquals(PROJECT_VARIABLE_PROVIDER_ID, bindings.getValue("project").providerId)
        assertEquals(APP_SHARED_VARIABLE_PROVIDER_ID, bindings.getValue("shared").providerId)
        assertEquals(TASKER_VARIABLE_PROVIDER_ID, bindings.getValue("external").providerId)
        assertNotEquals(bindings.getValue("project").variableId, bindings.getValue("shared").variableId)
    }

    @Test
    fun concurrentCompareAndSetProducesOneWriteAndOneConflict() {
        val root = Files.createTempDirectory("variable-cas-store").toFile()
        val store = AtomicFileVariableValueStore(root)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            assertTrue(store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(0.0), null, token()) is VariableWriteResult.Written)
            val writes = listOf(1.0, 2.0).map { value ->
                executor.submit<VariableWriteResult> {
                    start.await()
                    store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(value), 0L, token())
                }
            }
            start.countDown()
            val results = writes.map { it.get() }

            assertEquals(1, results.count { it is VariableWriteResult.Written })
            assertEquals(1, results.count { (it as? VariableWriteResult.Failed)?.failure?.code == VariableFailureCode.REVISION_CONFLICT })
        } finally {
            executor.shutdownNow()
            root.deleteRecursively()
        }
    }

    @Test
    fun cancellationBeforeWriteLeavesPersistentValueUntouched() {
        val root = Files.createTempDirectory("variable-cancel-store").toFile()
        try {
            val store = AtomicFileVariableValueStore(root)
            assertTrue(store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(1.0), null, token()) is VariableWriteResult.Written)
            val cancelled = MutableCancellationToken().apply { cancel() }

            val result = store.compareAndSet("project:a", "count", ExecutionValue.NumberValue(2.0), 0L, cancelled)

            assertEquals(VariableFailureCode.CANCELLED, (result as VariableWriteResult.Failed).failure.code)
            assertEquals(ExecutionValue.NumberValue(1.0), (store.read("project:a", "count") as VariableReadResult.Found).stored.value)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun externalProviderUnavailableAndProviderLossAreDiagnosed() {
        val unavailable = object : VariableProvider {
            override val id = "external.test"
            override fun read(binding: VariableBinding, cancellationToken: CancellationToken) = VariableReadResult.Failed(
                VariableFailure(VariableFailureCode.PROVIDER_UNAVAILABLE, "offline", binding.variableId, binding.scope, id),
            )
            override fun write(binding: VariableBinding, value: ExecutionValue, expectedRevision: Long?, cancellationToken: CancellationToken) =
                VariableWriteResult.Failed(VariableFailure(VariableFailureCode.WRITE_UNAVAILABLE, "lost", binding.variableId, binding.scope, id))
        }
        val unavailableSession = session(listOf(binding("remote", VariableScope.External, type = "String", providerId = unavailable.id)), listOf(unavailable))
        val readOnly = object : VariableProvider {
            override val id = "external.read-only"
            override fun read(binding: VariableBinding, cancellationToken: CancellationToken) = VariableReadResult.Missing
            override fun write(binding: VariableBinding, value: ExecutionValue, expectedRevision: Long?, cancellationToken: CancellationToken) =
                VariableWriteResult.Failed(VariableFailure(VariableFailureCode.WRITE_UNAVAILABLE, "read only", binding.variableId, binding.scope, id))
        }
        val readOnlySession = session(listOf(binding("remote", VariableScope.External, type = "String", providerId = readOnly.id)), listOf(readOnly))

        assertEquals(VariableFailureCode.PROVIDER_UNAVAILABLE, assertFailed(unavailableSession.read("remote")).failure.code)
        assertEquals(VariableFailureCode.WRITE_UNAVAILABLE, assertFailed(readOnlySession.write("remote", ExecutionValue.StringValue("x"), true)).failure.code)
    }

    @Test
    fun providerReadCannotBypassDeclaredType() {
        val provider = object : VariableProvider {
            override val id = "external.bad-type"
            override fun read(binding: VariableBinding, cancellationToken: CancellationToken) =
                VariableReadResult.Found(VariableStoredValue(ExecutionValue.StringValue("not-a-number"), 0L))
            override fun write(binding: VariableBinding, value: ExecutionValue, expectedRevision: Long?, cancellationToken: CancellationToken) =
                VariableWriteResult.Written(VariableStoredValue(value, 0L))
        }
        val runtime = session(
            listOf(binding("remote", VariableScope.External, type = "Number", providerId = provider.id)),
            listOf(provider),
        )

        assertEquals(VariableFailureCode.TYPE_MISMATCH, assertFailed(runtime.read("remote")).failure.code)
    }

    @Test
    fun runnerUsesPersistentScopeAndPublishesValueFreeProvenance() {
        val root = Files.createTempDirectory("variable-runner-store").toFile()
        try {
            val document = import("LET count:Number = 1\nSET count = count + 2\nlog(count)")
            val binding = binding("count", VariableScope.Project, projectId = "project:runner")
            val provider = ProjectVariableProvider(AtomicFileVariableValueStore(root))
            val runtimeRequest = request(document, binding)
            val capabilities = (WorkflowExecutionPlanner().plan(runtimeRequest) as ExecutionPlanResult.Ready).plan.requiredCapabilities
            val logProvider = RecordingExecutionProvider(capabilities)
            val runner = DeterministicExecutionRunner(
                providers = ExecutionProviderRegistry(listOf(logProvider)),
                variableRuntimeFactory = DefaultVariableRuntimeSessionFactory(VariableProviderRegistry(listOf(provider))),
            )
            val original = document

            val first = runner.execute(runtimeRequest)
            val second = runner.execute(request(document, binding, id = "run:2"))

            assertEquals(first.diagnostics.joinToString { it.message }, ExecutionState.COMPLETED, first.state)
            assertEquals(ExecutionValue.NumberValue(3.0), first.variables["count"])
            assertEquals(ExecutionValue.NumberValue(5.0), second.variables["count"])
            assertEquals(original, document)
            assertTrue(second.trace.any { it.kind == ExecutionTraceEventKind.VARIABLE_READ })
            assertTrue(second.trace.any { it.kind == ExecutionTraceEventKind.VARIABLE_WRITTEN })
            assertTrue(second.trace.mapNotNull { it.variableTrace }.all { it.variableId == "count" && it.providerId == PROJECT_VARIABLE_PROVIDER_ID })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rootAndSessionBindingsStayFixedForHeadlessExecutionRequest() {
        val document = import("LET value = 1")
        val binding = binding("value", VariableScope.Script, projectId = "project:a", rootId = "root:a")
        val request = request(document, binding)

        assertEquals("project:a", request.projectId)
        assertEquals("root:a", request.workflowRootId)
        assertEquals("root:a", request.variableBindings.getValue("value").workflowRootId)
    }

    @Test
    fun traceProjectionExposesScopeDecisionWithoutVariableValue() {
        val info = VariableTraceInfo("secret", "Project", PROJECT_VARIABLE_PROVIDER_ID, "READ", "FOUND")
        val snapshot = AndroidRuntimeSnapshot(
            requestId = "run",
            workflowId = "root",
            trace = listOf(
                ExecutionTraceEvent(1, ExecutionTraceEventKind.VARIABLE_READ, ExecutionState.RUNNING, variableTrace = info),
            ),
        )

        val operation = snapshot.toExecutionTrace()!!.operations.single()

        assertEquals(info, operation.variableTrace)
        assertFalse(operation.message.contains("secret-value"))
    }

    private fun session(
        bindings: List<VariableBinding>,
        providers: List<VariableProvider> = emptyList(),
    ) = VariableRuntimeSession(
        VariableScopeResolver(bindings),
        VariableProviderRegistry(providers),
        emptyMap(),
        token(),
    )

    private fun binding(
        id: String,
        scope: VariableScope,
        label: String = id,
        type: String = if (id == "script") "String" else "Number",
        projectId: String? = "project:test",
        rootId: String? = "root:test",
        providerId: String? = when (scope) {
            VariableScope.Project, VariableScope.Global -> PROJECT_VARIABLE_PROVIDER_ID
            VariableScope.AppShared -> APP_SHARED_VARIABLE_PROVIDER_ID
            else -> null
        },
    ) = VariableBinding(
        variableId = id,
        variableLabel = label,
        declaredType = type,
        scope = if (scope == VariableScope.Global) VariableScope.Project else scope,
        storagePolicy = when (scope) {
            VariableScope.Local -> VariableStoragePolicy.VOLATILE
            VariableScope.Script -> VariableStoragePolicy.SESSION
            VariableScope.Global, VariableScope.Project -> VariableStoragePolicy.PROJECT
            VariableScope.AppShared -> VariableStoragePolicy.APP_SHARED
            VariableScope.External -> VariableStoragePolicy.EXTERNAL
        },
        projectId = projectId,
        workflowRootId = rootId,
        providerId = providerId,
    )

    private fun request(
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        binding: VariableBinding,
        id: String = "run:1",
    ) = ExecutionRequest(
        id = id,
        workflowId = "root:test",
        document = document,
        projectId = binding.projectId,
        workflowRootId = binding.workflowRootId,
        variableBindings = mapOf(binding.variableId to binding),
    )

    private fun token() = MutableCancellationToken()
    private fun import(script: String) = EmscriptWorkspaceImporter().import(script, "m8-variable-runtime").let { result ->
        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        requireNotNull(result.document)
    }
    private fun assertSuccess(result: VariableRuntimeResult) = result as VariableRuntimeResult.Success
    private fun assertFailed(result: VariableRuntimeResult) = result as VariableRuntimeResult.Failed

    private class RecordingExecutionProvider(override val capabilities: Set<ExecutionCapability>) : ExecutionProvider {
        override val id = "test.log"
        override fun invoke(invocation: ProviderInvocation, context: ExecutionContext) = ProviderResult.Success()
    }
}
