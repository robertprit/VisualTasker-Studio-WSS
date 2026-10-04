package com.visualtasker.wss.flowchart

import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.NormalizedOperator
import de.visualtasker.workflow.core.OperatorNormalization
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.WorkspaceGraph
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowDiagnosticId
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphDiagnostic
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphEdge
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowGraphSourceReference
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSemanticValue

enum class FlowchartProjectionStatus {
    RUNNING,
    DEGRADED,
}

data class FlowchartProjectionResult(
    val graph: FlowGraphDocument,
    val status: FlowchartProjectionStatus,
)

object BlockEditorFlowchartProjector {
    private const val STAGE = "BLOCK_DOCUMENT_TO_FLOWCHART"

    fun project(document: WorkspaceDocument): FlowchartProjectionResult {
        val diagnostics = mutableListOf<FlowGraphDiagnostic>()
        val nodes = document.blocks.values
            .sortedBy { it.id.value }
            .map { block ->
                val kind = nodeKindFor(block, document, diagnostics)
                FlowGraphNode(
                    id = flowNodeId(block.id),
                    kind = kind,
                    label = nodeLabelFor(block, document, diagnostics),
                    sourceReference = sourceReference(document),
                    properties = nodeProperties(block, document),
                )
            }
        val edges = buildEdges(document, diagnostics)
        val status = if (diagnostics.any { it.severity == FlowDiagnosticSeverity.ERROR }) {
            FlowchartProjectionStatus.DEGRADED
        } else {
            FlowchartProjectionStatus.RUNNING
        }
        val graph = FlowGraphDocument(
            documentId = FlowDocumentId("blockeditor-${document.id}"),
            documentRevision = FlowDocumentRevision(document.version.toString()),
            producerId = "blockeditor-flowchart-projector",
            producerVersion = "1",
            sourceRevision = document.version.toString(),
            sourceHash = "${document.id}:${document.version}:${document.blocks.size}:${document.rootBlocks.size}",
            entryNodeId = document.rootBlocks.firstOrNull()?.let(::flowNodeId),
            nodes = nodes,
            edges = edges,
            diagnostics = diagnostics,
        )
        return FlowchartProjectionResult(graph = graph, status = status)
    }

    private fun buildEdges(
        document: WorkspaceDocument,
        diagnostics: MutableList<FlowGraphDiagnostic>,
    ): List<FlowGraphEdge> {
        val edges = linkedMapOf<String, FlowGraphEdge>()
        document.blocks.values.forEach { block ->
            val source = flowNodeId(block.id)
            block.next?.connectedTo?.let { nextConn ->
                val (targetId, _) = WorkspaceGraph.findConnection(document, nextConn) ?: return@let
                val edgeKind = when (block.type) {
                    BlockTypes.CONTROL_REPEAT,
                    BlockTypes.CONTROL_WHILE -> FlowEdgeKind.LOOP_EXIT
                    else -> FlowEdgeKind.SEQUENCE
                }
                putEdge(
                    edges = edges,
                    source = source,
                    target = flowNodeId(targetId),
                    kind = edgeKind,
                    label = null,
                )
            }
            block.statementInputs.forEach { statementInput ->
                val connected = statementInput.connection.connectedTo ?: return@forEach
                val (targetId, _) = WorkspaceGraph.findConnection(document, connected) ?: return@forEach
                val edgeKind = when (statementInput.name) {
                    BlockTypes.SLOT_THEN -> FlowEdgeKind.TRUE_BRANCH
                    BlockTypes.SLOT_ELSE -> FlowEdgeKind.FALSE_BRANCH
                    BlockTypes.SLOT_ELIF -> FlowEdgeKind.ELSE_IF_BRANCH
                    BlockTypes.SLOT_DO,
                    BlockTypes.SLOT_BODY -> FlowEdgeKind.LOOP_BODY
                    else -> FlowEdgeKind.SEQUENCE
                }
                putEdge(
                    edges = edges,
                    source = source,
                    target = flowNodeId(targetId),
                    kind = edgeKind,
                    label = statementInput.name,
                )
            }
            block.valueInputs.forEach { valueInput ->
                val connected = valueInput.connection.connectedTo ?: return@forEach
                val partner = WorkspaceGraph.findConnection(document, connected)
                if (partner == null) {
                    diagnostics += diagnostic(
                        code = "BROKEN_VALUE_CONNECTION",
                        message = "Broken value connection for input ${valueInput.name}",
                        document = document,
                        block = block,
                    )
                    return@forEach
                }
                val sourceBlockId = partner.first
                val edgeKind = if (valueInput.name.endsWith("CONDITION")) {
                    FlowEdgeKind.CONDITION
                } else {
                    FlowEdgeKind.DATA_FLOW
                }
                putEdge(
                    edges = edges,
                    source = flowNodeId(sourceBlockId),
                    target = source,
                    kind = edgeKind,
                    label = valueInput.name,
                )
            }
        }
        return edges.values.toList()
    }

