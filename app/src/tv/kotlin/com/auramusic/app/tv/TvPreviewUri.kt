package com.auramusic.app.tv

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

/** Bump when the encoded artwork, indicator, or media container changes. */
internal const val TV_PREVIEW_PIPELINE_VERSION = 32

internal fun tvPreviewSourceRevision(
    mediaId: String,
    artworkUri: String,
    durationMs: Long,
    audioSourceUri: String? = null,
    pipelineVersion: Int = TV_PREVIEW_PIPELINE_VERSION,
): String = listOf(
    pipelineVersion.toString(),
    mediaId,
    artworkUri.substringBefore('?'),
    durationMs.toString(),
    audioSourceUri.orEmpty(),
).joinToString("\n")

internal object TvPreviewUri {
    fun forProgram(context: Context, internalProviderId: String, revision: String): Uri =
        Uri.Builder()
            .scheme("content")
            .authority("${context.packageName}.tvpreview")
            .appendPath("track")
            .appendPath(internalProviderId)
            .appendQueryParameter("v", revision)
            .build()

    fun forAudioOnlyProgram(context: Context, internalProviderId: String, revision: String): Uri =
        forProgram(context, internalProviderId, revision)
            .buildUpon()
            .appendQueryParameter(AUDIO_ONLY_PARAMETER, AUDIO_ONLY_VALUE)
            .appendQueryParameter(MIME_PARAMETER, AUDIO_MP4_MIME)
            .build()

    fun waveform(context: Context, internalProviderId: String, revision: String): Uri =
        Uri.Builder()
            .scheme("content")
            .authority("${context.packageName}.tvpreview")
            .appendPath("track")
            .appendPath(internalProviderId)
            .appendPath("waveform.gif")
            .appendQueryParameter("v", revision)
            .build()

    fun revision(
        mediaId: String,
        artworkUri: String,
        durationMs: Long,
        audioSourceUri: String? = null,
    ): String {
        val identity = tvPreviewSourceRevision(mediaId, artworkUri, durationMs, audioSourceUri)
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private const val AUDIO_ONLY_PARAMETER = "mode"
    private const val AUDIO_ONLY_VALUE = "audio"
    private const val MIME_PARAMETER = "mime"
    private const val AUDIO_MP4_MIME = "audio/mp4"
}
