package com.visualtasker.wss.emscript.parser

data class EmscriptGroupFacet(
    val id: String,
    val label: String,
    val kind: String,
    val startLine: Int,
    val endLine: Int?,
)

object EmscriptEditorFacetScanner {
    private val markerPattern = Regex("""^\s*(?://|REM)\s+@vt\.group\.(start|end)\b(.*)$""", RegexOption.IGNORE_CASE)
    private val attributePattern = Regex("""([A-Za-z][A-Za-z0-9_-]*)="([^"]*)"""")

    fun scan(script: String): List<EmscriptGroupFacet> {
        val open = linkedMapOf<String, PendingGroupFacet>()
        val complete = mutableListOf<EmscriptGroupFacet>()
        script.lineSequence().forEachIndexed { index, line ->
            val lineNumber = index + 1
            val match = markerPattern.find(line) ?: return@forEachIndexed
            val command = match.groupValues[1].lowercase()
            val attributes = parseAttributes(match.groupValues[2])
            val id = attributes["id"]?.takeIf { it.isNotBlank() } ?: return@forEachIndexed
            when (command) {
                "start" -> {
                    open[id] = PendingGroupFacet(
                        id = id,
                        label = attributes["label"]?.takeIf { it.isNotBlank() } ?: id,
                        kind = attributes["kind"]?.takeIf { it.isNotBlank() } ?: "region",
                        startLine = lineNumber,
                    )
                }
                "end" -> {
                    val pending = open.remove(id) ?: return@forEachIndexed
                    complete += EmscriptGroupFacet(
                        id = pending.id,
                        label = pending.label,
                        kind = pending.kind,
                        startLine = pending.startLine,
                        endLine = lineNumber,
                    )
                }
            }
        }
        complete += open.values.map {
            EmscriptGroupFacet(
                id = it.id,
                label = it.label,
                kind = it.kind,
                startLine = it.startLine,
                endLine = null,
            )
        }
        return complete
    }

    private fun parseAttributes(source: String): Map<String, String> =
        attributePattern.findAll(source).associate { it.groupValues[1] to it.groupValues[2] }
}

private data class PendingGroupFacet(
    val id: String,
    val label: String,
    val kind: String,
    val startLine: Int,
)