    private fun putEdge(
        edges: MutableMap<String, FlowGraphEdge>,
        source: FlowNodeId,
        target: FlowNodeId,
        kind: FlowEdgeKind,
        label: String?,
    ) {
        val key = "${source.value}|${target.value}|${kind.name}|${label.orEmpty()}"
        if (key in edges) return
        edges[key] = FlowGraphEdge(
            id = FlowEdgeId("edge:$key"),
            sourceNodeId = source,
            targetNodeId = target,
            kind = kind,
            label = label,
        )
    }

    private fun nodeKindFor(
        block: BlockNode,
        document: WorkspaceDocument,
        diagnostics: MutableList<FlowGraphDiagnostic>,
    ): FlowSemanticKind = when (block.type) {
        BlockTypes.EVENT_START -> FlowSemanticKind(FlowNodeKind.ENTRY)
        BlockTypes.ACTION_CLICK_TEXT,
        BlockTypes.ACTION_WAIT,
        BlockTypes.ACTION_FIND_TEMPLATE,
        BlockTypes.DEBUG_LOG,
        BlockTypes.FEEDBACK_BEEP,
        BlockTypes.FEEDBACK_VIBRATE,
        -> FlowSemanticKind(FlowNodeKind.ACTION)
        BlockTypes.CONTROL_REPEAT,
        BlockTypes.CONTROL_WHILE,
        -> FlowSemanticKind(FlowNodeKind.LOOP_START)
        BlockTypes.CONTROL_IF,
        BlockTypes.CONTROL_IF_ELSE,
        BlockTypes.CONTROL_IF_ELSEIF_ELSE -> FlowSemanticKind(FlowNodeKind.DECISION)
        BlockTypes.LOGIC_COMPARE -> FlowSemanticKind(FlowNodeKind.DECISION)
        BlockTypes.LOGIC_BOOLEAN,
        BlockTypes.LOGIC_AND,
        BlockTypes.LOGIC_OR,
        -> FlowSemanticKind(FlowNodeKind.DECISION)
        BlockTypes.LOGIC_OPERATE -> FlowSemanticKind(FlowNodeKind.ASSIGNMENT)
        BlockTypes.VARIABLE_SET -> FlowSemanticKind(FlowNodeKind.ASSIGNMENT)
        BlockTypes.LITERAL_NUMBER,
        BlockTypes.LITERAL_STRING,
        BlockTypes.LITERAL_REGION,
        BlockTypes.LITERAL_BOOLEAN,
        -> FlowSemanticKind(FlowNodeKind.INPUT)
        BlockTypes.VARIABLE_GET,
        BlockTypes.VARIABLE_REPORTER,
        BlockTypes.VARIABLE_VALUE,
        BlockTypes.VARIABLES_GET -> FlowSemanticKind(FlowNodeKind.PROPERTY_ACCESS)
        else -> {
            if (block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
                FlowSemanticKind(FlowNodeKind.PROPERTY_ACCESS)
            } else if (block.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX)) {
                FlowSemanticKind(FlowNodeKind.ACTION)
            } else {
                diagnostics += unsupportedBlockDiagnostic(document, block)
                FlowSemanticKind(FlowNodeKind.UNKNOWN_SOURCE)
            }
        }
    }

    private fun nodeLabelFor(
        block: BlockNode,
        document: WorkspaceDocument,
        diagnostics: MutableList<FlowGraphDiagnostic>,
    ): String = when (block.type) {
        BlockTypes.EVENT_START -> "START"
        BlockTypes.ACTION_CLICK_TEXT -> {
            val text = textField(block, "text").orEmpty()
            "CLICK \"$text\""
        }
        BlockTypes.ACTION_WAIT -> {
            val ms = numberField(block, "ms") ?: 0.0
            "WAIT ${ms.toLong()}ms"
        }
        BlockTypes.ACTION_FIND_TEMPLATE -> genericCommandLabel(block, "findTemplate")
        BlockTypes.DEBUG_LOG -> {
            val message = textField(block, "message").orEmpty()
            when {
                message.isNotBlank() -> "LOG \"$message\""
                else -> "LOG ${expressionInputLabel(document, block, "value") ?: "value"}"
            }
        }
        BlockTypes.FEEDBACK_BEEP -> {
            val frequency = numberField(block, "frequency") ?: 1000.0
            val duration = numberField(block, "durationMs") ?: 200.0
            val volume = numberField(block, "volume") ?: 100.0
            "BEEP ${frequency.toLong()}Hz ${duration.toLong()}ms ${volume.toLong()}%"
        }
        BlockTypes.FEEDBACK_VIBRATE -> {
            val pattern = textField(block, "pattern").orEmpty().ifBlank { "80" }
            "VIBRATE $pattern"
        }
        BlockTypes.CONTROL_REPEAT -> {
            val times = numberField(block, "times") ?: 0.0
            "REPEAT ${times.toLong()}x"
        }
        BlockTypes.CONTROL_WHILE -> "WHILE"
        BlockTypes.CONTROL_IF,
        BlockTypes.CONTROL_IF_ELSE,
        BlockTypes.CONTROL_IF_ELSEIF_ELSE -> "IF"
        BlockTypes.LOGIC_BOOLEAN -> "BOOL ${(boolField(block, "value") ?: false).toString().uppercase()}"
        BlockTypes.LOGIC_AND -> "AND"
        BlockTypes.LOGIC_OR -> "OR"
        BlockTypes.LOGIC_COMPARE -> {
            val op = normalizeCompareOperator(block)
            if (op == null) {
                diagnostics += unsupportedCompareDiagnostic(document, block, operatorRawValue(block))
                "COMPARE ?"
            } else {
                "COMPARE ${op.symbol}"
            }
        }
        BlockTypes.LOGIC_OPERATE -> {
            when (val normalized = OperatorNormalization.normalize(operatorRawValue(block))) {
                is NormalizedOperator.Arithmetic -> when (normalized.value) {
                    de.visualtasker.workflow.core.ArithmeticOperator.ADD -> "ADD"
                    de.visualtasker.workflow.core.ArithmeticOperator.SUB -> "SUB"
                    de.visualtasker.workflow.core.ArithmeticOperator.MUL -> "MUL"
                    de.visualtasker.workflow.core.ArithmeticOperator.DIV -> "DIV"
                    de.visualtasker.workflow.core.ArithmeticOperator.MOD -> "MOD"
                }
                is NormalizedOperator.Compare -> "COMPARE ${normalized.value.symbol}"
                null -> {
                    diagnostics += unsupportedCompareDiagnostic(document, block, operatorRawValue(block))
                    "OPERATE ?"
                }
            }
        }
        BlockTypes.LITERAL_NUMBER -> "NUM ${numberField(block, "value") ?: 0.0}"
        BlockTypes.LITERAL_STRING -> "STR \"${textField(block, "value").orEmpty()}\""
        BlockTypes.LITERAL_REGION -> "REGION ${stableNumber(numberField(block, "x") ?: 0.0)}, " +
            "${stableNumber(numberField(block, "y") ?: 0.0)}, " +
            "${stableNumber(numberField(block, "width") ?: 1.0)} x " +
            stableNumber(numberField(block, "height") ?: 1.0)
        BlockTypes.LITERAL_BOOLEAN -> "BOOL ${(boolField(block, "value") ?: false).toString().uppercase()}"
        BlockTypes.VARIABLE_GET,
        BlockTypes.VARIABLE_REPORTER,
        BlockTypes.VARIABLE_VALUE,
        BlockTypes.VARIABLES_GET -> variableLabel(block, document, diagnostics)
        BlockTypes.VARIABLE_SET -> {
            val variable = textField(block, "variable", "variableLabel", "variableId").orEmpty()
            val assignmentKind = textField(block, "assignmentKind").orEmpty().ifBlank { "SET" }
            "$assignmentKind $variable"
        }
        else -> if (block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
            variableLabel(block, document, diagnostics)
        } else if (block.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX)) {
            genericCommandLabel(block, block.type.removePrefix(BlockTypes.EMSCRIPT_COMMAND_PREFIX))
        } else {
            "UNSUPPORTED ${block.type}"
        }
    }

    private fun genericCommandLabel(block: BlockNode, fallbackCommand: String): String {
        val command = textField(block, "command")
            ?.takeIf { it.isNotBlank() }
            ?: fallbackCommand
        val args = commandArgumentKeys(block)
            .mapNotNull { key -> fieldAsDisplayText(block, key)?.let { key to it } }
        return if (args.isEmpty()) {
            command.uppercase()
        } else {
            "${command.uppercase()} ${args.joinToString(", ") { (_, value) -> value }}"
        }
    }

    private fun expressionInputLabel(
        document: WorkspaceDocument,
        block: BlockNode,
        inputName: String,
    ): String? {
        val connected = block.valueInputs.firstOrNull { it.name == inputName }?.connection?.connectedTo ?: return null
        val childId = WorkspaceGraph.findConnection(document, connected)?.first ?: return null
        val child = document.blocks[childId] ?: return null
        return when (child.type) {
            BlockTypes.LITERAL_STRING -> "\"${textField(child, "value").orEmpty()}\""
            BlockTypes.LITERAL_NUMBER -> numberField(child, "value")?.let(::stableNumber)
            BlockTypes.LITERAL_REGION -> "region(" + listOf("x", "y", "width", "height")
                .joinToString(", ") { key -> stableNumber(numberField(child, key) ?: if (key in setOf("width", "height")) 1.0 else 0.0) } + ")"
            BlockTypes.LITERAL_BOOLEAN,
            BlockTypes.LOGIC_BOOLEAN -> (boolField(child, "value") ?: false).toString()
            else -> if (child.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
                variableLabel(child, document, mutableListOf())
            } else {
                nodeLabelFor(child, document, mutableListOf())
            }
        }
    }

    private fun stableNumber(value: Double): String =
        if (value.isFinite() && value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun variableLabel(
        block: BlockNode,
        document: WorkspaceDocument,
        diagnostics: MutableList<FlowGraphDiagnostic>,
    ): String {
        val id = variableId(block, document)
        if (id == null) {
            diagnostics += missingVariableDiagnostic(document, block)
            return "VAR ?"
        }
        val label = textField(block, "variableLabel", "variableName", "label")
            ?: document.variables.variables[id]?.name
            ?: id
        return label
    }

    private fun variableId(block: BlockNode, document: WorkspaceDocument): String? {
        val explicit = textField(block, "variableId", "varId", "variable_id")
        if (!explicit.isNullOrBlank()) return explicit
        val prefixed = block.type
            .takeIf { it.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX) }
            ?.removePrefix(BlockTypes.VARIABLE_REPORTER_PREFIX)
            ?.takeIf { it.isNotBlank() }
        if (!prefixed.isNullOrBlank()) return prefixed
        val legacy = textField(block, "variable")
        return legacy?.takeIf { it in document.variables.variables }
    }

    private fun nodeProperties(
        block: BlockNode,
        document: WorkspaceDocument,
    ): Map<String, FlowSemanticValue> {
        val props = linkedMapOf<String, FlowSemanticValue>()
        props["blockType"] = FlowSemanticValue.StringValue(block.type)
        props["blockId"] = FlowSemanticValue.StringValue(block.id.value)
        variableId(block, document)?.let { props["variableId"] = FlowSemanticValue.StringValue(it) }
        textField(block, "variableLabel", "variableName", "label", "variable")
            ?.let { props["variableLabel"] = FlowSemanticValue.StringValue(it) }
        operatorRawValue(block)?.let { props["operator"] = FlowSemanticValue.StringValue(it) }
        props["inputPorts"] = FlowSemanticValue.ListValue(inputPortValues(block))
        props["outputPorts"] = FlowSemanticValue.ListValue(outputPortValues(block))
        numberField(block, "value")?.let { props["literalNumber"] = FlowSemanticValue.NumberValue(it.toString()) }
        textField(block, "value")?.let { props["literalString"] = FlowSemanticValue.StringValue(it) }
        boolField(block, "value")?.let { props["literalBoolean"] = FlowSemanticValue.BooleanValue(it) }
        numberField(block, "ms")?.let { props["waitMs"] = FlowSemanticValue.NumberValue(it.toString()) }
        numberField(block, "frequency")?.let { props["frequency"] = FlowSemanticValue.NumberValue(it.toString()) }
        numberField(block, "durationMs")?.let { props["durationMs"] = FlowSemanticValue.NumberValue(it.toString()) }
        numberField(block, "volume")?.let { props["volume"] = FlowSemanticValue.NumberValue(it.toString()) }
        textField(block, "pattern")?.let { props["pattern"] = FlowSemanticValue.StringValue(it) }
        textField(block, "message")?.let { props["message"] = FlowSemanticValue.StringValue(it) }
        textField(block, "text")?.let { props["text"] = FlowSemanticValue.StringValue(it) }
        textField(block, "command")?.let { props["command"] = FlowSemanticValue.StringValue(it) }
        commandArgumentKeys(block).forEach { key ->
            fieldAsDisplayText(block, key)?.let { props[key] = FlowSemanticValue.StringValue(it) }
        }
        return props
    }

    private fun commandArgumentKeys(block: BlockNode): List<String> =
        block.fields.keys
            .filter { it.startsWith("arg") }
            .sortedWith(compareBy({ it.removePrefix("arg").toIntOrNull() ?: Int.MAX_VALUE }, { it }))

    private fun fieldAsDisplayText(block: BlockNode, key: String): String? = when (val value = block.fields[key]) {
        is FieldValue.Text -> value.value.takeIf { it.isNotBlank() }
        is FieldValue.Number -> value.value.toString()
        is FieldValue.Bool -> value.value.toString()
        null -> null
    }

    private fun inputPortValues(block: BlockNode): List<FlowSemanticValue> = buildList {
        if (block.previous != null) {
            add(portValue(name = "previous", label = "Previous", kind = FlowEdgeKind.SEQUENCE))
        }
        block.valueInputs.forEach { input ->
            add(
                portValue(
                    name = input.name,
                    label = input.name,
                    kind = if (input.name.endsWith("CONDITION")) FlowEdgeKind.CONDITION else FlowEdgeKind.DATA_FLOW,
                )
            )
        }
    }

    private fun outputPortValues(block: BlockNode): List<FlowSemanticValue> = buildList {
        if (block.next != null) {
            add(
                portValue(
                    name = "next",
                    label = "Next",
                    kind = when (block.type) {
                        BlockTypes.CONTROL_REPEAT,
                        BlockTypes.CONTROL_WHILE -> FlowEdgeKind.LOOP_EXIT
                        else -> FlowEdgeKind.SEQUENCE
                    },
                )
            )
        }
        block.statementInputs.forEach { input ->
            add(
                portValue(
                    name = input.name,
                    label = input.name,
                    kind = when (input.name) {
                        BlockTypes.SLOT_THEN -> FlowEdgeKind.TRUE_BRANCH
                        BlockTypes.SLOT_ELSE -> FlowEdgeKind.FALSE_BRANCH
                        BlockTypes.SLOT_ELIF -> FlowEdgeKind.ELSE_IF_BRANCH
                        BlockTypes.SLOT_DO,
                        BlockTypes.SLOT_BODY -> FlowEdgeKind.LOOP_BODY
                        else -> FlowEdgeKind.SEQUENCE
                    },
                )
            )
        }
        if (block.output != null) {
            add(portValue(name = "output", label = "Output", kind = FlowEdgeKind.DATA_FLOW))
        }
    }

    private fun portValue(
        name: String,
        label: String,
        kind: FlowEdgeKind,
    ): FlowSemanticValue =
        FlowSemanticValue.ObjectValue(
            mapOf(
                "name" to FlowSemanticValue.StringValue(name),
                "label" to FlowSemanticValue.StringValue(label),
                "kind" to FlowSemanticValue.StringValue(kind.name),
            )
        )

    private fun normalizeCompareOperator(block: BlockNode): de.visualtasker.workflow.core.CompareOperator? {
        return when (val normalized = OperatorNormalization.normalize(operatorRawValue(block))) {
            is NormalizedOperator.Compare -> normalized.value
            else -> null
        }
    }

    private fun operatorRawValue(block: BlockNode): String? =
        textField(block, "operator", "compare", "op", "operation", "COMPARE_OP")

    private fun textField(block: BlockNode, vararg keys: String): String? {
        keys.forEach { key ->
            when (val value = block.fields[key]) {
                is FieldValue.Text -> return value.value.takeIf { it.isNotBlank() }
                else -> Unit
            }
        }
        return null
    }

    private fun numberField(block: BlockNode, key: String): Double? = when (val value = block.fields[key]) {
        is FieldValue.Number -> value.value
        is FieldValue.Text -> value.value.toDoubleOrNull()
        else -> null
    }

    private fun boolField(block: BlockNode, key: String): Boolean? = when (val value = block.fields[key]) {
        is FieldValue.Bool -> value.value
        is FieldValue.Text -> value.value.equals("true", ignoreCase = true)
        else -> null
    }

    private fun unsupportedBlockDiagnostic(document: WorkspaceDocument, block: BlockNode): FlowGraphDiagnostic =
        diagnostic(
            code = "UNSUPPORTED_FLOWCHART_BLOCK",
            message = "UNSUPPORTED_FLOWCHART_BLOCK: type=${block.type},blockId=${block.id.value}," +
                "fields=${block.fields.keys.sorted()},inputIds=${block.valueInputs.map { it.name }.sorted()}," +
                "stage=$STAGE,documentRevision=${document.version}",
            document = document,
            block = block,
        )

    private fun unsupportedCompareDiagnostic(
        document: WorkspaceDocument,
        block: BlockNode,
        operator: String?,
    ): FlowGraphDiagnostic = diagnostic(
        code = "UNSUPPORTED_COMPARE_OPERATOR",
        message = "UNSUPPORTED_COMPARE_OPERATOR: type=${block.type},operator=${operator ?: "<null>"}," +
            "blockId=${block.id.value},fields=${block.fields.keys.sorted()}," +
            "inputIds=${block.valueInputs.map { it.name }.sorted()},stage=$STAGE,documentRevision=${document.version}",
        document = document,
        block = block,
    )

    private fun missingVariableDiagnostic(document: WorkspaceDocument, block: BlockNode): FlowGraphDiagnostic =
        diagnostic(
            code = "MISSING_VARIABLE_ID",
            message = "MISSING_VARIABLE_ID: type=${block.type},blockId=${block.id.value}," +
                "fields=${block.fields.keys.sorted()},inputIds=${block.valueInputs.map { it.name }.sorted()}," +
                "stage=$STAGE,documentRevision=${document.version}",
            document = document,
            block = block,
        )

    private fun diagnostic(
        code: String,
        message: String,
        document: WorkspaceDocument,
        block: BlockNode,
    ): FlowGraphDiagnostic = FlowGraphDiagnostic(
        id = FlowDiagnosticId("diag:${code}:${block.id.value}"),
        severity = FlowDiagnosticSeverity.ERROR,
        code = code,
        message = message,
        nodeId = flowNodeId(block.id),
        sourceReference = sourceReference(document),
    )

    private fun sourceReference(document: WorkspaceDocument): FlowGraphSourceReference = FlowGraphSourceReference(
        producerDocumentId = document.id,
        sourceRevision = document.version.toString(),
        sourceKind = "blockeditor-workspace",
    )

    private fun flowNodeId(id: BlockId): FlowNodeId = FlowNodeId("block:${id.value}")
}
