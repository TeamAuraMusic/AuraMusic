/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.di

import com.auramusic.app.voice.VoiceCommandManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VoiceCommandEntryPoint {
    fun voiceCommandManager(): VoiceCommandManager
}
