package com.visualtasker.wss.workspace.model

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object RecordingEventStore {
    const val RECORDS_DIR = "emscript-runtime/records"

    private val activeSession = AtomicReference<RecordingSessionWriter?>(null)

    fun start(context: Context, source: String = "floatingOverlay"): File {
        val target = File(context.filesDir, "$RECORDS_DIR/overlay-${timestamp()}.jsonl")
        target.parentFile?.mkdirs()
        val writer = RecordingSessionWriter(
            file = target,
            source = source,
            startedAtMs = System.currentTimeMillis(),
        )
        activeSession.set(writer)
        writer.record("recording.started", "Aufnahme gestartet", mapOf("file" to target.name))
        return target
    }

    fun stop(): File? {
        val writer = activeSession.getAndSet(null) ?: return null
        writer.record(
            kind = "recording.stopped",
            label = "Aufnahme gestoppt",
            attributes = mapOf("durationMs" to (System.currentTimeMillis() - writer.startedAtMs).toString()),
        )
        return writer.file
    }

    fun isRecording(): Boolean = activeSession.get() != null

    fun activeFileName(): String? = activeSession.get()?.file?.name

    fun recordOverlayEvent(kind: String, label: String, attributes: Map<String, String> = emptyMap()) {
        activeSession.get()?.record(kind, label, attributes)
    }

    fun recordAccessibilityEvent(event: AccessibilityEvent) {
        val writer = activeSession.get() ?: return
        val kind = event.recordingKind() ?: return
        val bounds = event.source?.screenBoundsOrNull()
        val attributes = buildMap {
            put("eventType", event.eventType.toString())
            event.packageName?.toString()?.takeIf { it.isNotBlank() }?.let { put("package", it) }
            event.className?.toString()?.takeIf { it.isNotBlank() }?.let { put("activity", it) }
            event.text?.joinToString(separator = " ")?.takeIf { it.isNotBlank() }?.let { put("text", it) }
            event.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { put("description", it) }
            bounds?.let {
                put("bounds", "${it.left},${it.top},${it.right},${it.bottom}")
                put("x", it.centerX().toString())
                put("y", it.centerY().toString())
            }
        }
        writer.record(kind, event.recordingLabel(kind), attributes)
    }

    fun latestRecordingFile(context: Context): File? =
        File(context.filesDir, RECORDS_DIR)
            .listFiles { file -> file.isFile && file.extension == "jsonl" }
            ?.maxByOrNull { it.lastModified() }

    fun latestRecordingSteps(context: Context): List<RecorderStepUi> =
        latestRecordingFile(context)?.toRecorderSteps().orEmpty()

    fun recordingSessions(context: Context): List<RecordingSessionUi> =
        File(context.filesDir, RECORDS_DIR)
            .listFiles { file -> file.isFile && file.extension == "jsonl" }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val steps = file.toRecorderSteps()
                RecordingSessionUi(
                    path = file.absolutePath,
                    fileName = file.name,
                    label = file.nameWithoutExtension.removePrefix("overlay-").ifBlank { file.nameWithoutExtension },
                    lastModifiedMs = file.lastModified(),
                    stepCount = steps.size,
                    durationMs = steps.maxOfOrNull { (it.timestampMs ?: 0L) + (it.durationMs ?: 0L) } ?: 0L,
                )
            }

    fun recordingStepsFor(path: String?): List<RecorderStepUi> =
        path
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.toRecorderSteps()
            .orEmpty()

    fun File.toRecorderSteps(): List<RecorderStepUi> {
        if (!isFile) return emptyList()
        var currentActivity: String? = null
        return readLines()
            .mapNotNull(::parseRecordingEventLine)
            .filterNot { it.kind == "recording.started" || it.kind == "recording.stopped" }
            .map { event ->
                val eventActivity = event.attributes["activity"]
                    ?: event.attributes["package"]
                    ?: event.source
                if (event.kind == "activity.change") {
                    currentActivity = eventActivity
                }
                RecorderStepUi(
                    id = "record-${nameWithoutExtension}-${event.index}",
                    label = event.label,
                    actionType = event.kind,
                    status = StepStatus.Recorded,
                    timestampMs = event.elapsedMs,
                    durationMs = event.durationMs(),
                    activityName = if (event.kind == "activity.change") eventActivity else currentActivity ?: eventActivity,
                    detail = event.detail(),
                )
            }
    }

    private fun parseRecordingEventLine(line: String): RecordingEventLine? {
        val fields = parseFlatJsonObject(line)
        val index = fields["index"]?.toIntOrNull() ?: return null
        val elapsedMs = fields["elapsedMs"]?.toLongOrNull() ?: return null
        val kind = fields["kind"] ?: return null
        val source = fields["source"] ?: "recording"
        val label = fields["label"] ?: fields["message"] ?: kind
        val attributes = fields
            .filterKeys { it !in setOf("index", "timestampMs", "elapsedMs", "source", "kind", "label", "message") }
        return RecordingEventLine(
            index = index,
            elapsedMs = elapsedMs,
            kind = kind,
            source = source,
            label = label,
            attributes = attributes,
        )
    }

    private fun parseFlatJsonObject(raw: String): Map<String, String> {
        val text = raw.trim()
        if (!text.startsWith("{") || !text.endsWith("}")) return emptyMap()
        val result = linkedMapOf<String, String>()
        var index = 1
        while (index < text.lastIndex) {
            while (index < text.lastIndex && (text[index].isWhitespace() || text[index] == ',')) index++
            if (index >= text.lastIndex || text[index] != '"') break
            val keyEnd = findJsonStringEnd(text, index + 1)
            if (keyEnd < 0) break
            val key = unescapeJsonString(text.substring(index + 1, keyEnd))
            index = keyEnd + 1
            while (index < text.lastIndex && (text[index].isWhitespace() || text[index] == ':')) index++
            val value = if (index < text.lastIndex && text[index] == '"') {
                val valueEnd = findJsonStringEnd(text, index + 1)
                if (valueEnd < 0) break
                val parsed = unescapeJsonString(text.substring(index + 1, valueEnd))
                index = valueEnd + 1
                parsed
            } else {
                val valueStart = index
                while (index < text.lastIndex && text[index] != ',') index++
                text.substring(valueStart, index).trim()
            }
            result[key] = value
        }
        return result
    }

    private fun findJsonStringEnd(text: String, start: Int): Int {
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                return index
            }
        }
        return -1
    }

    private fun unescapeJsonString(value: String): String =
        buildString(value.length) {
            var escaped = false
            value.forEach { char ->
                if (escaped) {
                    append(
                        when (char) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            '\\' -> '\\'
                            '"' -> '"'
                            else -> char
                        }
                    )
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else {
                    append(char)
                }
            }
            if (escaped) append('\\')
        }

    private fun AccessibilityEvent.recordingKind(): String? =
        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "activity.change"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "click"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "longClick"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "text.change"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "scroll"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "focus"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> "gesture.start"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> "gesture.end"
            else -> null
        }

    private fun AccessibilityEvent.recordingLabel(kind: String): String =
        when (kind) {
            "activity.change" -> "Activity: ${className?.toString().orEmpty().ifBlank { packageName?.toString().orEmpty() }}"
            "click" -> "Click ${textLabel()}"
            "longClick" -> "Long Click ${textLabel()}"
            "text.change" -> "Text geaendert ${textLabel()}"
            "scroll" -> "Scroll ${className?.toString().orEmpty()}"
            "focus" -> "Focus ${textLabel()}"
            "gesture.start" -> "Geste gestartet"
            "gesture.end" -> "Geste beendet"
            else -> kind
        }.trim()

    private fun AccessibilityEvent.textLabel(): String =
        text?.joinToString(separator = " ")
            ?.takeIf { it.isNotBlank() }
            ?: contentDescription?.toString()?.takeIf { it.isNotBlank() }
            ?: className?.toString().orEmpty()

    private fun AccessibilityNodeInfo.screenBoundsOrNull(): Rect? =
        Rect().also(::getBoundsInScreen).takeUnless { it.isEmpty }

    private fun RecordingEventLine.durationMs(): Long? =
        attributes["durationMs"]?.toLongOrNull()

    private fun RecordingEventLine.detail(): String =
        attributes.entries.joinToString(separator = " | ") { (key, value) -> "$key=$value" }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())

    private data class RecordingSessionWriter(
        val file: File,
        val source: String,
        val startedAtMs: Long,
        var index: Int = 0,
    ) {
        fun record(kind: String, label: String, attributes: Map<String, String>) {
            val now = System.currentTimeMillis()
            val values = linkedMapOf(
                "index" to index++.toString(),
                "timestampMs" to now.toString(),
                "elapsedMs" to (now - startedAtMs).toString(),
                "source" to source,
                "kind" to kind,
                "label" to label,
            )
            values.putAll(attributes)
            val line = values.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "\"${key.jsonEscape()}\":\"${value.jsonEscape()}\""
            }
            runCatching { file.appendText(line + "\n") }
        }
    }

    private data class RecordingEventLine(
        val index: Int,
        val elapsedMs: Long,
        val kind: String,
        val source: String,
        val label: String,
        val attributes: Map<String, String>,
    )
}

private fun String.jsonEscape(): String =
    buildString(length) {
        this@jsonEscape.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

data class RecordingSessionUi(
    val path: String,
    val fileName: String,
    val label: String,
    val lastModifiedMs: Long,
    val stepCount: Int,
    val durationMs: Long,
)
