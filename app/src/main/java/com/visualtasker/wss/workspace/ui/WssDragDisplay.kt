package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.WssDragPayloadKind
import com.visualtasker.wss.workspace.model.WssDropEffectKind

fun WssDragPayloadKind.wssDisplayName(): String =
    when (this) {
        WssDragPayloadKind.Command -> "Command"
        WssDragPayloadKind.Block -> "Block"
        WssDragPayloadKind.FlowNode -> "Node"
        WssDragPayloadKind.Marker -> "Marker"
        WssDragPayloadKind.Resource -> "Resource"
        WssDragPayloadKind.DatasetEntry -> "Dataset"
        WssDragPayloadKind.InspectorField -> "Inspector"
        WssDragPayloadKind.RailTraceStep -> "Step"
        WssDragPayloadKind.VisualAsset -> "Asset"
        WssDragPayloadKind.TextSnippet -> "Text"
    }

fun WssDropEffectKind.wssDisplayName(): String =
    when (this) {
        WssDropEffectKind.None -> "kein Drop"
        WssDropEffectKind.InsertText -> "Text einfuegen"
        WssDropEffectKind.CreateBlock -> "Block erzeugen"
        WssDropEffectKind.CreateFlowNode -> "Node erzeugen"
        WssDropEffectKind.LinkResource -> "verknuepfen"
        WssDropEffectKind.SelectRailStep -> "auswaehlen"
    }
