package com.visualtasker.ime.ime.actions

import com.visualtasker.ime.ime.vtbridge.VisionTaskerBridge

sealed class ImeCommand {
    data class InputCommand(val text: String) : ImeCommand()
    data class VtCommand(val action: String, val payload: Map<String, String>) : ImeCommand()
    data class UiCommand(val sheet: String) : ImeCommand()
}

class ActionRouter(
    private val visionTaskerBridge: VisionTaskerBridge,
    private val onInput: (String) -> Unit,
    private val onUiSheet: (String) -> Unit
) {
    fun route(command: ImeCommand): Boolean {
        return when (command) {
            is ImeCommand.InputCommand -> {
                onInput(command.text)
                true
            }
            is ImeCommand.UiCommand -> {
                onUiSheet(command.sheet)
                true
            }
            is ImeCommand.VtCommand -> visionTaskerBridge.dispatch(command.action, command.payload)
        }
    }
}
