package com.auramusic.app.voice.wakeword

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
     * Set a callback to be invoked when wake word is detected.
     */
    fun setOnWakeWordDetectedListener(callback: () -> Unit)
}
