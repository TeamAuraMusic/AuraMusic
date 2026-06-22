package com.auramusic.rush

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import com.auramusic.rush.TTMLParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object RushLyrics {
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

            expectSuccess = false
        }
    }

    private val servers = listOf(
        "https://lyricsplus.atomix.one",
        "https://lyricsplus-seven.vercel.app",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyrics-plus-backend.vercel.app",
        "https://youlyplus.binimum.org",
    )

    private const val GENIUS_API_TOKEN =
        "qLSDtgIqHgzGNjOFUmdOxJKGJOg5RIAPzOKTfrs7rNxqYXwfdSh9HTHMJUs2X27Y"

    private val dumbGeniusMirrors = listOf(
        "dumb.ducks.party/",
        "dumb.lunar.icu/",
        "dumb.bloat.cat/",
        "dumb.jeikobu.net/",
    )

    private val geniusBoilerplatePatterns = listOf(
        Regex("^\\d+Contributors.*"),
        Regex("^\\d+ Contributor.*"),
        Regex("(?i)^Translations.*"),
        Regex("(?i)^Read More.*"),
        Regex("(?i)^.*Lyrics$"),
        Regex("(?i)^Embed$"),
    )

    @Serializable
    private data class TTMLResponse(val ttml: String)

    @Serializable
    private data class GeniusSearchResponse(val response: GeniusResponse)

    @Serializable
    private data class GeniusResponse(val hits: List<GeniusHit> = emptyList())

    @Serializable
    private data class GeniusHit(
        val type: String,
        val result: GeniusResult,
    )

    @Serializable
    private data class GeniusResult(
        val id: Long,
        val title: String,
        val url: String,
        @SerialName("artist_names") val artistNames: String,
    )

    @Serializable
    private data class SearchResponse(
        val results: List<SearchResult> = emptyList()
    )

    @Serializable
    data class SearchResult(
        val id: String = "",
        val title: String = "",
        val artist: String = "",
        val album: String? = null,
        val duration: Int? = null,
        val provider: String = ""
    )

    /**
     * Fetch TTML lyrics from LyricsPlus-compatible API
     */
    private suspend fun fetchTTML(
        title: String,
        artist: String,
    ): String? = runCatching {
        for (server in servers) {
            try {
                val ttml = client.get("$server/v1/ttml/get") {
                    parameter("title", title)
                    parameter("artist", artist)
                }.body<TTMLResponse>().ttml

                if (TTMLParser.parseTTML(ttml).isNotEmpty()) {
                    return@runCatching ttml
                }
            } catch (e: Exception) {
                continue
            }
        }
        null
    }.getOrNull()

    /**
     * Fetch LRC lyrics directly from API
     */
    private suspend fun fetchLRC(
        title: String,
        artist: String,
    ): String? = runCatching {
        val ttml = fetchTTML(title, artist) ?: return@runCatching null
        val parsed = TTMLParser.parseTTML(ttml)
        if (parsed.isNotEmpty()) {
            return@runCatching TTMLParser.toLRC(parsed)
        }
        null
    }.getOrNull()

    /**
     * Search for lyrics
     */
    suspend fun searchLyrics(
        title: String,
        artist: String,
    ): List<SearchResult> = runCatching {
        if (fetchTTML(title, artist) != null || fetchGeniusLyrics(title, artist) != null) {
            return@runCatching listOf(
                SearchResult(
                    id = "$title-$artist",
                    title = title,
                    artist = artist,
                    provider = "RushLyrics"
                )
            )
        }
        emptyList()
    }.getOrDefault(emptyList())

    private suspend fun fetchGeniusLyrics(title: String, artist: String): String? {
        val geniusUrl = searchGenius(title, artist)?.url ?: return null
        return scrapeGenius(geniusUrl)
    }

    private suspend fun searchGenius(title: String, artist: String): GeniusResult? = runCatching {
        val response = client.get("https://api.genius.com/search") {
            header(HttpHeaders.Authorization, "Bearer $GENIUS_API_TOKEN")
            parameter("q", listOf(title, artist).filter { it.isNotBlank() }.joinToString(" "))
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val results = response.body<GeniusSearchResponse>().response.hits
            .filter { it.type == "song" }
            .map { it.result }

        results.maxByOrNull { result ->
            val requestedTitle = normalizeForMatch(title)
            val requestedArtist = normalizeForMatch(artist)
            val resultTitle = normalizeForMatch(result.title)
            val resultArtist = normalizeForMatch(result.artistNames)
            var score = 0
            if (resultTitle == requestedTitle) score += 4
            else if (resultTitle.contains(requestedTitle) || requestedTitle.contains(resultTitle)) score += 2
            if (resultArtist == requestedArtist) score += 4
            else if (resultArtist.contains(requestedArtist) || requestedArtist.contains(resultArtist)) score += 2
            score
        }
    }.getOrNull()

    private suspend fun scrapeGenius(url: String): String? {
        scrapeGeniusUrl(url, "div[data-lyrics-container=true]")?.let { return it }

        dumbGeniusMirrors.forEach { mirror ->
            val mirrorUrl = url.replace("genius.com/", mirror)
            scrapeGeniusUrl(mirrorUrl, "#lyrics")?.let { return it }
        }

        return null
    }

    private suspend fun scrapeGeniusUrl(url: String, selector: String): String? = runCatching {
        val response = client.get(url)
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val document = Jsoup.parse(response.bodyAsText())
        val elements = document.select(selector)
        if (elements.isEmpty()) return@runCatching null

        elements.joinToString("\n") { element ->
            extractLyricsText(element)
        }.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun extractLyricsText(element: Element): String {
        element.select("br").append("\n")
        return element.wholeText()
            .lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    line != "(" &&
                    line != ")" &&
                    geniusBoilerplatePatterns.none { it.matches(line) }
            }
            .joinToString("\n")
            .trim()
    }

    private fun normalizeForMatch(value: String): String =
        value.lowercase()
            .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
            .replace(Regex("\\b(feat\\.?|ft\\.?|with|lyrics?|official|video|audio|remaster(ed)?|live)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    /**
     * Get lyrics - fetches TTML and converts to LRC
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
    ) = runCatching {
        val lrc = fetchLRC(title, artist)
        if (lrc != null && lrc.isNotBlank()) {
            val fixedLrc = fixMalformedLrc(lrc, duration)
            return@runCatching fixedLrc
        }

        val ttml = fetchTTML(title, artist)
            ?: return@runCatching fetchGeniusLyrics(title, artist)
                ?: throw IllegalStateException("Lyrics unavailable")
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        if (!TTMLParser.hasWordLevelTiming(parsedLines)) {
            val linesWithTiming = generateLineTiming(parsedLines, duration)
            if (linesWithTiming != null) {
                return@runCatching TTMLParser.toLRC(linesWithTiming)
            }
        }
        
        TTMLParser.toLRC(parsedLines)
    }

    /**
     * Fix malformed LRC by distributing timestamps evenly if all are at 0
     */
    private fun fixMalformedLrc(lrc: String, duration: Int): String {
        val lines = lrc.lines().filter { it.isNotBlank() }
        val linePattern = Regex("\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        
        // Extract line timestamps
        val timestamps = lines.mapNotNull { line ->
            val match = linePattern.matchEntire(line.trim())
            match?.let {
                val min = it.groupValues[1].toLongOrNull() ?: 0L
                val sec = it.groupValues[2].toLongOrNull() ?: 0L
                val ms = it.groupValues[3].let { ms -> 
                    if (ms.length == 3) ms.toLongOrNull() ?: 0L else (ms.toLongOrNull() ?: 0L) * 10 
                }
                min * 60000 + sec * 1000 + ms
            }
        }
        
        // Check if all line timestamps are 0 (malformed)
        val maxLineTimestamp = timestamps.maxOrNull() ?: 0L
        val hasWordTimings = lrc.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"))
        
        // Fix if: all line timestamps are 0 AND there are no valid word timings
        if (maxLineTimestamp == 0L && timestamps.isNotEmpty()) {
            val validLines = lines.filter { linePattern.matches(it.trim()) }
            if (validLines.isNotEmpty()) {
                // Use estimated duration or default to 3 minutes if not provided
                val effectiveDuration = if (duration > 0) duration * 1000L else 180000L
                val interval = effectiveDuration / validLines.size
                
                return buildString {
                    validLines.forEachIndexed { index, line ->
                        val text = linePattern.matchEntire(line.trim())?.groupValues?.get(4) ?: ""
                        val timeMs = index * interval
                        val minutes = timeMs / 60000
                        val seconds = (timeMs % 60000) / 1000
                        val centiseconds = (timeMs % 1000) / 10
                        appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, text))
                    }
                }
            }
        }
        
        // Check if word timings are all at 0 (another malformed case)
        if (hasWordTimings) {
            val wordPattern = Regex("<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>\\s*([^<]+)")
            val wordMatches = wordPattern.findAll(lrc).toList()
            
            if (wordMatches.isNotEmpty()) {
                val firstWordTime = wordMatches.first().let { match ->
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val ms = match.groupValues[3].let { if (it.length == 3) it.toLongOrNull() ?: 0L else (it.toLongOrNull() ?: 0L) * 10 }
                    min * 60000 + sec * 1000 + ms
                }
                
                // If all words start at time 0, regenerate word timings
                val allWordsAtZero = wordMatches.all { match ->
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val ms = match.groupValues[3].let { if (it.length == 3) it.toLongOrNull() ?: 0L else (it.toLongOrNull() ?: 0L) * 10 }
                    (min * 60000 + sec * 1000 + ms) == 0L
                }
                
                if (allWordsAtZero && timestamps.isNotEmpty()) {
                    if (maxLineTimestamp > 0L) {
                        // Line timestamps are valid — just strip the malformed word timings
                        val wordTagPattern = Regex("\\s*<\\d{1,2}:\\d{2}\\.\\d{2,3}>[^<]*")
                        return lines.joinToString("\n") { line ->
                            line.trim().replace(wordTagPattern, "")
                        }
                    }
                    
                    // All line timestamps are also 0 — redistribute everything evenly
                    val effectiveDuration = if (duration > 0) duration * 1000L else 180000L
                    val validLines = lines.filter { linePattern.matches(it.trim()) }
                    if (validLines.isNotEmpty()) {
                        return buildString {
                            validLines.forEachIndexed { lineIndex, line ->
                                val match = linePattern.matchEntire(line.trim())
                                val text = match?.groupValues?.get(4) ?: ""
                                // Strip any malformed word tags from text
                                val cleanText = text.replace(Regex("\\s*<\\d{1,2}:\\d{2}\\.\\d{2,3}>[^<]*"), "").trim()
                                val lineTimeMs = lineIndex * (effectiveDuration / validLines.size)
                                
                                val minutes = lineTimeMs / 60000
                                val seconds = (lineTimeMs % 60000) / 1000
                                val centiseconds = (lineTimeMs % 1000) / 10
                                appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, cleanText))
                            }
                        }
                    }
                }
            }
        }
        
        return lrc
    }

    /**
     * Generate line-level timing for TTML parsed lines that lack word timing
     */
    private fun generateLineTiming(lines: List<TTMLParser.ParsedLine>, duration: Int): List<TTMLParser.ParsedLine>? {
        if (lines.isEmpty() || duration <= 0) return null
        
        val totalDuration = duration.toDouble() * 1000
        val interval = totalDuration / lines.size
        
        return lines.mapIndexed { index, line ->
            line.copy(startTime = index * interval / 1000)
        }
    }

    /**
     * Get all available lyrics (for search results)
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }

    data class LyricsResult(
        val lrc: String,
        val hasWordSync: Boolean,
        val quality: TTMLParser.LyricsQuality
    )

    /**
     * Get lyrics with quality metadata - helps UI decide between word/line sync
     */
    suspend fun getLyricsWithQuality(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
    ) = runCatching {
        val lrc = fetchLRC(title, artist)
        if (lrc != null && lrc.isNotBlank()) {
            val fixedLrc = fixMalformedLrc(lrc, duration)
            val lines = fixedLrc.lines().filter { it.startsWith("[") && it.contains("]") }
            return@runCatching LyricsResult(
                lrc = fixedLrc,
                hasWordSync = fixedLrc.contains(Regex("<.*>")),
                quality = TTMLParser.LyricsQuality(
                    hasLineSync = lines.isNotEmpty(),
                    hasWordSync = fixedLrc.contains(Regex("<.*>")),
                    totalLines = lines.size,
                    linesWithWords = 0
                )
            )
        }

        val ttml = fetchTTML(title, artist)
            ?: return@runCatching LyricsResult(
                lrc = fetchGeniusLyrics(title, artist)
                    ?: throw IllegalStateException("Lyrics unavailable"),
                hasWordSync = false,
                quality = TTMLParser.LyricsQuality(
                    hasLineSync = false,
                    hasWordSync = false,
                    totalLines = 0,
                    linesWithWords = 0
                )
            )
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        if (!TTMLParser.hasWordLevelTiming(parsedLines)) {
            val linesWithTiming = generateLineTiming(parsedLines, duration)
            if (linesWithTiming != null) {
                val quality = TTMLParser.analyzeQuality(linesWithTiming)
                return@runCatching LyricsResult(
                    lrc = TTMLParser.toLRC(linesWithTiming),
                    hasWordSync = quality.hasWordSync,
                    quality = quality
                )
            }
        }

        val quality = TTMLParser.analyzeQuality(parsedLines)
        LyricsResult(
            lrc = TTMLParser.toLRC(parsedLines),
            hasWordSync = quality.hasWordSync,
            quality = quality
        )
    }
}
