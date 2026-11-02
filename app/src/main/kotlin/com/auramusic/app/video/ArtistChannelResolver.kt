/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeSearchResultItem

/**
 * Finds the channel a music artist should be linked to.
 *
 * An artist's channel id is often an auto-generated "- Topic" channel rather than the
 * one a person actually runs, so following it lands on an auto-generated page instead of
 * the artist's. The two describe the same artist and carry the same subscriber count,
 * which is what this matches on: when the id on hand is a generated one, the artist is
 * looked up by name and the channel whose subscriber count agrees with the artist's is
 * taken as the real one.
 */
object ArtistChannelResolver {

    /** Auto-generated music channels are named like this. */
    private const val TOPIC_SUFFIX = " - Topic"

    /** Subscriber counts drift as people join, so "same" means within this margin. */
    private const val SUBSCRIBER_TOLERANCE = 0.02

    data class Resolved(
        val channelId: String,
        val channelName: String,
        /** True when the channel we were given was a generated one and had to be replaced. */
        val replacedGenerated: Boolean,
    )

    /**
     * The channel to open for [artistName].
     *
     * [artistChannelId] and [artistSubscribersText] come from the artist page; both may be
     * absent, in which case only the name is used to look the artist up.
     */
    suspend fun resolve(
        artistName: String,
        artistChannelId: String?,
        artistSubscribersText: String?,
    ): Resolved? {
        val targetSubs = artistSubscribersText?.let { parseSubscribers(it) }

        // What the id we were given actually points at. This is only ever used as a
        // fallback. It used to decide the answer up front - "the page loaded and its
        // title has no Topic suffix, so it must be the artist" - but a Topic channel's
        // page is still titled with the artist's name, and its header sometimes parses
        // to nothing at all. Either way the early return sent people to the Topic page,
        // which is the page they were complaining about.
        val givenPage = artistChannelId?.let { id -> YouTube.youtubeChannel(id).getOrNull() }
        val givenName = givenPage?.title.orEmpty()

        val name = artistName.ifBlank { givenName.replace(TOPIC_SUFFIX, "", ignoreCase = true).trim() }
        if (name.isBlank()) {
            return artistChannelId?.let { Resolved(it, givenName, replacedGenerated = false) }
        }

        // Only channels named exactly this artist. The old match also accepted names
        // that merely contained it, then took whichever one YouTube returned first -
        // a same-named cover act, a fan page, a label channel. That is the channel
        // that opened instead of the artist's own. Topic channels are excluded here,
        // so whatever survives is a channel a person actually runs.
        val candidates = YouTube.youtubeSearch(name).getOrNull()?.items
            ?.filterIsInstance<YouTubeSearchResultItem.Channel>()
            .orEmpty()
            .filterNot { it.title.trim().endsWith(TOPIC_SUFFIX, ignoreCase = true) }
            .filter { normalize(it.title) == normalize(name) }
            .distinctBy { it.channelId }

        // The id from the artist page is one of the real channels: that is the one.
        // It can only match here when it is not a Topic channel, because those were
        // just filtered out.
        candidates.firstOrNull { it.channelId == artistChannelId }?.let { given ->
            return Resolved(
                channelId = given.channelId,
                channelName = given.title,
                replacedGenerated = false,
            )
        }

        // The id is not a channel people can find - which is what an auto-generated
        // Topic page looks like from here. Take the best exact-name channel instead:
        // the audience that agrees with the artist page, or the largest one if no
        // count can settle it. Guessing between two exact-name channels is still a
        // better answer than a Topic page.
        pickChannel(
            candidates = candidates,
            targetSubs = targetSubs,
            requireConfidence = false,
        )?.let { best ->
            return Resolved(
                channelId = best.channelId,
                channelName = best.title,
                replacedGenerated = true,
            )
        }

        // Nothing exact. A generated channel is still this artist even if it is not the
        // one a person runs; a differently named channel is not, and is not worth
        // guessing at.
        return artistChannelId?.let {
            Resolved(channelId = it, channelName = givenName.ifBlank { name }, replacedGenerated = false)
        }
    }

    /**
     * Among channels that are exactly this artist, the one most likely to be theirs:
     * the closest audience to the artist page's, or failing that the largest.
     */
    private fun pickChannel(
        candidates: List<YouTubeSearchResultItem.Channel>,
        targetSubs: Long?,
        requireConfidence: Boolean,
    ): YouTubeSearchResultItem.Channel? {
        if (candidates.isEmpty()) return null

        if (targetSubs != null && targetSubs > 0) {
            val measured = candidates.mapNotNull { candidate ->
                val subs = candidate.subscriberCountText?.let { parseSubscribers(it) }
                    ?: return@mapNotNull null
                candidate to kotlin.math.abs(subs - targetSubs).toDouble() / targetSubs
            }
            measured.filter { it.second <= SUBSCRIBER_TOLERANCE }
                .minByOrNull { it.second }
                ?.let { return it.first }
        }

        // Not sure enough to overrule the id we were given.
        if (requireConfidence && candidates.size > 1) return null

        // No count to compare, or nothing within tolerance: the biggest channel with
        // exactly this name is the artist's own far more often than not.
        return candidates.maxByOrNull { parseSubscribers(it.subscriberCountText.orEmpty()) ?: 0L }
    }

    /**
     * Reads a subscriber count out of the shapes YouTube writes it in: "38.9M subscribers",
     * "1.2K", "@markiplier" (which carries no count at all).
     */
    fun parseSubscribers(text: String): Long? {
        val cleaned = text.trim()
        if (cleaned.startsWith("@")) return null
        val match = Regex("""([\d.,]+)\s*([KMB])?""", RegexOption.IGNORE_CASE).find(cleaned)
            ?: return null
        val number = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].uppercase()) {
            "K" -> 1_000
            "M" -> 1_000_000
            "B" -> 1_000_000_000
            else -> 1
        }
        return (number * multiplier).toLong()
    }

    /**
     * The name with case, punctuation and spacing taken out, so "A.R. Rahman" and
     * "A. R. Rahman" are the same artist. It deliberately does not accept one name
     * containing another - "Arijit" is not "Arijit Singh", and treating it as if it
     * were is how a fan page gets opened instead of the artist.
     */
    private fun normalize(s: String): String =
        s.lowercase()
            .replace(TOPIC_SUFFIX.lowercase(), "")
            .replace(Regex("[^a-z0-9]+"), "")
            .trim()
}