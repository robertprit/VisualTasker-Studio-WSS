package com.visualtasker.wss.workspace.data

import android.content.Context
import com.visualtasker.wss.workspace.model.PanelState
import com.visualtasker.wss.workspace.model.PanelType
import org.json.JSONArray
import org.json.JSONObject

data class WorkspaceSessionSnapshot(
    val panels: List<PanelState>
)

class WorkspaceSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("workspace_shell_session", Context.MODE_PRIVATE)

    fun save(snapshot: WorkspaceSessionSnapshot) {
        val root = JSONObject()
        val array = JSONArray()
        snapshot.panels.forEach { panel ->
            val p = JSONObject()
            p.put("id", panel.id)
            p.put("type", panel.type.name)
            p.put("title", panel.title)
            p.put("x", panel.x.toDouble())
            p.put("y", panel.y.toDouble())
            p.put("width", panel.width.toDouble())
            p.put("height", panel.height.toDouble())
            p.put("zIndex", panel.zIndex)
            p.put("minimized", panel.minimized)
            p.put("locked", panel.locked)
            p.put("accent", panel.accentColor.value.toLong())
            p.put("maximized", panel.isMaximized)
            array.put(p)
        }
        root.put("panels", array)
        prefs.edit().putString("session_json", root.toString()).apply()
    }

    fun load(): WorkspaceSessionSnapshot? {
        val raw = prefs.getString("session_json", null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val panelsJson = root.optJSONArray("panels") ?: JSONArray()
            val panels = buildList {
                for (index in 0 until panelsJson.length()) {
                    val p = panelsJson.getJSONObject(index)
                    val type = restorePanelType(p.optString("type", PanelType.BlockEditor.name))
                        ?: continue
                    add(
                        PanelState(
                            id = p.optString("id"),
                            type = type,
                            title = p.optString("title", "Panel"),
                            x = p.optDouble("x", 0.0).toFloat(),
                            y = p.optDouble("y", 0.0).toFloat(),
                            width = p.optDouble("width", 320.0).toFloat(),
                            height = p.optDouble("height", 240.0).toFloat(),
                            zIndex = p.optInt("zIndex", index + 1),
                            minimized = p.optBoolean("minimized", false),
                            locked = p.optBoolean("locked", false),
                            accentColor = restorePanelAccent(type, p.optLong("accent", defaultAccentForPanelType(type).value.toLong())),
                            isMaximized = p.optBoolean("maximized", false)
                        )
                    )
                }
            }
            WorkspaceSessionSnapshot(panels = panels)
        }.getOrNull()
    }
}

private fun restorePanelType(raw: String): PanelType? =
    runCatching { PanelType.valueOf(raw) }
        .getOrNull()
        ?.takeIf { it in supportedWorkspacePanelTypes }

internal val supportedWorkspacePanelTypes: Set<PanelType> = setOf(
    PanelType.RecorderSteps,
    PanelType.BlockEditor,
    PanelType.Flowchart,
    PanelType.Screenshot,
    PanelType.Marker,
    PanelType.Vision,
    PanelType.Datastore,
    PanelType.RuntimeLog,
    PanelType.TextEditor,
    PanelType.LogConsole,
    PanelType.DebugInfo
)

internal fun defaultAccentForPanelType(type: PanelType): androidx.compose.ui.graphics.Color = when (type) {
    PanelType.RecorderSteps -> androidx.compose.ui.graphics.Color(0xFF00D4AA)
    PanelType.BlockEditor -> androidx.compose.ui.graphics.Color(0xFF6C5CE7)
    PanelType.Flowchart -> androidx.compose.ui.graphics.Color(0xFF00B8FF)
    PanelType.RuntimeLog -> androidx.compose.ui.graphics.Color(0xFFFFC857)
    PanelType.TextEditor -> androidx.compose.ui.graphics.Color(0xFF8EC5FC)
    PanelType.LogConsole -> androidx.compose.ui.graphics.Color(0xFFFFC857)
    PanelType.DebugInfo -> androidx.compose.ui.graphics.Color(0xFFB39DDB)
    PanelType.Screenshot -> androidx.compose.ui.graphics.Color(0xFF7C4DFF)
    PanelType.Marker -> androidx.compose.ui.graphics.Color(0xFFFF5DA2)
    PanelType.Vision -> androidx.compose.ui.graphics.Color(0xFF21E6C1)
    PanelType.Datastore -> androidx.compose.ui.graphics.Color(0xFF8BC34A)
    PanelType.Emscript -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
    PanelType.M3Director -> androidx.compose.ui.graphics.Color(0xFF6C5CE7)
}

private fun restorePanelAccent(type: PanelType, raw: Long): androidx.compose.ui.graphics.Color {
    val default = defaultAccentForPanelType(type)
    if (raw == LEGACY_GENERIC_PANEL_ACCENT && default.value.toLong() != LEGACY_GENERIC_PANEL_ACCENT) {
        return default
    }
    val color = androidx.compose.ui.graphics.Color(raw)
    return if (color.alpha < 0.35f) default else color
}

private const val LEGACY_GENERIC_PANEL_ACCENT = 0xFF6C5CE7L
