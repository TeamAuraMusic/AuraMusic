package com.auramusic.music.betterlyrics

import com.auramusic.music.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.math.abs

object BetterLyrics {
    private const val TAG = "BetterLyrics"
    var apiKey: String = ""
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api.boidu.dev")
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    append("Accept", "application/json")
                }
            }

            expectSuccess = false
        }
    }

    @Serializable
    private data class ApiResponse<T>(
        val success: Boolean = false,
        val data: T? = null,
    )

    @Serializable
    private data class UnisonLyrics(
        val lyrics: String = "",
        val format: String = "",
    )

    @Serializable
    private data class LrcLibTrack(
        val trackName: String = "",
        val artistName: String = "",
        val duration: Double = 0.0,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    )

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        duration: Int = -1,
        album: String? = null,
    ): String? = runCatching {
        Timber.tag(TAG).d("Fetching TTML for: $title by $artist (dur=$duration, album=$album)")
        val response = client.get("/getLyrics") {
            if (apiKey.isNotEmpty()) {
                header("X-API-Key", apiKey)
            }
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) {
                parameter("d", duration)
            }
            if (!album.isNullOrBlank()) {
                parameter("al", album)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val ttml = response.body<TTMLResponse>().ttml?.trim()?.takeIf { it.isNotEmpty() }
            ttml
        } else {
            Timber.tag(TAG).w("API returned status: ${response.status}")
            null
        }
    }.getOrElse { e ->
        Timber.tag(TAG).e(e, "Exception during fetchTTML")
        null
    }

    private suspend fun fetchUnisonLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): String? = runCatching {
        val response = client.get("https://unison.boidu.dev/lyrics") {
            parameter("song", title)
            parameter("artist", artist)
            if (duration > 0) parameter("duration", duration)
            if (!album.isNullOrBlank()) parameter("album", album)
        }

        if (response.status != HttpStatusCode.OK) return@runCatching null
        val item = response.body<ApiResponse<UnisonLyrics>>().data ?: return@runCatching null
        when (item.format.lowercase()) {
            "ttml" -> TTMLParser.parseTTML(item.lyrics)
                .takeIf { it.isNotEmpty() }
                ?.let(TTMLParser::toLRC)
            "lrc", "plain" -> item.lyrics.takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrElse { e ->
        Timber.tag(TAG).e(e, "Exception during fetchUnisonLyrics")
        null
    }

    private suspend fun fetchLrcLibLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): String? = runCatching {
        val tracks = queryLrcLib(title, artist, album)
        val best = chooseBestLrcLibTrack(tracks, title, artist, duration) ?: return@runCatching null
        best.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: best.plainLyrics?.takeIf { it.isNotBlank() }
    }.getOrElse { e ->
        Timber.tag(TAG).e(e, "Exception during fetchLrcLibLyrics")
        null
    }

    private suspend fun queryLrcLib(
        title: String,
        artist: String,
        album: String? = null,
    ): List<LrcLibTrack> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val queries = listOf(
            LrcLibQuery(trackName = cleanedTitle, artistName = cleanedArtist, albumName = album),
            LrcLibQuery(trackName = cleanedTitle, artistName = cleanedArtist),
            LrcLibQuery(query = "$cleanedArtist $cleanedTitle"),
            LrcLibQuery(query = cleanedTitle),
        )

        for (query in queries) {
            val results = runCatching {
                val response = client.get("https://lrclib.net/api/search") {
                    query.trackName?.let { parameter("track_name", it) }
                    query.artistName?.let { parameter("artist_name", it) }
                    query.albumName?.let { parameter("album_name", it) }
                    query.query?.let { parameter("q", it) }
                }
                if (response.status != HttpStatusCode.OK) emptyList() else response.body<List<LrcLibTrack>>()
            }.getOrDefault(emptyList())
                .filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }

            if (results.isNotEmpty()) return results
        }

        return emptyList()
    }

    private data class LrcLibQuery(
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        val query: String? = null,
    )

    private fun chooseBestLrcLibTrack(
        tracks: List<LrcLibTrack>,
        title: String,
        artist: String,
        duration: Int,
    ): LrcLibTrack? {
        if (tracks.isEmpty()) return null
        val cleanedTitle = normalize(cleanTitle(title))
        val cleanedArtist = normalize(cleanArtist(artist))

        return tracks.maxByOrNull { track ->
            var score = similarity(cleanedTitle, normalize(track.trackName)) * 4
            score += similarity(cleanedArtist, normalize(track.artistName)) * 4
            if (!track.syncedLyrics.isNullOrBlank()) score += 1.0
            if (duration > 0) {
                val delta = abs(track.duration.toInt() - duration)
                score += when {
                    delta <= 2 -> 2.0
                    delta <= 5 -> 1.0
                    delta <= 10 -> 0.25
                    else -> -2.0
                }
            }
            score
        }?.takeIf { track ->
            val titleScore = similarity(cleanedTitle, normalize(track.trackName))
            val artistScore = similarity(cleanedArtist, normalize(track.artistName))
            duration <= 0 || abs(track.duration.toInt() - duration) <= 10 || (titleScore + artistScore) / 2 >= 0.75
        }
    }

    private fun cleanTitle(title: String): String = title.trim()
        .replace(Regex("""\s*\([^)]*(official|video|audio|lyrics|lyric|visualizer|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit)[^)]*\)""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\[[^]]*(official|video|audio|lyrics|lyric|visualizer|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit)[^]]*]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\|.*$"""), "")
        .trim()

    private fun cleanArtist(artist: String): String {
        val separators = listOf(" & ", " and ", ", ", " x ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")
        var cleaned = artist.trim()
        for (separator in separators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
        .replace(Regex("\\b(feat\\.?|ft\\.?|with|lyrics?|official|video|audio|remaster(ed)?|live)\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.isBlank() || right.isBlank()) return 0.0
        val containsScore = if (left.contains(right) || right.contains(left)) 0.8 else 0.0
        val maxLength = maxOf(left.length, right.length)
        val distanceScore = 1.0 - (levenshteinDistance(left, right).toDouble() / maxLength)
        return maxOf(containsScore, distanceScore)
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        val matrix = Array(left.length + 1) { IntArray(right.length + 1) }
        for (i in 0..left.length) matrix[i][0] = i
        for (j in 0..right.length) matrix[0][j] = j
        for (i in 1..left.length) {
            for (j in 1..right.length) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                matrix[i][j] = minOf(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + cost)
            }
        }
        return matrix[left.length][right.length]
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) = runCatching {
        // Use exact title and artist - no normalization to ensure correct sync
        // Normalizing can return wrong lyrics (e.g., radio edit vs original)
        val ttml =
            fetchTTML(artist, title, duration, album)
        if (ttml != null) {
            val parsedLines = TTMLParser.parseTTML(ttml)
            if (parsedLines.isNotEmpty()) {
                return@runCatching TTMLParser.toLRC(parsedLines)
            }
        }

        fetchUnisonLyrics(title, artist, duration, album)
            ?: fetchLrcLibLyrics(title, artist, duration, album)
            ?: throw IllegalStateException("Lyrics unavailable")
    }
}
