/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.auramusic.app.alarm

/** Source pool the alarm picks songs from. */
enum class AlarmSource(val displayName: String) {
    DOWNLOADS("Downloaded"),
    CACHED("Cached"),
    PLAYLIST("Playlist"),
    ;

    companion object {
        fun fromName(name: String?): AlarmSource =
            values().firstOrNull { it.name == name } ?: DOWNLOADS
    }
}
