/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import android.content.Context
import com.auramusic.app.db.entities.Song
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.video.VideoPlaybackManager.RecommendationItem
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YTItem

/**
 * How anything on a television decides what playing something actually means.
 *
 * There are two kinds of thing a shelf can hold and they are not played the same
 * way. A song goes into the music queue and plays. A video is a video: it is handed
 * to the video pipeline, which resolves its stream and puts it on the player as a
 * video, so the picture is there from the first frame.
 *
 * Queuing a video as music and switching a video in afterwards is what this exists
 * to stop doing. That route starts as an audio item and then goes looking for a
 * video to put in its place, so every video arrives as sound first, a gap, then the
 * picture it was always going to show - on the home screen, in search, in a shelf,
 * everywhere alike, because it was not a property of where the video was found but
 * of how it was played.
 */
object TvPlayback {

    /**
     * Plays a shelf item.
     *
     * Songs, albums, playlists and podcasts are routed to where they belong; only
     * what is actually a video is handed to the video pipeline.
     */
    fun playYtItem(
        context: Context,
        playerConnection: PlayerConnection?,
        navigator: TvNavigator?,
        item: YTItem,
    ) {
        when (item) {
            is SongItem -> playVideoOrSong(context, playerConnection, navigator, item)
            // An episode is a video with a person talking in front of it.
            is EpisodeItem -> playVideo(context, navigator, item.id, item.title, "")
            else -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.id)))
        }
    }

    /**
     * Plays a song, or plays its video if that is what it is.
     *
     * A video song in the library is the same video as any other, and it deserves the
     * same treatment - it is simply the one case where the answer is known before
     * playback starts rather than discovered halfway through it.
     */
    fun playVideoOrSong(
        context: Context,
        playerConnection: PlayerConnection?,
        navigator: TvNavigator?,
        item: SongItem,
        withRadio: Boolean = false,
    ) {
        if (isVideo(item)) {
            playVideo(
                context = context,
                navigator = navigator,
                videoId = item.id,
                title = item.title,
                channelName = item.artists.joinToString(", ") { it.name },
            )
            // Only for a song picked on its own (search): the list beside the player is that
            // song's radio. A song picked from a playlist or album keeps that list.
            if (withRadio) VideoPlaybackManager.startRadio(item.id)
        } else {
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
    }

    /** A library song: a video song is its video, played as a video. */
    fun playLibrarySong(
        context: Context,
        playerConnection: PlayerConnection?,
        navigator: TvNavigator?,
        song: Song,
    ) {
        if (song.song.isVideo) {
            playVideo(
                context = context,
                navigator = navigator,
                videoId = song.id,
                title = song.title,
                channelName = song.artists.firstOrNull()?.name.orEmpty(),
            )
        } else {
            playerConnection?.playQueue(
                YouTubeQueue.radio(song.toMediaMetadata()),
            )
        }
    }

    /** Whether this shelf item is a video rather than a song. */
    fun isVideo(item: YTItem): Boolean = when (item) {
        is SongItem -> item.isVideoSong || item.album == null
        is EpisodeItem -> true
        else -> false
    }

    /**
     * Plays a video as a video.
     *
     * Nothing to switch afterwards, because the stream is resolved before playback
     * rather than during it.
     */
    fun playVideo(
        context: Context,
        navigator: TvNavigator?,
        videoId: String,
        title: String,
        channelName: String,
        queue: List<RecommendationItem>? = null,
    ) {
        VideoPlaybackManager.playWithDetails(
            context = context,
            videoId = videoId,
            title = title,
            channelName = channelName,
            queueOnStart = queue,
        )
        // A video started from a shelf has to be shown, not merely begun: without
        // this it plays on behind whatever the listener is looking at, with no way
        // back to it - no picture, no player, nothing to pause. The video is what
        // they asked for, so the player is where they end up.
        if (navigator?.current != TvDestination.Player) {
            navigator?.navigate(TvDestination.Player)
        }
    }
}