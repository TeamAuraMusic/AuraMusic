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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Single source of truth for AuraCanvas video lookups.
 *
 * Resolution chain for a (title, artist, album) query:
 *   1) Static community manifest at https://auramusiccanvas.vercel.app/canvas.json
 *   2) Remote AuraMusicCanvasServer (https://auramusiccanvasserver.onrender.com)
 *      which does the YT-Music → Spotify trackId search server-side and returns
 *      the canvas URL from the internal `canvaz-cache` endpoint.
 *
 * Both layers are cached in memory (24h positive, 1h negative) so a repeat
 * track only hits the network once.
 *
 * No Spotify developer key, sp_dc, or protobuf parsing is needed in the app –
 * the Render server owns all of that.
 */
object AuraCanvasRepository {

    private const val MANIFEST_URL = "https://auramusiccanvas.vercel.app/canvas.json"
    private const val REMOTE_BASE_URL = "https://auramusiccanvasserver.onrender.com"

    private const val MANIFEST_TTL_MS = 6 * 60 * 60 * 1000L     // 6h
    private const val POSITIVE_TTL_MS = 24 * 60 * 60 * 1000L    // 24h
    private const val NEGATIVE_TTL_MS = 60 * 60 * 1000L         // 1h

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

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            // First Render call after idle can take 30–90s (free-tier cold start).
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 90_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val mutex = Mutex()
    @Volatile private var cachedItems: List<CanvasItem>? = null
    @Volatile private var cachedAt: Long = 0L
    @Volatile private var manifestFailureAt: Long = 0L

    /** Resolved URL cache keyed by normalized(title|artist|album). */
    private data class CacheEntry(val url: String?, val expiresAt: Long)
    private val resultCache = mutableMapOf<String, CacheEntry>()

    @Volatile private var warmedUp = false

    /**
     * Fire-and-forget ping of the Render `/health` endpoint to wake the dyno.
     * Call once on app start or when the player UI becomes visible.
     */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun warmUp() {
        if (warmedUp) return
        warmedUp = true
        GlobalScope.launch(Dispatchers.IO + SupervisorJob()) {
            runCatching {
                client.get("$REMOTE_BASE_URL/health")
                Timber.d("AuraCanvas: warm-up ping sent")
            }
        }
    }

    // --------------------------------------------------------------
    //  Manifest layer
    // --------------------------------------------------------------

    private suspend fun ensureManifest(): List<CanvasItem> = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedItems
        if (cached != null && now - cachedAt < MANIFEST_TTL_MS) return@withLock cached
        if (cached != null && now - manifestFailureAt < NEGATIVE_TTL_MS) return@withLock cached
        try {
            val manifest: CanvasManifest = withContext(Dispatchers.IO) {
                client.get(MANIFEST_URL).body()
            }
            cachedItems = manifest.items
            cachedAt = now
            Timber.d("AuraCanvas: loaded ${manifest.items.size} manifest entries")
            manifest.items
        } catch (t: Throwable) {
            Timber.w(t, "AuraCanvas: failed to load manifest")
            manifestFailureAt = now
            cached ?: emptyList()
        }
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("\\bfeat\\.?|\\bft\\.?|\\bwith\\b"), " ")
            .replace(Regex("\\b(remaster(ed)?|live|explicit|official video|lyrics|audio|hd|hq)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun artistMatches(haystack: String, needle: String): Boolean {
        if (haystack.isBlank() || needle.isBlank()) return false
        if (haystack == needle) return true
        val a = haystack.split(' ').filter { it.length > 1 }.toSet()
        val b = needle.split(' ').filter { it.length > 1 }.toSet()
        if (a.isEmpty() || b.isEmpty()) return false
        val overlap = a.intersect(b).size
        return overlap > 0 && overlap >= minOf(a.size, b.size) / 2
    }

    private fun manifestLookup(items: List<CanvasItem>, title: String, artist: String): String? {
        val nTitle = normalize(title)
        val nArtist = normalize(artist)
        if (nTitle.isBlank() || nArtist.isBlank()) return null
        return items.firstOrNull { item ->
            val s = normalize(item.song)
            val a = normalize(item.artist)
            (s == nTitle || s.contains(nTitle) || nTitle.contains(s)) &&
                artistMatches(a, nArtist)
        }?.url
    }

    // --------------------------------------------------------------
    //  Remote layer (Render server)
    // --------------------------------------------------------------

    private suspend fun remoteLookup(
        title: String?,
        artist: String?,
        album: String?,
        durationMs: Long?,
    ): String? = withContext(Dispatchers.IO) {
        if (title.isNullOrBlank() && artist.isNullOrBlank() && album.isNullOrBlank()) return@withContext null
        try {
            val response: HttpResponse = client.get("$REMOTE_BASE_URL/api/canvas") {
                url {
                    if (!title.isNullOrBlank()) parameters.append("song", title)
                    if (!artist.isNullOrBlank()) parameters.append("artist", artist)
                    if (!album.isNullOrBlank()) parameters.append("album", album)
                    if (durationMs != null && durationMs > 0) parameters.append("durationMs", durationMs.toString())
                }
            }
            if (response.status == HttpStatusCode.NotFound) return@withContext null
            if (!response.status.isSuccess()) {
                Timber.w("AuraCanvas: remote HTTP ${response.status.value} for $title / $artist")
                return@withContext null
            }
            val body = response.bodyAsText()
            extractCanvasUrl(body)
        } catch (t: Throwable) {
            Timber.w(t, "AuraCanvas: remote lookup failed for $title / $artist")
            null
        }
    }

    private fun extractCanvasUrl(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            obj["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: obj["canvasUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: obj["canvasesList"]?.jsonArray?.firstOrNull()?.jsonObject?.get("canvasUrl")
                    ?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: (obj["data"] as? JsonObject)
                    ?.get("canvasesList")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("canvasUrl")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    // --------------------------------------------------------------
    //  Public API
    // --------------------------------------------------------------

    /**
     * Resolve a canvas URL for the current track. Returns null if neither the
     * manifest nor the remote server has one. Safe to call on every track
     * change — results are memoised.
     */
    suspend fun findCanvasUrl(
        title: String?,
        artist: String?,
        album: String? = null,
        durationMs: Long? = null,
    ): String? {
        if (title.isNullOrBlank() && artist.isNullOrBlank() && album.isNullOrBlank()) return null
        val key = listOf(title, artist, album).joinToString("\u0001") { normalize(it ?: "") }
        val now = System.currentTimeMillis()
        synchronized(resultCache) {
            val hit = resultCache[key]
            if (hit != null && hit.expiresAt > now) return hit.url
        }

        // 1) Manifest
        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            val manifestHit = manifestLookup(ensureManifest(), title, artist)
            if (manifestHit != null) {
                synchronized(resultCache) {
                    resultCache[key] = CacheEntry(manifestHit, now + POSITIVE_TTL_MS)
                }
                return manifestHit
            }
        }

        // 2) Remote (Render) – does Spotify search + canvas fetch server-side.
        warmUp() // ensure dyno is awake on first real call
        val remoteHit = remoteLookup(title, artist, album, durationMs)
        synchronized(resultCache) {
            resultCache[key] = CacheEntry(
                remoteHit,
                now + if (remoteHit != null) POSITIVE_TTL_MS else NEGATIVE_TTL_MS,
            )
        }
        return remoteHit
    }
}
