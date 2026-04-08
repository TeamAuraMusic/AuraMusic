/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.filterExplicit
import com.auramusic.innertube.pages.ExplorePage
import com.auramusic.innertube.PodcastsPage
import com.auramusic.innertube.MixesPage
import com.auramusic.innertube.Top100ChartsPage
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.app.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val podcastsPage = MutableStateFlow<PodcastsPage?>(null)
    val mixesPage = MutableStateFlow<MixesPage?>(null)
    val top100ChartsPage = MutableStateFlow<Top100ChartsPage?>(null)

    private suspend fun loadPodcastsInternal() {
        YouTube.podcasts().onSuccess { page ->
            podcastsPage.value = page
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadMixesInternal() {
        YouTube.mixes().onSuccess { page ->
            mixesPage.value = page
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadTop100ChartsInternal(countryCode: String = "US") {
        YouTube.getTop100Charts(countryCode).onSuccess { page ->
            top100ChartsPage.value = page
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun load() {
        YouTube
            .explore()
            .onSuccess { page ->
                val artists: MutableMap<Int, String> = mutableMapOf()
                val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                database.allArtistsByPlayTime().first().let { list ->
                    var favIndex = 0
                    for ((artistsIndex, artist) in list.withIndex()) {
                        artists[artistsIndex] = artist.id
                        if (artist.artist.bookmarkedAt != null) {
                            favouriteArtists[favIndex] = artist.id
                            favIndex++
                        }
                    }
                }
                val podcasts = podcastsPage.value?.featured ?: emptyList()
                val mixes = mixesPage.value?.mixes ?: emptyList()
                explorePage.value =
                    page.copy(
                        newReleaseAlbums =
                        page.newReleaseAlbums
                            .sortedBy { album ->
                                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                val firstArtistKey =
                                    artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtists.values) {
                                            favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artists.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                firstArtistKey
                            }.filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                        podcasts = podcasts,
                        mixes = mixes,
                    )
            }.onFailure {
                reportException(it)
            }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Load podcasts and mixes first so they're available when load() merges them into explorePage
            loadPodcastsInternal()
            loadMixesInternal()
            load()
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadTop100ChartsInternal()
        }
    }

    fun loadPodcasts() = viewModelScope.launch(Dispatchers.IO) { loadPodcastsInternal() }
    fun loadMixes() = viewModelScope.launch(Dispatchers.IO) { loadMixesInternal() }
    fun loadTop100Charts(countryCode: String = "US") = viewModelScope.launch(Dispatchers.IO) { loadTop100ChartsInternal(countryCode) }
}
