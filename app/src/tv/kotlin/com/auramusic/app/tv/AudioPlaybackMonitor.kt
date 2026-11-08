package com.auramusic.app.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import androidx.annotation.RequiresApi

/** Reports system media activity; Android does not expose player UID in this public callback. */
@RequiresApi(26)
internal class AudioPlaybackMonitor(
    context: Context,
    handler: Handler,
    private val onStateChanged: (Boolean) -> Unit,
) : AutoCloseable {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            update(configs.orEmpty())
        }
    }

    init {
        audioManager.registerAudioPlaybackCallback(callback, handler)
        update(audioManager.activePlaybackConfigurations)
    }

    private fun update(configs: List<AudioPlaybackConfiguration>) {
        onStateChanged(configs.any { it.audioAttributes.usage == AudioAttributes.USAGE_MEDIA })
    }

    fun refresh() {
        update(audioManager.activePlaybackConfigurations)
    }

    override fun close() {
        audioManager.unregisterAudioPlaybackCallback(callback)
    }
}
