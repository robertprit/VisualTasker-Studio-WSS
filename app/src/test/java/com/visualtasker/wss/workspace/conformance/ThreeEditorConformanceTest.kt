package com.visualtasker.wss.workspace.conformance

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.workspace.model.FlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import com.visualtasker.wss.workspace.model.applyFlowchartWorkspaceMutation
import de.visualtasker.blockeditor.compose.host.BlockEditorController
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowLayoutMetadata
import de.visualtasker.flowchart.domain.FlowNodeView
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewAnnotation
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.domain.FlowViewport
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticRelationKind
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
    fun typedPointerPathSurvivesTextBlockFlowPersistenceAndSourceApply() {
        val result = assertSupportedRoundtrip(
            """
                LET duration:Number = 900
                LET route:PointerPath = pointerPath(path([lineSegment(point(100,200),point(200,200)),quadraticBezierSegment(point(200,200),point(250,260),point(300,200)),cubicBezierSegment(point(300,200),point(340,140),point(380,260),point(420,200))]),duration=duration,startHold=20,endHold=30,delay=10,repeat=2)
                swipe(route)
            """.trimIndent(),
            "typed-pointer-path",
        )

        assertTrue(result.generatedSource, result.generatedSource.contains("let route:PointerPath"))
        assertTrue(result.generatedSource, result.generatedSource.contains("quadraticBezierSegment"))
        assertTrue(result.generatedSource, result.generatedSource.contains("cubicBezierSegment"))
        assertTrue(result.generatedSource, result.generatedSource.contains("duration=duration"))
        assertTrue(result.flowNodeIds.containsAll(result.blockProjection.keys.map { "block:$it" }))
    }

    @Test
    fun pointerPathTimingModesSurviveThreeEditorRoundtrip() {
        val result = assertSupportedRoundtrip(
            """
                LET timed:PointerPath = pointerPath(path([lineSegment(point(10,20),point(30,40)),lineSegment(point(30,40),point(50,60))]),segmentDurations=[100,250])
                LET paced:PointerPath = pointerPath(path(point(100,200),point(400,600)),speed=250)
                swipe(timed)
                swipe(paced)
            """.trimIndent(),
            "typed-pointer-path-timing-modes",
        )

        assertTrue(result.generatedSource, result.generatedSource.contains("segmentDurations=[100, 250]"))
        assertTrue(result.generatedSource, result.generatedSource.contains("speed=250"))
        assertTrue(result.flowNodeIds.containsAll(result.blockProjection.keys.map { "block:$it" }))
    }

    @Test
    fun pathTimingEditsConvergeThroughBlockAndFlowWithUndoRedoAndStableIdentity() {
        val imported = (ThreeEditorConformanceHarness.importSource(
            "LET route:PointerPath = pointerPath(path(point(100,200),point(300,400)),duration=600)\nswipe(route)",
            "typed-pointer-path-mutation",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val pointerPath = imported.blocks.values.single {
            it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}gesture.pointerPath"
        }
        val duration = imported.blocks.values.single { it.fields["value"] == FieldValue.Number(600.0) }
        val decorated = imported.copy(
            blocks = imported.blocks + (
                pointerPath.id to pointerPath.copy(metadata = pointerPath.metadata + ("unknown.owner" to "m5-5"))
            ),
        )
        val identityBefore = requireNotNull(decorated.canonical).entities.map { it.ref.id }.toSet()

        withBlockEditor(decorated) { controller ->
            controller.onAction(WorkspaceAction.UpdateField(duration.id, "value", FieldValue.Number(700.0)))
            val blockChanged = controller.document
            assertTrue(ThreeEditorConformanceHarness.project(blockChanged).generatedSource.contains("duration=700"))
            assertTrue(controller.undo())
            assertTrue(ThreeEditorConformanceHarness.project(controller.document).generatedSource.contains("duration=600"))
            assertTrue(controller.redo())
            assertTrue(ThreeEditorConformanceHarness.project(controller.document).generatedSource.contains("duration=700"))

            val flowChanged = applyFlowchartWorkspaceMutation(
                controller.document,
                FlowchartWorkspaceMutation.UpdateNodeField(
                    nodeId = de.visualtasker.flowchart.domain.FlowNodeId("block:${duration.id.value}"),
                    fieldKey = "value",
                    rawValue = "800",
                ),
            )
            val projection = ThreeEditorConformanceHarness.project(flowChanged.document)
            assertTrue(flowChanged.applied)
            assertTrue(projection.generatedSource.contains("duration=800"))
            assertEquals("m5-5", flowChanged.document.blocks.getValue(pointerPath.id).metadata["unknown.owner"])
            assertEquals(identityBefore, requireNotNull(flowChanged.document.canonical).entities.map { it.ref.id }.toSet())
            assertEquals(1, flowChanged.document.blocks.values.count {
                it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}gesture.pointerPath"
            })
        }
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
    fun blockEditorControllerCreatesAndConnectsStatementThroughCanonicalTransactions() {
        val imported = (ThreeEditorConformanceHarness.importSource("beep()", "block-create-connect")
            as EmscriptApplyGuardResult.Success).importedDocument
        val beep = imported.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val before = imported.copy(
            blocks = imported.blocks + (
                beep.id to beep.copy(metadata = beep.metadata + ("unknown.owner" to "m4-2"))
            ),
        )
        val createdId = BlockId("m4-2-created-wait")

        withBlockEditor(before) { controller ->
            controller.onAction(WorkspaceAction.InstantiateBlock(BlockTypes.ACTION_WAIT, 160f, 240f, createdId))
            val created = controller.document.blocks.getValue(createdId)
            controller.onAction(WorkspaceAction.Connect(requireNotNull(beep.next).id, requireNotNull(created.previous).id))

            val after = controller.document
            val sequence = requireNotNull(after.canonical).relations.single {
                it.kind == SemanticRelationKind.Sequence &&
                    it.source.id == requireEntityId(after, beep.id) &&
                    it.target.id == requireEntityId(after, createdId)
            }
            val projections = ThreeEditorConformanceHarness.project(after)

            assertEquals(before.version + 2, after.version)
            assertEquals("m4-2", after.blocks.getValue(beep.id).metadata["unknown.owner"])
            assertTrue(sequence.id.value.isNotBlank())
            assertTrue(projections.generatedSource.indexOf("beep()") < projections.generatedSource.indexOf("wait("))
            assertTrue("block:${createdId.value}" in projections.flowNodeIds)
        }
    }

    @Test
    fun blockEditorControllerReconnectsNestedReporterWithStableIdentity() {
        val before = (ThreeEditorConformanceHarness.importSource(
            "LET total:Number = (1 + 2) * (3 + 4)\nlog(total)",
            "block-nested-reporter",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val operators = before.blocks.values.filter { it.type == BlockTypes.LOGIC_OPERATE }
        val nested = operators.firstNotNullOf { parent ->
            parent.valueInputs.firstNotNullOfOrNull { input ->
                operators.firstOrNull { child -> child.output?.id == input.connection.connectedTo }
                    ?.let { child -> Triple(parent, input, child) }
            }
        }
        val (parent, input, child) = nested
        val relationBefore = requireNotNull(before.canonical).relations.single {
            it.kind == SemanticRelationKind.Expression &&
                it.source.id == requireEntityId(before, parent.id) &&
                it.target.id == requireEntityId(before, child.id)
        }

        withBlockEditor(before) { controller ->
            controller.onAction(WorkspaceAction.Disconnect(input.connection.id))
            assertFalse(
                requireNotNull(controller.document.canonical).relations.any { it.id == relationBefore.id },
            )
            controller.onAction(
                WorkspaceAction.Connect(
                    requireNotNull(controller.document.blocks.getValue(child.id).output).id,
                    controller.document.blocks.getValue(parent.id).valueInputs
                        .single { it.name == input.name }
                        .connection.id,
                ),
            )

            val after = controller.document
            assertEquals(before.version + 2, after.version)
            val restored = requireNotNull(after.canonical).relations.single {
                it.kind == relationBefore.kind &&
                    it.source == relationBefore.source &&
                    it.target == relationBefore.target &&
                    it.role == relationBefore.role &&
                    it.order == relationBefore.order
            }
            assertTrue(restored.id.value.isNotBlank())
            assertEquals(
                requireNotNull(before.canonical).entities.map { it.ref }.toSet(),
                requireNotNull(after.canonical).entities.map { it.ref }.toSet(),
            )
            assertEquals(
                sourceAnchorDirectives(ThreeEditorConformanceHarness.project(before).generatedSource)
                    .filter { it.startsWith("@source.entity") },
                sourceAnchorDirectives(ThreeEditorConformanceHarness.project(after).generatedSource)
                    .filter { it.startsWith("@source.entity") },
            )
            assertTrue(ThreeEditorConformanceHarness.project(after).generatedSource.contains("(1 + 2) * (3 + 4)"))
        }
    }

    @Test
    fun textEditorSourceApplyChangesPropertyWithoutReplacingIdentityAnchorsOrMetadata() {
        val imported = (ThreeEditorConformanceHarness.importSource("wait(100)", "text-property-apply")
            as EmscriptApplyGuardResult.Success).importedDocument
        val wait = imported.blocks.values.single { it.type == BlockTypes.ACTION_WAIT }
        val before = imported.copy(
            blocks = imported.blocks + (
                wait.id to wait.copy(metadata = wait.metadata + ("unknown.owner" to "m4-2-source"))
            ),
        )
        val source = ThreeEditorConformanceHarness.project(before).generatedSource
        val anchorsBefore = sourceAnchorDirectives(source)
        val changedSource = source.replaceFirst(Regex("wait\\([^\\n)]*\\)"), "wait(250)")
        val applied = ThreeEditorConformanceHarness.applySource(changedSource, before.id, before)
            as EmscriptApplyGuardResult.Success
        val after = applied.importedDocument

        assertEquals(before.version + 1, after.version)
        assertTrue(applied.sourceApplyPlan.transaction.operations.isNotEmpty())
        assertEquals(requireEntityId(before, wait.id), requireEntityId(after, wait.id))
        assertEquals("m4-2-source", after.blocks.getValue(wait.id).metadata["unknown.owner"])
        assertEquals(
            ThreeEditorConformanceHarness.snapshot(before).identityStructure,
            ThreeEditorConformanceHarness.snapshot(after).identityStructure,
        )
        assertEquals(anchorsBefore, sourceAnchorDirectives(ThreeEditorConformanceHarness.project(after).generatedSource))
        assertEquals(FieldValue.Number(250.0), after.blocks.getValue(wait.id).fields["ms"])
    }

    @Test
    fun blockEditorDetachAndDeletePreserveUnaffectedSemanticIdentities() {
        val before = (ThreeEditorConformanceHarness.importSource(
            "wait(10)\nbeep()\nlog(\"done\")",
            "block-detach-delete",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val beep = before.blocks.values.single { it.type == BlockTypes.FEEDBACK_BEEP }
        val beforeEntityIds = requireNotNull(before.canonical).entities.map { it.ref.id }.toSet()

        withBlockEditor(before) { controller ->
            controller.onAction(WorkspaceAction.DetachBlock(beep.id))
            val detached = controller.document

            assertEquals(before.version + 1, detached.version)
            assertEquals(beforeEntityIds, requireNotNull(detached.canonical).entities.map { it.ref.id }.toSet())
            assertFalse(
                requireNotNull(detached.canonical).relations.any {
                    it.kind == SemanticRelationKind.Sequence &&
                        (it.source.id == requireEntityId(detached, beep.id) || it.target.id == requireEntityId(detached, beep.id))
                },
            )
        }

        withBlockEditor(before) { controller ->
            controller.onAction(WorkspaceAction.DeleteBlockPreservingChain(beep.id))
            val deleted = controller.document
            val remainingIds = requireNotNull(deleted.canonical).entities.map { it.ref.id }.toSet()

            assertEquals(before.version + 1, deleted.version)
            assertFalse(beep.id in deleted.blocks)
            assertFalse(requireEntityId(before, beep.id) in remainingIds)
            assertTrue(beforeEntityIds.minus(requireEntityId(before, beep.id)).all(remainingIds::contains))
            assertFalse(ThreeEditorConformanceHarness.project(deleted).generatedSource.contains("beep()"))
        }
    }

    @Test
    fun blockEditorReparentsStatementSequenceWithoutReplacingEntities() {
        val before = (ThreeEditorConformanceHarness.importSource(
            "wait(10)\nbeep()\nlog(\"done\")",
            "block-reparent",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val wait = before.blocks.values.single { it.type == BlockTypes.ACTION_WAIT }
        val beep = before.blocks.values.single { it.type == BlockTypes.FEEDBACK_BEEP }
        val log = before.blocks.values.single { it.type == BlockTypes.DEBUG_LOG }
        val entityRefs = requireNotNull(before.canonical).entities.map { it.ref }.toSet()

        withBlockEditor(before) { controller ->
            controller.onAction(WorkspaceAction.DetachBlock(beep.id))
            controller.onAction(
                WorkspaceAction.Connect(
                    requireNotNull(wait.next).id,
                    requireNotNull(log.previous).id,
                ),
            )
            controller.onAction(
                WorkspaceAction.Connect(
                    requireNotNull(log.next).id,
                    requireNotNull(beep.previous).id,
                ),
            )
            val after = controller.document
            val generated = ThreeEditorConformanceHarness.project(after).generatedSource

            assertEquals(before.version + 3, after.version)
            assertEquals(entityRefs, requireNotNull(after.canonical).entities.map { it.ref }.toSet())
            assertTrue(generated.indexOf("wait(") < generated.indexOf("log("))
            assertTrue(generated.indexOf("log(") < generated.indexOf("beep()"))
            repeat(3) { assertTrue(controller.undo()) }
            assertEquals(before, controller.document)
            repeat(3) { assertTrue(controller.redo()) }
            assertEquals(after, controller.document)
        }
    }

    @Test
    fun blockEditorUndoRedoRestoresAllFourFingerprintDimensions() {
        val imported = (ThreeEditorConformanceHarness.importSource("wait(100)", "block-undo-redo")
            as EmscriptApplyGuardResult.Success).importedDocument
        val wait = imported.blocks.values.single { it.type == BlockTypes.ACTION_WAIT }

        withBlockEditor(imported) { controller ->
            val before = ThreeEditorConformanceHarness.snapshot(controller.document)
            controller.onAction(WorkspaceAction.UpdateField(wait.id, "ms", FieldValue.Number(500.0)))
            val changed = ThreeEditorConformanceHarness.snapshot(controller.document)

            assertTrue(controller.undo())
            assertEquals(before, ThreeEditorConformanceHarness.snapshot(controller.document))
            assertTrue(controller.redo())
            assertEquals(changed, ThreeEditorConformanceHarness.snapshot(controller.document))
        }
    }

    @Test
    fun saveReloadAndReapplyPreserveAllEditorProjectionsAnchorsAndFingerprints() {
        val imported = (ThreeEditorConformanceHarness.importSource(
            "LET score:Number = 8\nIF score > 3\nlog(\"high\")\nELSE\nwait(10)\nEND IF",
            "save-reload-three-editors",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val sourceWithAnchors = ThreeEditorConformanceHarness.project(imported).generatedSource
        val anchored = (ThreeEditorConformanceHarness.applySource(sourceWithAnchors, imported.id, imported)
            as EmscriptApplyGuardResult.Success).importedDocument
        val before = ThreeEditorConformanceHarness.snapshot(anchored)
        val reloaded = ThreeEditorConformanceHarness.saveReload(anchored)
        val afterReload = ThreeEditorConformanceHarness.snapshot(reloaded)
        val projections = ThreeEditorConformanceHarness.project(reloaded)
        val reapplied = ThreeEditorConformanceHarness.applySource(projections.generatedSource, reloaded.id, reloaded)
            as EmscriptApplyGuardResult.Success

        assertEquals(before, afterReload)
        assertTrue(projections.generatedSource.contains("@source.entity"))
        assertTrue(projections.blockIds.isNotEmpty())
        assertTrue(projections.flowNodeIds.containsAll(projections.blockIds.map { "block:$it" }))
        assertTrue(reapplied.sourceApplyPlan.transaction.operations.isEmpty())
        assertEquals(before.fingerprints, ThreeEditorConformanceHarness.snapshot(reapplied.importedDocument).fingerprints)
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

    private inline fun <T> withBlockEditor(
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        block: (BlockEditorController) -> T,
    ): T {
        val controller = BlockEditorController(initialDocument = document, debounceMillis = 0L)
        return try {
            block(controller)
        } finally {
            controller.close()
        }
    }

    private fun requireEntityId(
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        blockId: BlockId,
    ): SemanticEntityId = requireNotNull(document.canonical).entities
        .single { it.legacySourceId == blockId.value }
        .ref.id

    private fun sourceAnchorDirectives(source: String): List<String> = source.lineSequence()
        .map(String::trim)
        .filter { it.startsWith("@source.") }
        .toList()
}
