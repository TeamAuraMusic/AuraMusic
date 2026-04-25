package com.auramusic.app.eq


import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.auramusic.app.eq.audio.CustomEqualizerAudioProcessor
import com.auramusic.app.eq.audio.VocalSuppressionAudioProcessor
import com.auramusic.app.eq.data.ParametricEQ
import com.auramusic.app.eq.data.SavedEQProfile
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing custom EQ using ExoPlayer's AudioProcessor
 * Supports 10+ band Parametric EQ format (APO)
 */
@Singleton
class EqualizerService @Inject constructor() {

    @SuppressLint("UnsafeOptInUsageError")
    private val audioProcessors = mutableListOf<CustomEqualizerAudioProcessor>()

    @SuppressLint("UnsafeOptInUsageError")
    private val vocalSuppressionProcessors = mutableListOf<VocalSuppressionAudioProcessor>()

    private var pendingProfile: SavedEQProfile? = null
    private var shouldDisable: Boolean = false
    private var vocalSuppressionEnabled = false
    private var vocalSuppressionStrength = 0.7f

    companion object {
        private const val TAG = "EqualizerService"
    }

    /**
     * Add an audio processor instance
     * This should be called when ExoPlayer is initialized
     */
    @OptIn(UnstableApi::class)
    fun addAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.add(processor)
        Timber.tag(TAG).d("Audio processor added. Total: ${audioProcessors.size}")

        // Apply pending profile if one was set before processor was available
        if (shouldDisable) {
            processor.disable()
            // Don't clear shouldDisable here, as we might add more processors
        } else if (pendingProfile != null) {
            val profile = pendingProfile!!
            applyProfileToProcessor(processor, profile)
            // Don't clear pendingProfile here
        }
    }

    /**
     * Remove an audio processor instance
     */
    fun removeAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.remove(processor)
    }

    /**
     * Apply an EQ profile
     * If audio processor is not set, stores as pending profile
     */
    @OptIn(UnstableApi::class)
    fun applyProfile(profile: SavedEQProfile): Result<Unit> {
        if (audioProcessors.isEmpty()) {
            Timber.tag(TAG)
                .w("No audio processors set yet. Storing profile as pending: ${profile.name}")
            pendingProfile = profile
            shouldDisable = false
            return Result.success(Unit)
        }

        pendingProfile = profile // Keep it for future processors
        shouldDisable = false
        
        var success = true
        var lastError: Exception? = null

        audioProcessors.forEach { processor ->
            try {
                applyProfileToProcessor(processor, profile)
            } catch (e: Exception) {
                success = false
                lastError = e
            }
        }

        return if (success) Result.success(Unit) else Result.failure(lastError ?: Exception("Unknown error"))
    }

    private fun applyProfileToProcessor(processor: CustomEqualizerAudioProcessor, profile: SavedEQProfile) {
        val parametricEQ = ParametricEQ(
            preamp = profile.preamp,
            bands = profile.bands
        )
        processor.applyProfile(parametricEQ)
    }

    /**
     * Disable the equalizer (flat response)
     * If audio processor is not set, stores pending disable request
     */
    @OptIn(UnstableApi::class)
    fun disable() {
        if (audioProcessors.isEmpty()) {
            Timber.tag(TAG).w("No audio processors set yet. Storing disable as pending")
            shouldDisable = true
            pendingProfile = null
            return
        }

        shouldDisable = true // Keep state
        pendingProfile = null

        audioProcessors.forEach { processor ->
            try {
                processor.disable()
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to disable equalizer: ${e.message}")
            }
        }
        Timber.tag(TAG).d("Equalizer disabled on all processors")
    }

    /**
     * Check if audio processor is set
     */
    fun isInitialized(): Boolean {
        return audioProcessors.isNotEmpty()
    }

    /**
     * Check if equalizer is enabled
     */
    @OptIn(UnstableApi::class)
    fun isEnabled(): Boolean {
        return audioProcessors.any { it.isEnabled() }
    }

    /**
     * Get information about the current EQ capabilities
     */
    fun getEqualizerInfo(): EqualizerInfo {
        return EqualizerInfo(
            supportsUnlimitedBands = true,
            maxBands = Int.MAX_VALUE,
            description = "Custom ExoPlayer AudioProcessor with biquad filters"
        )
    }

    /**
     * Add a vocal suppression audio processor instance
     */
    @OptIn(UnstableApi::class)
    fun addVocalSuppressionProcessor(processor: VocalSuppressionAudioProcessor) {
        vocalSuppressionProcessors.add(processor)
        Timber.tag(TAG).d("Vocal suppression processor added. Total: ${vocalSuppressionProcessors.size}")

        // Apply current vocal suppression state
        if (vocalSuppressionEnabled) {
            processor.enable(vocalSuppressionStrength)
        } else {
            processor.disable()
        }
    }

    /**
     * Remove a vocal suppression audio processor instance
     */
    @OptIn(UnstableApi::class)
    fun removeVocalSuppressionProcessor(processor: VocalSuppressionAudioProcessor) {
        vocalSuppressionProcessors.remove(processor)
        Timber.tag(TAG).d("Vocal suppression processor removed. Total: ${vocalSuppressionProcessors.size}")
    }

    /**
     * Enable vocal suppression
     */
    @OptIn(UnstableApi::class)
    fun enableVocalSuppression(strength: Float = 0.7f): Result<Unit> = runCatching {
        vocalSuppressionStrength = strength.coerceIn(0f, 1f)
        vocalSuppressionEnabled = true

        vocalSuppressionProcessors.forEach { processor ->
            processor.enable(strength)
        }

        Timber.tag(TAG).d("Vocal suppression enabled with strength: $strength")
    }

    /**
     * Disable vocal suppression
     */
    @OptIn(UnstableApi::class)
    fun disableVocalSuppression(): Result<Unit> = runCatching {
        vocalSuppressionEnabled = false

        vocalSuppressionProcessors.forEach { processor ->
            processor.disable()
        }

        Timber.tag(TAG).d("Vocal suppression disabled")
    }

    /**
     * Check if vocal suppression is enabled
     */
    fun isVocalSuppressionEnabled(): Boolean = vocalSuppressionEnabled

    /**
     * Get current vocal suppression strength
     */
    fun getVocalSuppressionStrength(): Float = vocalSuppressionStrength

    /**
     * Release resources (not needed for AudioProcessor, but kept for API compatibility)
     */
    fun release() {
        // AudioProcessor is managed by ExoPlayer, we just clear our reference
        audioProcessors.clear()
        vocalSuppressionProcessors.clear()
        Timber.tag(TAG).d("Audio processor references cleared")
    }
}

/**
 * Information about equalizer capabilities
 */
data class EqualizerInfo(
    val supportsUnlimitedBands: Boolean,
    val maxBands: Int,
    val description: String
)
