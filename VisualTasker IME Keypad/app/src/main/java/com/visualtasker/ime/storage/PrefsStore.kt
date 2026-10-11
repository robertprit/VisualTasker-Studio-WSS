package com.visualtasker.ime.storage

import android.content.Context

data class VtKeySlot(
    val label: String,
    val script: String
)

data class CustomLayoutKeyBinding(
    val label: String,
    val output: String
)

class PrefsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ime_keypad_prefs", Context.MODE_PRIVATE)

    fun getClipboardHistory(): List<String> {
        return prefs.getString(KEY_CLIPBOARD, "")!!
            .split(SEP)
            .filter { it.isNotBlank() }
    }

    fun addClipboardEntry(value: String) {
        if (!isClipboardHistoryEnabled()) return
        if (value.isBlank()) return
        val deduped = mutableListOf(value)
        deduped += getClipboardHistory().filter { it != value }
        prefs.edit().putString(KEY_CLIPBOARD, deduped.take(20).joinToString(SEP)).apply()
    }

    fun setClipboardHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD_ENABLED, enabled).apply()
    }

    fun isClipboardHistoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_CLIPBOARD_ENABLED, true)
    }

    fun getMacros(): List<String> {
        val current = prefs.getString(KEY_MACROS, "")!!
            .split(SEP)
            .filter { it.isNotBlank() }
            .toMutableList()
        while (current.size < 6) current.add("M${current.size + 1}")
        return current.take(6)
    }

    fun setMacro(index: Int, text: String) {
        if (index !in 0..5) return
        val macros = getMacros().toMutableList()
        macros[index] = text
        prefs.edit().putString(KEY_MACROS, macros.joinToString(SEP)).apply()
    }

    fun setPendingTaskerText(text: String) {
        prefs.edit().putString(KEY_PENDING_TASKER, text).apply()
    }

    fun consumePendingTaskerText(): String? {
        val value = prefs.getString(KEY_PENDING_TASKER, null)
        if (!value.isNullOrBlank()) {
            prefs.edit().remove(KEY_PENDING_TASKER).apply()
        }
        return value
    }

    fun setColorInsertModeArgb(isArgb: Boolean) {
        prefs.edit().putBoolean(KEY_COLOR_MODE_ARGB, isArgb).apply()
    }

    fun isColorInsertModeArgb(): Boolean {
        return prefs.getBoolean(KEY_COLOR_MODE_ARGB, true)
    }

    fun setDarkKeyboardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_KEYBOARD, enabled).apply()
    }

    fun isDarkKeyboardEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_KEYBOARD, true)
    }

    fun setSpecialKeyMapping(keyCode: Int, androidKeyCode: Int) {
        prefs.edit().putInt("$KEY_SPECIAL_PREFIX$keyCode", androidKeyCode).apply()
    }

    fun getSpecialKeyMapping(keyCode: Int, defaultAndroidKeyCode: Int): Int {
        return prefs.getInt("$KEY_SPECIAL_PREFIX$keyCode", defaultAndroidKeyCode)
    }

    fun setEmscriptContextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMSCRIPT_CONTEXT_ENABLED, enabled).apply()
    }

    fun isEmscriptContextEnabled(): Boolean {
        return prefs.getBoolean(KEY_EMSCRIPT_CONTEXT_ENABLED, false)
    }

    fun setVisionTaskerContextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VISION_TASKER_CONTEXT_ENABLED, enabled).apply()
    }

    fun isVisionTaskerContextEnabled(): Boolean {
        return prefs.getBoolean(KEY_VISION_TASKER_CONTEXT_ENABLED, false)
    }

    fun getVtKeySlot(index: Int): VtKeySlot {
        require(index in 1..2)
        return VtKeySlot(
            label = prefs.getString("${KEY_VT_SLOT_LABEL}$index", "VT$index")
                .orEmpty()
                .ifBlank { "VT$index" }
                .take(6),
            script = prefs.getString("${KEY_VT_SLOT_SCRIPT}$index", "").orEmpty()
        )
    }

    fun setVtKeySlot(index: Int, label: String, script: String) {
        require(index in 1..2)
        prefs.edit()
            .putString("${KEY_VT_SLOT_LABEL}$index", label.trim().ifBlank { "VT$index" }.take(6))
            .putString("${KEY_VT_SLOT_SCRIPT}$index", script.trim())
            .apply()
    }

    fun getSelectedFontFamily(): String {
        return prefs.getString(KEY_FONT_FAMILY, FONT_ROBOTO) ?: FONT_ROBOTO
    }

    fun setSelectedFontFamily(fontFamily: String) {
        val normalized = if (fontFamily == FONT_NOTO) FONT_NOTO else FONT_ROBOTO
        prefs.edit().putString(KEY_FONT_FAMILY, normalized).apply()
    }

    fun getCustom13x4Bindings(): List<CustomLayoutKeyBinding> {
        val raw = prefs.getString(KEY_CUSTOM_13X4_BINDINGS, null)
        if (raw.isNullOrBlank()) return defaultCustom13x4Bindings()
        val parsed = raw.split(SEP).mapNotNull { item ->
            val parts = item.split(PAIR_SEP, limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val label = parts[0].ifBlank { "K" }
            val output = parts[1]
            CustomLayoutKeyBinding(label = label.take(8), output = output)
        }
        return if (parsed.size == CUSTOM_13X4_KEY_COUNT) parsed else defaultCustom13x4Bindings()
    }

    fun setCustom13x4Bindings(bindings: List<CustomLayoutKeyBinding>) {
        if (bindings.size != CUSTOM_13X4_KEY_COUNT) return
        val compact = bindings.joinToString(SEP) { binding ->
            "${binding.label.trim().ifBlank { "K" }.take(8)}$PAIR_SEP${binding.output.trim()}"
        }
        prefs.edit().putString(KEY_CUSTOM_13X4_BINDINGS, compact).apply()
    }

    fun getCustom13x4BindingsAsConfigText(): String {
        return getCustom13x4Bindings().joinToString("\n") { "${it.label}=${escapeConfigOutput(it.output)}" }
    }

    fun updateCustom13x4BindingsFromConfig(configText: String): Boolean {
        val lines = configText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size != CUSTOM_13X4_KEY_COUNT) return false
        val bindings = lines.map { line ->
            val split = line.split("=", limit = 2)
            val label = split.firstOrNull()?.trim().orEmpty()
            val outputRaw = split.getOrNull(1)?.trim().orEmpty()
            CustomLayoutKeyBinding(
                label = label.ifBlank { "K" }.take(8),
                output = unescapeConfigOutput(outputRaw)
            )
        }
        setCustom13x4Bindings(bindings)
        return true
    }

    companion object {
        private const val KEY_CLIPBOARD = "clipboard_history"
        private const val KEY_MACROS = "macros"
        private const val KEY_PENDING_TASKER = "pending_tasker_text"
        private const val KEY_COLOR_MODE_ARGB = "color_mode_argb"
        private const val KEY_DARK_KEYBOARD = "dark_keyboard_enabled"
        private const val KEY_SPECIAL_PREFIX = "special_key_"
        private const val KEY_EMSCRIPT_CONTEXT_ENABLED = "emscript_context_enabled"
        private const val KEY_VISION_TASKER_CONTEXT_ENABLED = "vision_tasker_context_enabled"
        private const val KEY_CLIPBOARD_ENABLED = "clipboard_enabled"
        private const val KEY_VT_SLOT_LABEL = "vt_slot_label_"
        private const val KEY_VT_SLOT_SCRIPT = "vt_slot_script_"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_CUSTOM_13X4_BINDINGS = "custom_13x4_bindings"
        private const val CUSTOM_13X4_KEY_COUNT = 52
        private const val PAIR_SEP = "\u001E"
        private const val SEP = "\u001F"

        const val FONT_ROBOTO = "roboto"
        const val FONT_NOTO = "noto"

        private fun defaultCustom13x4Bindings(): List<CustomLayoutKeyBinding> {
            val defaults = mutableListOf<CustomLayoutKeyBinding>()
            for (index in 1..CUSTOM_13X4_KEY_COUNT) {
                val id = index.toString().padStart(2, '0')
                defaults += CustomLayoutKeyBinding(label = "K$id", output = "")
            }
            return defaults
        }

        private fun escapeConfigOutput(value: String): String {
            return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
        }

        private fun unescapeConfigOutput(value: String): String {
            val step1 = value.replace("\\n", "\n").replace("\\t", "\t")
            return step1.replace("\\\\", "\\")
        }
    }
}
