package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvStaleProgramMetadataTest {
    @Test
    fun retainsDeletedProgramIdentityAudioArtworkAndExpiryWindow() {
        val now = 1_700_000_000_000L
        val retained = retainRecentlyDeletedTvPrograms(
            existing = emptyList(),
            deleted = listOf(
                ExistingPreviewProgram(
                    programId = 7,
                    internalProviderId = "auramusic:track:song-a:0",
                    contentId = "song-a",
                    previewAudioUri = "https://media.example/song-a?mime=audio%2Fwebm",
                    posterArtUri = "https://images.example/song-a.jpg",
                    title = "Song A",
                    durationMs = 185_000,
                ),
            ),
            nowMillis = now,
            ttlMillis = 300_000,
            maxEntries = 20,
        )

        assertEquals(1, retained.size)
        assertEquals("song-a", retained.single().contentId)
        assertEquals("Song A", retained.single().title)
        assertEquals(185_000L, retained.single().durationMs)
        assertEquals("https://images.example/song-a.jpg", retained.single().posterArtUri)
        assertEquals("https://media.example/song-a?mime=audio%2Fwebm", retained.single().previewAudioUri)
        assertEquals(now + 300_000L, retained.single().expiresAtMs)
        assertEquals(
            retained.single(),
            findRecentlyDeletedTvProgram(retained, "auramusic:track:song-a:0", now + 299_999L),
        )
        assertNull(findRecentlyDeletedTvProgram(retained, "auramusic:track:song-a:0", now + 300_000L))
    }

    @Test
    fun dropsExpiredEntriesAndBoundsTheCacheToNewestSnapshots() {
        val now = 1_000L
        val existing = listOf(
            retained("expired", expiresAt = now),
            retained("older", expiresAt = now + 10),
            retained("newer", expiresAt = now + 20),
        )
        val result = retainRecentlyDeletedTvPrograms(
            existing = existing,
            deleted = listOf(deleted("fresh")),
            nowMillis = now,
            ttlMillis = 100,
            maxEntries = 2,
        )

        assertEquals(listOf("fresh", "newer"), result.map { it.internalProviderId })
        assertTrue(result.all { it.expiresAtMs > now })
        assertEquals(2, result.size)
    }

    @Test
    fun aNewDeletionRefreshesSameProgramKeyAndIgnoresIncompleteRows() {
        val existing = listOf(retained("program:track-a", expiresAt = 200, title = "old title"))
        val result = retainRecentlyDeletedTvPrograms(
            existing = existing,
            deleted = listOf(
                deleted("program:track-a", contentId = "new-id", title = "new title"),
                ExistingPreviewProgram(programId = 9, internalProviderId = "missing-art", contentId = "x"),
            ),
            nowMillis = 100,
            ttlMillis = 50,
            maxEntries = 20,
        )

        assertEquals(1, result.size)
        assertEquals("new-id", result.single().contentId)
        assertEquals("new title", result.single().title)
        assertEquals(150L, result.single().expiresAtMs)
    }

    private fun retained(
        id: String,
        expiresAt: Long,
        title: String = id,
    ) = RetainedTvProgramMetadata(
        internalProviderId = id,
        contentId = id,
        previewAudioUri = null,
        posterArtUri = "https://images.example/$id.jpg",
        durationMs = 60_000,
        title = title,
        expiresAtMs = expiresAt,
    )

    private fun deleted(
        id: String,
        contentId: String = id,
        title: String = id,
    ) = ExistingPreviewProgram(
        programId = 1,
        internalProviderId = id,
        contentId = contentId,
        posterArtUri = "https://images.example/$id.jpg",
        title = title,
        durationMs = 60_000,
    )
}
