package com.visualtasker.ime.ime.context

enum class KeyboardProfile {
    NORMAL,
    DEVELOPER,
    EMSCRIPT,
    VISIONTASKER
}

enum class KeyboardLayout {
    QWERTZ,
    SYMBOLS,
    FUNCTION,
    KEYPAD
}

enum class FieldSemanticType {
    TEXT,
    NUMBER,
    PHONE,
    EMAIL,
    URL,
    PASSWORD,
    MULTILINE,
    CODE,
    EMSCRIPT,
    VISIONTASKER
}

data class EditorContext(
    val semanticType: FieldSemanticType,
    val isSensitive: Boolean,
    val isMultiline: Boolean,
    val packageName: String?,
    val fieldName: String?
)

data class ResolvedKeyboardProfile(
    val profile: KeyboardProfile,
    val layout: KeyboardLayout
)
