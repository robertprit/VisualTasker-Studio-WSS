package com.visualtasker.wss.workspace.conformance

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.workspace.model.FlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import com.visualtasker.wss.workspace.model.applyFlowchartWorkspaceMutation
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowLayoutMetadata
import de.visualtasker.flowchart.domain.FlowNodeView
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewAnnotation
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.domain.FlowViewport
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticPropertyId
import de.visualtasker.workflow.core.WorkspaceAction
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.core.WorkspaceOperationDiagnosticCode
import de.visualtasker.workflow.core.WorkspaceOperationExecutor
import de.visualtasker.workflow.core.WorkspaceOperationId
import de.visualtasker.workflow.core.WorkspaceOperationResult
import de.visualtasker.workflow.core.WorkspacePoint
import de.visualtasker.workflow.core.WorkspacePropertyValue
import de.visualtasker.workflow.core.WorkspaceReducer
import de.visualtasker.workflow.core.WorkspaceTransaction
import de.visualtasker.workflow.semantics.VisualTaskerSemanticPropertySchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreeEditorConformanceTest {
    @Test
    fun linearWorkflowSurvivesTextBlockFlowTextRoundtrip() {
        assertSupportedRoundtrip("wait(10)\nbeep()\nlog(\"done\")", "linear")
    }

    @Test
    fun nestedExpressionsAndReporterConnectionsSurviveRoundtrip() {
        assertSupportedRoundtrip("LET total:Number = (1 + 2) * (3 + 4)\nlog(total)", "nested-expressions")
    }

    @Test
    fun ifElseIfElseKeepsBranchIdentityAndOrder() {
        val result = assertSupportedRoundtrip(
            """
                LET score:Number = 8
                IF score < 3
                    log("low")
                ELSEIF score >= 7
                    log("high")
                ELSE
                    log("middle")
                END IF
            """.trimIndent(),
            "branches",
        )

        val beforeBranches = result.sourceSnapshot.identityStructure.filter { it.startsWith("branch|") }
        val afterBranches = result.reappliedSnapshot.identityStructure.filter { it.startsWith("branch|") }
        assertEquals(beforeBranches, afterBranches)
        assertTrue(beforeBranches.size >= 3)
    }

    @Test
    fun typedVariablesAndPropertiesSurviveRoundtrip() {
        assertSupportedRoundtrip(
            "LET count:Number = 1\nSET count = count + 2\nLET enabled:Bool = true\nlog(count)",
            "typed-variables",
        )
    }

    @Test
    fun metadataAndProjectionLayoutsSurviveSourceAndPersistenceRoundtrip() {
        val result = ThreeEditorConformanceHarness.roundtrip(
            source = "wait(100)",
            workspaceId = "metadata-layout",
            decorate = { imported ->
                val command = imported.blocks.values.single { it.type != BlockTypes.EVENT_START }
                imported.copy(
                    blocks = imported.blocks + (
                        command.id to command.copy(
                            fields = command.fields +
                                ("displayLabel" to FieldValue.Text("Human wait")) +
                                ("note" to FieldValue.Text("Preserve this note")),
                            metadata = command.metadata + ("unknown.owner" to "m4-fixture"),
                            collapsed = true,
                        )
                    ),
                    rootPositions = imported.rootPositions + (command.id to WorkspacePoint(144f, 288f)),
                )
            },
            flowView = ::fixtureFlowView,
        )
        val reloadedWorkspace = ThreeEditorConformanceHarness.saveReload(result.sourceDocument)
        val reloadedFlowView = ThreeEditorConformanceHarness.saveReload(
            requireNotNull(result.sourceSnapshot.flowView),
            result.sourceDocument,
        )
        val reloaded = ThreeEditorConformanceHarness.snapshot(reloadedWorkspace, reloadedFlowView)

        ThreeEditorConformanceHarness.assertStructurallyEqual(result.sourceSnapshot, result.reappliedSnapshot)
        assertEquals(result.sourceSnapshot.fingerprints.metadata, result.reappliedSnapshot.fingerprints.metadata)
        assertEquals(result.sourceDocument, reloadedWorkspace)
        assertEquals(result.sourceSnapshot.flowView, reloadedFlowView)
        assertEquals(result.sourceSnapshot.persistenceStructure, reloaded.persistenceStructure)
        assertEquals(result.sourceSnapshot.fingerprints.persistence, reloaded.fingerprints.persistence)
    }

    @Test
    fun unsupportedLanguageConstructIsRejectedDeterministically() {
        val results = List(3) {
            ThreeEditorConformanceHarness.importSource(
                "FOR item IN items\n    log(item)\nEND FOR",
                "unsupported-$it",
            )
        }

        assertTrue(results.all { it is EmscriptApplyGuardResult.Failure })
        val diagnostics = results.map { result ->
            result as EmscriptApplyGuardResult.Failure
            Triple(result.stage, result.diagnosticCode, result.message.replace(Regex("unsupported-\\d"), "unsupported"))
        }
        assertEquals(1, diagnostics.distinct().size)
    }

    @Test
    fun formatOnlyChangeDoesNotMutateSemanticsIdentityOrRevision() {
        val first = ThreeEditorConformanceHarness.importSource("LET a = 1\nclick(\"Login\")", "format-only")
            as EmscriptApplyGuardResult.Success
        val formatted = ThreeEditorConformanceHarness.applySource(
            "// formatting only\n\nLET a = 1\n\nclick(\"Login\")",
            "format-only",
            first.importedDocument,
        ) as EmscriptApplyGuardResult.Success

        assertEquals(first.importedDocument.version, formatted.importedDocument.version)
        assertTrue(formatted.sourceApplyPlan.transaction.operations.isEmpty())
        assertEquals(
            ThreeEditorConformanceHarness.snapshot(first.importedDocument).fingerprints.semantics,
            ThreeEditorConformanceHarness.snapshot(formatted.importedDocument).fingerprints.semantics,
        )
        assertEquals(
            ThreeEditorConformanceHarness.snapshot(first.importedDocument).fingerprints.identity,
            ThreeEditorConformanceHarness.snapshot(formatted.importedDocument).fingerprints.identity,
        )
    }

    @Test
    fun readOnlyEditorProjectionsAndSaveReloadDoNotAdvanceRevision() {
        val imported = ThreeEditorConformanceHarness.importSource("LET value = 1\nlog(value)", "read-only")
            as EmscriptApplyGuardResult.Success
        val before = imported.importedDocument
        val first = WorkspaceWorkflowState.fromDocument(before, "m4-0:first")
        val second = WorkspaceWorkflowState.fromDocument(first.document, "m4-0:second")
        val reloaded = ThreeEditorConformanceHarness.saveReload(second.document)

        assertEquals(before.version, first.document.version)
        assertEquals(before.version, second.document.version)
        assertEquals(before.version, reloaded.version)
        assertEquals(first.canonicalDocument, second.canonicalDocument)
        assertEquals(first.document, reloaded)
    }

    @Test
    fun validTransactionIsAtomicAndAdvancesRevisionOnce() {
        val before = (ThreeEditorConformanceHarness.importSource("wait(100)", "valid-transaction")
            as EmscriptApplyGuardResult.Success).importedDocument
        val waitBlock = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val waitEntity = requireNotNull(before.canonical).entities.single { it.legacySourceId == waitBlock.id.value }.ref.id
        val result = WorkspaceOperationExecutor.execute(
            before,
            WorkspaceTransaction(
                transactionId = WorkspaceOperationId("m4-valid"),
                expectedRevision = before.version,
                operations = listOf(
                    WorkspaceOperation.SetProperty(
                        WorkspaceOperationId("m4-valid-ms"),
                        waitEntity,
                        SemanticPropertyId("ms"),
                        WorkspacePropertyValue.Number(250.0),
                    ),
                    WorkspaceOperation.SetProperty(
                        WorkspaceOperationId("m4-valid-label"),
                        waitEntity,
                        SemanticPropertyId("displayLabel"),
                        WorkspacePropertyValue.Text("Quarter second"),
                    ),
                ),
            ),
            VisualTaskerSemanticPropertySchema,
        )

        assertTrue(result is WorkspaceOperationResult.Success)
        result as WorkspaceOperationResult.Success
        assertEquals(before.version + 1, result.document.version)
        assertEquals(FieldValue.Number(250.0), result.document.blocks.getValue(waitBlock.id).fields["ms"])
        assertEquals(FieldValue.Text("Quarter second"), result.document.blocks.getValue(waitBlock.id).fields["displayLabel"])
    }

    @Test
    fun invalidTransactionRollsBackDocumentIdentityMetadataAndRevision() {
        val before = metadataFixture("rollback")
        val beforeSnapshot = ThreeEditorConformanceHarness.snapshot(before)
        val result = WorkspaceOperationExecutor.execute(
            before,
            WorkspaceTransaction(
                transactionId = WorkspaceOperationId("m4-invalid"),
                expectedRevision = before.version,
                operations = listOf(
                    WorkspaceOperation.SetProperty(
                        WorkspaceOperationId("m4-invalid-property"),
                        SemanticEntityId("entity:missing"),
                        SemanticPropertyId("displayLabel"),
                        WorkspacePropertyValue.Text("must not publish"),
                    ),
                ),
            ),
            VisualTaskerSemanticPropertySchema,
        )

        assertTrue(result is WorkspaceOperationResult.Failure)
        result as WorkspaceOperationResult.Failure
        assertEquals(WorkspaceOperationDiagnosticCode.ENTITY_NOT_FOUND, result.diagnostic.code)
        assertEquals(before, result.document)
        assertEquals(beforeSnapshot, ThreeEditorConformanceHarness.snapshot(result.document))
    }

    @Test
    fun blockEditorWorkspaceActionMutatesCanonicalWorkspaceThroughProductionFacade() {
        val imported = (ThreeEditorConformanceHarness.importSource("wait(100)", "block-mutation")
            as EmscriptApplyGuardResult.Success).importedDocument
        val command = imported.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val before = imported.copy(
            blocks = imported.blocks + (command.id to command.copy(metadata = command.metadata + ("unknown.owner" to "m4-1"))),
        )
        val waitBlock = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val beforeSnapshot = ThreeEditorConformanceHarness.snapshot(before)
        val after = WorkspaceReducer.reduce(
            document = before,
            action = WorkspaceAction.UpdateField(waitBlock.id, "ms", FieldValue.Number(250.0)),
            propertySchema = VisualTaskerSemanticPropertySchema,
        )
        val afterSnapshot = ThreeEditorConformanceHarness.snapshot(after)
        val projections = ThreeEditorConformanceHarness.project(after)

        assertEquals(before.version + 1, after.version)
        assertEquals(FieldValue.Number(250.0), after.blocks.getValue(waitBlock.id).fields["ms"])
        assertEquals("m4-1", after.blocks.getValue(waitBlock.id).metadata["unknown.owner"])
        assertEquals(beforeSnapshot.identityStructure, afterSnapshot.identityStructure)
        assertEquals(beforeSnapshot.fingerprints.identity, afterSnapshot.fingerprints.identity)
        assertEquals(beforeSnapshot.fingerprints.metadata, afterSnapshot.fingerprints.metadata)
        assertTrue(projections.generatedSource.contains("wait(250"))
        assertTrue("block:${waitBlock.id.value}" in projections.flowNodeIds)
        assertNotEquals(
            beforeSnapshot.fingerprints.semantics,
            afterSnapshot.fingerprints.semantics,
        )
    }

    @Test
    fun flowEditorWorkspaceActionFacadeMutatesAndReprojectsCanonicalWorkspace() {
        val before = (ThreeEditorConformanceHarness.importSource("wait(100)", "flow-mutation")
            as EmscriptApplyGuardResult.Success).importedDocument
        val waitBlock = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val flowNode = WorkspaceWorkflowState.fromDocument(before, "m4-0:flow-before")
            .flowchartProjection.graph.nodes.single { it.id.value == "block:${waitBlock.id.value}" }
        val mutation = applyFlowchartWorkspaceMutation(
            before,
            FlowchartWorkspaceMutation.UpdateNodeField(flowNode.id, "displayLabel", "Flow label"),
        )
        val projected = WorkspaceWorkflowState.fromDocument(mutation.document, "m4-0:flow-after")

        assertTrue(mutation.applied)
        assertEquals(before.version + 1, mutation.document.version)
        assertEquals(FieldValue.Text("Flow label"), mutation.document.blocks.getValue(waitBlock.id).fields["displayLabel"])
        assertEquals("Flow label", projected.flowchartProjection.graph.nodes.single { it.id == flowNode.id }.label)
    }

    @Test
    fun flowEditorCompoundAddPublishesOneRevisionAndProjectsToTextAndBlocks() {
        val before = (ThreeEditorConformanceHarness.importSource("beep()", "flow-compound-add")
            as EmscriptApplyGuardResult.Success).importedDocument
        val beep = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val beforeIds = requireNotNull(before.canonical).entities.map { it.ref.id }.toSet()

        val mutation = applyFlowchartWorkspaceMutation(
            before,
            FlowchartWorkspaceMutation.AddNode(
                definitionId = BlockTypes.ACTION_WAIT,
                afterNodeId = de.visualtasker.flowchart.domain.FlowNodeId("block:${beep.id.value}"),
                initialFields = mapOf("ms" to "250"),
            ),
        )
        val projections = ThreeEditorConformanceHarness.project(mutation.document)
        val afterIds = requireNotNull(mutation.document.canonical).entities.map { it.ref.id }.toSet()

        assertTrue(mutation.applied)
        assertEquals(before.version + 1, mutation.document.version)
        assertTrue(beforeIds.all(afterIds::contains))
        assertTrue(projections.generatedSource.contains("beep()"))
        assertTrue(projections.generatedSource.contains("wait(250"))
        assertEquals(mutation.document.blocks.keys.map { it.value }.sorted(), projections.blockIds)
        assertTrue(projections.flowNodeIds.containsAll(projections.blockIds.map { "block:$it" }))
    }

    @Test
    fun invalidFlowPropertyMutationRollsBackAllFingerprintsAndRevision() {
        val before = (ThreeEditorConformanceHarness.importSource("wait(100)", "flow-invalid-property")
            as EmscriptApplyGuardResult.Success).importedDocument
        val waitBlock = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val flowNode = WorkspaceWorkflowState.fromDocument(before, "m4-1:invalid-flow")
            .flowchartProjection.graph.nodes.single { it.id.value == "block:${waitBlock.id.value}" }
        val beforeSnapshot = ThreeEditorConformanceHarness.snapshot(before)

        val mutation = applyFlowchartWorkspaceMutation(
            before,
            FlowchartWorkspaceMutation.UpdateNodeField(flowNode.id, "ms", "not-a-number"),
        )

        assertFalse(mutation.applied)
        assertEquals(before, mutation.document)
        assertEquals(beforeSnapshot, ThreeEditorConformanceHarness.snapshot(mutation.document))
    }

    @Test
    fun repeatedBlockEditorMutationIsDeterministic() {
        val before = (ThreeEditorConformanceHarness.importSource("wait(100)", "block-determinism")
            as EmscriptApplyGuardResult.Success).importedDocument
        val waitBlock = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val runs = List(3) {
            WorkspaceReducer.reduce(
                before,
                WorkspaceAction.UpdateField(waitBlock.id, "ms", FieldValue.Number(250.0)),
                propertySchema = VisualTaskerSemanticPropertySchema,
            )
        }
        val snapshots = runs.map(ThreeEditorConformanceHarness::snapshot)

        assertEquals(1, runs.distinct().size)
        assertEquals(1, snapshots.map { it.fingerprints }.distinct().size)
        assertTrue(runs.all { it.version == before.version + 1 })
    }

    @Test
    fun threeConsecutiveRunsProduceIdenticalFingerprintsAndDiagnostics() {
        val source = "LET value:Number = 1 + 2\nIF value > 1\n    log(value)\nELSE\n    wait(10)\nEND IF"
        val baseline = ThreeEditorConformanceHarness.roundtrip(source, "determinism")
        val runs = List(3) {
            ThreeEditorConformanceHarness.applySource(
                baseline.generatedSource,
                "determinism",
                baseline.sourceDocument,
            ) as EmscriptApplyGuardResult.Success
        }
        val snapshots = runs.map { ThreeEditorConformanceHarness.snapshot(it.importedDocument) }

        assertEquals(1, snapshots.map { it.fingerprints }.distinct().size)
        assertEquals(1, runs.map { it.sourceApplyPlan.transaction }.distinct().size)
        assertEquals(1, runs.map { it.sourceApplyPlan.propertyDecisions }.distinct().size)
        assertTrue(runs.all { it.sourceApplyPlan.transaction.operations.isEmpty() })
    }

    private fun assertSupportedRoundtrip(source: String, workspaceId: String) =
        ThreeEditorConformanceHarness.roundtrip(source, workspaceId).also { result ->
            assertTrue(result.blockProjection.isNotEmpty())
            assertTrue(result.flowNodeIds.isNotEmpty())
            assertEquals(0, result.sourceApplyOperationCount)
            assertEquals(result.sourceDocument.version, result.reappliedDocument.version)
            assertEquals(result.sourceSnapshot.fingerprints.semantics, result.reappliedSnapshot.fingerprints.semantics)
            assertEquals(result.sourceSnapshot.fingerprints.identity, result.reappliedSnapshot.fingerprints.identity)
            ThreeEditorConformanceHarness.assertStructurallyEqual(result.sourceSnapshot, result.reappliedSnapshot)
        }

    private fun metadataFixture(workspaceId: String) =
        (ThreeEditorConformanceHarness.importSource("wait(100)", workspaceId) as EmscriptApplyGuardResult.Success)
            .importedDocument
            .let { imported ->
                val command = imported.blocks.values.single { it.type != BlockTypes.EVENT_START }
                imported.copy(
                    blocks = imported.blocks + (
                        command.id to command.copy(
                            fields = command.fields + ("note" to FieldValue.Text("rollback note")),
                            metadata = command.metadata + ("unknown.owner" to "rollback"),
                        )
                    ),
                )
            }

    private fun fixtureFlowView(state: WorkspaceWorkflowState): FlowViewDocument {
        val graph = state.flowchartProjection.graph
        val node = graph.nodes.first()
        return FlowViewDocument(
            documentId = graph.documentId,
            compatibleDocumentRevision = graph.documentRevision,
            surfaceId = FlowSurfaceId("m4-0-flow"),
            viewport = FlowViewport(FlowPoint(24.0, 48.0), 1.25),
            nodeViews = listOf(FlowNodeView(node.id, FlowPoint(192.0, 288.0), pinned = true)),
            annotations = listOf(FlowViewAnnotation("note-1", "Keep flow note", FlowPoint(320.0, 240.0))),
            layoutMetadata = FlowLayoutMetadata("m4-grid", "1", 42L),
        )
    }
}
