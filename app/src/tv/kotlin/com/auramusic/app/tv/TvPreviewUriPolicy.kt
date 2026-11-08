package com.auramusic.app.tv

internal const val TV_PREVIEW_EXPIRY_SAFETY_MS = 2 * 60 * 1000L

internal fun canReuseTvAudioPreview(
    scheme: String?,
    mimeType: String?,
    expiresAtSeconds: String?,
    nowMillis: Long,
): Boolean =
    scheme.equals("https", ignoreCase = true) &&
        mimeType?.startsWith("audio/", ignoreCase = true) == true &&
        previewExpiryMillis(expiresAtSeconds)?.let { it > nowMillis + TV_PREVIEW_EXPIRY_SAFETY_MS } == true

internal fun canPublishTvPreview(
    scheme: String?,
    host: String?,
    expiresAtSeconds: String?,
    nowMillis: Long,
): Boolean {
    if (!scheme.equals("https", ignoreCase = true) || host.isNullOrBlank()) return false
    if (expiresAtSeconds == null) return true
    return previewExpiryMillis(expiresAtSeconds)
        ?.let { it > nowMillis + TV_PREVIEW_EXPIRY_SAFETY_MS } == true
}

internal enum class ProjectivyAudioPreviewPreparation {
    DIRECT_MP4,
    TRANSCODE_TO_MP4,
    UNSUPPORTED,
}

internal enum class ProjectivyPreviewUriMode {
    AUDIO_ONLY_PROVIDER,
    GENERATED_VIDEO_PROVIDER,
}

/** Projectivy 4.73 accepts audio-only AAC/MP4; the focus overlay supplies in-card artwork and motion. */
internal fun projectivyAudioPreviewPreparation(mimeType: String?): ProjectivyAudioPreviewPreparation = when {
    mimeType.equals("audio/mp4", ignoreCase = true) -> ProjectivyAudioPreviewPreparation.DIRECT_MP4
    mimeType?.startsWith("audio/", ignoreCase = true) == true ->
        ProjectivyAudioPreviewPreparation.TRANSCODE_TO_MP4
    else -> ProjectivyAudioPreviewPreparation.UNSUPPORTED
}

/** Use the fast audio-only provider; the focused-card overlay supplies the animated artwork. */
internal fun projectivyPreviewUriMode(mimeType: String?): ProjectivyPreviewUriMode =
    if (projectivyAudioPreviewPreparation(mimeType) == ProjectivyAudioPreviewPreparation.UNSUPPORTED) {
        ProjectivyPreviewUriMode.GENERATED_VIDEO_PROVIDER
    } else {
        ProjectivyPreviewUriMode.AUDIO_ONLY_PROVIDER
    }

internal fun hasProjectivyCompatibleAudioOnlyTracks(trackMimeTypes: List<String>): Boolean =
    trackMimeTypes.size == 1 && trackMimeTypes.single().equals("audio/mp4a-latm", ignoreCase = true)

private fun previewExpiryMillis(expiresAtSeconds: String?): Long? =
    expiresAtSeconds?.toLongOrNull()?.let { seconds ->
        seconds.takeIf { it > 0L && it <= Long.MAX_VALUE / 1000L }?.times(1000L)
    }
