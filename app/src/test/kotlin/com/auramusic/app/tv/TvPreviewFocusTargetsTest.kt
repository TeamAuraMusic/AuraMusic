package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPreviewFocusTargetsTest {
    @Test
    fun matchesAuraMusicChannelRowInFocusedCardAncestors() {
        assertTrue(
            TvPreviewFocusTargets.isInChannelRow(
                expectedChannelNames = setOf("AuraMusic"),
                ancestorDescriptions = listOf(null, "", "AuraMusic"),
            ),
        )
    }

    @Test
    fun rejectsMatchingSongTitleOutsideAuraMusicRow() {
        assertFalse(
            TvPreviewFocusTargets.isInChannelRow(
                expectedChannelNames = setOf("AuraMusic"),
                ancestorDescriptions = listOf(null, "Continue Watching", "Other App"),
            ),
        )
    }

    @Test
    fun rejectsBlankOrDifferentChannelNames() {
        assertFalse(TvPreviewFocusTargets.isInChannelRow(emptySet(), listOf("AuraMusic")))
        assertFalse(TvPreviewFocusTargets.isInChannelRow(setOf("AuraMusic"), listOf("auramusic")))
    }

    @Test
    fun retainsArtworkWhenDuplicateTitlesShareTheSameArtwork() {
        assertEquals("content://art/shared", TvPreviewFocusTargets.unambiguousArtworkUri(
            listOf("content://art/shared", "content://art/shared"),
        ))
    }

    @Test
    fun leavesOriginalCardArtworkWhenDuplicateTitlesHaveDifferentArtwork() {
        assertNull(TvPreviewFocusTargets.unambiguousArtworkUri(
            listOf("content://art/one", "content://art/two"),
        ))
    }

    @Test
    fun leavesOriginalArtworkWhenDuplicateMetadataIncludesMissingArtwork() {
        assertNull(TvPreviewFocusTargets.unambiguousArtworkUri(listOf("content://art/one", null)))
    }

    @Test
    fun allowsFocusPrefetchOnlyForAuraMusicLocalAudioOnlyUri() {
        assertTrue(
            TvPreviewFocusTargets.isLocalAudioOnlyPreviewUri(
                "content", "com.auramusic.app.debug.tvpreview", "audio", "audio/mp4",
                "com.auramusic.app.debug.tvpreview",
            ),
        )
        assertFalse(
            TvPreviewFocusTargets.isLocalAudioOnlyPreviewUri(
                "https", "media.example", "audio", "audio/mp4",
                "com.auramusic.app.debug.tvpreview",
            ),
        )
        assertFalse(
            TvPreviewFocusTargets.isLocalAudioOnlyPreviewUri(
                "content", "other.provider", "audio", "audio/mp4",
                "com.auramusic.app.debug.tvpreview",
            ),
        )
        assertFalse(
            TvPreviewFocusTargets.isLocalAudioOnlyPreviewUri(
                "content", "com.auramusic.app.debug.tvpreview", null, null,
                "com.auramusic.app.debug.tvpreview",
            ),
        )
    }
}
