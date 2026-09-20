package com.visualtasker.ime.storage

import android.content.Context

data class VtKeySlot(
    val label: String,
    val script: String
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
        private const val SEP = "\u001F"
    }
}
