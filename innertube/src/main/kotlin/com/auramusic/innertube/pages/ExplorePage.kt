package com.auramusic.innertube.pages

import com.auramusic.innertube.MixesPage
import com.auramusic.innertube.PodcastsPage
import com.auramusic.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
    val podcasts: List<PodcastsPage.PodcastSection> = emptyList(),
    val mixes: List<MixesPage.MixSection> = emptyList(),
)
