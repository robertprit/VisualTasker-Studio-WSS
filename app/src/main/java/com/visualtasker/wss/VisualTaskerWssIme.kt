package com.visualtasker.wss

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.visualtasker.wss.ui.theme.VisualTaskerWssTheme

class VisualTaskerWssIme : InputMethodService() {

    private var keyboardMode by mutableStateOf(KeyboardMode.BOTTOM_SHEET)
    private var isNumpadMode by mutableStateOf(false)
    private var capsLock by mutableStateOf(false)
    private var settings by mutableStateOf(KeyboardSettings())

    private lateinit var prefs: SharedPreferences
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("visualtasker_wss_prefs", Context.MODE_PRIVATE)
        loadSettings()
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).also { keyboardView = it }.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VisualTaskerWssTheme {
                    KeyboardIMEView(
                        mode = keyboardMode,
                        isNumpadMode = isNumpadMode,
                        capsLock = capsLock,
                        settings = settings,
                        onKeyPress = { handleKeyPress(it) },
                        onToggleNumpadMode = { isNumpadMode = !isNumpadMode },
                        onToggleMode = {
                            keyboardMode = when (keyboardMode) {
                                KeyboardMode.BOTTOM_SHEET -> KeyboardMode.FLOATING
                                KeyboardMode.FLOATING -> KeyboardMode.BOTTOM_SHEET
                            }
                        },
                        onOpenSettings = { /* TODO settings dialog */ },
                        onCut = { performContextAction(android.R.id.cut) },
                        onCopy = { performContextAction(android.R.id.copy) },
                        onPaste = { performContextAction(android.R.id.paste) },
                    )
                }
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        capsLock = false
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        outInsets.contentTopInsets = outInsets.visibleTopInsets
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "BACK" -> ic.deleteSurroundingText(1, 0)
            "SPACE" -> ic.commitText(" ", 1)
            "ENTER" -> ic.commitText("\n", 1)
            "CAPS" -> capsLock = !capsLock
            "UP" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP)
            "DOWN" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN)
            "LEFT" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            "RIGHT" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            "PGUP" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_UP)
            "PGDN" -> sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_DOWN)
            else -> {
                val text = if (capsLock) key.uppercase() else key.lowercase()
                ic.commitText(text, 1)
            }
        }
        if (settings.hapticEnabled) {
            keyboardView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
    }

    private fun performContextAction(id: Int) {
        currentInputConnection?.performContextMenuAction(id)
    }

    private fun loadSettings() {
        settings = KeyboardSettings(
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            hapticEnabled = prefs.getBoolean("haptic_enabled", true),
            repeatDelayMs = prefs.getLong("repeat_delay", 400L),
        )
    }

    @Suppress("UNUSED")
    private fun saveSettings() {
        prefs.edit()
            .putBoolean("sound_enabled", settings.soundEnabled)
            .putBoolean("haptic_enabled", settings.hapticEnabled)
            .putLong("repeat_delay", settings.repeatDelayMs)
            .apply()
    }
}
