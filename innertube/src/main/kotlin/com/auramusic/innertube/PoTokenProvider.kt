package com.auramusic.innertube

/**
 * Provides Proof of Origin (PO) tokens required by YouTube for certain InnerTube clients
 * (primarily WEB and WEB_REMIX) to obtain working playback URLs.
 *
 * Implementations typically use a WebView running BotGuard to solve the attestation challenge.
 *
 * PO tokens can be:
 * - Video-bound (recommended for current YouTube enforcement)
 * - VisitorData-bound (older behavior)
 *
 * Callers should request a fresh token per video when possible.
 */
interface PoTokenProvider {

    /**
     * Returns a PO token to be sent in `serviceIntegrityDimensions.poToken`
     * inside the `/player` request body.
     *
     * @param videoId the video for which the token is needed
     * @return base64-encoded PO token, or null if unavailable
     */
    suspend fun getPlayerPoToken(videoId: String): String?

    /**
     * Returns a PO token that should be appended as a `pot` query parameter
     * to the final adaptive format / videoplayback URLs (GVS token).
     *
     * Many clients need a separate token for the actual media delivery.
     */
    suspend fun getStreamingPoToken(videoId: String): String? = null
}
