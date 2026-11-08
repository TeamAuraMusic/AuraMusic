package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class TvPreviewAudioCachePolicyTest {
    @Test
    fun evictsOldestCompletedEntriesAndProtectsJustWrittenTrack() {
        val entries = listOf(
            PreviewAudioCacheEntry("old", sizeBytes = 45, lastAccessMs = 10),
            PreviewAudioCacheEntry("middle", sizeBytes = 45, lastAccessMs = 20),
            PreviewAudioCacheEntry("new", sizeBytes = 45, lastAccessMs = 30),
        )

        assertEquals(listOf("old"), planPreviewAudioCacheEvictions(entries, 90, setOf("new")))
    }

    @Test
    fun doesNotEvictWhenAlreadyWithinLimit() {
        val entries = listOf(
            PreviewAudioCacheEntry("a", sizeBytes = 30, lastAccessMs = 10),
            PreviewAudioCacheEntry("b", sizeBytes = 40, lastAccessMs = 20),
        )

        assertEquals(emptyList<String>(), planPreviewAudioCacheEvictions(entries, 70, setOf("b")))
    }

    @Test
    fun evictsAllUnprotectedEntriesWhenNecessary() {
        val entries = listOf(
            PreviewAudioCacheEntry("old", sizeBytes = 80, lastAccessMs = 10),
            PreviewAudioCacheEntry("new", sizeBytes = 64, lastAccessMs = 20),
        )

        assertEquals(listOf("old"), planPreviewAudioCacheEvictions(entries, 96, setOf("new")))
    }

    @Test
    fun preservesMultipleFilesPinnedByConcurrentProviderOpens() {
        val entries = listOf(
            PreviewAudioCacheEntry("a", sizeBytes = 64, lastAccessMs = 10),
            PreviewAudioCacheEntry("b", sizeBytes = 64, lastAccessMs = 20),
            PreviewAudioCacheEntry("old", sizeBytes = 40, lastAccessMs = 5),
        )

        assertEquals(
            listOf("old"),
            planPreviewAudioCacheEvictions(entries, 128, setOf("a", "b")),
        )
    }

    @Test
    fun cacheReturnsUnderLimitAfterAllOpenPinsAreReleased() {
        val entries = listOf(
            PreviewAudioCacheEntry("a", sizeBytes = 64, lastAccessMs = 10),
            PreviewAudioCacheEntry("b", sizeBytes = 64, lastAccessMs = 20),
        )

        assertEquals(emptyList<String>(), planPreviewAudioCacheEvictions(entries, 96, setOf("a", "b")))
        assertEquals(listOf("a"), planPreviewAudioCacheEvictions(entries, 96, emptySet()))
    }
}
