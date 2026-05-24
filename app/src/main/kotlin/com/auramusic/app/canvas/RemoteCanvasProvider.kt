package com.auramusic.app.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Remote canvas provider that fetches from a self-hosted instance
 * of the Spotify Canvas API tool (e.g. one you deploy on Render).
 *
 * This allows you to use your own sp_dc without keeping a local server running.
 */
class RemoteCanvasProvider(
    private val baseUrl: String,
    private val apiKey: String? = null
) : AuraCanvasProvider {

    override suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String?
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        try {
            val normalizedSong = song.trim()
            val normalizedArtist = artist.trim()

            if (normalizedSong.isBlank() && normalizedArtist.isBlank()) return@withContext null

            // Build URL with query parameters
            val urlBuilder = StringBuilder(baseUrl.trimEnd('/'))
            urlBuilder.append("/api/canvas")
            urlBuilder.append("?song=").append(java.net.URLEncoder.encode(normalizedSong, "UTF-8"))
            urlBuilder.append("&artist=").append(java.net.URLEncoder.encode(normalizedArtist, "UTF-8"))
            if (!album.isNullOrBlank()) {
                urlBuilder.append("&album=").append(java.net.URLEncoder.encode(album.trim(), "UTF-8"))
            }

            val url = URL(urlBuilder.toString())
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")

                apiKey?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            // Expected minimal response: { "url": "https://..." } or full structure
            val json = org.json.JSONObject(response)
            val canvasUrl = when {
                json.has("url") -> json.getString("url")
                json.has("canvasUrl") -> json.getString("canvasUrl")
                json.has("data") && json.getJSONObject("data").has("canvasesList") -> {
                    val list = json.getJSONObject("data").getJSONArray("canvasesList")
                    if (list.length() > 0) list.getJSONObject(0).getString("canvasUrl") else null
                }
                else -> null
            }

            if (canvasUrl.isNullOrBlank()) return@withContext null

            AuraCanvasResult(
                url = canvasUrl,
                source = "remote",
                song = normalizedSong,
                artist = normalizedArtist,
                album = album
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * On-demand artist canvas lookup.
     * The app should pass candidate track IDs from the artist's top songs.
     */
    suspend fun getArtistCanvas(
        artist: String,
        candidateTracks: List<String>
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        try {
            if (artist.isBlank() || candidateTracks.isEmpty()) return@withContext null

            val url = URL("${baseUrl.trimEnd('/')}/api/canvas/artist")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")

                apiKey?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            val body = """{"artist":"$artist","tracks":${candidateTracks.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}"""
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != 200) {
                connection.disconnect()
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = org.json.JSONObject(response)
            if (!json.optBoolean("success", false)) return@withContext null

            val canvasUrl = json.optString("canvasUrl", json.optString("url", ""))
            if (canvasUrl.isBlank()) return@withContext null

            AuraCanvasResult(
                url = canvasUrl,
                source = json.optString("source", "remote"),
                artist = json.optString("artist", artist),
                song = json.optString("trackId", json.optString("song", null))
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * On-demand album canvas lookup.
     * The app should pass candidate track IDs (e.g. tracks from the album).
     */
    suspend fun getAlbumCanvas(
        album: String,
        artist: String,
        candidateTracks: List<String>
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        try {
            if (album.isBlank() || artist.isBlank() || candidateTracks.isEmpty()) return@withContext null

            val url = URL("${baseUrl.trimEnd('/')}/api/canvas/album")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")

                apiKey?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            val body = """{"album":"$album","artist":"$artist","tracks":${candidateTracks.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}"""
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != 200) {
                connection.disconnect()
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = org.json.JSONObject(response)
            if (!json.optBoolean("success", false)) return@withContext null

            val canvasUrl = json.optString("canvasUrl", json.optString("url", ""))
            if (canvasUrl.isBlank()) return@withContext null

            AuraCanvasResult(
                url = canvasUrl,
                source = json.optString("source", "remote"),
                album = json.optString("album", album),
                artist = json.optString("artist", artist),
                song = json.optString("trackId", json.optString("song", null))
            )
        } catch (e: Exception) {
            null
        }
    }
}
