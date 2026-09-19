package com.visualtasker.ime.ime.stylus

import android.os.Build

enum class StylusMode {
    KEYBOARD,
    HANDWRITING,
    GESTURE_PAD,
    COMMAND_PAD
}

interface StylusSupport {
    fun supportsHandwriting(): Boolean
    fun currentMode(): StylusMode
}

class AndroidStylusSupport : StylusSupport {
    private var mode: StylusMode = StylusMode.KEYBOARD

    override fun supportsHandwriting(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    override fun currentMode(): StylusMode = mode

    fun setMode(newMode: StylusMode) {
        mode = newMode
    }
}
