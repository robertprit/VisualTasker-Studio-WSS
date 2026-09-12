package com.visualtasker.wss.workspace.model

enum class WssDropEffectKind {
    None,
    InsertText,
    CreateBlock,
    CreateFlowNode,
    LinkResource,
    SelectRailStep,
}

data class WssDropEffect(
    val kind: WssDropEffectKind,
    val label: String,
    val targetPanelId: String,
    val payloadId: String,
    val transferMode: WssDragTransferMode?,
    val text: String? = null,
    val commandId: String? = null,
    val resourceId: String? = null,
    val stepId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    val isProductive: Boolean
        get() = kind != WssDropEffectKind.None
}

object WssDropEffectResolver {
    fun resolve(result: WssDropResult?): WssDropEffect {
        if (result == null || !result.decision.accepted) {
            return none()
        }
        val payload = result.payload
        val target = result.target
        val mode = result.decision.mode
        return when (target.kind) {
            WssDragTargetKind.TextEditor -> WssDropEffect(
                kind = WssDropEffectKind.InsertText,
                label = "Text einfügen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                text = payload.toEmscriptSnippet(),
                commandId = payload.data["commandId"],
                resourceId = payload.data["resourceId"],
                stepId = payload.data["stepId"],
                metadata = payload.data,
            )
            WssDragTargetKind.BlockEditor -> WssDropEffect(
                kind = WssDropEffectKind.CreateBlock,
                label = "Block erzeugen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                text = payload.toEmscriptSnippet(),
                commandId = payload.data["commandId"],
                resourceId = payload.data["resourceId"],
                stepId = payload.data["stepId"],
                metadata = payload.data,
            )
            WssDragTargetKind.FlowEditor -> WssDropEffect(
                kind = WssDropEffectKind.CreateFlowNode,
                label = "Node erzeugen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                text = payload.toEmscriptSnippet(),
                commandId = payload.data["commandId"],
                resourceId = payload.data["resourceId"],
                stepId = payload.data["stepId"],
                metadata = payload.data,
            )
            WssDragTargetKind.Datastore,
            WssDragTargetKind.Inspector,
            WssDragTargetKind.MarkerPanel,
            WssDragTargetKind.VisualAssetManager,
            -> WssDropEffect(
                kind = WssDropEffectKind.LinkResource,
                label = "Resource verknüpfen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                resourceId = payload.data["resourceId"] ?: payload.data["candidateId"],
                stepId = payload.data["stepId"],
                metadata = payload.data,
            )
            WssDragTargetKind.RailTrace -> WssDropEffect(
                kind = WssDropEffectKind.SelectRailStep,
                label = "RailTrace auswählen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                stepId = payload.data["stepId"],
                metadata = payload.data,
            )
            WssDragTargetKind.Toolbox -> WssDropEffect(
                kind = WssDropEffectKind.LinkResource,
                label = "Toolbox Item verknüpfen: ${payload.label}",
                targetPanelId = target.panelId,
                payloadId = payload.id,
                transferMode = mode,
                commandId = payload.data["commandId"],
                resourceId = payload.data["resourceId"],
                metadata = payload.data,
            )
        }
    }

    private fun none(): WssDropEffect =
        WssDropEffect(
            kind = WssDropEffectKind.None,
            label = "Kein Drop",
            targetPanelId = "",
            payloadId = "",
            transferMode = null,
        )
}

private fun WssDragPayload.toEmscriptSnippet(): String =
    data["emscript"]
        ?: data["script"]
        ?: when (kind) {
            WssDragPayloadKind.TextSnippet -> data["text"].orEmpty()
            WssDragPayloadKind.Marker -> markerSnippet()
            WssDragPayloadKind.DatasetEntry -> datasetSnippet()
            WssDragPayloadKind.RailTraceStep -> railTraceSnippet()
            WssDragPayloadKind.Resource -> resourceSnippet()
            WssDragPayloadKind.VisualAsset -> assetSnippet()
            else -> "log(${label.emscriptQuote()})"
        }

private fun WssDragPayload.markerSnippet(): String {
    val resourceId = data["resourceId"] ?: data["candidateId"] ?: id
    return "markerLoad(${resourceId.emscriptQuote()})"
}

private fun WssDragPayload.datasetSnippet(): String {
    val resourceId = data["resourceId"] ?: data["candidateId"] ?: id
    return "datasetLoad(${resourceId.emscriptQuote()})"
}

private fun WssDragPayload.resourceSnippet(): String {
    val resourceId = data["resourceId"] ?: id
    return "resourceLoad(${resourceId.emscriptQuote()})"
}

private fun WssDragPayload.assetSnippet(): String {
    val assetId = data["assetId"] ?: data["resourceId"] ?: id
    return "assetLoad(${assetId.emscriptQuote()})"
}

private fun WssDragPayload.railTraceSnippet(): String {
    val actionType = data["actionType"]?.lowercase().orEmpty()
    val detail = data["detail"] ?: label
    return when (actionType) {
        "wait" -> "wait(${detail.filter(Char::isDigit).ifBlank { "100" }})"
        "log" -> "log(${detail.emscriptQuote()})"
        "beep" -> "beep()"
        "vibrate" -> "vibrate(40)"
        else -> "log(${("RailTrace: $label").emscriptQuote()})"
    }
}

private fun String.emscriptQuote(): String =
    buildString {
        append('"')
        this@emscriptQuote.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
