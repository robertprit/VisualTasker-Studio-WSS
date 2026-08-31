package com.visualtasker.wss.logging

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class StudioLogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
}

data class StudioLogEntry(
    val id: String,
    val timestamp: Long,
    val level: StudioLogLevel,
    val source: String,
    val message: String,
    val details: String? = null,
    val documentRevision: Long? = null,
    val groupKey: String? = null,
    val repeatCount: Int = 1,
)

data class StudioLogFilters(
    val levels: Set<StudioLogLevel> = StudioLogLevel.entries.toSet(),
    val sources: Set<String> = emptySet(),
    val query: String = "",
)

class StudioLogStore(
    private val maxEntries: Int = 500,
) {
    private val lock = Any()
    private val entries = mutableListOf<StudioLogEntry>()
    private val pausedBuffer = mutableListOf<PendingLogEntry>()
    private var nextId = 0L

    var isPaused by mutableStateOf(false)
        private set

    var changeToken by mutableIntStateOf(0)
        private set

    fun append(
        level: StudioLogLevel,
        source: String,
        message: String,
        details: String? = null,
        documentRevision: Long? = null,
        groupKey: String? = null,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        val pending = PendingLogEntry(level, source, message, details, documentRevision, groupKey, timestamp)
        synchronized(lock) {
            if (isPaused) {
                pausedBuffer += pending
                return
            }
            appendInternal(pending)
        }
    }

    fun setEmissionPaused(paused: Boolean) {
        synchronized(lock) {
            if (isPaused == paused) return
            isPaused = paused
            if (!paused && pausedBuffer.isNotEmpty()) {
                pausedBuffer.forEach(::appendInternal)
                pausedBuffer.clear()
            }
        }
    }

    fun clearVisible(filters: StudioLogFilters) {
        synchronized(lock) {
            val remaining = entries.filterNot { entry -> matchesFilters(entry, filters) }
            if (remaining.size == entries.size) return
            entries.clear()
            entries += remaining
            bumpToken()
        }
    }

    fun availableSources(): Set<String> = synchronized(lock) { entries.map { it.source }.toSortedSet() }

    fun visibleEntries(filters: StudioLogFilters): List<StudioLogEntry> =
        synchronized(lock) { entries.filter { entry -> matchesFilters(entry, filters) } }

    fun allEntries(): List<StudioLogEntry> = synchronized(lock) { entries.toList() }

    private fun appendInternal(pending: PendingLogEntry) {
        val key = pending.groupKey ?: deriveGroupKey(pending)
        val last = entries.lastOrNull()
        if (last != null && last.groupKey == key) {
            entries[entries.lastIndex] = last.copy(
                timestamp = pending.timestamp,
                repeatCount = last.repeatCount + 1,
            )
        } else {
            entries += StudioLogEntry(
                id = "log-${nextId++}",
                timestamp = pending.timestamp,
                level = pending.level,
                source = pending.source,
                message = pending.message,
                details = pending.details,
                documentRevision = pending.documentRevision,
                groupKey = key,
                repeatCount = 1,
            )
            if (entries.size > maxEntries) {
                val overflow = entries.size - maxEntries
                repeat(overflow) { entries.removeAt(0) }
            }
        }
        bumpToken()
    }

    private fun matchesFilters(entry: StudioLogEntry, filters: StudioLogFilters): Boolean {
        if (entry.level !in filters.levels) return false
        if (filters.sources.isNotEmpty() && entry.source !in filters.sources) return false
        val query = filters.query.trim()
        if (query.isBlank()) return true
        val needle = query.lowercase(Locale.ROOT)
        val haystack = buildString {
            append(entry.message.lowercase(Locale.ROOT))
            append(' ')
            append(entry.source.lowercase(Locale.ROOT))
            append(' ')
            append(entry.level.name.lowercase(Locale.ROOT))
            append(' ')
            append(entry.details?.lowercase(Locale.ROOT).orEmpty())
        }
        return haystack.contains(needle)
    }

    private fun deriveGroupKey(entry: PendingLogEntry): String = buildString {
        append(entry.level.name)
        append('|')
        append(entry.source)
        append('|')
        append(entry.message)
        append('|')
        append(entry.documentRevision ?: "no-rev")
        append('|')
        append(entry.details ?: "no-details")
    }

    private fun bumpToken() {
        changeToken += 1
    }

    private data class PendingLogEntry(
        val level: StudioLogLevel,
        val source: String,
        val message: String,
        val details: String?,
        val documentRevision: Long?,
        val groupKey: String?,
        val timestamp: Long,
    )
}
