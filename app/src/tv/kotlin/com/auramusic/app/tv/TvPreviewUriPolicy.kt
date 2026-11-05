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

private fun previewExpiryMillis(expiresAtSeconds: String?): Long? =
    expiresAtSeconds?.toLongOrNull()?.let { seconds ->
        seconds.takeIf { it > 0L && it <= Long.MAX_VALUE / 1000L }?.times(1000L)
    }
