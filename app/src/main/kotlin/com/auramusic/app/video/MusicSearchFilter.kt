package com.auramusic.app.video

import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeSearchResultItem
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Applies the music-only rule to a mixed YouTube search result set.
 *
 * A YouTube search returns videos, channels and playlists in one list, and none of
 * them are music by default. Each kind is judged differently:
 *  - **video** - YouTube's own music tag, via [MusicContentFilter].
 *  - **channel** - an auto-generated music channel passes with no network call;
 *    otherwise a few of its videos are sampled.
 *  - **playlist** - its first few videos are sampled; a playlist with no music in it
 *    is not music just because its title says so.
 */
object MusicSearchFilter {

    /** How many of a channel's/playlist's videos we fetch and check. */
    private const val SAMPLE = MusicContentFilter.SAMPLE_SIZE

    private const val MAX_PARALLEL = 6

    suspend fun filter(items: List<YouTubeSearchResultItem>): List<YouTubeSearchResultItem> =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext emptyList()

            // Chunked because a single search page can hold dozens of channels and
            // playlists, each of which costs an extra browse before it can be judged.
            items.chunked(MAX_PARALLEL).flatMap { chunk ->
                coroutineScope {
                    chunk.map { item -> async { item to isAllowed(item) } }.awaitAll()
                }
            }.filter { it.second }.map { it.first }
        }

    private suspend fun isAllowed(item: YouTubeSearchResultItem): Boolean = when (item) {
        is YouTubeSearchResultItem.Video -> MusicContentFilter.isMusic(item.video)
        is YouTubeSearchResultItem.Channel -> MusicContentFilter.isMusicChannel(
            channelName = item.title,
            sampleVideos = sampleChannelVideos(item.channelId),
        )
        is YouTubeSearchResultItem.Playlist -> MusicContentFilter.isMusicPlaylist(
            samplePlaylistVideos(item.playlistId),
        )
    }

    private suspend fun sampleChannelVideos(channelId: String): List<YouTubeVideoItem> =
        YouTube.youtubeChannel(channelId).getOrNull()?.videos?.take(SAMPLE).orEmpty()

    private suspend fun samplePlaylistVideos(playlistId: String): List<YouTubeVideoItem> =
        YouTube.youtubePlaylist(playlistId, limit = SAMPLE).getOrNull()?.videos.orEmpty()
}
