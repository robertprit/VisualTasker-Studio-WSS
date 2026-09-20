package com.visualtasker.wss.recording

import android.content.Context
import org.json.JSONObject

interface RecordingReplayBookmarkStore {
    fun load(recordId: String): RecordingReplayBookmark?
    fun save(bookmark: RecordingReplayBookmark)
    fun clear(recordId: String)
}

class SharedPreferencesRecordingReplayBookmarkStore(context: Context) : RecordingReplayBookmarkStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(recordId: String): RecordingReplayBookmark? =
        RecordingReplayBookmarkCodec.decode(preferences.getString(key(recordId), null))
            ?.takeIf { it.recordId == recordId }

    override fun save(bookmark: RecordingReplayBookmark) {
        check(
            preferences.edit()
                .putString(key(bookmark.recordId), RecordingReplayBookmarkCodec.encode(bookmark))
                .commit()
        ) { "Replay bookmark could not be persisted for ${bookmark.recordId}." }
    }

    override fun clear(recordId: String) {
        check(preferences.edit().remove(key(recordId)).commit()) {
            "Replay bookmark could not be deleted for $recordId."
        }
    }

    private fun key(recordId: String): String = "record:$recordId"

    private companion object {
        const val PREFERENCES_NAME = "recording_replay_bookmarks"
    }
}

object RecordingReplayBookmarkCodec {
    fun encode(bookmark: RecordingReplayBookmark): String = JSONObject()
        .put("recordId", bookmark.recordId)
        .put("entryId", bookmark.entryId)
        .put("entryIndex", bookmark.entryIndex)
        .put("phase", bookmark.phase.name)
        .put("positionMs", bookmark.positionMs)
        .put("speed", bookmark.speed.toDouble())
        .put("selectedA11yNodeId", bookmark.selectedA11yNodeId)
        .put("wasPlaying", bookmark.wasPlaying)
        .toString()

    fun decode(raw: String?): RecordingReplayBookmark? = runCatching {
        if (raw.isNullOrBlank()) return@runCatching null
        val json = JSONObject(raw)
        RecordingReplayBookmark(
            recordId = json.getString("recordId"),
            entryId = json.nullableString("entryId"),
            entryIndex = json.optInt("entryIndex", 0).coerceAtLeast(0),
            phase = runCatching { RecordingPlaybackPhase.valueOf(json.optString("phase")) }
                .getOrDefault(RecordingPlaybackPhase.BEFORE),
            positionMs = json.optLong("positionMs", 0L).coerceAtLeast(0L),
            speed = json.optDouble("speed", 1.0).toFloat()
                .takeIf { it in RecordingPlaybackController.SupportedPlaybackSpeeds }
                ?: 1f,
            selectedA11yNodeId = json.nullableString("selectedA11yNodeId"),
            wasPlaying = json.optBoolean("wasPlaying", false),
        )
    }.getOrNull()

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}

internal fun shouldPersistReplayBookmark(
    previous: RecordingReplayBookmark?,
    next: RecordingReplayBookmark,
): Boolean = previous == null ||
    previous.recordId != next.recordId ||
    previous.entryId != next.entryId ||
    previous.phase != next.phase ||
    previous.speed != next.speed ||
    previous.selectedA11yNodeId != next.selectedA11yNodeId ||
    previous.wasPlaying != next.wasPlaying ||
    previous.positionMs / 500L != next.positionMs / 500L
