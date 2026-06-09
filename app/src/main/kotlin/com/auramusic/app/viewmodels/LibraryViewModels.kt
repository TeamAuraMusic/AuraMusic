/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.auramusic.app.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.innertube.YouTube
import com.auramusic.app.constants.AlbumFilter
import com.auramusic.app.constants.AlbumFilterKey
import com.auramusic.app.constants.AlbumSortDescendingKey
import com.auramusic.app.constants.AlbumSortType
import com.auramusic.app.constants.AlbumSortTypeKey
import com.auramusic.app.constants.ArtistFilter
import com.auramusic.app.constants.ArtistFilterKey
import com.auramusic.app.constants.ArtistSongSortDescendingKey
import com.auramusic.app.constants.ArtistSongSortType
import com.auramusic.app.constants.ArtistSongSortTypeKey
import com.auramusic.app.constants.ArtistSortDescendingKey
import com.auramusic.app.constants.ArtistSortType
import com.auramusic.app.constants.ArtistSortTypeKey
import com.auramusic.app.constants.AudiobookIdsKey
import com.auramusic.app.constants.AudiobookPositionsKey
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.HideVideoSongsKey
import com.auramusic.app.constants.LibraryFilter
import com.auramusic.app.constants.PlaylistSortDescendingKey
import com.auramusic.app.constants.PlaylistSortType
import com.auramusic.app.constants.PlaylistSortTypeKey
import com.auramusic.app.constants.SongFilter
import com.auramusic.app.constants.SongFilterKey
import com.auramusic.app.constants.SongSortDescendingKey
import com.auramusic.app.constants.SongSortType
import com.auramusic.app.constants.SongSortTypeKey
import com.auramusic.app.constants.TopSize
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.Song
import com.auramusic.app.extensions.filterExplicit
import com.auramusic.app.extensions.filterExplicitAlbums
import com.auramusic.app.extensions.filterVideoSongs
import com.auramusic.app.extensions.toEnum
import com.auramusic.app.playback.DownloadUtil
import com.auramusic.app.utils.SyncUtils
import com.auramusic.app.utils.AUDIOBOOK_MIN_DURATION_SECONDS
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.decodeAudiobookIds
import com.auramusic.app.utils.decodeAudiobookPositions
import com.auramusic.app.utils.encodeAudiobookIds
import com.auramusic.app.utils.encodeAudiobookPositions
import com.auramusic.app.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allSongs =
        context.dataStore.data
            .map {
                Triple(
                    Triple(
                        it[SongFilterKey].toEnum(SongFilter.LIKED),
                        it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                        (it[SongSortDescendingKey] ?: true),
                    ),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit, hideVideoSongs) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    SongFilter.LIKED -> database.likedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    SongFilter.DOWNLOADED -> database.downloadedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    // Uploaded feature is temporarily disabled
                    SongFilter.UPLOADED -> kotlinx.coroutines.flow.flowOf(emptyList())
                    // SongFilter.UPLOADED -> database.uploadedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }

    // Uploaded feature is temporarily disabled
    fun syncUploadedSongs() {
        // viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }
}

data class AudiobookLibraryItem(
    val song: Song,
    val resumePositionMs: Long,
    val pinned: Boolean,
)

@HiltViewModel
class LibraryAudiobooksViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val audiobooks =
        combine(
            database.allSongs(),
            context.dataStore.data,
        ) { songs, preferences ->
            val audiobookIds = decodeAudiobookIds(preferences[AudiobookIdsKey])
            val positions = decodeAudiobookPositions(preferences[AudiobookPositionsKey])
            val hideExplicit = preferences[HideExplicitKey] ?: false
            val hideVideoSongs = preferences[HideVideoSongsKey] ?: false

            songs
                .asSequence()
                .filter { it.song.inLibrary != null || it.song.isDownloaded || it.id in audiobookIds }
                .filter { it.id in audiobookIds || it.song.duration >= AUDIOBOOK_MIN_DURATION_SECONDS }
                .filter { !hideExplicit || !it.song.explicit }
                .filter { !hideVideoSongs || !it.song.isVideo }
                .map {
                    AudiobookLibraryItem(
                        song = it,
                        resumePositionMs = positions[it.id]?.coerceIn(0L, (it.song.duration * 1000L).coerceAtLeast(0L)) ?: 0L,
                        pinned = it.id in audiobookIds,
                    )
                }
                .sortedWith(
                    compareByDescending<AudiobookLibraryItem> { it.resumePositionMs > 0L }
                        .thenByDescending { it.pinned }
                        .thenBy { it.song.song.title.lowercase() }
                )
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setPinned(songId: String, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                val ids = decodeAudiobookIds(preferences[AudiobookIdsKey]).toMutableSet()
                if (pinned) ids += songId else ids -= songId
                preferences[AudiobookIdsKey] = encodeAudiobookIds(ids)
            }
        }
    }

    fun clearResumePosition(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                val positions = decodeAudiobookPositions(preferences[AudiobookPositionsKey]).toMutableMap()
                positions -= songId
                preferences[AudiobookPositionsKey] = encodeAudiobookPositions(positions)
            }
        }
    }
}

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val artistDetailFetchAttempted = mutableSetOf<String>()

    val allArtists =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter { artist ->
                        val isStale = Duration.between(
                            artist.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                        val missingSubscriberCount =
                            artist.bookmarkedAt != null &&
                                artist.isYouTubeArtist &&
                                !artist.isPrivatelyOwnedArtist &&
                                artist.subscriberCountText.isNullOrBlank()

                        (artist.thumbnailUrl == null || isStale || missingSubscriberCount) &&
                            artistDetailFetchAttempted.add(artist.id)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allAlbums =
        context.dataStore.data
            .map {
                Pair(
                    Triple(
                        it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                        it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                        (it[AlbumSortDescendingKey] ?: true),
                    ),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                    // Uploaded feature is temporarily disabled
                    AlbumFilter.UPLOADED -> kotlinx.coroutines.flow.flowOf(emptyList())
                    // AlbumFilter.UPLOADED -> database.albumsUploaded(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedAlbums() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}
@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allPlaylists =
        context.dataStore.data
            .map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[PlaylistSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncSavedPlaylists() }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit, hideVideoSongs) ->
                val (sortType, descending) = sortDesc
                database.artistSongs(artistId, sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val artistDetailFetchAttempted = mutableSetOf<String>()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val syncAllLibrary = {
         viewModelScope.launch(Dispatchers.IO) {
             syncUtils.tryAutoSync()
         }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            syncUtils.performFullSyncSuspend()
            _isRefreshing.value = false
        }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    var artists =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true,
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var albums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var playlists = database.playlists(PlaylistSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter { artist ->
                        val isStale = Duration.between(
                            artist.lastUpdateTime,
                            LocalDateTime.now(),
                        ) > Duration.ofDays(10)
                        val missingSubscriberCount =
                            artist.bookmarkedAt != null &&
                                artist.isYouTubeArtist &&
                                !artist.isPrivatelyOwnedArtist &&
                                artist.subscriberCountText.isNullOrBlank()

                        (artist.thumbnailUrl == null || isStale || missingSubscriberCount) &&
                            artistDetailFetchAttempted.add(artist.id)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}
