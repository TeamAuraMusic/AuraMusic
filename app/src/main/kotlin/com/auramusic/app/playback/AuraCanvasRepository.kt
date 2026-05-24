/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.playback

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Fetches and caches the AuraCanvas manifest – a public, no-auth JSON list of
 * looping MP4 videos to show behind the now-playing artwork (Spotify-Canvas-style).
 *
 * The manifest is hosted at https://canvas.auramusic.site/canvas.json
 * (maintained in https://github.com/TeamAuraMusic/AuraMusicCanvas).
 *
 * Matching is fuzzy on normalised song + artist.
 */
object AuraCanvasRepository {

    private const val MANIFEST_URL = "https://canvas.auramusic.site/canvas.json"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6h
    private const val NEGATIVE_CACHE_TTL_MS = 60 * 60 * 1000L // 1h

    @Serializable
    data class CanvasManifest(
        val version: Int = 1,
        val updatedAt: String? = null,
        val items: List<CanvasItem> = emptyList(),
    )

    @Serializable
    data class CanvasItem(
        val song: String,
        val artist: String,
        val url: String,
        val trackId: String? = null,
        val duration: Int? = null,
    )

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val mutex = Mutex()
    @Volatile private var cachedItems: List<CanvasItem>? = null
    @Volatile private var cachedAt: Long = 0L
    @Volatile private var lastFailureAt: Long = 0L

    /** In-memory cache of failed lookups (song+artist key) to avoid re-scanning. */
    private val negativeLookups = mutableMapOf<String, Long>()

    private suspend fun ensureManifest(): List<CanvasItem> = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedItems
        if (cached != null && now - cachedAt < CACHE_TTL_MS) return@withLock cached
        // Don't hammer if the last attempt just failed.
        if (cached != null && now - lastFailureAt < NEGATIVE_CACHE_TTL_MS) return@withLock cached
        try {
            val manifest: CanvasManifest = withContext(Dispatchers.IO) {
                client.get(MANIFEST_URL).body()
            }
            cachedItems = manifest.items
            cachedAt = now
            Timber.d("AuraCanvas: loaded ${manifest.items.size} entries from manifest")
            manifest.items
        } catch (t: Throwable) {
            Timber.w(t, "AuraCanvas: failed to load manifest")
            lastFailureAt = now
            cached ?: emptyList()
        }
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("\\([^)]*\\)"), " ") // strip parenthesised qualifiers
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("\\bfeat\\.?|\\bft\\.?|\\bwith\\b"), " ")
            .replace(Regex("\\b(remaster(ed)?|live|explicit|official video|lyrics|audio|hd|hq)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun artistMatches(haystackArtists: String, needleArtist: String): Boolean {
        if (haystackArtists.isBlank() || needleArtist.isBlank()) return false
        if (haystackArtists == needleArtist) return true
        // primary artist often appears first in either; compare token sets
        val a = haystackArtists.split(' ').filter { it.length > 1 }.toSet()
        val b = needleArtist.split(' ').filter { it.length > 1 }.toSet()
        if (a.isEmpty() || b.isEmpty()) return false
        val overlap = a.intersect(b).size
        return overlap > 0 && overlap >= minOf(a.size, b.size) / 2
    }

    /**
     * Find a canvas video URL for the given song/artist (or null if none).
     *
     * Matching is intentionally generous: case-insensitive, parenthesised
     * qualifiers stripped, multi-artist strings handled by token overlap.
     * Safe to call on every track change – results are cached.
     */
    suspend fun findCanvasUrl(title: String?, artist: String?): String? {
        if (title.isNullOrBlank() || artist.isNullOrBlank()) return null
        val normTitle = normalize(title)
        val normArtist = normalize(artist)
        if (normTitle.isBlank() || normArtist.isBlank()) return null

        val key = "$normTitle\u0001$normArtist"
        val now = System.currentTimeMillis()
        synchronized(negativeLookups) {
            val cachedMissAt = negativeLookups[key]
            if (cachedMissAt != null && now - cachedMissAt < NEGATIVE_CACHE_TTL_MS) {
                return null
            }
        }

        val items = ensureManifest()
        val hit = items.firstOrNull { item ->
            val itemSong = normalize(item.song)
            val itemArtist = normalize(item.artist)
            (itemSong == normTitle || itemSong.contains(normTitle) || normTitle.contains(itemSong)) &&
                artistMatches(itemArtist, normArtist)
        }

        if (hit == null) {
            synchronized(negativeLookups) { negativeLookups[key] = now }
        }
        return hit?.url
    }
}
