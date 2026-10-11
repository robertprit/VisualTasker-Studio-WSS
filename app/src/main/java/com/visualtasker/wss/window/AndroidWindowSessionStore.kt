package com.visualtasker.wss.window

import android.content.Context
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import org.json.JSONArray
import org.json.JSONObject

class AndroidWindowSessionStore(context: Context) : WindowSessionPersistence {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): List<WindowSession> =
        preferences.getString(SESSIONS_KEY, null)
            ?.let(WindowSessionJsonCodec::decode)
            .orEmpty()

    override fun save(sessions: List<WindowSession>) {
        preferences.edit().putString(SESSIONS_KEY, WindowSessionJsonCodec.encode(sessions)).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "window_session_registry_v1"
        const val SESSIONS_KEY = "sessions"
    }
}

internal object WindowSessionJsonCodec {
    fun encode(sessions: List<WindowSession>): String {
        val array = JSONArray()
        sessions.forEach { session ->
            val bounds = session.placement.bounds
            val display = session.displayContext
            array.put(JSONObject().apply {
                put("id", session.id.value)
                put("panelId", session.panelId)
                put("hostType", session.hostType.name)
                put("displayId", display.displayId.value)
                put("displayWidth", display.physicalWidthPx)
                put("displayHeight", display.physicalHeightPx)
                put("density", display.density.toDouble())
                put("rotation", display.rotation)
                put("insetLeft", display.insets.leftPx)
                put("insetTop", display.insets.topPx)
                put("insetRight", display.insets.rightPx)
                put("insetBottom", display.insets.bottomPx)
                put("left", bounds.left.toDouble())
                put("top", bounds.top.toDouble())
                put("width", bounds.width.toDouble())
                put("height", bounds.height.toDouble())
                put("coordinateSpace", bounds.space.name)
                put("dock", session.placement.docking.position.name)
                put("dockTarget", session.placement.docking.targetSessionId?.value)
                put("zIndex", session.placement.zIndex)
                put("visibility", session.visibility.name)
                put("interaction", persistentInteractionMode(session.interactionMode).name)
            })
        }
        return JSONObject().put("schema", 1).put("sessions", array).toString()
    }

    fun decode(serialized: String): List<WindowSession> = runCatching {
        val root = JSONObject(serialized)
        if (root.optInt("schema", 0) != 1) return emptyList()
        val array = root.optJSONArray("sessions") ?: return emptyList()
        buildList {
            for (index in 0 until array.length()) {
                decodeSession(array.optJSONObject(index) ?: continue)?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun decodeSession(json: JSONObject): WindowSession? = runCatching {
        val display = DisplayContext(
            displayId = DisplayId(json.getInt("displayId")),
            physicalWidthPx = json.getInt("displayWidth"),
            physicalHeightPx = json.getInt("displayHeight"),
            density = json.getDouble("density").toFloat(),
            rotation = json.getInt("rotation"),
            insets = WindowInsets(
                leftPx = json.optInt("insetLeft", 0),
                topPx = json.optInt("insetTop", 0),
                rightPx = json.optInt("insetRight", 0),
                bottomPx = json.optInt("insetBottom", 0),
            ),
        )
        val bounds = WindowBounds(
            left = json.getDouble("left").toFloat(),
            top = json.getDouble("top").toFloat(),
            width = json.getDouble("width").toFloat(),
            height = json.getDouble("height").toFloat(),
            space = WindowCoordinateSpace.valueOf(json.getString("coordinateSpace")),
        )
        WindowSession(
            id = WindowSessionId(json.getString("id")),
            panelId = json.getString("panelId"),
            hostType = WindowHostType.valueOf(json.getString("hostType")),
            displayContext = display,
            placement = WindowPlacement(
                bounds = bounds,
                docking = WindowDocking(
                    position = WindowDockPosition.valueOf(json.optString("dock", WindowDockPosition.FLOATING.name)),
                    targetSessionId = json.optString("dockTarget").takeIf(String::isNotBlank)?.let(::WindowSessionId),
                ),
                zIndex = json.optInt("zIndex", 0),
            ),
            visibility = WindowVisibility.valueOf(json.optString("visibility", WindowVisibility.VISIBLE.name)),
            interactionMode = WindowInteractionMode.valueOf(
                json.optString("interaction", WindowInteractionMode.INTERACTIVE.name)
            ),
        )
    }.getOrNull()

    private fun persistentInteractionMode(mode: WindowInteractionMode): WindowInteractionMode = when (mode) {
        WindowInteractionMode.DRAGGING,
        WindowInteractionMode.RESIZING,
        -> WindowInteractionMode.INTERACTIVE

        else -> mode
    }
}

object AndroidDisplayContextFactory {
    @Suppress("DEPRECATION")
    fun from(context: Context): DisplayContext {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = runCatching { context.display }.getOrNull() ?: windowManager.defaultDisplay
        val metrics = DisplayMetrics().also(display::getRealMetrics)
        val rotation = when (display.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return DisplayContext(
            displayId = DisplayId(display.displayId),
            physicalWidthPx = metrics.widthPixels,
            physicalHeightPx = metrics.heightPixels,
            density = metrics.density,
            rotation = rotation,
            insets = WindowInsets(
                topPx = systemDimension(context, "status_bar_height"),
                bottomPx = systemDimension(context, "navigation_bar_height"),
            ),
        )
    }

    private fun systemDimension(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "dimen", "android")
        return if (id == 0) 0 else context.resources.getDimensionPixelSize(id)
    }
}

object WindowSessionProcessRegistry {
    @Volatile
    private var registry: WindowSessionRegistry? = null

    fun get(context: Context): WindowSessionRegistry = registry ?: synchronized(this) {
        registry ?: WindowSessionRegistry(AndroidWindowSessionStore(context.applicationContext)).also { registry = it }
    }
}
