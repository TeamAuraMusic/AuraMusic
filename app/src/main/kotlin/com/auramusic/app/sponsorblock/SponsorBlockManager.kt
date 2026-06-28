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
    private var loadGeneration = 0

    suspend fun loadPreferences() {
        val prefs = context.dataStore.data.first()
        updateEnabled(prefs[SponsorBlockEnabledKey] ?: false)
    }

    fun updateEnabled(value: Boolean) {
        _enabled.value = value
        if (!value) {
            loadGeneration++
            _segments.value = emptyList()
            _seekBarSegments.value = emptyList()
            _currentSegment.value = null
        }
    }

    fun setEnabled(value: Boolean) {
        updateEnabled(value)
        scope.launch {
            context.dataStore.edit { it[SponsorBlockEnabledKey] = value }
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
            loadGeneration++
            _segments.value = emptyList()
            _seekBarSegments.value = emptyList()
            return
        }
        // Always reload if videoId changed or segments are empty
        if (videoId == currentVideoId && _segments.value.isNotEmpty()) return

        val generation = ++loadGeneration
        currentVideoId = videoId
        val categories = getActiveCategories()
        if (categories.isEmpty()) {
            _segments.value = emptyList()
            _seekBarSegments.value = emptyList()
            _currentSegment.value = null
            return
        }

        val fetched = SponsorBlockApi.getSegments(videoId, categories)
        if (generation != loadGeneration || currentVideoId != videoId || !_enabled.value) return

        _segments.value = fetched
        _seekBarSegments.value = SponsorBlockApi.toSeekBarSegments(fetched, durationMs)
    }

    fun forceReload(videoId: String, durationMs: Long = 0) {
        currentVideoId = null
        loadGeneration++
        scope.launch {
            loadSegments(videoId, durationMs)
        }
    }

    fun updateDuration(durationMs: Long) {
        if (durationMs > 0 && _segments.value.isNotEmpty()) {
            _seekBarSegments.value = SponsorBlockApi.toSeekBarSegments(_segments.value, durationMs)
        }
    }

    fun findSkipTarget(positionMs: Long, speed: Float = 1f): Long? {
        if (!_enabled.value || _segments.value.isEmpty()) return null
        // Skip when playback is anywhere inside a segment. This also covers
        // segments that finish loading after playback has already entered them.
        val endToleranceMs = (250L * speed).toLong()
        val segment = _segments.value.find { seg ->
            positionMs >= seg.startMs && positionMs < seg.endMs - endToleranceMs
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
        loadGeneration++
        _segments.value = emptyList()
        _seekBarSegments.value = emptyList()
        _currentSegment.value = null
        currentVideoId = null
    }
}
