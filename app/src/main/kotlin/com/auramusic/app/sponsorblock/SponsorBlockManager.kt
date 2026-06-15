/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.sponsorblock

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.auramusic.app.constants.SponsorBlockEnabledKey
import com.auramusic.app.constants.SponsorBlockSkipSponsorKey
import com.auramusic.app.constants.SponsorBlockSkipIntroKey
import com.auramusic.app.constants.SponsorBlockSkipOutroKey
import com.auramusic.app.constants.SponsorBlockSkipSelfPromoKey
import com.auramusic.app.constants.SponsorBlockSkipInteractionKey
import com.auramusic.app.constants.SponsorBlockSkipPreviewKey
import com.auramusic.app.constants.SponsorBlockSkipMusicOffTopicKey
import com.auramusic.app.constants.SponsorBlockSkipFillerKey
import com.auramusic.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SponsorBlockManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _segments = MutableStateFlow<List<SponsorBlockSegment>>(emptyList())
    val segments = _segments.asStateFlow()

    private val _currentSegment = MutableStateFlow<SponsorBlockSegment?>(null)
    val currentSegment = _currentSegment.asStateFlow()

    private val _seekBarSegments = MutableStateFlow<List<SeekBarSegment>>(emptyList())
    val seekBarSegments = _seekBarSegments.asStateFlow()

    private var currentVideoId: String? = null

    suspend fun loadPreferences() {
        val prefs = context.dataStore.data.first()
        _enabled.value = prefs[SponsorBlockEnabledKey] ?: false
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        scope.launch {
            context.dataStore.edit { it[SponsorBlockEnabledKey] = value }
        }
        if (!value) {
            _segments.value = emptyList()
            _seekBarSegments.value = emptyList()
            _currentSegment.value = null
        }
    }

    private suspend fun getActiveCategories(): Set<String> {
        val prefs = context.dataStore.data.first()
        val categories = mutableSetOf<String>()
        if (prefs[SponsorBlockSkipSponsorKey] != false) categories.add("sponsor")
        if (prefs[SponsorBlockSkipSelfPromoKey] != false) categories.add("selfpromo")
        if (prefs[SponsorBlockSkipInteractionKey] != false) categories.add("interaction")
        if (prefs[SponsorBlockSkipIntroKey] != false) categories.add("intro")
        if (prefs[SponsorBlockSkipOutroKey] != false) categories.add("outro")
        if (prefs[SponsorBlockSkipPreviewKey] != false) categories.add("preview")
        if (prefs[SponsorBlockSkipMusicOffTopicKey] != false) categories.add("music_offtopic")
        if (prefs[SponsorBlockSkipFillerKey] != false) categories.add("filler")
        return categories
    }

    suspend fun loadSegments(videoId: String, durationMs: Long = 0) {
        if (!_enabled.value) {
            _segments.value = emptyList()
            _seekBarSegments.value = emptyList()
            return
        }
        if (videoId == currentVideoId && _segments.value.isNotEmpty()) return

        currentVideoId = videoId
        val categories = getActiveCategories()
        val fetched = SponsorBlockApi.getSegments(videoId, categories)
        _segments.value = fetched
        _seekBarSegments.value = SponsorBlockApi.toSeekBarSegments(fetched, durationMs)
    }

    fun updateDuration(durationMs: Long) {
        if (durationMs > 0 && _segments.value.isNotEmpty()) {
            _seekBarSegments.value = SponsorBlockApi.toSeekBarSegments(_segments.value, durationMs)
        }
    }

    fun findSkipTarget(positionMs: Long, speed: Float = 1f): Long? {
        if (!_enabled.value || _segments.value.isEmpty()) return null
        // 2-second window scaled by playback speed (matches SmartTube behavior)
        val windowMs = (2000L * speed).toLong()
        val segment = _segments.value.find { seg ->
            positionMs >= seg.startMs && positionMs <= minOf(seg.startMs + windowMs, seg.endMs)
        }
        return if (segment != null) {
            _currentSegment.value = segment
            segment.endMs
        } else {
            _currentSegment.value = null
            null
        }
    }

    fun reset() {
        _segments.value = emptyList()
        _seekBarSegments.value = emptyList()
        _currentSegment.value = null
        currentVideoId = null
    }
}
