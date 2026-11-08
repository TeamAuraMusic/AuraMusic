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
}
