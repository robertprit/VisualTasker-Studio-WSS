package com.visualtasker.wss.emscript.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CollapsibleLineRange(
    val key: String,
    val startLine: Int,
    val endLine: Int,
)

private data class DisplayLineInfo(
    val originalLine: Int,
    val isPlaceholder: Boolean = false,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EmScriptEditorScreen(
    session: EmscriptEditorSession,
    projectionStatus: String,
    overallStatus: String,
    revision: Int,
    uiState: EmscriptEditorUiState,
    onSessionChange: (EmscriptEditorSession) -> Unit,
    onSaveDraft: () -> Unit,
    onUseProjection: () -> Unit,
    onCompileCheck: () -> Unit = {},
    onDryRun: () -> Unit = {},
    canDryRun: Boolean = session.activeTab.content.isNotBlank(),
    onLiveRun: () -> Unit = {},
    canLiveRun: Boolean = false,
    canApplyDraft: Boolean,
    onRequestApplyPreview: () -> String?,
    onConfirmApply: () -> Unit,
    diagnostics: List<String>,
    syntaxPaletteOverride: SyntaxHighlighter.Palette? = null,
    modifier: Modifier = Modifier,
) {
    val activeTab = session.activeTab
    var fontSizeSp by remember(uiState.fontSizeSp) { mutableStateOf(uiState.fontSizeSp.coerceIn(9f, 24f)) }
    var showFindReplace by remember { mutableStateOf(false) }
    var showApplyPreview by remember { mutableStateOf(false) }
    var applyPreviewText by remember { mutableStateOf("") }
    var findQuery by remember { mutableStateOf("") }
    var replaceValue by remember { mutableStateOf("") }
    var editorValue by remember(activeTab.id) {
        val start = uiState.selectionStarts[activeTab.id]?.coerceIn(0, activeTab.content.length) ?: activeTab.content.length
        val end = uiState.selectionEnds[activeTab.id]?.coerceIn(start, activeTab.content.length) ?: start
        mutableStateOf(TextFieldValue(activeTab.content, TextRange(start, end)))
    }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val undoStack = remember(activeTab.id) { mutableStateListOf<String>() }
    val redoStack = remember(activeTab.id) { mutableStateListOf<String>() }
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()
    val syntaxPalette = remember(isDark, syntaxPaletteOverride) {
        syntaxPaletteOverride ?: if (isDark) {
            SyntaxHighlighter.defaultDarkPalette()
        } else {
            SyntaxHighlighter.defaultLightPalette()
        }
    }

    LaunchedEffect(activeTab.content) {
        if (activeTab.content != editorValue.text) {
            editorValue = editorValue.copy(
                text = activeTab.content,
                selection = TextRange(activeTab.content.length.coerceAtMost(activeTab.content.length))
            )
        }
    }

    val allLines = remember(editorValue.text) { editorValue.text.lines() }
    var collapsedBlocks by remember(activeTab.id) {
        mutableStateOf(uiState.foldedKeysByTab[activeTab.id].orEmpty())
    }
    val collapsibleRanges = remember(allLines) { findCollapsibleLineRanges(allLines) }
    val displayText = remember(editorValue.text, collapsedBlocks, collapsibleRanges) {
        buildDisplayText(allLines, collapsibleRanges, collapsedBlocks)
    }
    val lineMapping = remember(editorValue.text, collapsedBlocks, collapsibleRanges) {
        buildLineMapping(allLines, collapsibleRanges, collapsedBlocks)
    }
    val codeLineHeight = (fontSizeSp * 1.5f).sp
    val editorScrollState = rememberScrollState()
    val editorHorizontalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gutterTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(fontSizeSp) {
        uiState.fontSizeSp = fontSizeSp
    }
    LaunchedEffect(activeTab.id, editorValue.selection.start, editorValue.selection.end) {
        uiState.selectionStarts[activeTab.id] = editorValue.selection.start
        uiState.selectionEnds[activeTab.id] = editorValue.selection.end
    }
    LaunchedEffect(activeTab.id, collapsedBlocks) {
        uiState.foldedKeysByTab[activeTab.id] = collapsedBlocks
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditorToolbarIconButton(Icons.AutoMirrored.Filled.NoteAdd, "Neu", {
                        if (!activeTab.readOnly) {
                            undoStack.add(activeTab.content)
                            onSessionChange(session.updateManualContent(""))
                        }
                    }, enabled = !activeTab.readOnly)
                    EditorToolbarIconButton(Icons.Default.Upload, "Projektion übernehmen", onUseProjection)
                    EditorToolbarDivider()
                    EditorToolbarIconButton(Icons.AutoMirrored.Filled.Undo, "Undo", {
                        if (undoStack.isNotEmpty() && !activeTab.readOnly) {
                            val prev = undoStack.removeLast()
                            redoStack.add(activeTab.content)
                            onSessionChange(session.updateManualContent(prev))
                        }
                    }, enabled = !activeTab.readOnly && undoStack.isNotEmpty())
                    EditorToolbarIconButton(Icons.AutoMirrored.Filled.Redo, "Redo", {
                        if (redoStack.isNotEmpty() && !activeTab.readOnly) {
                            val next = redoStack.removeLast()
                            undoStack.add(activeTab.content)
                            onSessionChange(session.updateManualContent(next))
                        }
                    }, enabled = !activeTab.readOnly && redoStack.isNotEmpty())
                    EditorToolbarDivider()
                    EditorToolbarIconButton(Icons.Default.ContentCut, "Ausschneiden", {
                        if (activeTab.readOnly) return@EditorToolbarIconButton
                        val sel = editorValue.selection
                        if (sel.length > 0) {
                            clipboardManager.setText(AnnotatedString(editorValue.text.substring(sel.min, sel.max)))
                            val newText = editorValue.text.removeRange(sel.min, sel.max)
                            onSessionChange(session.updateManualContent(newText))
                        }
                    }, enabled = !activeTab.readOnly)
                    EditorToolbarIconButton(Icons.Default.ContentCopy, "Kopieren", {
                        val sel = editorValue.selection
                        if (sel.length > 0) {
                            clipboardManager.setText(AnnotatedString(editorValue.text.substring(sel.min, sel.max)))
                        }
                    })
                    EditorToolbarIconButton(Icons.Default.ContentPaste, "Einfügen", {
                        if (activeTab.readOnly) return@EditorToolbarIconButton
                        val clip = clipboardManager.getText()?.text ?: return@EditorToolbarIconButton
                        val sel = editorValue.selection
                        val newText = editorValue.text.replaceRange(sel.min, sel.max, clip)
                        onSessionChange(session.updateManualContent(newText))
                    }, enabled = !activeTab.readOnly)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditorToolbarIconButton(Icons.Default.Build, "Compile Check", onCompileCheck, enabled = activeTab.content.isNotBlank())
                    EditorToolbarIconButton(Icons.Default.PlayArrow, "Run Dry", onDryRun, enabled = canDryRun)
                    EditorToolbarIconButton(Icons.Default.PlayCircle, "Run Live", onLiveRun, enabled = canLiveRun)
                    EditorToolbarIconButton(Icons.Default.Pause, "Pause (NOT_IMPLEMENTED)", {}, enabled = false)
                    EditorToolbarIconButton(Icons.Default.Stop, "Stop (NOT_IMPLEMENTED)", {}, enabled = false)
                    EditorToolbarIconButton(Icons.Default.Done, "Apply", {
                        val preview = onRequestApplyPreview()
                        if (preview != null) {
                            applyPreviewText = preview
                            showApplyPreview = true
                        }
                    }, enabled = canApplyDraft)
                    EditorToolbarIconButton(Icons.Default.TextDecrease, "Text kleiner", {
                        fontSizeSp = (fontSizeSp - 1f).coerceAtLeast(9f)
                    })
                    EditorToolbarIconButton(Icons.Default.TextIncrease, "Text größer", {
                        fontSizeSp = (fontSizeSp + 1f).coerceAtMost(24f)
                    })
                    EditorToolbarIconButton(
                        Icons.Default.Search,
                        "Suchen/Ersetzen",
                        { showFindReplace = true },
                    )
                }
            }
        }

        TabRow(selectedTabIndex = session.tabs.indexOfFirst { it.id == session.activeTabId }.coerceAtLeast(0)) {
            session.tabs.forEach { tab ->
                Tab(
                    selected = tab.id == session.activeTabId,
                    onClick = { onSessionChange(session.selectTab(tab.id)) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = buildString {
                                    append(tab.title)
                                    if (tab.dirty) append(" *")
                                },
                            )
                            if (tab.id != EmscriptEditorSession.MANUAL_TAB_ID) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text("Tab schließen") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(
                                        onClick = { onSessionChange(session.closeTab(tab.id)) },
                                        modifier = Modifier.width(20.dp).height(20.dp),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Tab schließen")
                                    }
                                }
                            }
                        }
                    },
                    icon = {
                        if (tab.executionLocked) {
                            Icon(Icons.Default.Stop, contentDescription = "Read-Only")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Ausführbar")
                        }
                    },
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(editorScrollState)
                    .padding(vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .drawBehind {
                            val layout = textLayoutResult ?: return@drawBehind
                            lineMapping.forEachIndexed { displayIdx, info ->
                                if (displayIdx >= layout.lineCount) return@forEachIndexed
                                val lineTop = layout.getLineTop(displayIdx)
                                val lineBottom = layout.getLineBottom(displayIdx)
                                val lineNo = textMeasurer.measure(
                                    text = info.originalLine.toString(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSizeSp.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = gutterTextColor,
                                    )
                                )
                                drawText(
                                    textLayoutResult = lineNo,
                                    topLeft = Offset(
                                        10f * density.density,
                                        lineTop + (lineBottom - lineTop - lineNo.size.height) / 2f,
                                    )
                                )
                                val range = collapsibleRanges.firstOrNull { it.startLine == info.originalLine }
                                if (range != null) {
                                    val marker = if (range.key in collapsedBlocks) "▶" else "▼"
                                    val markerText = textMeasurer.measure(
                                        text = marker,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = gutterTextColor,
                                        )
                                    )
                                    drawText(
                                        textLayoutResult = markerText,
                                        topLeft = Offset(
                                            42f * density.density,
                                            lineTop + (lineBottom - lineTop - markerText.size.height) / 2f,
                                        )
                                    )
                                }
                            }
                        }
                        .pointerInput(lineMapping, collapsibleRanges, collapsedBlocks) {
                            detectTapGestures { offset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                val toggleAreaStartX = 28f * density.density
                                val toggleAreaEndX = 64f * density.density
                                if (offset.x < toggleAreaStartX || offset.x > toggleAreaEndX) return@detectTapGestures
                                lineMapping.forEachIndexed { displayIdx, info ->
                                    if (displayIdx >= layout.lineCount) return@forEachIndexed
                                    val lineTop = layout.getLineTop(displayIdx)
                                    val lineBottom = layout.getLineBottom(displayIdx)
                                    if (offset.y in lineTop..lineBottom) {
                                        val range = collapsibleRanges.firstOrNull { it.startLine == info.originalLine } ?: return@forEachIndexed
                                        collapsedBlocks = if (range.key in collapsedBlocks) collapsedBlocks - range.key else collapsedBlocks + range.key
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .height((lineMapping.size.coerceAtLeast(1) * (fontSizeSp * 1.7f)).dp)
                            .width(64.dp)
                    )
                }

                BasicTextField(
                    value = editorValue.copy(text = displayText),
                    onValueChange = { value ->
                        if (activeTab.readOnly) return@BasicTextField
                        val newText = value.text
                        val oldText = editorValue.text
                        val newlineAdded = newText.length == oldText.length + 1 &&
                            newText.count { it == '\n' } == oldText.count { it == '\n' } + 1 &&
                            value.selection.start == value.selection.end
                        val finalText = if (newlineAdded) {
                            autoIndentAfterNewline(newText, value.selection.start)
                        } else {
                            newText
                        }
                        undoStack.add(oldText)
                        if (undoStack.size > 100) undoStack.removeAt(0)
                        redoStack.clear()
                        editorValue = TextFieldValue(finalText, TextRange(value.selection.end.coerceAtMost(finalText.length)))
                        onSessionChange(session.updateManualContent(finalText))
                    },
                    readOnly = activeTab.readOnly,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(editorHorizontalScrollState)
                        .padding(horizontal = 6.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSizeSp.sp,
                        lineHeight = codeLineHeight,
                        color = syntaxPalette.plain,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = SyntaxHighlighter.visualTransformation(
                        palette = syntaxPalette,
                        indentGuides = SyntaxHighlighter.IndentGuideSettings(
                            enabled = true,
                            tabSize = 4,
                            indentUnit = 4,
                            guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    ),
                    onTextLayout = { result -> textLayoutResult = result },
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Proj: $projectionStatus | Status: $overallStatus | Rev: $revision",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (activeTab.readOnly) {
                        "GENERATED PROJECTION (READ-ONLY) | Runtime: DRY_RUN_ONLY"
                    } else {
                        "LOCAL DRAFT - NOT APPLIED TO WORKSPACE | Runtime: DRY_RUN_ONLY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.secondary,
                )
                diagnostics.take(3).forEach { message ->
                    Text(
                        text = "Diag: $message",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showApplyPreview) {
        AlertDialog(
            onDismissRequest = { showApplyPreview = false },
            title = { Text("Apply Vorschau") },
            text = {
                Text(
                    text = applyPreviewText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmApply()
                        showApplyPreview = false
                    },
                ) {
                    Text("Anwenden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyPreview = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    if (showFindReplace) {
        AlertDialog(
            onDismissRequest = { showFindReplace = false },
            title = { Text("Suchen & Ersetzen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        label = { Text("Suchen") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = replaceValue,
                        onValueChange = { replaceValue = it },
                        label = { Text("Ersetzen durch") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val next = findNext(editorValue.text, findQuery, editorValue.selection.end)
                            if (next != null) {
                                editorValue = editorValue.copy(selection = TextRange(next.first, next.second))
                            }
                        },
                        enabled = findQuery.isNotBlank(),
                    ) { Text("Weiter") }
                    TextButton(
                        onClick = {
                            if (activeTab.readOnly || findQuery.isBlank()) return@TextButton
                            val replaced = replaceSelectionOrNext(editorValue.text, editorValue.selection, findQuery, replaceValue)
                                ?: return@TextButton
                            undoStack.add(editorValue.text)
                            if (undoStack.size > 100) undoStack.removeAt(0)
                            redoStack.clear()
                            editorValue = TextFieldValue(replaced.text, TextRange(replaced.cursor))
                            onSessionChange(session.updateManualContent(replaced.text))
                        },
                        enabled = !activeTab.readOnly && findQuery.isNotBlank(),
                    ) { Text("Ersetzen") }
                    TextButton(
                        onClick = {
                            if (activeTab.readOnly || findQuery.isBlank()) return@TextButton
                            val newText = editorValue.text.replace(findQuery, replaceValue)
                            if (newText != editorValue.text) {
                                undoStack.add(editorValue.text)
                                if (undoStack.size > 100) undoStack.removeAt(0)
                                redoStack.clear()
                                editorValue = TextFieldValue(newText, TextRange(newText.length))
                                onSessionChange(session.updateManualContent(newText))
                            }
                        },
                        enabled = !activeTab.readOnly && findQuery.isNotBlank(),
                    ) { Text("Alle") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplace = false }) {
                    Text("Schließen")
                }
            },
        )
    }
}

private fun findCollapsibleLineRanges(lines: List<String>): List<CollapsibleLineRange> {
    val blockStartPattern = Regex("^\\s*(IF|FOR|LOOP|WHILE|LOOP\\s+WHILE|FUNC)\\b", RegexOption.IGNORE_CASE)
    val blockEndPattern = Regex("^\\s*(END\\s+IF|END\\s+FOR|END\\s+LOOP|END\\s+WHILE|END\\s+FUNC)\\b", RegexOption.IGNORE_CASE)
    val ranges = mutableListOf<CollapsibleLineRange>()
    val stack = ArrayDeque<Pair<Int, String>>()
    var counter = 0

    lines.forEachIndexed { index, line ->
        val lineNum = index + 1
        if (blockStartPattern.containsMatchIn(line)) {
            counter++
            stack.addLast(lineNum to "block_$counter")
        } else if (blockEndPattern.containsMatchIn(line)) {
            val match = stack.removeLastOrNull()
            if (match != null) {
                ranges += CollapsibleLineRange(match.second, match.first, lineNum)
            }
        }
    }
    return ranges
}

private fun buildLineMapping(
    lines: List<String>,
    ranges: List<CollapsibleLineRange>,
    collapsedKeys: Set<String>,
): List<DisplayLineInfo> {
    if (collapsedKeys.isEmpty()) return lines.indices.map { DisplayLineInfo(originalLine = it + 1) }
    val hiddenLines = mutableSetOf<Int>()
    ranges.filter { it.key in collapsedKeys }.forEach { range ->
        for (line in (range.startLine + 1)..range.endLine) hiddenLines += line
    }
    return lines.indices.mapNotNull { index ->
        val lineNum = index + 1
        if (lineNum in hiddenLines) null else DisplayLineInfo(originalLine = lineNum)
    }
}

private fun buildDisplayText(
    lines: List<String>,
    ranges: List<CollapsibleLineRange>,
    collapsedKeys: Set<String>,
): String {
    if (collapsedKeys.isEmpty()) return lines.joinToString("\n")
    val hiddenLines = mutableSetOf<Int>()
    val placeholderLines = mutableMapOf<Int, String>()
    ranges.filter { it.key in collapsedKeys }.forEach { range ->
        for (line in (range.startLine + 1)..range.endLine) hiddenLines += line
        val count = range.endLine - range.startLine
        placeholderLines[range.startLine] = "${lines[range.startLine - 1]}  // ... $count lines"
    }
    return lines.indices.mapNotNull { index ->
        val lineNum = index + 1
        when {
            lineNum in hiddenLines -> null
            lineNum in placeholderLines -> placeholderLines[lineNum]
            else -> lines[index]
        }
    }.joinToString("\n")
}

private fun autoIndentAfterNewline(text: String, cursorPos: Int): String {
    val safeCursor = cursorPos.coerceIn(0, text.length)
    val textBeforeCursor = text.substring(0, safeCursor)
    val prevNewline = textBeforeCursor.lastIndexOf('\n', safeCursor - 2)
    val prevLine = if (prevNewline >= 0) {
        text.substring(prevNewline + 1, safeCursor - 1)
    } else {
        text.substring(0, safeCursor - 1)
    }
    val indent = calculateAutoIndent(prevLine)
    return if (indent.isEmpty()) text else text.substring(0, safeCursor) + indent + text.substring(safeCursor)
}

private val indentTriggerPattern = Regex("^\\s*(IF|FOR|LOOP|WHILE|FUNC|ELSE)\\b", RegexOption.IGNORE_CASE)

private fun calculateAutoIndent(previousLine: String): String {
    val prevIndent = previousLine.takeWhile { it == ' ' }
    val indentUnit = "    "
    return if (indentTriggerPattern.containsMatchIn(previousLine)) prevIndent + indentUnit else prevIndent
}

private data class ReplaceResult(
    val text: String,
    val cursor: Int,
)

private fun findNext(text: String, query: String, startAt: Int): Pair<Int, Int>? {
    if (query.isEmpty()) return null
    val start = startAt.coerceIn(0, text.length)
    val forward = text.indexOf(query, startIndex = start)
    if (forward >= 0) return forward to (forward + query.length)
    val wrapped = text.indexOf(query, startIndex = 0)
    return if (wrapped >= 0) wrapped to (wrapped + query.length) else null
}

private fun replaceSelectionOrNext(
    text: String,
    selection: TextRange,
    query: String,
    replacement: String,
): ReplaceResult? {
    if (query.isEmpty()) return null
    if (selection.length > 0) {
        val selected = text.substring(selection.min, selection.max)
        if (selected == query) {
            val newText = text.replaceRange(selection.min, selection.max, replacement)
            return ReplaceResult(newText, selection.min + replacement.length)
        }
    }
    val next = findNext(text, query, selection.end) ?: return null
    val newText = text.replaceRange(next.first, next.second, replacement)
    return ReplaceResult(newText, next.first + replacement.length)
}
