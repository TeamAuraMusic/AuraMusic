package com.auramusic.innertube

import com.auramusic.innertube.models.MusicResponsiveListItemRenderer
import com.auramusic.innertube.models.Runs
import com.auramusic.innertube.models.Run
import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.ThumbnailRenderer
import com.auramusic.innertube.models.Thumbnails
import com.auramusic.innertube.pages.PlaylistPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistPageParserTest {

    private fun row(setVideoId: String?) = MusicResponsiveListItemRenderer(
        badges = null,
        fixedColumns = null,
        flexColumns = listOf(
            MusicResponsiveListItemRenderer.FlexColumn(
                musicResponsiveListItemFlexColumnRenderer =
                    MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer(
                        text = Runs(runs = listOf(Run(text = "A song", navigationEndpoint = null))),
                    ),
            ),
        ),
        thumbnail = ThumbnailRenderer(
            musicThumbnailRenderer = ThumbnailRenderer.MusicThumbnailRenderer(
                thumbnail = Thumbnails(listOf(Thumbnail(url = "https://i.ytimg.com/vi/abc123/hq.jpg", width = 128, height = 128))),
                thumbnailCrop = null,
                thumbnailScale = null,
            ),
            musicAnimatedThumbnailRenderer = null,
            croppedSquareThumbnailRenderer = null,
        ),
        menu = null,
        playlistItemData = MusicResponsiveListItemRenderer.PlaylistItemData(
            playlistSetVideoId = setVideoId,
            videoId = "abc123",
        ),
        overlay = null,
        navigationEndpoint = null,
    )

    @Test
    fun rowWithoutPlaylistSetVideoIdStillParses() {
        // YouTube Music increasingly omits playlistSetVideoId. Previously this row
        // returned null, so a playlist made entirely of such rows rendered as 0 songs.
        val song = PlaylistPage.fromMusicResponsiveListItemRenderer(row(setVideoId = null))
        assertNotNull(song)
        assertEquals("abc123", song!!.id)
        assertEquals("A song", song.title)
        assertNull(song.setVideoId)
    }

    @Test
    fun rowWithPlaylistSetVideoIdKeepsIt() {
        val song = PlaylistPage.fromMusicResponsiveListItemRenderer(row(setVideoId = "set-1"))
        assertNotNull(song)
        assertEquals("set-1", song!!.setVideoId)
    }
}
