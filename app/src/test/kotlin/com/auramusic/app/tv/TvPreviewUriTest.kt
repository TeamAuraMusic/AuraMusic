package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TvPreviewUriTest {
    @Test
    fun sourceRevisionIsStableAndTracksSongArtworkAndDuration() {
        val base = tvPreviewSourceRevision("song-a", "https://img.example/a.jpg?size=small", 180_000)

        assertEquals(
            base,
            tvPreviewSourceRevision("song-a", "https://img.example/a.jpg?size=large", 180_000),
        )
        assertNotEquals(base, tvPreviewSourceRevision("song-b", "https://img.example/a.jpg", 180_000))
        assertNotEquals(base, tvPreviewSourceRevision("song-a", "https://img.example/b.jpg", 180_000))
        assertNotEquals(base, tvPreviewSourceRevision("song-a", "https://img.example/a.jpg", 181_000))
    }

    @Test
    fun sourceRevisionTracksRefreshedSignedAudioUrlWithoutEmbeddingIt() {
        val firstUrl = "https://media.example/song.m4a?expire=100&token=first-secret"
        val refreshedUrl = "https://media.example/song.m4a?expire=200&token=second-secret"
        val first = TvPreviewUri.revision(
            "song-a", "https://img.example/a.jpg", 180_000, audioSourceUri = firstUrl,
        )
        val refreshed = TvPreviewUri.revision(
            "song-a", "https://img.example/a.jpg", 180_000, audioSourceUri = refreshedUrl,
        )

        assertNotEquals(first, refreshed)
        assertFalse(first.contains("secret"))
        assertFalse(refreshed.contains("secret"))
    }

    @Test
    fun pipelineVersionInvalidatesExistingEncodedPreviewCache() {
        val first = tvPreviewSourceRevision(
            mediaId = "song-a",
            artworkUri = "https://img.example/a.jpg",
            durationMs = 180_000,
            pipelineVersion = 1,
        )
        val afterRendererChange = tvPreviewSourceRevision(
            mediaId = "song-a",
            artworkUri = "https://img.example/a.jpg",
            durationMs = 180_000,
            pipelineVersion = 2,
        )

        assertNotEquals(first, afterRendererChange)
    }
}
