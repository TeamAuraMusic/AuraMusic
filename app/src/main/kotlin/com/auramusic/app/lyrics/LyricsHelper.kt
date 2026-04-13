/**
 * AuraMusic Project (C) 2026
 * Licensed under GPL-3.0. See LICENSE file for details.
 */

package com.auramusic.app.lyrics

import android.content.Context
import android.util.LruCache
import com.auramusic.app.constants.LyricsProviderOrderKey
import com.auramusic.app.constants.PreferredLyricsProvider
import com.auramusic.app.constants.PreferredLyricsProviderKey
import com.auramusic.app.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.auramusic.app.extensions.toEnum
import com.auramusic.app.models.MediaMetadata
import com.auramusic.app.utils.NetworkConnectivityObserver
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    // Initialize with default providers from registry
    @Volatile
    private var lyricsProviders = LyricsProviderRegistry.getOrderedProviders(
        LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder())
    )
    @Volatile
    private var providersLoaded = false

    init {
        // Collect the flow to update lyricsProviders when preferences change
        CoroutineScope(SupervisorJob()).launch {
            context.dataStore.data
                .map { preferences ->
                    val providerOrder = preferences[LyricsProviderOrderKey] ?: ""
                    Timber.tag("LyricsHelper").d("Provider order from prefs: '$providerOrder'")
                    if (providerOrder.isNotBlank()) {
                        // Use the new provider order if available
                        LyricsProviderRegistry.getOrderedProviders(providerOrder)
                    } else {
                        // Fall back to preferred provider logic for backward compatibility
                        Timber.tag("LyricsHelper").d("Using fallback provider logic")
                        val preferredProvider = preferences[PreferredLyricsProviderKey]
                            .toEnum(PreferredLyricsProvider.LRCLIB)
                        when (preferredProvider) {
                            PreferredLyricsProvider.LRCLIB -> listOf(
                                LrcLibLyricsProvider,
                                BetterLyricsProvider,
                                RushLyricsProvider,
                                SimpMusicLyricsProvider,
                                KuGouLyricsProvider,
                            )
                            PreferredLyricsProvider.KUGOU -> listOf(
                                KuGouLyricsProvider,
                                BetterLyricsProvider,
                                RushLyricsProvider,
                                SimpMusicLyricsProvider,
                                LrcLibLyricsProvider,
                            )
                            PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                                BetterLyricsProvider,
                                RushLyricsProvider,
                                SimpMusicLyricsProvider,
                                LrcLibLyricsProvider,
                                KuGouLyricsProvider,
                            )
                            PreferredLyricsProvider.SIMPMUSIC -> listOf(
                                SimpMusicLyricsProvider,
                                BetterLyricsProvider,
                                RushLyricsProvider,
                                LrcLibLyricsProvider,
                                KuGouLyricsProvider,
                            )
                            PreferredLyricsProvider.RUSH_LYRICS -> listOf(
                                RushLyricsProvider,
                                BetterLyricsProvider,
                                SimpMusicLyricsProvider,
                                LrcLibLyricsProvider,
                                KuGouLyricsProvider,
                            )
                        }
                    }
                }.distinctUntilChanged()
                .collect { providers ->
                    Timber.tag("LyricsHelper").d("Updated providers: ${providers.map { it.name }}")
                    lyricsProviders = providers
                    providersLoaded = true
                }
        }
    }

    private val cache = LruCache<String, LyricsWithProvider>(MAX_CACHE_SIZE)
    private val allLyricsCache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        // Wait briefly for provider preferences to load on first call
        if (!providersLoaded) {
            var waited = 0
            while (!providersLoaded && waited < 500) {
                delay(10)
                waited += 10
            }
        }

        val cached = cache.get(mediaMetadata.id)
        if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
            Timber.tag("LyricsHelper").d("Returning cached lyrics from ${cached.provider}")
            return cached
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            Timber.tag("LyricsHelper").w("Network not available, returning not found")
            // Still proceed but return not found to avoid hanging
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val scope = CoroutineScope(SupervisorJob())
        val deferred = scope.async {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val artistString = mediaMetadata.artists.joinToString { it.name }
            Timber.tag("LyricsHelper").d("Searching lyrics for: title='$cleanedTitle', artist='$artistString'")
            Timber.tag("LyricsHelper").d("Available providers: ${lyricsProviders.map { it.name }}")
            for (provider in lyricsProviders) {
                val isEnabled = provider.isEnabled(context)
                Timber.tag("LyricsHelper").d("Provider ${provider.name} isEnabled=$isEnabled")
                if (isEnabled) {
                    try {
                        Timber.tag("LyricsHelper")
                            .d("Trying provider: ${provider.name} for $cleanedTitle")
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            cleanedTitle,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                            mediaMetadata.album?.title,
                        )
                        result.onSuccess { lyrics ->
                            Timber.tag("LyricsHelper").i("Successfully got lyrics from ${provider.name}")
                            return@async LyricsWithProvider(lyrics, provider.name)
                        }.onFailure { e ->
                            Timber.tag("LyricsHelper").w("${provider.name} failed: ${e.message}")
                            reportException(e)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        Timber.tag("LyricsHelper").w("${provider.name} threw exception: ${e.message}")
                        reportException(e)
                    }
                } else {
                    Timber.tag("LyricsHelper").d("Provider ${provider.name} is disabled, skipping")
                }
            }
            Timber.tag("LyricsHelper").w("All providers failed or disabled for ${mediaMetadata.title}")
            return@async LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val result = deferred.await()
        scope.cancel()
        
        // Cache the result if lyrics were found
        if (result.lyrics != LYRICS_NOT_FOUND) {
            cache.put(mediaMetadata.id, result)
            Timber.tag("LyricsHelper").d("Cached lyrics from ${result.provider} for ${mediaMetadata.id}")
        }
        
        return result
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        allLyricsCache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            // Still try to proceed in case of false negative
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            allLyricsCache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
