package com.auramusic.app.tv

import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Muxer
import androidx.media3.transformer.InAppFragmentedMp4Muxer

/** Writes independently playable fragments as soon as the encoder produces them. */
@OptIn(UnstableApi::class)
internal class TvPreviewMuxerFactory(videoDurationUs: Long) : Muxer.Factory {
    private val delegate = InAppFragmentedMp4Muxer.Factory(FRAGMENT_DURATION_MS)
        .setVideoDurationUs(videoDurationUs)

    override fun create(path: String): Muxer = delegate.create(path)

    override fun getSupportedSampleMimeTypes(trackType: Int) =
        delegate.getSupportedSampleMimeTypes(trackType)

    private companion object {
        const val FRAGMENT_DURATION_MS = 500L
    }
}
