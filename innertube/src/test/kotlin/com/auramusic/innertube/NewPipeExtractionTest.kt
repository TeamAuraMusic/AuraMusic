package com.auramusic.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live checks against YouTube for the three extraction paths the video player leans on:
 * collaboration credits, comment pagination avatars, and the channel header backstop.
 */
class NewPipeExtractionTest {

    /**
     * Collaboration uploads ("A & B") credit both channels only inside the owner dialog
     * payload. Finds such a video and asserts both channels come back with real UC ids.
     */
    @Test(timeout = 180_000)
    fun collaborationVideoExposesBothChannels() = runBlocking {
        val queries = listOf(
            "Zayn NeYo",
            "Zayn and Neyo collaboration",
            "two artists collaboration official video",
            "artist collaboration feat official video",
            "joint venture music video",
        )
        val candidates = LinkedHashSet<String>()
        for (query in queries) {
            val result = YouTube.youtubeSearch(query).getOrNull() ?: continue
            result.items
                .filterIsInstance<com.auramusic.innertube.models.YouTubeSearchResultItem.Video>()
                .map { it.video }
                // A multi-run byline or an "&" credit is the likeliest to ship the dialog.
                .sortedByDescending { it.channels.size > 1 || it.channelName.contains("&") }
                .take(6)
                .forEach { candidates += it.videoId }
        }
        assertTrue("search returned no video candidates", candidates.isNotEmpty())

        var best = emptyList<com.auramusic.innertube.models.Artist>()
        var bestId = ""
        var tried = 0
        for (videoId in candidates.take(16)) {
            tried++
            val rows = YouTube.videoCollaborators(videoId).getOrNull().orEmpty()
            if (rows.size > best.size) {
                best = rows
                bestId = videoId
            }
            if (rows.size >= 2) break
        }
        println("collaboration extraction: tried=$tried best=${best.size} for $bestId -> $best")
        assertTrue(
            "no video exposed >=2 credited channels among $tried candidates",
            best.size >= 2,
        )
        assertEquals("credited channels must be distinct", best.size, best.map { it.id }.distinct().size)
        best.forEach { channel ->
            assertTrue("channel id must be a UC id: ${channel.id}", channel.id.orEmpty().startsWith("UC"))
            assertTrue("channel name must not be blank", channel.name.isNotBlank())
        }
    }

    /**
     * Continuation pages are where avatars go missing. Every paginated comment must ship
     * either an avatar or a channel reference the player can backfill from - a row with
     * neither would stay a placeholder forever.
     */
    @Test(timeout = 180_000)
    fun continuationCommentsAreBackfillable() = runBlocking {
        val videoId = firstCommentableVideoId()
        assumeTrue("no video with a comments continuation found", videoId != null)

        val first = NewPipeExtractor.getComments(videoId!!)
        assumeTrue("first page has no continuation", first.nextPage != null)
        assertTrue("first page came back empty", first.comments.isNotEmpty())

        val more = NewPipeExtractor.getMoreComments(videoId, first.nextPage!!)
        println(
            "comments page 2: ${more.comments.size} rows, " +
                "withAvatar=${more.comments.count { !it.authorThumbnail.isNullOrBlank() }}, " +
                "withChannelRef=${more.comments.count { !it.authorChannelRef.isNullOrBlank() }}",
        )
        assertTrue("continuation page came back empty", more.comments.isNotEmpty())
        more.comments.forEach { comment ->
            assertTrue(
                "comment '${comment.authorName}' has neither avatar nor channel ref",
                !comment.authorThumbnail.isNullOrBlank() || !comment.authorChannelRef.isNullOrBlank(),
            )
        }

        val needsBackfill = more.comments.firstOrNull {
            it.authorThumbnail.isNullOrBlank() && !it.authorChannelRef.isNullOrBlank()
        }
        if (needsBackfill != null) {
            val avatar = NewPipeExtractor.fetchCommentAvatar(needsBackfill.authorChannelRef)
            println("backfill avatar for ${needsBackfill.authorName}: $avatar")
            assertNotNull("channel-page avatar lookup failed", avatar)
        }
    }

    /** The channel header backstop (name / avatar / subs / description / banner). */
    @Test(timeout = 120_000)
    fun channelMetadataBackstopCarriesHeaderFields() = runBlocking {
        val result = YouTube.youtubeSearch("music").getOrNull() ?: return@runBlocking
        val channelId = result.items
            .filterIsInstance<com.auramusic.innertube.models.YouTubeSearchResultItem.Video>()
            .firstNotNullOfOrNull { it.video.channelId }
        assumeTrue("search returned no channel ids", channelId != null)

        val metadata = NewPipeExtractor.getChannelMetadata(channelId!!)
        println(
            "channel metadata for $channelId: name=${metadata?.name} subs=${metadata?.subscriberCount} " +
                "avatar=${metadata?.avatarUrl != null} banner=${metadata?.bannerUrl != null} " +
                "description=${metadata?.description?.take(60)}",
        )
        assertNotNull("channel metadata lookup failed", metadata)
        assertTrue("channel name missing", metadata!!.name.isNotBlank())
        assertTrue("channel avatar missing", !metadata.avatarUrl.isNullOrBlank())
        assertTrue("subscriber count not resolved", metadata.subscriberCount >= 0)
    }

    private suspend fun firstCommentableVideoId(): String? {
        val result = YouTube.youtubeSearch("official music video").getOrNull() ?: return null
        return result.items
            .filterIsInstance<com.auramusic.innertube.models.YouTubeSearchResultItem.Video>()
            .map { it.video.videoId }
            .take(6)
            .firstOrNull { id ->
                runCatching { NewPipeExtractor.getComments(id).nextPage != null }.getOrDefault(false)
            }
    }
}
