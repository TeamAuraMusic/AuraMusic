package com.auramusic.kizzy.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.logging.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves external image URLs to Discord-compatible mp:external references
 * using Discord's official External Assets API.
 */
object DiscordExternalAssets {
    private val logger = Logger.getLogger(DiscordExternalAssets::class.java.name)
    private const val API_URL = "https://discord.com/api/v9/applications/%s/external-assets"
    private val cache = ConcurrentHashMap<String, String>(128)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp)

    /**
     * Resolve image URLs to mp:external references.
     * Returns a map of original URL -> mp:external/<hash>.
     */
    suspend fun resolve(
        urls: List<String>,
        appId: String,
        token: String,
        userAgent: String = "Discord-Android/314013;RNA",
        superProperties: String? = null
    ): Map<String, String> {
        if (urls.isEmpty()) return emptyMap()

        // Check cache first
        val uncached = urls.filter { it !in cache }
        if (uncached.isEmpty()) {
            return urls.associateWith { cache[it]!! }
        }

        return try {
            val response = client.post(API_URL.format(appId)) {
                header("Authorization", token)
                header("User-Agent", userAgent)
                if (superProperties != null) {
                    header("X-Super-Properties", superProperties)
                }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ExternalAssetRequest.serializer(), ExternalAssetRequest(urls = uncached)))
            }.bodyAsText()

            val result = json.decodeFromString(ListSerializer(ExternalAssetResult.serializer()), response)
            val resolved = result.associate { it.url to "mp:${it.externalAssetPath}" }

            // Cache results
            resolved.forEach { (url, ref) -> cache[url] = ref }

            // Return all resolved (cached + newly resolved)
            urls.associateWith { cache[it] ?: it }
        } catch (e: Exception) {
            logger.warning("Failed to resolve external assets: ${e.message}")
            // Return original URLs as fallback
            urls.associateWith { it }
        }
    }

    fun clearCache() {
        cache.clear()
    }
}

@Serializable
data class ExternalAssetRequest(
    val urls: List<String>
)

@Serializable
data class ExternalAssetResult(
    @SerialName("url")
    val url: String,
    @SerialName("external_asset_path")
    val externalAssetPath: String
)
