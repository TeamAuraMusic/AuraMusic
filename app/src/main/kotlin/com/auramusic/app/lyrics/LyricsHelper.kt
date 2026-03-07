/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    // Initialize with default providers - will be updated by the Flow when preferences are loaded
    private var lyricsProviders: List<LyricsProvider> =
        listOf(
            BetterLyricsProvider,
            SimpMusicLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            LyricsPlusProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )

    val preferred =
        context.dataStore.data
            .map { preferences ->
                val providerOrder = preferences[LyricsProviderOrderKey] ?: ""
                if (providerOrder.isNotBlank()) {
                    // Use custom order from drag-and-drop
                    LyricsProviderRegistry.getOrderedProviders(providerOrder)
                } else {
                    // Fall back to preferred provider logic
                    val preferredProvider = preferences[PreferredLyricsProviderKey]
                        .toEnum(PreferredLyricsProvider.BETTER_LYRICS)
                    when (preferredProvider) {
                        PreferredLyricsProvider.LRCLIB -> listOf(
                            LrcLibLyricsProvider,
                            BetterLyricsProvider,
                            SimpMusicLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.KUGOU -> listOf(
                            KuGouLyricsProvider,
                            BetterLyricsProvider,
                            SimpMusicLyricsProvider,
                            LrcLibLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                            BetterLyricsProvider,
                            SimpMusicLyricsProvider,
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.SIMPMUSIC -> listOf(
                            SimpMusicLyricsProvider,
                            BetterLyricsProvider,
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.LYRICS_PLUS -> listOf(
                            LyricsPlusProvider,
                            BetterLyricsProvider,
                            SimpMusicLyricsProvider,
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    }
                }
            }.distinctUntilChanged()
            .map { providers ->
                lyricsProviders = providers
            }

    init {
        // Trigger the Flow to start collecting
        // This ensures lyricsProviders gets updated from preferences
        kotlinx.coroutines.GlobalScope.launch {
            preferred.collect { }
        }
    }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
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
            // Still proceed but return not found to avoid hanging
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val scope = CoroutineScope(SupervisorJob())
        val deferred = scope.async {
            for (provider in lyricsProviders) {
                if (provider.isEnabled(context)) {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                            mediaMetadata.album?.title,
                        )
                        result.onSuccess { lyrics ->
                            return@async LyricsWithProvider(lyrics, provider.name)
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            return@async LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val result = deferred.await()
        scope.cancel()
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
        cache.get(cacheKey)?.let { results ->
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
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, duration, album) { lyrics ->
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
            cache.put(cacheKey, allResult)
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