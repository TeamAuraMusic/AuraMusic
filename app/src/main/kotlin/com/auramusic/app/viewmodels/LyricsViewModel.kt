/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.auramusic.app.lyrics.LyricsEntry
import com.auramusic.app.lyrics.LyricsUtils
import com.auramusic.app.ui.component.LyricsListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LyricsViewModel @Inject constructor() : ViewModel() {
    private var processJob: kotlinx.coroutines.Job? = null

    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean
    ) {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            val processedLines = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList()
                } else {
                    val timestampRegex = Regex("\\[\\d{1,2}:\\d{2}")
                    val isLrc = timestampRegex.containsMatchIn(lyrics)
                    val parsedLines = if (isLrc) LyricsUtils.parseLyrics(lyrics) else emptyList()

                    if (parsedLines.isNotEmpty()) {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
                    } else {
                        val baseTime = 1000000L
                        lyrics.lines()
                            .filter { it.isNotBlank() && !timestampRegex.containsMatchIn(it) }
                            .mapIndexed { index, line ->
                                LyricsEntry(baseTime + index, line)
                            }
                    }
                }
            }

            _lines.value = processedLines
            updateMergedList(processedLines, showIntervalIndicator)

            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        if (entry.text.isBlank()) return@forEach
                        val romanized = romanizeIfEnabled(
                            text = entry.text,
                            enabledLanguages = enabledLanguages,
                            romanizeCyrillicByLine = romanizeCyrillicByLine,
                            allLyrics = lyrics
                        )
                        if (romanized != null) {
                            entry.romanizedTextFlow.value = romanized
                        }
                    }
                }
            }
        }
    }

    private suspend fun romanizeIfEnabled(
        text: String,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        allLyrics: String
    ): String? {
        val detectionText = if (romanizeCyrillicByLine) text else allLyrics
        return when {
            "ja" in enabledLanguages && LyricsUtils.isJapanese(text) && !LyricsUtils.isChinese(text) ->
                LyricsUtils.romanizeJapanese(text)
            "ko" in enabledLanguages && LyricsUtils.isKorean(text) ->
                LyricsUtils.romanizeKorean(text)
            "zh" in enabledLanguages && LyricsUtils.isChinese(text) ->
                LyricsUtils.romanizeChinese(text)
            "ru" in enabledLanguages && LyricsUtils.isRussian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "uk" in enabledLanguages && LyricsUtils.isUkrainian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "sr" in enabledLanguages && LyricsUtils.isSerbian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "bg" in enabledLanguages && LyricsUtils.isBulgarian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "be" in enabledLanguages && LyricsUtils.isBelarusian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "ky" in enabledLanguages && LyricsUtils.isKyrgyz(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            "mk" in enabledLanguages && LyricsUtils.isMacedonian(detectionText) ->
                LyricsUtils.romanizeCyrillic(text)
            else -> null
        }
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}
