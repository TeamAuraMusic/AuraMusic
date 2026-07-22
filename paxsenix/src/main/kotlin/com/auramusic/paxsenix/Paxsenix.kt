package com.auramusic.paxsenix

import android.content.Context
import com.auramusic.paxsenix.models.AppleMusicSearchResponse
import com.auramusic.paxsenix.models.LyricsResponse
import com.auramusic.paxsenix.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URLEncoder
import kotlin.math.abs

object Paxsenix {
    @Volatile
    private var client: HttpClient? = null
    private var appVersion: String = "Unknown"

    fun init(context: Context) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            } catch (_: Exception) { "Unknown" }

            client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 10000
                }
                install(ContentNegotiation) {
                    json(Json { isLenient = true; ignoreUnknownKeys = true })
                }
                defaultRequest {
                    url("https://lyrics.paxsenix.org")
                    header("User-Agent", "AuraMusic/$appVersion")
                }
                expectSuccess = true
            }
        }
    }

    private val httpClient: HttpClient
        get() = client ?: throw IllegalStateException("Paxsenix.init() must be called first")

    private const val APPLE_MUSIC_API_BASE = "https://amp-api.music.apple.com/v1/catalog/us"
    private val appleJson = Json { ignoreUnknownKeys = true }

    @Volatile
    private var appleTokenManager: AppleTokenManager? = null
    private val tokenManager: AppleTokenManager
        get() = appleTokenManager ?: synchronized(this) {
            appleTokenManager ?: AppleTokenManager(httpClient).also { appleTokenManager = it }
        }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) { cleaned = cleaned.replace(pattern, "") }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        val separators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")
        var cleaned = artist.trim()
        for (sep in separators) {
            if (cleaned.contains(sep, ignoreCase = true)) {
                cleaned = cleaned.split(sep, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String? = null): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        Timber.d("Paxsenix: searching '$cleanedTitle' by '$cleanedArtist' (duration=$duration)")

        val queries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) add("$cleanedTitle $cleanedArtist $album")
        }

        var allResults: List<Pair<SearchResult, Double>> = emptyList()
        for (query in queries) {
            if (allResults.isEmpty()) {
                val results = search(query)
                if (results.isNotEmpty()) allResults = scoreAndFilter(results, title, artist, duration)
            }
        }
        if (allResults.isEmpty()) throw IllegalStateException("No tracks found on Paxsenix")

        var bestLyrics: String? = null
        var bestQuality = 0
        for ((result, _) in allResults.take(10)) {
            val lrc = fetchLyricsForTrack(result.id).getOrNull() ?: continue
            if (lrc.isEmpty()) continue
            val quality = getQuality(lrc)
            if (quality > bestQuality) { bestQuality = quality; bestLyrics = lrc }
            if (bestQuality == 3) break
        }
        bestLyrics?.let { return Result.success(it) }
        return Result.failure(IllegalStateException("No lyrics available from Paxsenix"))
    }

    private fun getQuality(lrc: String): Int {
        if (lrc.isBlank()) return 0
        if (lrc.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"))) return 3
        if (lrc.contains(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"))) return 2
        return 1
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        val token = tokenManager.getToken()
        searchWithToken(token, query)
    }.getOrElse { e ->
        if (e is ClientRequestException && e.response.status.value == 401) {
            tokenManager.clearToken()
            runCatching { searchWithToken(tokenManager.getToken(), query) }.getOrElse { emptyList() }
        } else { Timber.e(e, "Paxsenix search error"); emptyList() }
    }

    private suspend fun searchWithToken(token: String, query: String): List<SearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = httpClient.get("$APPLE_MUSIC_API_BASE/search?term=$encoded&types=songs&limit=25&l=en-US&platform=web&format[resources]=map&include[songs]=artists&extend=artistUrl") {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0")
            header("Accept", "application/json")
            header("x-apple-renewal", "true")
        }
        val body = try {
            appleJson.decodeFromString<AppleMusicSearchResponse>(response.bodyAsText())
        } catch (e: Exception) { Timber.e(e, "Failed to parse Apple Music response"); return emptyList() }

        return body.results.songs?.data?.mapNotNull { songData ->
            val detail = body.resources?.songs?.get(songData.id) ?: return@mapNotNull null
            SearchResult(
                id = songData.id,
                trackName = detail.attributes.name,
                artistName = detail.attributes.artistName,
                albumName = detail.attributes.albumName,
                duration = detail.attributes.durationInMillis?.toInt()?.div(1000),
                artwork = detail.attributes.artwork?.url?.replace("{w}", "100")?.replace("{h}", "100"),
            )
        } ?: emptyList()
    }

    private fun scoreAndFilter(results: List<SearchResult>, title: String, artist: String, duration: Int): List<Pair<SearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")
        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()

        return results.map { result ->
            var score = 0.0
            result.duration?.let { d ->
                val diff = abs(d - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }
            val resultTitle = result.trackName.replace(cleanupRegex, "").lowercase().trim()
            when {
                resultTitle == cleanedTitle -> score += 80
                resultTitle.contains(cleanedTitle) || cleanedTitle.contains(resultTitle) -> score += 40
            }
            val resultArtist = result.artistName.lowercase()
            when {
                resultArtist.contains(cleanedArtist) -> score += 50
                else -> { val words = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }; if (words.any { resultArtist.contains(it) }) score += 25 }
            }
            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        val response = httpClient.get("/apple-music/lyrics") { parameter("id", id) }.body<LyricsResponse>()
        // Try TTML first, then ELRC, then plain
        if (!response.ttmlContent.isNullOrBlank()) return@runCatching response.ttmlContent
        if (!response.elrcMultiPerson.isNullOrBlank()) return@runCatching response.elrcMultiPerson
        if (!response.elrc.isNullOrBlank()) return@runCatching response.elrc
        if (!response.plain.isNullOrBlank()) return@runCatching response.plain
        if (response.content.isEmpty()) throw IllegalStateException("No lyrics found")
        // Build LRC from content array
        buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp ?: return@forEach
                val min = timeMs / 1000 / 60; val sec = (timeMs / 1000) % 60; val cs = (timeMs % 1000) / 10
                val text = line.text.joinToString(" ") { it.text }
                if (text.isNotBlank()) appendLine(String.format("[%02d:%02d.%02d]%s", min, sec, cs, text))
            }
        }.ifBlank { throw IllegalStateException("No lyrics content") }
    }

    private class AppleTokenManager(private val httpClient: HttpClient) {
        private var cachedToken: String? = null
        private val mutex = Mutex()

        suspend fun getToken(): String = mutex.withLock {
            cachedToken?.let { return it }
            val mainPage = httpClient.get("https://beta.music.apple.com").bodyAsText()
            val jsUrl = Regex("""/assets/index~[^/]+\.js""").find(mainPage)?.value
                ?: throw Exception("Could not find index JS URL")
            val jsBody = httpClient.get("https://beta.music.apple.com$jsUrl").bodyAsText()
            val token = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                .find(jsBody)?.value ?: throw Exception("Could not find token")
            cachedToken = token
            token
        }

        fun clearToken() { cachedToken = null }
    }
}
