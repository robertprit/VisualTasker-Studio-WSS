package com.visualtasker.wss.emscript.editor

object EmsScriptFormatter {
    private const val INDENT_UNIT = "    "

    private val leadingPhrases = listOf(
        "END IF", "END FOR", "END LOOP", "END WHILE", "END UNTIL", "END FUNC",
        "ELSE IF", "ELSEIF", "LOOP WHILE", "WAIT LOAD", "WAIT ELEMENT", "END",
        "IF", "FOR", "LOOP", "WHILE", "UNTIL", "FUNC", "ELSE", "CATCH",
        "ENDIF", "ENDFOR", "ENDLOOP", "ENDWHILE", "ENDUNTIL",
    ).sortedByDescending { it.length }

    fun format(source: String): String {
        if (source.isEmpty()) return source
        val hadTrailingNewline = source.endsWith('\n')
        val lines = source.replace("\r\n", "\n").split('\n')
        var level = 0
        val formatted = mutableListOf<String>()

        for (rawLine in lines) {
            val (codePart, commentSuffix) = splitCodeAndComment(rawLine)
            if (codePart.isBlank()) {
                formatted += commentSuffix?.let { rawLine.trimEnd().ifBlank { it } } ?: ""
                continue
            }

            val normalized = normalizeCodeLine(codePart)
            val delta = indentDeltaForLine(normalized)
            level = (level - delta.dedentBefore).coerceAtLeast(0)
            val indent = INDENT_UNIT.repeat(level)
            val lineBody = buildString {
                append(indent)
                append(normalized)
                commentSuffix?.let { append(' ').append(it.trimStart()) }
            }
            formatted += lineBody.trimEnd()
            level += delta.indentAfter
        }

        val result = formatted.joinToString("\n")
        return if (hadTrailingNewline && result.isNotEmpty()) "$result\n" else result
    }

    private data class IndentDelta(val dedentBefore: Int = 0, val indentAfter: Int = 0)

    private fun indentDeltaForLine(normalizedUpper: String): IndentDelta {
        val first = normalizedUpper.trim()
        return when {
            first.startsWith("END IF") || first == "ENDIF" ||
                first.startsWith("END FOR") || first == "ENDFOR" ||
                first.startsWith("END LOOP") || first == "ENDLOOP" ||
                first.startsWith("END WHILE") || first == "ENDWHILE" ||
                first.startsWith("END UNTIL") || first == "ENDUNTIL" ||
                first.startsWith("END FUNC") -> IndentDelta(dedentBefore = 1)
            first.startsWith("ELSE") -> IndentDelta(dedentBefore = 1, indentAfter = 1)
            first.startsWith("IF ") || first == "IF" ||
                first.startsWith("FOR ") || first.startsWith("LOOP ") || first == "LOOP" ||
                first.startsWith("WHILE ") || first.startsWith("UNTIL ") ||
                first.startsWith("FUNC ") || first == "CATCH" -> IndentDelta(indentAfter = 1)
            else -> IndentDelta()
        }
    }

    private fun normalizeCodeLine(code: String): String {
        val trimmed = code.trim()
        val (phrase, remainder) = extractLeadingPhrase(trimmed)
        val normalizedPhrase = when (phrase.uppercase()) {
            "ENDIF" -> "END IF"
            "ENDFOR" -> "END FOR"
            "ENDLOOP" -> "END LOOP"
            "ENDWHILE" -> "END WHILE"
            "ENDUNTIL" -> "END UNTIL"
            "ELSEIF" -> "ELSE IF"
            else -> phrase.uppercase()
        }
        val normalizedRemainder = collapseSpacesPreservingStrings(remainder)
        return if (normalizedRemainder.isEmpty()) normalizedPhrase else "$normalizedPhrase $normalizedRemainder"
    }

    private fun extractLeadingPhrase(line: String): Pair<String, String> {
        val upper = line.uppercase()
        for (phrase in leadingPhrases) {
            if (upper == phrase) return phrase to ""
            if (upper.startsWith("$phrase ")) return phrase to line.substring(phrase.length).trim()
        }
        return "" to line
    }

    private fun splitCodeAndComment(line: String): Pair<String, String?> {
        var inString = false
        var i = 0
        while (i < line.length - 1) {
            when (line[i]) {
                '"' -> inString = !inString
                '/' -> if (!inString && line[i + 1] == '/') {
                    val code = line.substring(0, i).trimEnd()
                    val comment = line.substring(i).trim()
                    return code to comment.ifBlank { null }
                }
            }
            i++
        }
        return line.trimEnd() to null
    }

    private fun collapseSpacesPreservingStrings(text: String): String {
        if (text.isBlank()) return ""
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inString = false
        text.forEach { ch ->
            when {
                ch == '"' -> {
                    if (current.isNotBlank() && !inString) {
                        parts += current.toString().trim()
                        current.clear()
                    }
                    inString = !inString
                    current.append(ch)
                }
                inString -> current.append(ch)
                ch.isWhitespace() -> {
                    if (current.isNotBlank()) {
                        parts += current.toString().trim()
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) parts += current.toString().trim()
        return parts.joinToString(" ")
    }
}
