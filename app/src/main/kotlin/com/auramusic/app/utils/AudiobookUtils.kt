/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val AUDIOBOOK_MIN_DURATION_SECONDS = 20 * 60
const val AUDIOBOOK_RESUME_THRESHOLD_MS = 30_000L

private val audiobookJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class AudiobookPosition(
    val id: String,
    val positionMs: Long,
)

fun decodeAudiobookIds(value: String?): Set<String> =
    runCatching {
        if (value.isNullOrBlank()) emptySet() else audiobookJson.decodeFromString<List<String>>(value).toSet()
    }.getOrDefault(emptySet())

fun encodeAudiobookIds(value: Set<String>): String =
    audiobookJson.encodeToString(value.toList().sorted())

fun decodeAudiobookPositions(value: String?): Map<String, Long> =
    runCatching {
        if (value.isNullOrBlank()) {
            emptyMap()
        } else {
            audiobookJson.decodeFromString<List<AudiobookPosition>>(value)
                .associate { it.id to it.positionMs.coerceAtLeast(0L) }
        }
    }.getOrDefault(emptyMap())

fun encodeAudiobookPositions(value: Map<String, Long>): String =
    audiobookJson.encodeToString(
        value
            .filterValues { it > 0L }
            .map { AudiobookPosition(id = it.key, positionMs = it.value) }
            .sortedBy { it.id }
    )
