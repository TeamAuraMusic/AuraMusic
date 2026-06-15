/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.sponsorblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

@Serializable
data class SponsorBlockSegment(
    val segment: List<Double>,
    val category: String,
    val actionType: String,
    val UUID: String = "",
    val videoDuration: Double = 0.0,
)

object SponsorBlockApi {
    private const val BASE_URL = "https://sponsor.ajay.app/api"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

    // Categories to auto-skip
    val defaultSkippableCategories = setOf(
        "sponsor",
        "selfpromo",
        "interaction",
        "intro",
        "outro",
        "preview",
        "music_offtopic",
        "filler",
    )

    suspend fun getSegments(
        videoId: String,
        categories: Set<String> = defaultSkippableCategories,
    ): List<SponsorBlockSegment> = withContext(Dispatchers.IO) {
        try {
            val categoriesParam = categories.joinToString(",") { "\"$it\"" }
            val url = "$BASE_URL/skipSegments?videoID=$videoId&categories=[$categoriesParam]"
            val response = client.get(url)
            val body = response.bodyAsText()
            if (response.status.value == 200) {
                json.decodeFromString<List<SponsorBlockSegment>>(body)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
