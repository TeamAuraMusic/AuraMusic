/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.sponsorblock

import android.graphics.Color
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
) {
    val startMs: Long get() = (segment[0] * 1000).toLong()
    val endMs: Long get() = (segment[1] * 1000).toLong()

    companion object {
        const val CATEGORY_SPONSOR = "sponsor"
        const val CATEGORY_INTRO = "intro"
        const val CATEGORY_OUTRO = "outro"
        const val CATEGORY_SELF_PROMO = "selfpromo"
        const val CATEGORY_INTERACTION = "interaction"
        const val CATEGORY_MUSIC_OFFTOPIC = "music_offtopic"
        const val CATEGORY_PREVIEW = "preview"
        const val CATEGORY_FILLER = "filler"
    }
}

data class SeekBarSegment(
    val startProgress: Float,
    val endProgress: Float,
    val color: Int,
    val category: String,
)

object SponsorBlockColors {
    val SPONSOR = Color.parseColor("#00D400")       // green
    val INTRO = Color.parseColor("#00FFFF")         // cyan
    val OUTRO = Color.parseColor("#0202ED")         // blue
    val SELF_PROMO = Color.parseColor("#FFFF00")    // yellow
    val INTERACTION = Color.parseColor("#CC00FF")   // magenta
    val MUSIC_OFFTOPIC = Color.parseColor("#FF6600") // orange
    val PREVIEW = Color.parseColor("#3399FF")       // light blue
    val FILLER = Color.parseColor("#7B2FBE")        // violet

    fun colorForCategory(category: String): Int = when (category) {
        SponsorBlockSegment.CATEGORY_SPONSOR -> SPONSOR
        SponsorBlockSegment.CATEGORY_INTRO -> INTRO
        SponsorBlockSegment.CATEGORY_OUTRO -> OUTRO
        SponsorBlockSegment.CATEGORY_SELF_PROMO -> SELF_PROMO
        SponsorBlockSegment.CATEGORY_INTERACTION -> INTERACTION
        SponsorBlockSegment.CATEGORY_MUSIC_OFFTOPIC -> MUSIC_OFFTOPIC
        SponsorBlockSegment.CATEGORY_PREVIEW -> PREVIEW
        SponsorBlockSegment.CATEGORY_FILLER -> FILLER
        else -> SPONSOR
    }
}

object SponsorBlockApi {
    private const val BASE_URL = "https://sponsor.ajay.app/api"
    private const val MIN_SEGMENT_DURATION_MS = 5000L
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

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
                val raw = json.decodeFromString<List<SponsorBlockSegment>>(body)
                raw.filter { (it.endMs - it.startMs) >= MIN_SEGMENT_DURATION_MS }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toSeekBarSegments(
        segments: List<SponsorBlockSegment>,
        durationMs: Long,
    ): List<SeekBarSegment> {
        if (durationMs <= 0) return emptyList()
        return segments.map { seg ->
            SeekBarSegment(
                startProgress = (seg.startMs.toFloat() / durationMs).coerceIn(0f, 1f),
                endProgress = (seg.endMs.toFloat() / durationMs).coerceIn(0f, 1f),
                color = SponsorBlockColors.colorForCategory(seg.category),
                category = seg.category,
            )
        }
    }
}
