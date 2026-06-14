/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.wrapped

import com.auramusic.innertube.models.AccountInfo
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.SongWithStats

data class WrappedState(
    val accountInfo: AccountInfo? = null,
    val totalMinutes: Long = 0,
    val topSongs: List<SongWithStats> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val top5Albums: List<Album> = emptyList(),
    val topAlbum: Album? = null,
    val uniqueSongCount: Int = 0,
    val uniqueArtistCount: Int = 0,
    val totalAlbums: Int = 0,
    val isDataReady: Boolean = false,
    val trackMap: Map<WrappedScreenType, String?> = emptyMap(),
    val playlistCreationState: PlaylistCreationState = PlaylistCreationState.Idle,
    val topArtistAlbums: List<Album> = emptyList(),
    // New stats
    val listeningByDayOfWeek: Map<Int, Long> = emptyMap(), // dayOfWeek(1=Mon..7=Sun) -> minutes
    val listeningByTimeOfDay: Map<String, Long> = emptyMap(), // "Morning"|"Afternoon"|"Evening"|"Night" -> minutes
    val repeatOffenderSong: SongWithStats? = null, // Most replayed song
    val discoveryScore: Int = 0, // Number of new artists discovered
    val totalPlayCount: Int = 0,
    val previousMonthMinutes: Long = 0,
    val previousMonthUniqueSongs: Int = 0,
    val previousMonthUniqueArtists: Int = 0,
    val monthOverMonthChange: Float = 0f, // percentage change in listening time
)
