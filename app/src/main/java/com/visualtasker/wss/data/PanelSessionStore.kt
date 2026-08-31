package com.visualtasker.wss.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class PanelSessionSnapshot(
    val panels: List<PanelState>,
    val activeTargetPanelId: String,
    val panelTexts: Map<String, String>,
    val panelCursors: Map<String, Int>,
    val insertModes: Map<String, Boolean>,
    val functionKeyActions: Map<String, String>,
    val emscriptActiveTabId: String = "manual",
    val emscriptFontSizeSp: Float = 12f,
    val emscriptSelectionStarts: Map<String, Int> = emptyMap(),
    val emscriptSelectionEnds: Map<String, Int> = emptyMap(),
    val emscriptFoldedKeysByTab: Map<String, String> = emptyMap(),
    val emscriptFileManagerCurrentName: String = "draft",
    val emscriptFileManagerScripts: Map<String, String> = emptyMap(),
)

class PanelSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("panel_session_store", Context.MODE_PRIVATE)

    fun save(snapshot: PanelSessionSnapshot) {
        prefs.edit().putString("session_json", serializeSnapshot(snapshot)).apply()
    }

    fun load(): PanelSessionSnapshot? {
        val raw = prefs.getString("session_json", null) ?: return null
        return deserializeSnapshot(raw)
    }
}

internal fun serializeSnapshot(snapshot: PanelSessionSnapshot): String {
    val root = JSONObject()
    val panelsJson = JSONArray()
    snapshot.panels.forEach { panel ->
        val p = JSONObject()
        p.put("id", panel.id)
        p.put("x", panel.position.x.toDouble())
        p.put("y", panel.position.y.toDouble())
        p.put("width", panel.width)
        p.put("height", panel.height)
        p.put("accent", panel.accentColor.value.toLong())
        p.put("title", panel.title)
        p.put("panelType", panel.panelType.name)
        p.put("zIndex", panel.zIndex)
        p.put("min", panel.isMinimized)
        p.put("max", panel.isMaximized)
        panelsJson.put(p)
    }
    root.put("panels", panelsJson)
    root.put("activeTargetPanelId", snapshot.activeTargetPanelId)
    root.put("panelTexts", JSONObject(snapshot.panelTexts))
    root.put("panelCursors", JSONObject(snapshot.panelCursors))
    root.put("insertModes", JSONObject(snapshot.insertModes))
    root.put("functionKeyActions", JSONObject(snapshot.functionKeyActions))
    root.put("emscriptActiveTabId", snapshot.emscriptActiveTabId)
    root.put("emscriptFontSizeSp", snapshot.emscriptFontSizeSp.toDouble())
    root.put("emscriptSelectionStarts", JSONObject(snapshot.emscriptSelectionStarts))
    root.put("emscriptSelectionEnds", JSONObject(snapshot.emscriptSelectionEnds))
    root.put("emscriptFoldedKeysByTab", JSONObject(snapshot.emscriptFoldedKeysByTab))
    root.put("emscriptFileManagerCurrentName", snapshot.emscriptFileManagerCurrentName)
    root.put("emscriptFileManagerScripts", JSONObject(snapshot.emscriptFileManagerScripts))
    return root.toString()
}

internal fun deserializeSnapshot(raw: String): PanelSessionSnapshot? = runCatching {
    val root = JSONObject(raw)
    val panelsJson = root.optJSONArray("panels") ?: JSONArray()
    val panels = buildList {
        for (i in 0 until panelsJson.length()) {
            val p = panelsJson.getJSONObject(i)
            add(
                PanelState(
                    id = p.optString("id"),
                    position = Offset(
                        p.optDouble("x", 0.0).toFloat(),
                        p.optDouble("y", 0.0).toFloat(),
                    ),
                    width = p.optInt("width", 320),
                    height = p.optInt("height", 260),
                    accentColor = Color(p.optLong("accent", 0xFF6C5CE7)),
                    title = p.optString("title", "Panel"),
                    panelType = restorePanelType(p.optString("panelType", PanelType.EDITOR.name)),
                    zIndex = p.optInt("zIndex", i + 1),
                    isMinimized = p.optBoolean("min", false),
                    isMaximized = p.optBoolean("max", false),
                ),
            )
        }
    }
    PanelSessionSnapshot(
        panels = panels,
        activeTargetPanelId = root.optString("activeTargetPanelId", "1"),
        panelTexts = root.optJSONObject("panelTexts").toStringMap(),
        panelCursors = root.optJSONObject("panelCursors").toIntMap(),
        insertModes = root.optJSONObject("insertModes").toBooleanMap(),
        functionKeyActions = root.optJSONObject("functionKeyActions").toStringMap(),
        emscriptActiveTabId = root.optString("emscriptActiveTabId", "manual"),
        emscriptFontSizeSp = root.optDouble("emscriptFontSizeSp", 12.0).toFloat(),
        emscriptSelectionStarts = root.optJSONObject("emscriptSelectionStarts").toIntMap(),
        emscriptSelectionEnds = root.optJSONObject("emscriptSelectionEnds").toIntMap(),
        emscriptFoldedKeysByTab = root.optJSONObject("emscriptFoldedKeysByTab").toStringMap(),
        emscriptFileManagerCurrentName = root.optString("emscriptFileManagerCurrentName", "draft"),
        emscriptFileManagerScripts = root.optJSONObject("emscriptFileManagerScripts").toStringMap(),
    )
}.getOrNull()

internal fun restorePanelType(rawType: String?): PanelType =
    runCatching { PanelType.valueOf(rawType ?: PanelType.EDITOR.name) }
        .getOrDefault(PanelType.EDITOR)

private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val map = mutableMapOf<String, String>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = optString(key, "")
    }
    return map
}

private fun JSONObject?.toIntMap(): Map<String, Int> {
    if (this == null) return emptyMap()
    val map = mutableMapOf<String, Int>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = optInt(key, 0)
    }
    return map
}

private fun JSONObject?.toBooleanMap(): Map<String, Boolean> {
    if (this == null) return emptyMap()
    val map = mutableMapOf<String, Boolean>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = optBoolean(key, false)
    }
    return map
}

