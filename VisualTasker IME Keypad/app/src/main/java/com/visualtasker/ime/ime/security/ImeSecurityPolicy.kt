package com.visualtasker.ime.ime.security

import com.visualtasker.ime.ime.context.EditorContext

object ImeSecurityPolicy {
    fun canPersistClipboard(context: EditorContext?): Boolean = context?.isSensitive != true

    fun canAnalyzeSelection(context: EditorContext?): Boolean = context?.isSensitive != true

    fun canSendToAutomation(context: EditorContext?): Boolean = context?.isSensitive != true
}
