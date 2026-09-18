package com.visualtasker.wss.overlay

import android.content.Context

internal data class OverlayPlacement(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val visible: Boolean,
)

internal class OverlayStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(key: String): OverlayPlacement? =
        preferences.getString(key, null)?.let(OverlayPlacementCodec::decode)

    fun save(key: String, placement: OverlayPlacement) {
        preferences.edit().putString(key, OverlayPlacementCodec.encode(placement)).apply()
    }

    fun setVisible(key: String, visible: Boolean) {
        val current = load(key) ?: return
        save(key, current.copy(visible = visible))
    }

    fun visibleKeys(): Set<String> =
        preferences.all
            .mapNotNull { (key, value) ->
                key.takeIf { value is String && OverlayPlacementCodec.decode(value)?.visible == true }
            }
            .toSet()

    private companion object {
        const val PREFERENCES_NAME = "studio_overlay_state_v1"
    }
}

internal object OverlayPlacementCodec {
    fun encode(value: OverlayPlacement): String =
        listOf(value.x, value.y, value.width, value.height, if (value.visible) 1 else 0).joinToString(",")

    fun decode(serialized: String): OverlayPlacement? {
        val values = serialized.split(',')
        if (values.size != 5) return null
        val numbers = values.map { it.toIntOrNull() ?: return null }
        return OverlayPlacement(
            x = numbers[0],
            y = numbers[1],
            width = numbers[2],
            height = numbers[3],
            visible = numbers[4] == 1,
        )
    }
}
