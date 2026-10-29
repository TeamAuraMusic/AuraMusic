/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.constants.AppBarHeight
import com.auramusic.app.ui.component.CollectionSubscribeButton
import com.auramusic.app.ui.component.HeroIconButton
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.MarqueeTitle
import com.auramusic.app.ui.component.MetaChip
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.menu.YouTubeVideoMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.VideoThumbnails
import com.auramusic.app.utils.compactViewCount
import com.auramusic.app.utils.linkifiedText
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeChannelPost
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.innertube.pages.YouTubeChannelPage
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

private enum class ChannelTab(val labelRes: Int) {
    Videos(R.string.channel_tab_videos),
    Shorts(R.string.channel_tab_shorts),
    Live(R.string.channel_tab_live),
    Posts(R.string.channel_tab_posts),
    About(R.string.channel_tab_about),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    navController: NavController,
    channelIdOrUrl: String,
) {
    val context = LocalContext.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // The hero is drawn under the transparent top bar, so the list itself gets no top
    // padding: the system bar + app bar height is applied inside the backdrop instead.
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = systemBarsTopPadding + AppBarHeight
    val topBarHeightPx = with(density) { topBarHeight.roundToPx() }

    val channelId = remember(channelIdOrUrl) { extractChannelId(channelIdOrUrl) }
    var resolvedChannelId by remember(channelId) { mutableStateOf(channelId) }
    var channelReloadKey by remember { mutableStateOf(0) }

    var header by remember { mutableStateOf<ChannelHeader?>(null) }
    var videos by remember(channelId) { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var channelListView by remember { mutableStateOf(false) }
    var subscribeError by remember { mutableStateOf<String?>(null) }
    var continuation by remember(channelId) { mutableStateOf<String?>(null) }
    var posts by remember(channelId) { mutableStateOf<List<YouTubeChannelPost>>(emptyList()) }
    var postsContinuation by remember(channelId) { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSubscribed by remember(channelId) { mutableStateOf(false) }
    var isSubscribing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(ChannelTab.Videos.ordinal) }
    var shortsPagerIndex by remember { mutableIntStateOf(-1) }

    // Accent drawn from the channel's own art, the way every collection screen now
    // takes its colour from the thing it is showing.
    val accent = rememberArtworkAccent(header?.bannerUrl ?: header?.avatarUrl)
    val collapseThresholdPx = with(density) { 210.dp.roundToPx() }
    val solidTopBar by remember(collapseThresholdPx) {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 ||
                gridState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (solidTopBar) 1f else 0f,
        animationSpec = tween(250),
        label = "channelTopBarAlpha",
    )

    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    suspend fun loadFirstPage(tab: ChannelTab, clear: Boolean = true) {
        if (clear) {
            isLoading = true
            videos = emptyList()
            continuation = null
            posts = emptyList()
            postsContinuation = null
        }
        error = null
        val requestChannelId = withContext(Dispatchers.IO) {
            if (resolvedChannelId.startsWith("@")) {
                YouTube.getChannelId(resolvedChannelId).ifBlank { resolvedChannelId }
            } else {
                resolvedChannelId
            }
        }
        val params = when (tab) {
            ChannelTab.Videos -> YouTubeChannelPage.VIDEOS_PARAMS
            ChannelTab.Shorts -> YouTubeChannelPage.SHORTS_PARAMS
            ChannelTab.Live -> YouTubeChannelPage.LIVE_PARAMS
            ChannelTab.Posts -> null
            ChannelTab.About -> null
        }
        if (tab == ChannelTab.Posts) {
            val page = withContext(Dispatchers.IO) {
                YouTube.youtubeChannelPosts(requestChannelId).getOrNull()
            }
            if (page != null) {
                resolvedChannelId = requestChannelId
                posts = page.posts
                postsContinuation = page.continuation
            } else if (clear) {
                error = context.getString(R.string.videos_feed_error)
            }
            isLoading = false
            return
        }
        val result = withContext(Dispatchers.IO) {
            YouTube.youtubeChannel(requestChannelId, params).getOrNull()
        }
        if (result != null) {
            resolvedChannelId = result.channelId
            header = ChannelHeader(
                channelId = result.channelId,
                title = result.title,
                avatarUrl = result.avatarUrl,
                bannerUrl = result.bannerUrl,
                subscriberCountText = result.subscriberCountText,
                videosCountText = result.videosCountText,
                description = result.description,
            )
            // The Innertube channel page sometimes hands back its shell without the
            // header fields. The extractor parses them on every response, so anything
            // still blank gets topped up from there instead of leaving the banner,
            // about text or subscriber count missing.
            val needsHeaderTopUp = result.bannerUrl == null ||
                result.subscriberCountText == null ||
                result.description.isNullOrBlank() ||
                result.avatarUrl == null ||
                result.title.isBlank()
            if (needsHeaderTopUp) {
                val extra = withContext(Dispatchers.IO) {
                    NewPipeExtractor.getChannelMetadata(result.channelId)
                }
                val current = header
                if (extra != null && current != null) {
                    header = current.copy(
                        title = current.title.ifBlank { extra.name },
                        avatarUrl = current.avatarUrl ?: extra.avatarUrl,
                        bannerUrl = current.bannerUrl ?: extra.bannerUrl,
                        subscriberCountText = current.subscriberCountText
                            ?: extra.subscriberCount.takeIf { it >= 0 }
                                ?.let { "${compactViewCount(it.toString())} subscribers" },
                        description = current.description?.takeIf { it.isNotBlank() }
                            ?: extra.description,
                    )
                }
            }
            result.isSubscribed?.let { isSubscribed = it }
            if (result.isSubscribed == null) {
                // Remote page didn't expose the toggle state; fall back to the
                // locally-persisted record from a previous subscribe action.
                val locallySubscribed = VideoPlaybackManager
                    .readSubscribedChannels(context)
                    .any { it.channelId == resolvedChannelId }
                if (locallySubscribed) isSubscribed = true
            }
            videos = result.videos
            continuation = result.continuation
        } else {
            // Innertube's channel page failed outright; the extractor still delivers
            // the header (name, avatar, banner, about), so the page opens with real
            // metadata rather than an error shell.
            val fallback = withContext(Dispatchers.IO) {
                NewPipeExtractor.getChannelMetadata(requestChannelId)
            }
            if (fallback != null) {
                resolvedChannelId = requestChannelId
                header = ChannelHeader(
                    channelId = requestChannelId,
                    title = fallback.name,
                    avatarUrl = fallback.avatarUrl,
                    bannerUrl = fallback.bannerUrl,
                    subscriberCountText = fallback.subscriberCount.takeIf { it >= 0 }
                        ?.let { "${compactViewCount(it.toString())} subscribers" },
                    videosCountText = null,
                    description = fallback.description,
                )
            } else if (clear) {
                error = context.getString(R.string.videos_feed_error)
            }
        }
        isLoading = false
    }

     suspend fun loadMore() {
        if (isLoadingMore || selectedTab == ChannelTab.About.ordinal) return
        isLoadingMore = true
        if (selectedTab == ChannelTab.Posts.ordinal) {
            val cont = postsContinuation
            if (cont != null) {
                val page = withContext(Dispatchers.IO) {
                    YouTube.youtubeChannelPosts(resolvedChannelId, cont).getOrNull()
                }
                page?.let {
                    val existing = posts.map { it.postId }.toSet()
                    posts = posts + it.posts.filter { p -> p.postId !in existing }
                    postsContinuation = it.continuation
                }
            }
            isLoadingMore = false
            return
        }
        val cont = continuation ?: run {
            isLoadingMore = false
            return
        }
        val result = withContext(Dispatchers.IO) {
            YouTube.youtubeChannelContinuation(resolvedChannelId, cont).getOrNull()
        }
        result?.let {
            val existing = videos.map { it.videoId }.toSet()
            val newVideos = it.videos.filter { v -> v.videoId !in existing }
            videos = videos + newVideos
            continuation = it.continuation
        }
        isLoadingMore = false
    }

     suspend fun refreshChannel() {
        isRefreshing = true
        loadFirstPage(ChannelTab.entries[selectedTab], clear = false)
        isRefreshing = false
    }

    fun toggleSubscribe() {
        if (isSubscribing) return
        val newSubscribed = !isSubscribed
        val previous = isSubscribed
        // Flipped immediately: the control used to wait for the round-trip and simply
        // never changed when it failed - a signed-out tap looked like a dead button.
        isSubscribed = newSubscribed
        isSubscribing = true
        scope.launch {
            // Recorded locally either way, so the Library's channel list and the video
            // overlay agree with this screen immediately.
            VideoPlaybackManager.persistChannelSubscription(
                context = context,
                channelId = resolvedChannelId,
                name = header?.title.orEmpty(),
                avatarUrl = header?.avatarUrl,
                subscribe = newSubscribed,
            )
            VideoPlaybackManager.markChannelSubscribed(resolvedChannelId, newSubscribed)
            val result = YouTube.subscribeChannel(resolvedChannelId, newSubscribed)
            if (result.isFailure) {
                // Put it back and say why, rather than leaving a state that never
                // reached the account.
                isSubscribed = previous
                VideoPlaybackManager.persistChannelSubscription(
                    context = context,
                    channelId = resolvedChannelId,
                    name = header?.title.orEmpty(),
                    avatarUrl = header?.avatarUrl,
                    subscribe = previous,
                )
                VideoPlaybackManager.markChannelSubscribed(resolvedChannelId, previous)
                subscribeError = if (!YouTube.isLoggedIn()) {
                    context.getString(R.string.subscribe_needs_account)
                } else {
                    result.exceptionOrNull()?.message ?: context.getString(R.string.subscribe_failed)
                }
            }
            isSubscribing = false
        }
    }

    LaunchedEffect(channelId, selectedTab, channelReloadKey) {
        loadFirstPage(ChannelTab.entries[selectedTab])
    }

    // Seeded from storage and then kept in step with the shared set, so a channel
    // subscribed from the video overlay already reads as subscribed here.
    LaunchedEffect(Unit) {
        VideoPlaybackManager.primeSubscribedChannels(context)
    }

    val sharedSubscriptions by VideoPlaybackManager.subscribedChannelIds.collectAsState()
    LaunchedEffect(sharedSubscriptions, resolvedChannelId) {
        if (resolvedChannelId.isNotBlank() && sharedSubscriptions.contains(resolvedChannelId)) {
            isSubscribed = true
        }
    }

    subscribeError?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(4_000)
            subscribeError = null
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && !isLoadingMore) {
            val nearEnd = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (nearEnd >= gridState.layoutInfo.totalItemsCount - 4) loadMore()
        }
    }
    LaunchedEffect(gridState, selectedTab) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 4
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }

    val isShorts = ChannelTab.entries[selectedTab] == ChannelTab.Shorts

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        refreshChannel()
                    }
                },
            ),
        contentPadding = PaddingValues(
            start = insets.calculateStartPadding(layoutDirection),
            end = insets.calculateEndPadding(layoutDirection),
            bottom = insets.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            ChannelHeaderSection(
                header = header,
                isLoading = isLoading,
                isSubscribed = isSubscribed,
                isSubscribing = isSubscribing,
                subscribeError = subscribeError,
                accent = accent,
                topBarHeightPx = topBarHeightPx,
                scrollState = gridState,
                onOpenArtist = {
                    // The artist page is keyed by this same id, so it resolves for any
                    // channel whether or not the artist happens to be in the library.
                    val channelId = header?.channelId ?: resolvedChannelId
                    if (channelId.isNotBlank()) navController.navigate("artist/$channelId")
                },
                onSubscribeClick = { toggleSubscribe() },
            )
        }

        item(
            key = "tabs",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            ChannelTabBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                listView = channelListView,
                onToggleView = { channelListView = !channelListView },
                accent = accent,
            )
        }

        when (ChannelTab.entries[selectedTab]) {
            ChannelTab.About -> {
                item(
                    key = "about",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    ChannelAboutSection(header = header, accent = accent)
                }
            }
            ChannelTab.Posts -> {
                if (isLoading && posts.isEmpty()) {
                    item(
                        key = "posts_loading",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (error != null && posts.isEmpty()) {
                    item(
                        key = "posts_error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ChannelErrorState(message = error.orEmpty())
                    }
                } else if (posts.isEmpty()) {
                    item(
                        key = "posts_empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ChannelErrorState(message = stringResource(R.string.no_posts_found))
                    }
                } else {
                    items(
                        count = posts.size,
                        key = { "post_${posts[it].postId}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { index ->
                        ChannelPostCard(post = posts[index])
                    }
                    if (isLoadingMore) {
                        item(
                            key = "posts_loading_more",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                if (isLoading && videos.isEmpty()) {
                    items(count = 4, key = { "skeleton_$it" }, span = { GridItemSpan(1) }) {
                        ChannelVideoSkeleton()
                    }
                } else if (error != null && videos.isEmpty()) {
                    item(
                        key = "error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ChannelErrorState(message = error.orEmpty())
                    }
                } else if (videos.isEmpty()) {
                    item(
                        key = "empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ChannelErrorState(message = stringResource(R.string.no_videos_found))
                    }
                } else if (isShorts) {
                    items(
                        count = videos.size,
                        key = { "short_${videos[it].videoId}" },
                        span = { GridItemSpan(1) },
                    ) { index ->
                        val video = videos[index]
                        ChannelShortsCard(
                            video = video,
                            onClick = {
                                shortsPagerIndex = index
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(
                            key = "loading_more",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                } else {
                    val playVideo: (YouTubeVideoItem) -> Unit = { video ->
                        VideoPlaybackManager.playWithDetails(
                            context = context,
                            videoId = video.videoId,
                            title = video.title,
                            channelName = video.channelName.ifBlank { header?.title.orEmpty() },
                            channelId = video.channelId ?: header?.channelId,
                            channelThumbnail = header?.avatarUrl,
                            description = video.description,
                            viewCountText = video.viewCountText,
                            publishedTimeText = video.publishedTimeText,
                            thumbnails = video.thumbnails,
                            channels = video.channels,
                        )
                    }
                    if (channelListView) {
                        items(
                            count = videos.size,
                            key = { "video_${videos[it].videoId}" },
                            span = { GridItemSpan(maxLineSpan) },
                        ) { index ->
                            val video = videos[index]
                            ChannelVideoRow(video = video, onClick = { playVideo(video) })
                        }
                    } else {
                        // Grid: the same run of videos, tiled. YouTube's channel API no
                        // longer returns shelves or section titles for a channel (verified
                        // live: a flat list of items, zero shelf renderers), so the layout
                        // is the only grouping available - grid or list, nothing invented.
                        items(
                            count = videos.size,
                            key = { "video_${videos[it].videoId}" },
                        ) { index ->
                            val video = videos[index]
                            ChannelVideoGridCard(video = video, onClick = { playVideo(video) })
                        }
                    }
                    if (isLoadingMore) {
                        item(
                            key = "loading_more",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
        }

        if (shortsPagerIndex >= 0 && videos.isNotEmpty()) {
            ShortsVerticalPager(
                shorts = videos,
                startIndex = shortsPagerIndex,
                channelTitle = header?.title,
                channelAvatarUrl = header?.avatarUrl,
                onClose = {
                    shortsPagerIndex = -1
                    VideoPlaybackManager.collapse()
                },
            )
        }

        // Transparent over the banner; solid with the channel name once the hero has
        // scrolled under it. The back button lives here instead of floating over the
        // picture, and the long press still returns to the main screen.
        val appBarSurface = MaterialTheme.colorScheme.surface
        TopAppBar(
            title = {
                if (topBarAlpha > 0.01f) {
                    Text(
                        text = header?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
                    )
                }
            },
            navigationIcon = {
                androidx.compose.material3.IconButton(
                    onClick = navController::navigateUp,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val channelLink = "https://www.youtube.com/channel/" +
                            (header?.channelId ?: resolvedChannelId)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Channel Link", channelLink))
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.link),
                        contentDescription = stringResource(R.string.share),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = appBarSurface.copy(alpha = topBarAlpha),
                scrolledContainerColor = appBarSurface.copy(alpha = topBarAlpha),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun ChannelHeaderSection(
    header: ChannelHeader?,
    isLoading: Boolean,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    subscribeError: String?,
    accent: Color,
    topBarHeightPx: Int,
    scrollState: LazyGridState,
    onOpenArtist: () -> Unit,
    onSubscribeClick: () -> Unit,
) {
    val channel = header
    if (channel == null) {
        if (isLoading) ChannelHeroSkeleton(topBarHeightPx = topBarHeightPx)
        return
    }

    val background = MaterialTheme.colorScheme.background
    val banner = channel.bannerUrl
    val avatar = channel.avatarUrl

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .drawUnderTopBar(topBarHeightPx)
            .clipToBounds(),
    ) {
        // Channel art is far wider than it is tall; the hero keeps enough room for
        // the avatar, name and stats that sit at its foot.
        val heroHeight = (maxWidth * 0.95f).coerceIn(340.dp, 500.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        ) {
            if (banner != null) {
                // Same parallax drift as the artist hero, so scrolling the page moves
                // the art rather than sliding a flat picture.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val scrolled = if (scrollState.firstVisibleItemIndex == 0) {
                                scrollState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                0f
                            }
                            translationY = scrolled * 0.5f
                            if (size.height > 0f) {
                                alpha = 1f - (scrolled / size.height).coerceIn(0f, 1f) * 0.6f
                            }
                        },
                ) {
                    AsyncImage(
                        model = banner,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                // Accent wash towards the bottom of the banner.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.4f to Color.Transparent,
                                1f to accent.copy(alpha = 0.28f),
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.45f),
                                    background,
                                )
                            )
                        ),
                )
            }

            // Scrim under the top bar and a strong fade into the background.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0.55f),
                            0.28f to Color.Transparent,
                            0.5f to Color.Transparent,
                            0.78f to background.copy(alpha = 0.72f),
                            1f to background,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            ) {
                // Identity bottom-left over the fade, the way the artist hero sets its
                // name: a large avatar, the channel name at display size, stats beneath.
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = accent.copy(alpha = 0.5f),
                            spotColor = accent.copy(alpha = 0.7f),
                        )
                        .clip(CircleShape)
                        .background(background)
                        .padding(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatar != null) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = channel.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = channel.title.trim().firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.channel).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = channel.title.ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.displaySmall.copy(
                        shadow = Shadow(
                            color = background.copy(alpha = 0.6f),
                            blurRadius = 24f,
                        ),
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val chips = listOfNotNull(
                    channel.subscriberCountText,
                    channel.videosCountText,
                )
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chips.forEach { MetaChip(it) }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CollectionSubscribeButton(
                        subscribed = isSubscribed,
                        accent = accent,
                        onClick = onSubscribeClick,
                        enabled = !isSubscribing,
                    )
                    Spacer(Modifier.weight(1f))
                    HeroIconButton(
                        icon = R.drawable.person,
                        contentDescription = stringResource(R.string.open_artist),
                        onClick = onOpenArtist,
                    )
                }

                // A rejected subscribe used to look like a dead button: nothing moved
                // and nothing was said. This names the reason instead.
                subscribeError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelHeroSkeleton(topBarHeightPx: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .drawUnderTopBar(topBarHeightPx)
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .shimmer(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .shimmer(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelTabBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    listView: Boolean,
    onToggleView: () -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = accent,
        edgePadding = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ChannelTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab.ordinal,
                onClick = { onSelect(tab.ordinal) },
                text = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == tab.ordinal) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == tab.ordinal) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        }
        }
        // Grid or list: the channel feed is a flat run of videos, and a list reads far
        // better for the titles and metadata than a two-column grid does.
        IconButton(onClick = onToggleView) {
            Icon(
                painter = painterResource(
                    if (listView) R.drawable.grid_view else R.drawable.view_list,
                ),
                contentDescription = stringResource(
                    if (listView) R.string.videos_grid_view else R.string.videos_list_view,
                ),
            )
        }
    }
}

@Composable
private fun ChannelAboutSection(header: ChannelHeader?, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val description = header?.description?.takeIf { it.isNotBlank() }
        val chips = listOfNotNull(header?.subscriberCountText, header?.videosCountText)
        if (description == null && chips.isEmpty()) {
            Text(
                text = stringResource(R.string.channel_no_about),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (description != null) {
                        Text(
                            text = linkifiedText(description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (chips.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            chips.forEach { MetaChip(it) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelVideoGridCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    val showMenu = rememberChannelVideoMenu(video)
    Column(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = showMenu)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ThumbnailWithBadge(
            thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
                ?: VideoThumbnails.highQuality(video.videoId),
            durationText = video.durationText,
            isLive = video.isLive,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        )
        MarqueeTitle(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val meta = listOfNotNull(video.viewCountText, video.publishedTimeText)
            .joinToString(" \u00b7 ")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
            VideoMenuButton(onClick = showMenu)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelVideoRow(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    val showMenu = rememberChannelVideoMenu(video)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = showMenu)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        ThumbnailWithBadge(
            thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
                ?: VideoThumbnails.highQuality(video.videoId),
            durationText = video.durationText,
            isLive = video.isLive,
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    video.viewCountText?.let { compactViewCount(it) },
                    video.publishedTimeText,
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        VideoMenuButton(onClick = showMenu)
    }
}

/** Opens a channel video's menu; the channel entries are left out because it is already open. */
@Composable
private fun rememberChannelVideoMenu(video: YouTubeVideoItem): () -> Unit {
    val menuState = LocalMenuState.current
    return {
        menuState.show {
            YouTubeVideoMenu(
                video = video,
                channels = emptyList(),
                onChannelClick = null,
                onDismiss = menuState::dismiss,
            )
        }
    }
}

@Composable
private fun ChannelShortsCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
                    ?: VideoThumbnails.highQuality(video.videoId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            video.durationText?.let {
                TextBadge(
                    text = it,
                    containerColor = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }
        }
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        )
        Text(
            text = video.viewCountText?.let { compactViewCount(it) }.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun ChannelPostCard(post: YouTubeChannelPost) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.authorThumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                post.publishedTimeText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (post.contentText.isNotBlank()) {
            Text(
                text = post.contentText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            if (post.imageUrls.size == 1) {
                AsyncImage(
                    model = post.imageUrls.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.imageUrls.chunked(2).forEach { rowUrls ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowUrls.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (post.voteCountText != null || post.commentCountText != null) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (post.voteCountText != null) {
                    val voteCountText = post.voteCountText.orEmpty()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_thumb_up),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = voteCountText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (post.commentCountText != null) {
                    val commentCountText = post.commentCountText.orEmpty()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_comment),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = commentCountText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelVideoSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .shimmer(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .shimmer(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .shimmer(),
            )
        }
    }
}

@Composable
private fun ChannelErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ThumbnailWithBadge(
    thumbnailUrl: String,
    durationText: String?,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        when {
            isLive -> TextBadge(
                text = "LIVE",
                containerColor = Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
            durationText != null -> TextBadge(
                text = durationText,
                containerColor = Color.Black.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun TextBadge(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(containerColor)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private data class ChannelHeader(
    val channelId: String,
    val title: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCountText: String?,
    val videosCountText: String?,
    val description: String?,
)

/** Accepts raw UC ids, @handles, /c/ and /user/ URLs. */
internal fun extractChannelId(raw: String): String {
     val decoded = URLDecoder.decode(raw, "UTF-8")
     val clean = decoded.substringBefore("?")
     return when {
         clean.startsWith("UC") -> clean
         clean.startsWith("@") -> clean
         clean.contains("/channel/") -> clean.substringAfter("/channel/")
         clean.contains("/@") -> "@" + clean.substringAfter("/@").substringBefore("/")
         clean.contains("/c/") -> clean.substringAfter("/c/").substringBefore("/")
         clean.contains("/user/") -> clean.substringAfter("/user/").substringBefore("/")
         else -> clean
     }
 }
/**
 * Lays the content out [shiftPx] higher than its slot and reports that much less height,
 * so the hero sits under the transparent top bar without leaving a gap below it.
 */
private fun Modifier.drawUnderTopBar(shiftPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height - shiftPx).coerceAtLeast(0)) {
        placeable.place(0, -shiftPx)
    }
}
