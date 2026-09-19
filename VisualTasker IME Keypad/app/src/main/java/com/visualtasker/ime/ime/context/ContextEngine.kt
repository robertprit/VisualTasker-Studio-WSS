package com.visualtasker.ime.ime.context

import android.text.InputType
import android.view.inputmethod.EditorInfo

object ContextEngine {
    fun resolveContext(
        info: EditorInfo?,
        emscriptEnabled: Boolean,
        visionTaskerEnabled: Boolean
    ): EditorContext {
        if (info == null) {
            return EditorContext(
                semanticType = FieldSemanticType.TEXT,
                isSensitive = false,
                isMultiline = false,
                packageName = null,
                fieldName = null
            )
        }

        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val inputVariation = info.inputType and InputType.TYPE_MASK_VARIATION
        val isPasswordVariation = inputVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            inputVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            inputVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val isVisiblePassword = (info.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
        val isSensitive = isPasswordVariation || isVisiblePassword
        val isMultiline = (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ||
            (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        val fieldName = info.fieldName?.lowercase()
        val semanticType = when {
            isSensitive -> FieldSemanticType.PASSWORD
            inputClass == InputType.TYPE_CLASS_PHONE -> FieldSemanticType.PHONE
            inputClass == InputType.TYPE_CLASS_NUMBER -> FieldSemanticType.NUMBER
            inputVariation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                inputVariation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> FieldSemanticType.EMAIL
            inputVariation == InputType.TYPE_TEXT_VARIATION_URI -> FieldSemanticType.URL
            visionTaskerEnabled && fieldName.hasAny("visiontasker", "vt", "automation", "workflow") ->
                FieldSemanticType.VISIONTASKER
            emscriptEnabled && fieldName.hasAny("emscript", "script", "workflow") -> FieldSemanticType.EMSCRIPT
            fieldName.hasAny("code", "json", "xml", "kotlin", "java", "shell", "terminal", "query", "sql") ->
                FieldSemanticType.CODE
            isMultiline -> FieldSemanticType.MULTILINE
            else -> FieldSemanticType.TEXT
        }

        return EditorContext(
            semanticType = semanticType,
            isSensitive = isSensitive,
            isMultiline = isMultiline,
            packageName = info.packageName?.toString(),
            fieldName = info.fieldName
        )
    }

    fun resolveProfile(
        context: EditorContext,
        emscriptEnabled: Boolean,
        visionTaskerEnabled: Boolean
    ): ResolvedKeyboardProfile {
        if (visionTaskerEnabled) {
            return ResolvedKeyboardProfile(KeyboardProfile.VISIONTASKER, KeyboardLayout.FUNCTION)
        }
        return when (context.semanticType) {
            FieldSemanticType.PHONE ->
                ResolvedKeyboardProfile(KeyboardProfile.NORMAL, KeyboardLayout.KEYPAD)
            FieldSemanticType.NUMBER ->
                ResolvedKeyboardProfile(KeyboardProfile.NORMAL, KeyboardLayout.KEYPAD)
            FieldSemanticType.CODE ->
                ResolvedKeyboardProfile(KeyboardProfile.DEVELOPER, KeyboardLayout.FUNCTION)
            FieldSemanticType.EMSCRIPT ->
                if (emscriptEnabled) {
                    ResolvedKeyboardProfile(KeyboardProfile.EMSCRIPT, KeyboardLayout.FUNCTION)
                } else {
                    ResolvedKeyboardProfile(KeyboardProfile.NORMAL, KeyboardLayout.QWERTZ)
                }
            FieldSemanticType.VISIONTASKER ->
                if (visionTaskerEnabled) {
                    ResolvedKeyboardProfile(KeyboardProfile.VISIONTASKER, KeyboardLayout.FUNCTION)
                } else {
                    ResolvedKeyboardProfile(KeyboardProfile.NORMAL, KeyboardLayout.QWERTZ)
                }
            else -> ResolvedKeyboardProfile(KeyboardProfile.NORMAL, KeyboardLayout.QWERTZ)
        }
    }

    private fun String?.hasAny(vararg values: String): Boolean {
        val source = this ?: return false
        return values.any { source.contains(it) }
    }
}
