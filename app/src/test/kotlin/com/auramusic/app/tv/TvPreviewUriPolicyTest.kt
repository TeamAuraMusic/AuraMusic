package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPreviewUriPolicyTest {
    private val nowMillis = 1_700_000_000_000L

    @Test
    fun reusesOnlyHttpsAudioWithExpiryBeyondSafetyWindow() {
        assertTrue(
            canReuseTvAudioPreview(
                scheme = "https",
                mimeType = "audio/webm",
                expiresAtSeconds = ((nowMillis + 180_000L) / 1000L).toString(),
                nowMillis = nowMillis,
            )
        )
        assertFalse(
            canReuseTvAudioPreview(
                scheme = "https",
                mimeType = "audio/webm",
                expiresAtSeconds = ((nowMillis + TV_PREVIEW_EXPIRY_SAFETY_MS) / 1000L).toString(),
                nowMillis = nowMillis,
            )
        )
        assertFalse(
            canReuseTvAudioPreview(
                scheme = "https",
                mimeType = "video/mp4",
                expiresAtSeconds = ((nowMillis + 180_000L) / 1000L).toString(),
                nowMillis = nowMillis,
            )
        )
    }

    @Test
    fun unknownExpiryCanBePublishedButIsNeverReused() {
        assertTrue(canPublishTvPreview("https", "media.example", null, nowMillis))
        assertFalse(canReuseTvAudioPreview("https", "audio/webm", null, nowMillis))
    }

    @Test
    fun rejectsInvalidOrNearExpiryAndNonHttpsPreviewUris() {
        assertFalse(canPublishTvPreview("http", "media.example", null, nowMillis))
        assertFalse(canPublishTvPreview("https", null, null, nowMillis))
        assertFalse(canPublishTvPreview("https", "media.example", "not-a-time", nowMillis))
        assertFalse(
            canPublishTvPreview(
                "https",
                "media.example",
                ((nowMillis + 30_000L) / 1000L).toString(),
                nowMillis,
            )
        )
    }

    @Test
    fun audioOnlyProjectivyPreviewNormalizesOtherAudioFormats() {
        assertEquals(ProjectivyAudioPreviewPreparation.DIRECT_MP4,
            projectivyAudioPreviewPreparation("audio/mp4"))
        assertEquals(ProjectivyAudioPreviewPreparation.DIRECT_MP4,
            projectivyAudioPreviewPreparation("AUDIO/MP4"))
        assertEquals(ProjectivyAudioPreviewPreparation.TRANSCODE_TO_MP4,
            projectivyAudioPreviewPreparation("audio/webm"))
        assertEquals(ProjectivyAudioPreviewPreparation.TRANSCODE_TO_MP4,
            projectivyAudioPreviewPreparation("audio/mpeg"))
        assertEquals(ProjectivyAudioPreviewPreparation.UNSUPPORTED,
            projectivyAudioPreviewPreparation("video/mp4"))
        assertEquals(ProjectivyAudioPreviewPreparation.UNSUPPORTED,
            projectivyAudioPreviewPreparation(null))
    }

    @Test
    fun publisherKeepsFastAudioProviderAndOnlyUsesVideoFallbackForUnsupportedSources() {
        assertEquals(ProjectivyPreviewUriMode.AUDIO_ONLY_PROVIDER, projectivyPreviewUriMode("audio/mp4"))
        assertEquals(ProjectivyPreviewUriMode.AUDIO_ONLY_PROVIDER, projectivyPreviewUriMode("audio/webm"))
        assertEquals(ProjectivyPreviewUriMode.GENERATED_VIDEO_PROVIDER, projectivyPreviewUriMode("video/mp4"))
        assertEquals(ProjectivyPreviewUriMode.GENERATED_VIDEO_PROVIDER, projectivyPreviewUriMode(null))
    }

    @Test
    fun normalizedOutputMustContainOnlyOneAacTrack() {
        assertTrue(hasProjectivyCompatibleAudioOnlyTracks(listOf("audio/mp4a-latm")))
        assertFalse(hasProjectivyCompatibleAudioOnlyTracks(emptyList()))
        assertFalse(hasProjectivyCompatibleAudioOnlyTracks(listOf("audio/opus")))
        assertFalse(hasProjectivyCompatibleAudioOnlyTracks(listOf("audio/mp4a-latm", "video/avc")))
    }
}
