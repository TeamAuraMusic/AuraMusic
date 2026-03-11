package com.auramusic.app.recognition

import timber.log.Timber

/**
 * Interface for generating Shazam-compatible audio fingerprints.
 * 
 * This class first tries to use the native vibra_fp library, 
 * but falls back to the pure Kotlin implementation if the native library fails.
 */
object VibraSignature {

    const val REQUIRED_SAMPLE_RATE = 16_000

    // Whether native library is available
    private var nativeAvailable = false

    init {
        try {
            // Try to load the native library
            System.loadLibrary("vibra_fp")
            // Test if the native function works
            val testBytes = ByteArray(4) { 0 }
            fromINative(testBytes)
            nativeAvailable = true
            Timber.d("Native vibra_fp library loaded successfully")
        } catch (e: Throwable) {
            Timber.w(e, "Native vibra_fp library not available, using Kotlin implementation")
            nativeAvailable = false
        }
    }

    /**
     * Generates a Shazam signature from PCM audio data.
     * 
     * @param samples Raw PCM audio data (mono, 16-bit signed, 16kHz sample rate)
     * @return The encoded signature string suitable for Shazam API
     * @throws RuntimeException if signature generation fails
     */
    @JvmStatic
    fun fromI16(samples: ByteArray): String {
        return if (nativeAvailable) {
            try {
                fromINative(samples)
            } catch (e: Throwable) {
                Timber.w(e, "Native signature generation failed, falling back to Kotlin implementation")
                fromKotlin(samples)
            }
        } else {
            fromKotlin(samples)
        }
    }

    /**
     * Native implementation using the vibra_fp C++ library.
     */
    @JvmStatic
    private external fun fromINative(samples: ByteArray): String

    /**
     * Pure Kotlin implementation of Shazam signature generation.
     */
    private fun fromKotlin(samples: ByteArray): String {
        return ShazamSignatureGenerator.fromI16(samples)
    }
}
