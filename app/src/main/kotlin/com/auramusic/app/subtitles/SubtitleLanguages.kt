/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.subtitles

/**
 * Caption language choices offered to the user, as (code, label) pairs.
 *
 * Kept in one place so Player settings and the video player's captions sheet offer
 * exactly the same list - the two surfaces write the same preference, so a choice made
 * in one has to be selectable in the other.
 */
val SubtitleLanguageOptions: List<Pair<String, String>> = listOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
    "auto" to "Auto",
)
