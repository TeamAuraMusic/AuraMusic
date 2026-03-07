/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

import android.content.Context
import com.auramusic.app.constants.LyricsProviderOrderKey
import com.auramusic.app.utils.dataStore

object LyricsProviderRegistry {
    private val providers = mapOf(
        "BetterLyrics" to BetterLyricsProvider,
        "SimpMusic" to SimpMusicLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "LyricsPlus" to LyricsPlusProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
    )

    val defaultProviderOrder = listOf(
        "BetterLyrics",
        "SimpMusic",
        "LrcLib",
        "KuGou",
        "LyricsPlus",
        "YouTubeSubtitle",
    )

    fun getProviderByName(name: String): LyricsProvider? = providers[name]

    fun getDefaultProviderOrder(): List<String> = defaultProviderOrder

    fun serializeProviderOrder(order: List<String>): String = order.joinToString(",")

    fun deserializeProviderOrder(serialized: String?): List<String> {
        if (serialized.isNullOrBlank()) {
            return defaultProviderOrder
        }
        return serialized.split(",").filter { it.isNotBlank() }
    }

    suspend fun getProviderOrder(context: Context): List<String> {
        val stored = context.dataStore.data
        return stored.map { data ->
            val serialized = data[LyricsProviderOrderKey]
            deserializeProviderOrder(serialized)
        }.first()
    }
}
