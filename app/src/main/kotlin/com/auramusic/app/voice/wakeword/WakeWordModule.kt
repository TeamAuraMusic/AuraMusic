package com.auramusic.app.voice.wakeword

import android.content.Context
import com.auramusic.app.voice.VoiceCommandManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WakeWordModule {

    @Provides
    @Singleton
    fun providePorcupineWakeWordDetector(
        @ApplicationContext context: Context,
    ): PorcupineWakeWordDetector {
        return PorcupineWakeWordDetector(context)
    }
}
