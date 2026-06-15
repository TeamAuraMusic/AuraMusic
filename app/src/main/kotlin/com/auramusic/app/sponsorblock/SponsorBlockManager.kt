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
            _currentSegment.value = null
        }
    }

    suspend fun getActiveCategories(): Set<String> {
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

    suspend fun loadSegments(videoId: String) {
        if (!_enabled.value) {
            _segments.value = emptyList()
            return
        }
        if (videoId == currentVideoId && _segments.value.isNotEmpty()) return

        currentVideoId = videoId
        val categories = getActiveCategories()
        val fetched = SponsorBlockApi.getSegments(videoId, categories)
        _segments.value = fetched
    }

    fun findSkipTarget(positionMs: Long): Long? {
        if (!_enabled.value || _segments.value.isEmpty()) return null
        val positionSec = positionMs.toDouble() / 1000.0
        val segment = _segments.value.find { seg ->
            positionSec >= seg.segment[0] && positionSec < seg.segment[1] && seg.actionType == "skip"
        }
        return if (segment != null) {
            _currentSegment.value = segment
            (segment.segment[1] * 1000).toLong()
        } else {
            _currentSegment.value = null
            null
        }
    }

    fun reset() {
        _segments.value = emptyList()
        _currentSegment.value = null
        currentVideoId = null
    }
}
