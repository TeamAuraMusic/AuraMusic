package com.auramusic.innertube

import com.auramusic.innertube.models.YouTubeClient
import com.auramusic.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Proxy

class NewPipeDownloaderImpl(
    proxy: Proxy?,
    proxyAuth: String? = null,
) : Downloader() {
    private val client =
        OkHttpClient
            .Builder()
            .proxy(proxy)
            .proxyAuthenticator { _, response ->
                proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request
                .Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()
        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }
}

class NewPipeUtils(
    downloader: Downloader,
) {
    init {
        NewPipe.init(downloader)
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> =
        runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
    ): String? =
        try {
            val url =
                format.url ?: format.signatureCipher?.let { signatureCipher ->
                    val params = parseQueryString(signatureCipher)
                    val obfuscatedSignature =
                        params["s"]
                            ?: throw ParsingException("Could not parse cipher signature")
                    val signatureParam =
                        params["sp"]
                            ?: throw ParsingException("Could not parse cipher signature parameter")
                    val url =
                        params["url"]?.let { URLBuilder(it) }
                            ?: throw ParsingException("Could not parse cipher url")
                    url.parameters[signatureParam] =
                        YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                            videoId,
                            obfuscatedSignature,
                        )
                    url.toString()
                } ?: throw ParsingException("Could not find format url")

            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
}

object NewPipeExtractor {
    private var newPipeDownloader: NewPipeDownloaderImpl? = null
    private var newPipeUtils: NewPipeUtils? = null
    private var isInitialized = false

    fun init() {
        if (!isInitialized) {
            newPipeDownloader = NewPipeDownloaderImpl(
                proxy = YouTube.proxy,
                proxyAuth = YouTube.proxyAuth
            )
            newPipeUtils = NewPipeUtils(newPipeDownloader!!)
            isInitialized = true
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> {
        init()
        return newPipeUtils?.getSignatureTimestamp(videoId)
            ?: Result.failure(Exception("NewPipeUtils not initialized"))
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        init()
        return newPipeUtils?.getStreamUrl(format, videoId)
    }

    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        init()
        return try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getStreamInfo(videoId: String): StreamInfo? {
        init()
        return try {
            StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId",
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ------------------------------------------------------------------
    // Comments & channel metadata
    // ------------------------------------------------------------------

    data class VideoComment(
        val commentId: String,
        val authorName: String,
        val authorThumbnail: String?,
        val content: String,
        val publishedTime: String,
        val likeCount: Long,
        val replyCount: Int,
        /** Channel URL the comment is credited to; used to backfill a missing avatar. */
        val authorChannelRef: String? = null,
        /** Continuation for this comment's replies, when it has any. */
        val repliesToken: NextPage? = null,
        val isPinned: Boolean = false,
    )

    /** Opaque wrapper for a NewPipe [Page] token, so the app module never needs
     * NewPipe on its classpath. */
    class NextPage(val page: Page?) {
        val hasMore: Boolean get() = page != null
    }

    data class VideoCommentsResult(
        val comments: List<VideoComment> = emptyList(),
        val nextPage: NextPage? = null,
    )

    fun getComments(videoId: String): VideoCommentsResult =
        try {
            val info = CommentsInfo.getInfo(service(), watchUrl(videoId))
            VideoCommentsResult(
                comments = info.relatedItems
                    .filterIsInstance<CommentsInfoItem>()
                    .map { it.toVideoComment() },
                nextPage = info.nextPage?.let { NextPage(it) },
            )
        } catch (e: Exception) {
            e.printStackTrace()
            VideoCommentsResult()
        }

    fun getMoreComments(videoId: String, token: NextPage): VideoCommentsResult =
        try {
            val more = CommentsInfo.getMoreItems(service(), watchUrl(videoId), token.page)
            VideoCommentsResult(
                comments = more.items
                    .filterIsInstance<CommentsInfoItem>()
                    .map { it.toVideoComment() },
                nextPage = more.nextPage?.let { NextPage(it) },
            )
        } catch (e: Exception) {
            e.printStackTrace()
            VideoCommentsResult()
        }

    /** Replies for one comment. Reuses the watch page's continuation endpoint. */
    fun getCommentReplies(videoId: String, token: NextPage): VideoCommentsResult =
        try {
            val more = CommentsInfo.getMoreItems(service(), watchUrl(videoId), token.page)
            VideoCommentsResult(
                comments = more.items
                    .filterIsInstance<CommentsInfoItem>()
                    .map { it.toVideoComment() },
                nextPage = more.nextPage?.let { NextPage(it) },
            )
        } catch (e: Exception) {
            e.printStackTrace()
            VideoCommentsResult()
        }

    /**
     * Channel header: name, avatar, subscriber count. Backstops the watch-page
     * owner block, which Google reparents between renderers almost every
     * version, silently blanking the channel row.
     */
    data class ChannelMetadata(
        val name: String,
        val avatarUrl: String?,
        val subscriberCount: Long,
    )

    fun getChannelMetadata(channelId: String): ChannelMetadata? =
        try {
            val info = ChannelInfo.getInfo(service(), channelUrl(channelId))
            ChannelMetadata(
                name = info.name.orEmpty(),
                avatarUrl = info.avatars.bestImageUrl(),
                subscriberCount = info.subscriberCount,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    private fun service() = NewPipe.getService(0)
    private fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"
    /**
     * Accepts either a bare UC… channel id or an @handle. Some watch payloads
     * hand back a handle where the code expected an id, and the /channel/ form
     * 404s on those, which is how the channel row goes blank.
     */
    private fun channelUrl(channelId: String): String =
        if (channelId.startsWith("@")) {
            "https://www.youtube.com/$channelId"
        } else {
            "https://www.youtube.com/channel/$channelId"
        }

    /**
     * Avatar for a comment author, taken from the channel page. YouTube leaves
     * the avatar empty on many comment payloads (replies especially), so the
     * top-level list backfills from the author's own channel URL.
     *
     * @param ref a channel URL or handle the comment credits the author with.
     */
    fun fetchCommentAvatar(ref: String?): String? {
        if (ref.isNullOrBlank()) return null
        // NewPipe hands back absolute URLs; only a bare id/handle needs building.
        val url = if (ref.startsWith("http://") || ref.startsWith("https://")) {
            ref
        } else {
            channelUrl(ref)
        }
        return try {
            ChannelInfo.getInfo(service(), url).avatars.bestImageUrl()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun CommentsInfoItem.toVideoComment() = VideoComment(
        commentId = commentId
            ?: commentText.content.hashCode().toString(),
        authorName = uploaderName.orEmpty(),
        authorThumbnail = uploaderAvatars.bestImageUrl(),
        content = commentText.content,
        publishedTime = textualUploadDate.orEmpty(),
        likeCount = likeCount.toLong(),
        replyCount = replyCount,
        authorChannelRef = uploaderUrl?.takeIf { it.isNotBlank() },
        repliesToken = replies?.let { NextPage(it) },
        isPinned = isPinned,
    )

    /** Largest of any available dimension; YouTube comment avatars often ship
     * only a width, so height alone would pick nothing at all. */
    private fun List<Image>?.bestImageUrl(): String? =
        this.orEmpty().maxByOrNull { maxOf(it.width, it.height) }?.url
            ?: this?.firstOrNull()?.url
}
