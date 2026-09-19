package com.visualtasker.ime.ime.snippets

import com.visualtasker.ime.ime.context.KeyboardProfile

data class SnippetDefinition(
    val id: String,
    val label: String,
    val category: String,
    val template: String,
    val placeholders: List<SnippetPlaceholder>,
    val supportedProfiles: Set<KeyboardProfile>
)

data class SnippetPlaceholder(
    val key: String,
    val defaultValue: String = "",
    val selectAfterInsert: Boolean = false
)

data class SnippetRenderResult(
    val text: String,
    val selectionStart: Int?,
    val selectionEnd: Int?
)

object SnippetEngine {
    fun snippetsFor(profile: KeyboardProfile): List<SnippetDefinition> {
        return defaultSnippets().filter { it.supportedProfiles.contains(profile) }
    }

    fun render(snippet: SnippetDefinition, values: Map<String, String>): SnippetRenderResult {
        var rendered = snippet.template
        snippet.placeholders.forEach { placeholder ->
            val token = "\${${placeholder.key}}"
            rendered = rendered.replace(token, values[placeholder.key] ?: placeholder.defaultValue)
        }
        val selectionPlaceholder = snippet.placeholders.firstOrNull { it.selectAfterInsert }
        if (selectionPlaceholder == null) {
            return SnippetRenderResult(rendered, null, null)
        }
        val selectedValue = values[selectionPlaceholder.key] ?: selectionPlaceholder.defaultValue
        val tokenText = selectedValue
        val start = rendered.indexOf(tokenText)
        return if (start >= 0) {
            SnippetRenderResult(rendered, start, start + tokenText.length)
        } else {
            SnippetRenderResult(rendered, null, null)
        }
    }

    private fun defaultSnippets(): List<SnippetDefinition> = listOf(
        SnippetDefinition(
            id = "em_if_then",
            label = "IF ... THEN ... END IF",
            category = "EMScript",
            template = "IF \${COND} THEN\n    \${BODY}\nEND IF",
            placeholders = listOf(
                SnippetPlaceholder("COND", "condition", selectAfterInsert = true),
                SnippetPlaceholder("BODY", "doSomething()")
            ),
            supportedProfiles = setOf(KeyboardProfile.EMSCRIPT, KeyboardProfile.VISIONTASKER)
        ),
        SnippetDefinition(
            id = "em_click_text",
            label = "clickText(\"...\")",
            category = "EMScript",
            template = "clickText(\"\${TEXT}\")",
            placeholders = listOf(SnippetPlaceholder("TEXT", "Login", selectAfterInsert = true)),
            supportedProfiles = setOf(KeyboardProfile.EMSCRIPT, KeyboardProfile.VISIONTASKER)
        ),
        SnippetDefinition(
            id = "dev_if",
            label = "if () { }",
            category = "Developer",
            template = "if (\${COND}) {\n    \${BODY}\n}",
            placeholders = listOf(
                SnippetPlaceholder("COND", "condition", selectAfterInsert = true),
                SnippetPlaceholder("BODY", "// TODO")
            ),
            supportedProfiles = setOf(KeyboardProfile.DEVELOPER, KeyboardProfile.NORMAL)
        )
    )
}
