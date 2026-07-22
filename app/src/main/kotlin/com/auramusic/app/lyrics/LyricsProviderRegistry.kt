/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "BetterLyrics" to BetterLyricsProvider,
        "SimpMusic" to SimpMusicLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "RushLyrics" to RushLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        val storedOrder = orderString.split(",").map { it.trim() }.filter { it in providerNames }
        // If no valid providers found, return default order
        if (storedOrder.isEmpty()) {
            return getDefaultProviderOrder()
        }
        // Add any new providers that aren't in the stored order
        val defaultOrder = getDefaultProviderOrder()
        val result = storedOrder.toMutableList()
        for (provider in defaultOrder) {
            if (provider !in result) {
                result.add(provider)
            }
        }
        return result
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = listOf(
        "BetterLyrics",
        "RushLyrics",
        "Paxsenix",
        "SimpMusic",
        "LrcLib",
        "KuGou",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
