package com.auramusic.app.voice.wakeword

import com.picovoice.porcupine.Porcupine
import com.picovoice.porcupine.PorcupineException

/**
 * Interface for always-on wake word detection.
 * Runs independently of the main voice command system.
 */
interface WakeWordDetector : AutoCloseable {
    /**
     * Starts the detector. Should be called after initialization.
     */
    fun start()

    /**
     * Stops the detector.
     */
    fun stop()

    /**
     * Callback invoked when the wake word is detected.
     * @param keyword The detected keyword identifier
     */
    fun onDetected(keyword: String)
}
