/**
 * AuraMusic Project (C) 2026
 * Licensed under GPL-3.0. See LICENSE file for details.
 */

package com.auramusic.app.lyrics

import android.content.Context
import android.util.LruCache
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 25_000L
private const val PER_PROVIDER_TIMEOUT_MS = 8_000L
private const val PROVIDER_NONE = "Unknown"

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val cache = LruCache<String, LyricsWithProvider>(MAX_CACHE_SIZE)
    private val allLyricsCache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    /**
     * Fetch lyrics for [mediaMetadata], strictly honouring the user's preferred
     * provider order at the moment of the call.
     *
     * The previous implementation collected the preference flow asynchronously
     * into a member field, which meant that the very first call (and any call
     * made just after the user changed the priority) could iterate over a stale
     * list and return lyrics from the wrong provider.  We now resolve the
     * ordered, enabled provider list synchronously per call.
     */
    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val orderedProviders = resolveOrderedProviders()
        val preferredProviderName = orderedProviders.firstOrNull()?.name ?: PROVIDER_NONE

        // Only honour the in-memory cache when it actually matches the
        // currently-preferred provider, otherwise the user thinks they changed
        // priority but keeps seeing the old result.
        cache.get(mediaMetadata.id)?.let { cached ->
            if (cached.lyrics != LYRICS_NOT_FOUND && cached.provider == preferredProviderName) {
                Timber.tag(TAG).d("Returning cached lyrics from preferred provider ${cached.provider}")
                return cached
            }
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) {
            Timber.tag(TAG).w("Network not available, returning not found")
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
        val artistString = mediaMetadata.artists.joinToString { it.name }
        val durationSeconds = if (mediaMetadata.duration > 0) mediaMetadata.duration / 1000 else mediaMetadata.duration
        val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

        Timber.tag(TAG).d("Searching lyrics for: '$cleanedTitle' by '$artistString'")
        Timber.tag(TAG).d("Enabled providers in order: ${enabledProviders.joinToString { it.name }}")

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            for (provider in enabledProviders) {
                Timber.tag(TAG).d("Trying provider: ${provider.name}")
                val providerResult = try {
                    withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            cleanedTitle,
                            artistString,
                            durationSeconds,
                            mediaMetadata.album?.title,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w("${provider.name} threw: ${e.message}")
                    reportException(e)
                    null
                }

                if (providerResult != null && providerResult.isSuccess) {
                    val lyrics = providerResult.getOrNull()
                    if (!lyrics.isNullOrBlank() && lyrics != LYRICS_NOT_FOUND) {
                        Timber.tag(TAG).i("Got lyrics from ${provider.name}")
                        return@withTimeoutOrNull LyricsWithProvider(lyrics, provider.name)
                    }
                    Timber.tag(TAG).w("${provider.name} returned success with empty/NOT_FOUND body")
                } else {
                    val errorMsg = providerResult?.exceptionOrNull()?.message ?: "timeout or no result"
                    Timber.tag(TAG).w("${provider.name} failed: $errorMsg")
                }
            }
            Timber.tag(TAG).w("All providers failed for '${mediaMetadata.title}'")
            LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        } ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)

        if (result.lyrics != LYRICS_NOT_FOUND) {
            cache.put(mediaMetadata.id, result)
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
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        val orderedProviders = resolveOrderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            orderedProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            allLyricsCache.put(cacheKey, allResult)
        }
        currentLyricsJob?.join()
    }

    /**
     * Drop any cached lyrics for [mediaId] so the next [getLyrics] call goes
     * back to the network. Called by the refetch / retry path so the user's
     * action actually triggers a fresh request.
     */
    fun invalidateCache(mediaId: String?) {
        if (mediaId == null) {
            cache.evictAll()
            allLyricsCache.evictAll()
        } else {
            cache.remove(mediaId)
        }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    /**
     * Build the ordered, user-preferred provider list straight from DataStore
     * at call time, so changes to "preferred provider" or to the drag-and-drop
     * order take effect immediately on the next fetch.
     */
    private suspend fun resolveOrderedProviders(): List<LyricsProvider> {
        val preferences = context.dataStore.data
            .map { it }
            .first()
        return resolveOrderedProviders(preferences)
    }

    private fun resolveOrderedProviders(preferences: Preferences): List<LyricsProvider> {
        val defaultOrder = LyricsProviderRegistry.serializeProviderOrder(
            LyricsProviderRegistry.getDefaultProviderOrder()
        )
        val providerOrder = preferences[LyricsProviderOrderKey] ?: defaultOrder
        val preferredProvider = preferences[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.BETTER_LYRICS)

        val preferredInstance = providerInstanceOf(preferredProvider)

        return if (providerOrder != defaultOrder) {
            // The user has reordered the list explicitly — keep their order
            // but always promote their preferred provider to the front so the
            // "first lyrics provider" setting is never overruled.
            val customOrder = LyricsProviderRegistry.getOrderedProviders(providerOrder)
            val rest = customOrder.filter { it.name != preferredInstance.name }
            listOf(preferredInstance) + rest
        } else {
            // No custom order — derive the order from the preferred provider.
            when (preferredProvider) {
                PreferredLyricsProvider.LRCLIB -> listOf(
                    LrcLibLyricsProvider, BetterLyricsProvider, RushLyricsProvider,
                    PaxsenixLyricsProvider, SimpMusicLyricsProvider, KuGouLyricsProvider,
                )
                PreferredLyricsProvider.KUGOU -> listOf(
                    KuGouLyricsProvider, BetterLyricsProvider, RushLyricsProvider,
                    PaxsenixLyricsProvider, SimpMusicLyricsProvider, LrcLibLyricsProvider,
                )
                PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                    BetterLyricsProvider, RushLyricsProvider, PaxsenixLyricsProvider,
                    SimpMusicLyricsProvider, LrcLibLyricsProvider, KuGouLyricsProvider,
                )
                PreferredLyricsProvider.SIMPMUSIC -> listOf(
                    SimpMusicLyricsProvider, BetterLyricsProvider, RushLyricsProvider,
                    PaxsenixLyricsProvider, LrcLibLyricsProvider, KuGouLyricsProvider,
                )
                PreferredLyricsProvider.RUSH_LYRICS -> listOf(
                    RushLyricsProvider, BetterLyricsProvider, SimpMusicLyricsProvider,
                    PaxsenixLyricsProvider, LrcLibLyricsProvider, KuGouLyricsProvider,
                )
                PreferredLyricsProvider.PAXSENIX -> listOf(
                    PaxsenixLyricsProvider, BetterLyricsProvider, RushLyricsProvider,
                    SimpMusicLyricsProvider, LrcLibLyricsProvider, KuGouLyricsProvider,
                )
            }
        }
    }

    private fun providerInstanceOf(preferred: PreferredLyricsProvider): LyricsProvider = when (preferred) {
        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
        PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
        PreferredLyricsProvider.RUSH_LYRICS -> RushLyricsProvider
        PreferredLyricsProvider.PAXSENIX -> PaxsenixLyricsProvider
    }

    companion object {
        private const val TAG = "LyricsHelper"
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
