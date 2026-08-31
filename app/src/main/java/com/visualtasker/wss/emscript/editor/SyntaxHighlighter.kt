package com.visualtasker.wss.emscript.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {
    data class Palette(
        val keyword: Color,
        val control: Color,
        val parameter: Color,
        val string: Color,
        val number: Color,
        val comment: Color,
        val operator: Color,
        val plain: Color
    )

    data class IndentGuideSettings(
        val enabled: Boolean,
        val tabSize: Int,
        val indentUnit: Int,
        val guideColor: Color,
    )

    fun defaultDarkPalette(): Palette = Palette(
        keyword = Color(0xFF82B1FF),
        control = Color(0xFFCE93D8),
        parameter = Color(0xFFFFB74D),
        string = Color(0xFF81C784),
        number = Color(0xFF81C784),
        comment = Color(0xFF9E9E9E),
        operator = Color(0xFFFFB74D),
        plain = Color(0xFFE0E0E0)
    )

    fun defaultLightPalette(): Palette = Palette(
        keyword = Color(0xFF1565C0),
        control = Color(0xFF7B1FA2),
        parameter = Color(0xFFE65100),
        string = Color(0xFF2E7D32),
        number = Color(0xFF2E7D32),
        comment = Color(0xFF757575),
        operator = Color(0xFFE65100),
        plain = Color(0xFF212121)
    )

    private val commandKeywords = setOf(
        "OPEN", "OPENCT", "WAIT", "LOAD", "ELEMENT", "CLICK", "TYPE", "SCREENSHOT",
        "RECORD", "START", "STOP", "TOGGLE", "BEEP", "BACK", "HOME", "START_SCRIPT",
        "AI_REQUEST", "WAIT_AI_RESPONSE", "TASKER_EVENT", "WAIT_TASKER", "BPARAM", "SHARE",
        "DOWNLOAD", "FAVORITE", "ACTION", "CLOSE", "BROWSER", "COLOR", "LET", "SET", "CALL",
        "INTEROP", "GOTO", "OUTPUT", "LAUNCH", "OCR", "SWIPE", "INPUT", "SCAN", "CROP", "FIND",
        "REGION", "POINT"
    )

    private val controlKeywords = setOf(
        "IF", "ELSE", "ELSEIF", "END", "TO", "STEP", "FUNC", "TRY", "CATCH",
        "REPEAT", "WHILE", "UNTIL", "BREAK", "CONTINUE", "FOR", "LOOP", "THEN",
        "ENDIF", "ENDFOR", "ENDLOOP", "ENDWHILE", "ENDUNTIL"
    )

    private val operatorTokens = setOf("=", ".", "&&", "||", "!", "==", "!=", "<=", ">=", "+", "-", "*", "/", "<", ">")
    private val numberRegex = Regex("^-?\\d+(?:\\.\\d+)?(?:ms|s)?$", RegexOption.IGNORE_CASE)
    private val identifierRegex = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

    fun visualTransformation(palette: Palette): VisualTransformation = VisualTransformation { text ->
        TransformedText(highlight(text.text, palette), OffsetMapping.Identity)
    }

    fun visualTransformation(
        palette: Palette,
        indentGuides: IndentGuideSettings,
    ): VisualTransformation = VisualTransformation { text ->
        if (!indentGuides.enabled) {
            TransformedText(highlight(text.text, palette), OffsetMapping.Identity)
        } else {
            val transformed = applyIndentGuides(
                script = text.text,
                tabSize = indentGuides.tabSize,
                indentUnit = indentGuides.indentUnit,
            )
            val highlighted = highlight(transformed.text, palette)
            val guideStyle = SpanStyle(color = indentGuides.guideColor)
            val spans = highlighted.spanStyles + transformed.guideRanges.map { range ->
                Range(guideStyle, range.first, range.last + 1)
            }
            TransformedText(
                text = AnnotatedString(
                    text = highlighted.text,
                    spanStyles = spans,
                    paragraphStyles = highlighted.paragraphStyles,
                ),
                offsetMapping = transformed.offsetMapping,
            )
        }
    }

    fun highlight(script: String, palette: Palette): AnnotatedString = buildAnnotatedString {
        val lines = script.lines()
        lines.forEachIndexed { index, line ->
            appendHighlightedLine(line, palette)
            if (index != lines.lastIndex) append("\n")
        }
    }

    private fun AnnotatedString.Builder.appendHighlightedLine(line: String, palette: Palette) {
        val commentStart = line.indexOf("//").takeIf { it >= 0 }
        val codePart = if (commentStart != null) line.substring(0, commentStart) else line
        appendHighlightedCode(codePart, palette)
        if (commentStart != null) {
            withStyle(SpanStyle(color = palette.comment)) {
                append(line.substring(commentStart))
            }
        }
    }

    private fun AnnotatedString.Builder.appendHighlightedCode(code: String, palette: Palette) {
        val tokenRegex = Regex("\"(?:[^\"\\\\]|\\\\.)*\"|==|!=|<=|>=|&&|\\|\\||[A-Za-z_][A-Za-z0-9_]*|-?\\d+(?:\\.\\d+)?|[()=,.+\\-*/<>]|\\S")
        var cursor = 0
        tokenRegex.findAll(code).forEach { match ->
            if (match.range.first > cursor) {
                withStyle(SpanStyle(color = palette.plain)) {
                    append(code.substring(cursor, match.range.first))
                }
            }
            val token = match.value
            val styleColor = colorForToken(token, palette)
            withStyle(SpanStyle(color = styleColor)) {
                append(token)
            }
            cursor = match.range.last + 1
        }
        if (cursor < code.length) {
            withStyle(SpanStyle(color = palette.plain)) {
                append(code.substring(cursor))
            }
        }
    }

    private fun colorForToken(token: String, palette: Palette): Color {
        val upper = token.uppercase()
        return when {
            token.startsWith("\"") && token.endsWith("\"") -> palette.string
            upper in controlKeywords -> palette.control
            upper in commandKeywords -> palette.keyword
            upper in setOf("TRUE", "FALSE", "INFINITE") -> palette.number
            operatorTokens.contains(token) -> palette.operator
            numberRegex.matches(token) -> palette.number
            token.startsWith("#") || token.startsWith(".") || token.startsWith("$") -> palette.parameter
            identifierRegex.matches(token) && token.lowercase() != token.uppercase() -> palette.parameter
            else -> palette.plain
        }
    }
}

data class IndentGuideTransformResult(
    val text: String,
    val guideRanges: List<IntRange>,
    val offsetMapping: OffsetMapping,
)

fun applyIndentGuides(
    script: String,
    tabSize: Int = 4,
    indentUnit: Int = 4,
): IndentGuideTransformResult {
    val safeTabSize = tabSize.coerceIn(1, 16)
    val safeIndentUnit = indentUnit.coerceIn(1, 16)
    val out = StringBuilder(script.length)
    val guideRanges = mutableListOf<IntRange>()
    val originalToTransformed = IntArray(script.length + 1)
    val transformedToOriginal = mutableListOf<Int>()
    var originalIndex = 0
    var previousIndentColumns = 0

    fun appendMapped(char: Char, sourceOffset: Int, isGuide: Boolean = false) {
        val start = out.length
        out.append(char)
        transformedToOriginal += sourceOffset.coerceIn(0, script.length)
        if (isGuide) guideRanges += start..start
    }

    fun appendVirtualGuideLine(columns: Int, sourceOffset: Int) {
        val levels = columns / safeIndentUnit
        repeat(levels) { level ->
            appendMapped('│', sourceOffset, isGuide = true)
            val spaces = if (level == levels - 1) 0 else safeIndentUnit - 1
            repeat(spaces.coerceAtLeast(0)) {
                appendMapped(' ', sourceOffset)
            }
        }
    }

    script.split('\n').forEachIndexed { lineIndex, line ->
        originalToTransformed[originalIndex] = out.length
        val lineStartOffset = originalIndex
        val leadingLength = line.indexOfFirst { it != ' ' && it != '\t' }
            .let { if (it == -1) line.length else it }
        val isBlank = leadingLength == line.length
        var visualColumn = 0

        if (isBlank && line.isEmpty()) {
            appendVirtualGuideLine(previousIndentColumns, lineStartOffset)
        } else {
            line.forEachIndexed { charIndex, char ->
                val sourceOffset = lineStartOffset + charIndex
                originalToTransformed[sourceOffset] = out.length
                if (charIndex < leadingLength) {
                    val startsIndentLevel = visualColumn % safeIndentUnit == 0
                    when (char) {
                        '\t' -> {
                            appendMapped(if (startsIndentLevel) '│' else '\t', sourceOffset, startsIndentLevel)
                            visualColumn += safeTabSize
                        }
                        else -> {
                            appendMapped(if (startsIndentLevel) '│' else char, sourceOffset, startsIndentLevel)
                            visualColumn += 1
                        }
                    }
                } else {
                    appendMapped(char, sourceOffset)
                }
            }
            if (!isBlank) previousIndentColumns = visualColumn
        }
        originalIndex += line.length
        originalToTransformed[originalIndex] = out.length
        if (lineIndex != script.count { it == '\n' }) {
            appendMapped('\n', originalIndex)
            originalIndex += 1
        }
    }
    originalToTransformed[script.length] = out.length
    transformedToOriginal += script.length

    val transformedToOriginalArray = transformedToOriginal.toIntArray()
    return IndentGuideTransformResult(
        text = out.toString(),
        guideRanges = guideRanges,
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, originalToTransformed.lastIndex)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginalArray[offset.coerceIn(0, transformedToOriginalArray.lastIndex)]
        },
    )
}
