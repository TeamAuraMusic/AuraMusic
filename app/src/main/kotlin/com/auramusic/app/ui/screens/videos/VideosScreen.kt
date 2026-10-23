@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.constants.VideoFeedGridViewKey
import com.auramusic.app.ui.component.ChannelAvatarStack
import com.auramusic.app.ui.component.MarqueeTitle
import com.auramusic.app.ui.component.ChannelPickerDialog
import com.auramusic.app.ui.component.prefetchVideoChannels
import com.auramusic.app.ui.component.rememberVideoChannels
import com.auramusic.app.ui.component.shimmer.ShimmerHost
import com.auramusic.app.utils.compactViewCount
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.menu.YouTubeVideoMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    var gridView by rememberPreference(VideoFeedGridViewKey, true)

    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val configuration = LocalConfiguration.current

    // Personalised video recommendations
    val recommendationManager = remember {
        com.auramusic.app.video.VideoRecommendationManager(context)
    }
    com.auramusic.app.video.VideoPlaybackManager.onVideoPlayed = {
        recommendationManager.onVideoWatched()
    }
    val personalSections by recommendationManager.sections.collectAsState()
    val isLoadingPersonal by recommendationManager.isLoading.collectAsState()

    // Read once here rather than per row: reading uiState.value inside the items lambdas is
    // not a snapshot read, so the "now playing" highlight never updated until something else
    // happened to recompose the list.
    val nowPlayingVideoId by remember {
        VideoPlaybackManager.uiState
            .map { it.session?.videoId }
            .distinctUntilChanged()
    }.collectAsState(initial = VideoPlaybackManager.uiState.value.session?.videoId)
    val isCurrentlyPlaying: (String) -> Boolean = { it == nowPlayingVideoId }

    // Watch progress per video, keyed by id. Read once at the top of the screen (not per
    // row) and driven by DataStore, so the watch bars repaint as playback records progress
    // and a tap on any card knows where that video was left.
    val watchHistoryById: Map<String, VideoPlaybackManager.VideoHistoryEntry> by remember {
        VideoPlaybackManager.videoHistoryFlow(context)
            .map { history -> history.associateBy { it.videoId } }
            .distinctUntilChanged()
    }.collectAsState(initial = emptyMap())
    val resumePositionFor: (String) -> Long = { videoId ->
        // Only a partially watched video resumes; a finished one starts over.
        val entry = watchHistoryById[videoId]
        if (entry == null || !entry.shouldResume) 0L else entry.positionMs
    }

    // What the last visit to this tab found, kept for the life of the process, so
    // coming back to it paints rows instead of a skeleton. The refresh still runs -
    // it just lands on top of something rather than in place of nothing.
    var feed by remember { mutableStateOf(VideoFeedCache.items) }
    // Paging no longer rides on a continuation token, so "is there more" is tracked
    // directly: a fetch that adds nothing new counts against the budget below.
    var canLoadMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(VideoFeedCache.items.isEmpty()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    /** Consecutive pages that brought nothing new; a few in a row end the feed. */
    var emptyFetches by remember { mutableIntStateOf(0) }

    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    /** Rotates the feed query, steered by the recently watched videos and channels. */
    /** What the user has been watching, reduced to the words worth searching for. */
    fun feedKeywords(): List<String> {
        val history = watchHistoryById.values.toList()
        return com.auramusic.app.video.VideoFeedQueryPicker.keywordsFrom(
            titles = history.map { it.title },
            channels = history.map { it.channelName },
        )
    }

    suspend fun nextFeedQuery(): String =
        com.auramusic.app.video.VideoFeedQueryPicker.nextQuery(feedKeywords())

    // Whether this entry to the screen has already asked for its first page.
    //
    // This used to be inferred from the feed being empty, which stopped being the same
    // question once the rows from the last visit are shown straight away: on re-entry
    // the feed is full and the first request would have been a rotated phrase instead
    // of the one that is certain to come back with music in it.
    var hasFetchedFirstPage by remember { mutableStateOf(false) }

    suspend fun fetchFeed(showError: Boolean = true) {
        // The first page asks for something certain rather than a rotated query: a
        // personalised phrase can come back with no music in it at all, every row gets
        // filtered away, and the screen opens on nothing while each of those checks
        // waits on the network. Rotation starts from the second page on.
        val query = if (!hasFetchedFirstPage) {
            context.getString(R.string.video_category_music_query)
        } else {
            nextFeedQuery()
        }
        hasFetchedFirstPage = true
        withContext(Dispatchers.IO) {
            val result = YouTube.youtubeCategoryFeed(query).getOrNull()
            if (result != null && result.items.isNotEmpty()) {
                // Only keep what YouTube itself reports as music, and drop repeats within the
                // page: the grid keys rows by video id and a duplicate throws at runtime.
                feed = com.auramusic.app.video.MusicContentFilter.filter(result.items)
                    .distinctBy { it.videoId }
                emptyFetches = 0
                canLoadMore = true
                error = null
            } else {
                // The sure query can still come back thin. One rotated attempt before
                // showing an empty screen, so a bad round never blanks the feed.
                val retry = withContext(Dispatchers.IO) {
                    YouTube.youtubeCategoryFeed(nextFeedQuery()).getOrNull()
                }?.items
                    ?.let { com.auramusic.app.video.MusicContentFilter.filter(it).distinctBy { v -> v.videoId } }
                    ?.takeIf { it.isNotEmpty() }
                if (retry != null) {
                    feed = retry
                    error = null
                } else if (showError) {
                    error = context.getString(R.string.videos_feed_error)
                }
            }
        }
        VideoFeedCache.fetchedAt = System.currentTimeMillis()
        isLoading = false
    }

    suspend fun loadFirstPage() {
        // Only clear the page when there is nothing on it. Re-entering the tab with
        // rows already showing should refresh them, not take them away and put a
        // skeleton where they were.
        if (feed.isEmpty()) {
            isLoading = true
            error = null
            canLoadMore = true
            emptyFetches = 0
        }
        fetchFeed()
    }

    // Silently by default. A refresh that started itself has no one waiting on it,
    // and the indicator over the page - a spinner that appeared every time the tab was
    // opened - was that refresh announcing itself. Only a pull is a request the user
    // made and expects to see answered.
    suspend fun refreshFeed(showIndicator: Boolean = false) {
        if (showIndicator) isRefreshing = true
        try {
            fetchFeed(showError = false)
        } finally {
            isRefreshing = false
        }
    }

    /**
     * Next page of the feed.
     *
     * YouTube answers a search continuation with an empty page now, so paging is done by
     * asking for a fresh query instead - which also means the feed keeps changing, and
     * the query follows what has been watched rather than staying one fixed phrase.
     */
    suspend fun loadMore() {
        if (isLoadingMore) return
        isLoadingMore = true
        withContext(Dispatchers.IO) {
            // Rotating queries overlap heavily, so a page can come back holding nothing
            // this list has not already shown. One attempt is not enough to call the feed
            // finished - it takes several in a row, and each one asks a different
            // question, which is what stopped the spinner from ever turning into rows.
            var addedThisRound = false
            for (attempt in 0 until MAX_EMPTY_FEED_FETCHES) {
                // Several queries per scroll rather than a longer wait: they run
                // together, their results are checked against each other once, and the
                // rows all arrive in the same update instead of trickling in.
                val queries = com.auramusic.app.video.VideoFeedQueryPicker.nextQueries(
                    historyKeywords = feedKeywords(),
                    count = QUERIES_PER_FEED_PAGE,
                )
                val items = coroutineScope {
                    queries
                        .map { query -> async { YouTube.youtubeCategoryFeed(query).getOrNull()?.items.orEmpty() } }
                        .awaitAll()
                        .flatten()
                }
                if (items.isEmpty()) continue

                // One pass over everything: the content check is the expensive part, and
                // batching it across all of these queries costs far less than checking
                // each page on its own.
                val musicOnly = com.auramusic.app.video.MusicContentFilter
                    .filter(items)
                    .distinctBy { it.videoId }
                if (musicOnly.isEmpty()) continue

                val seen = feed.mapTo(HashSet()) { it.videoId }
                val fresh = musicOnly.filter { video -> seen.add(video.videoId) }
                if (fresh.isNotEmpty()) {
                    feed = feed + fresh
                    addedThisRound = true
                    break
                }
            }
            // The feed is only finished once whole rounds of different queries have come
            // back with nothing new at all.
            emptyFetches = if (addedThisRound) 0 else emptyFetches + 1
            canLoadMore = emptyFetches < MAX_EMPTY_FEED_FETCHES
        }
        isLoadingMore = false
    }


    val columns = when {
        configuration.screenWidthDp >= 550 -> 3
        else -> 2
    }

    // One place keeps the kept copy in step, whatever changed the feed - a first
    // page, a refresh, or rows appended at the bottom.
    LaunchedEffect(feed) {
        VideoFeedCache.items = feed
    }

    LaunchedEffect(Unit) {
        // Rows from a moment ago are still good. Opening the tab again should not
        // spend a request on them, nor a spinner - only a genuinely stale page or an
        // empty one is worth fetching on arrival.
        val stillFresh = feed.isNotEmpty() &&
            System.currentTimeMillis() - VideoFeedCache.fetchedAt < FEED_MIN_REFRESH_INTERVAL_MS
        if (!stillFresh) loadFirstPage()
    }

    // Warm the collaborations for the rows about to be drawn. Resolving them lazily
    // meant a card first drew a single avatar and then swapped to two, and a tap in
    // between opened the channel page instead of offering the choice.
    LaunchedEffect(feed) {
        if (feed.isEmpty()) return@LaunchedEffect
        prefetchVideoChannels(
            videos = feed.take(24).map { it.videoId to it.channelName },
            scope = scope,
        )
    }

    // The same for the personalised sections above them.
    LaunchedEffect(personalSections) {
        prefetchVideoChannels(
            videos = personalSections
                .flatMap { section -> section.videos }
                .take(16)
                .map { it.videoId to it.channelName },
            scope = scope,
        )
    }

    // Auto-refresh the feed on return: when the screen resumes (app foregrounded,
    // or the user navigates back from search / a channel / the video player),
    // silently reload the first page if we're not already loading.
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasLoadedOnce by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLoadedOnce && !isLoading) {
                // Coming back from the player is not a request for fresh rows, and it
                // is not an occasion to put a spinner over the page either. Only
                // refetch once what is on screen has had time to go stale, and then
                // quietly - the indicator is for refreshes the user asked for.
                val age = System.currentTimeMillis() - VideoFeedCache.fetchedAt
                if (age >= FEED_MIN_REFRESH_INTERVAL_MS) {
                    scope.launch { refreshFeed(showIndicator = false) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading && feed.isNotEmpty()) hasLoadedOnce = true
    }

    // Load the personalised sections on entry (skipped when a previous entry already
    // built them), then again after watching 2-3 videos. Without the initial refresh()
    // nothing ever populated them: shouldRefresh starts false, so the collector below
    // had nothing to react to until three new watches.
    LaunchedEffect(Unit) {
        if (personalSections.isEmpty()) recommendationManager.refresh()
        snapshotFlow { recommendationManager.shouldRefresh }
            .collect { shouldRefresh ->
                if (shouldRefresh && !recommendationManager.isLoading.value) {
                    scope.launch { recommendationManager.refresh() }
                }
            }
    }

    val gridListState = rememberLazyGridState()
    val listListState = rememberLazyListState()

    LaunchedEffect(gridListState, gridView) {
        snapshotFlow {
            val last = gridListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridListState.layoutInfo.totalItemsCount - 8
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    LaunchedEffect(listListState, gridView) {
        snapshotFlow {
            val last = listListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listListState.layoutInfo.totalItemsCount - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    // After a page finishes loading, re-check the end so pagination keeps
    // fetching when the user is already sitting at the bottom of the list.
    LaunchedEffect(isLoadingMore) {
        if (!isLoadingMore) {
            val nearEnd = if (gridView) {
                val last = gridListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= gridListState.layoutInfo.totalItemsCount - 8
            } else {
                val last = listListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= listListState.layoutInfo.totalItemsCount - 3
            }
            if (nearEnd) loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .padding(bottom = insets.calculateBottomPadding())
    ) {
        VideosTopBar(
            gridView = gridView,
            onToggleView = { gridView = !gridView },
            onSearchClick = { navController.navigate("video_search/") },
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            // The one refresh that should be visible: the user pulled.
                            refreshFeed(showIndicator = true)
                        }
                    },
                ),
            contentAlignment = Alignment.TopStart
        ) {
            when {
                // A skeleton is for a page with nothing on it, not for one being
                // updated. Covering Continue Watching with a placeholder until the
                // feed answers is what made the tab look empty for seconds at a time.
                isLoading && feed.isEmpty() && personalSections.isEmpty() ->
                    SkeletonFeed(columns = columns, gridView = gridView)
                error != null && feed.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.slow_motion_video),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        val err = error
                        Text(
                            text = err.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
else -> {
                    val playVideo: (YouTubeVideoItem) -> Unit = { video ->
                        VideoPlaybackManager.playWithDetails(
                            context = context,
                            videoId = video.videoId,
                            title = video.title,
                            channelName = video.channelName,
                            channelId = video.channelId,
                            description = video.description,
                            viewCountText = video.viewCountText,
                            publishedTimeText = video.publishedTimeText,
                            thumbnails = video.thumbnails,
                            channels = video.channels,
                            startPositionMs = resumePositionFor(video.videoId),
                        )
                    }
                     val openChannel: (String) -> Unit = { channelId ->
                         navController.navigate("youtube_channel/$channelId?ts=${System.currentTimeMillis()}")
                     }

                    // Personalised sections integrated at the top of the Music feed
                    // Shown as soon as any section exists, not once all of them do:
                    // the sections publish one at a time now, and the old guard on
                    // isLoadingPersonal held every one of them back until the last
                    // search had returned.
                    if (personalSections.isNotEmpty() || isLoadingPersonal) {
                        if (gridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                state = gridListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // While the sections build there is nothing else worth
                                // showing, so Home's contained spinner stands in.
                                if (personalSections.isEmpty() && isLoadingPersonal) {
                                    item(
                                        key = "sections_loading",
                                        span = { GridItemSpan(maxLineSpan) },
                                    ) {
                                        SectionsLoadingPlaceholder()
                                    }
                                }
                                personalSections.forEachIndexed { sectionIndex, section ->
                                    item(
                                        key = "section_${sectionIndex}_${section.title}",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        SectionHeader(title = section.title)
                                    }
                                    items(
                                        items = section.videos,
                                        key = { "section${sectionIndex}_video_${it.videoId}" }
                                    ) { video ->
                                        FeedVideoGridCard(
                                            video = video,
                                            isNowPlaying = isCurrentlyPlaying(video.videoId),
                                            watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                            onClick = { playVideo(video) },
                                            onChannelClick = openChannel,
                                        )
                                    }
                                }
                                // The main feed had no heading at all, so it ran straight
                                // on from Subscriptions and looked like part of it.
                                item(
                                    key = "feed_header",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    SectionHeader(title = stringResource(R.string.video_section_feed))
                                }
                                items(
                                    items = feed,
                                    key = { "video_${it.videoId}" }
                                ) { video ->
                                    FeedVideoGridCard(
                                        video = video,
                                        isNowPlaying = isCurrentlyPlaying(video.videoId),
                                        watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                        onClick = { playVideo(video) },
                                        onChannelClick = openChannel,
                                    )
                                }
                                item(
                                    key = "feed_footer",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    GridListFooter(
                                        isLoadingMore = isLoadingMore,
                                        hasMore = canLoadMore,
                                        columns = columns,
                                        gridView = gridView,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                if (personalSections.isEmpty() && isLoadingPersonal) {
                                    item(key = "sections_loading") {
                                        SectionsLoadingPlaceholder()
                                    }
                                }
                                personalSections.forEachIndexed { sectionIndex, section ->
                                    item(key = "section_${sectionIndex}_${section.title}") {
                                        SectionHeader(title = section.title)
                                    }
                                    items(
                                        items = section.videos,
                                        key = { "section${sectionIndex}_video_${it.videoId}" }
                                    ) { video ->
                                        FeedVideoListRow(
                                            video = video,
                                            isNowPlaying = isCurrentlyPlaying(video.videoId),
                                            watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                            onClick = { playVideo(video) },
                                            onChannelClick = openChannel,
                                        )
                                    }
                                }
                                item(key = "feed_header") {
                                    SectionHeader(title = stringResource(R.string.video_section_feed))
                                }
                                items(
                                    items = feed,
                                    key = { "video_${it.videoId}" }
                                ) { video ->
                                    FeedVideoListRow(
                                        video = video,
                                        isNowPlaying = isCurrentlyPlaying(video.videoId),
                                        watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                        onClick = { playVideo(video) },
                                        onChannelClick = openChannel,
                                    )
                                }
                                item(key = "feed_footer") {
                                    GridListFooter(
                                        isLoadingMore = isLoadingMore,
                                        hasMore = canLoadMore,
                                        columns = columns,
                                        gridView = gridView,
                                    )
                                }
                            }
                        }
                    } else if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            items(
                                items = feed,
                                key = { "video_${it.videoId}" }
                            ) { video ->
                                FeedVideoGridCard(
                                    video = video,
                                    isNowPlaying = isCurrentlyPlaying(video.videoId),
                                    watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                    onClick = { playVideo(video) },
                                    onChannelClick = openChannel,
                                )
                            }
                            item(
                                key = "feed_footer",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                GridListFooter(
                                    isLoadingMore = isLoadingMore,
                                    hasMore = canLoadMore,
                                    columns = columns,
                                    gridView = gridView,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = feed,
                                key = { "video_${it.videoId}" }
                            ) { video ->
                                FeedVideoListRow(
                                    video = video,
                                    isNowPlaying = isCurrentlyPlaying(video.videoId),
                                    watchProgress = watchHistoryById[video.videoId]?.watchProgress ?: 0f,
                                    onClick = { playVideo(video) },
                                    onChannelClick = openChannel,
                                )
                            }
                            item(key = "feed_footer") {
                                GridListFooter(
                                    isLoadingMore = isLoadingMore,
                                    hasMore = canLoadMore,
                                    columns = columns,
                                    gridView = gridView,
                                )
                            }
                        }
                    }
                }
            }

            // Drawn last, on purpose: inside this container the sections and the feed
            // are laid out on top of anything declared before them, so a spinner placed
            // up here sat behind Continue Watching and never showed at all.
            androidx.compose.animation.AnimatedVisibility(
                visible = isRefreshing && !isLoading,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}

/**
 * The last feed this process built.
 *
 * Not a cache that stops the page asking again - it asks every time it opens, exactly
 * as before. This is only what is on the screen while it does. Leaving the tab and
 * coming back used to mean a skeleton and a fresh round trip before a single card
 * appeared, for rows that had been found a minute earlier.
 */
private object VideoFeedCache {
    var items: List<YouTubeVideoItem> = emptyList()

    /** When [items] was last written, so a quick look round does not refetch. */
    var fetchedAt: Long = 0L
}

/**
 * How old the rows have to be before opening the tab refetches them.
 *
 * Short enough that coming back later gets fresh rows, long enough that flicking
 * between tabs - or back from a video - does not fire a request and a spinner.
 */
private const val FEED_MIN_REFRESH_INTERVAL_MS = 30_000L

@Composable
private fun VideosTopBar(
    gridView: Boolean,
    onToggleView: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.videos),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            onClick = onSearchClick,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search_youtube),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.search_tap_to_search),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleView) {
            Icon(
                painter = painterResource(if (gridView) R.drawable.list else R.drawable.grid_view),
                contentDescription = stringResource(
                    if (gridView) R.string.videos_list_view else R.string.videos_grid_view
                ),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SkeletonFeed(columns: Int, gridView: Boolean) {
    if (!gridView) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    ShimmerHost(
                        modifier = Modifier
                            .width(160.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerHost(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(15.dp)
                                .clip(RoundedCornerShape(7.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        ShimmerHost(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(8) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    ShimmerHost(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerHost(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridListFooter(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    columns: Int,
    gridView: Boolean,
) {
    when {
        // Skeleton tiles plus the same contained spinner Home shows, so it is obvious
        // the next page is on its way and not that the feed simply ended.
        isLoadingMore -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadMoreShimmer(columns = columns, gridView = gridView)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator()
            }
        }
        hasMore -> Unit
        else -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_more_content),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun LoadMoreShimmer(columns: Int, gridView: Boolean) {
    if (gridView) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(columns) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    ShimmerHost(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ShimmerHost(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerHost(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            ShimmerHost(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * The red watch bar along the bottom of a thumbnail. Deliberately two layers: a dark
 * backing that stays visible over a bright thumbnail, and the watched portion on top, so
 * the bar reads the same against a white sky and a black stage.
 */
/**
 * Section heading, with an icon that says what the section is.
 *
 * The sections are titled only, so a screen with four of them reads as a wall of
 * headings with no cue about which is which. Each known section gets its own Material
 * icon; an unrecognised title simply renders without one rather than guessing.
 */
/**
 * Section heading, styled like the Home screen's cards.
 *
 * A heading in a tinted block with the icon sitting in its own rounded plate reads as
 * part of the content instead of a label stuck on top of it, and gives each section a
 * visible edge - which is what was missing when the sections ran together as one long
 * undifferentiated scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionsLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        ContainedLoadingIndicator()
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val icon = sectionIconFor(title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Air between sections: they used to run into each other with nothing
            // marking where one ended, so a long scroll read as one flat run.
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // A short rule that trails off: a section marker without turning the heading
        // into a labelled block of its own.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        )
    }
}

private fun sectionIconFor(title: String): ImageVector? = when {
    title.contains("continue", ignoreCase = true) -> Icons.Filled.PlayArrow
    title.contains("channel", ignoreCase = true) -> Icons.Filled.Person
    title.contains("recommend", ignoreCase = true) -> Icons.Filled.AutoAwesome
    title.contains("subscription", ignoreCase = true) -> Icons.Filled.Subscriptions
    title.contains("feed", ignoreCase = true) -> Icons.Filled.VideoLibrary
    else -> null
}

@Composable
internal fun VideoWatchBar(progress: Float, modifier: Modifier = Modifier) {
    val watched = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(watched)
                .fillMaxHeight()
                .background(Color(0xFFFF0033)),
        )
    }
}

@Composable
internal fun NowPlayingBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "nowPlaying")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = "♫",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .height(12.dp)
                .padding(top = 1.dp)
        ) {
            listOf(0.45f, 0.95f, 0.62f, 0.82f).forEachIndexed { index, base ->
                val scale by transition.animateFloat(
                    initialValue = base * 0.55f,
                    targetValue = base,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 420 + index * 90),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar$index"
                )
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((12.dp * scale).coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}


@Composable
internal fun ChannelAvatar(
    channelName: String,
    channelThumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (channelThumbnailUrl != null) {
            AsyncImage(
                model = channelThumbnailUrl,
                contentDescription = channelName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedVideoGridCard(
    video: YouTubeVideoItem,
    isNowPlaying: Boolean,
    watchProgress: Float = 0f,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    var showChannelPicker by remember { mutableStateOf(false) }
    val channels = rememberVideoChannels(video.videoId, video.channels, video.channelName)
    val menuState = LocalMenuState.current
    val showMenu: () -> Unit = {
        menuState.show {
            YouTubeVideoMenu(
                video = video,
                channels = channels,
                onChannelClick = onChannelClick,
                onDismiss = menuState::dismiss,
            )
        }
    }
    Box {
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (isNowPlaying) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(18.dp)
                        )
                    } else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = showMenu,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            val thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))
                        )
                    )
            )

            if (isNowPlaying) {
                NowPlayingBars(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            if (video.isLive) {
                val livePulse by rememberInfiniteTransition(label = "live").animateFloat(
                    initialValue = 1f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "livePulse"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = livePulse))
                    )
                    Text(
                        text = stringResource(R.string.live),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (video.durationText != null) {
                val durationText = video.durationText
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText.orEmpty(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Watch bar along the thumbnail's bottom edge: how much of this video was
            // already watched. A finished video keeps a full bar, which is the only hint
            // that a row is "already seen" rather than brand new.
            if (watchProgress > 0f) {
                VideoWatchBar(
                    progress = watchProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }

        // Inset the text block so the card's rounded corners don't clip the
        // leading digit of the views line (e.g. "2.0M" losing the "2").
        MarqueeTitle(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val channelClick: () -> Unit = {
                if (channels.size > 1) showChannelPicker = true
                else video.channelId?.let(onChannelClick)
            }
            ChannelAvatarStack(
                channels = channels,
                primaryThumbnailUrl = video.channelThumbnailUrl,
                size = 34.dp,
                onClick = channelClick,
            )
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = channelClick)
                )
            }
            VideoMenuButton(onClick = showMenu)
        }
        val compactViews = video.viewCountText?.let { compactViewCount(it) }
        if (compactViews != null) {
            Text(
                text = listOfNotNull(compactViews, video.publishedTimeText).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 2.dp)
            )
        }
        }
        if (showChannelPicker) {
            ChannelPickerDialog(
                channels = channels,
                onDismiss = { showChannelPicker = false },
                onSelect = { channel ->
                    showChannelPicker = false
                    channel.id?.let(onChannelClick)
                },
                primaryThumbnailUrl = video.channelThumbnailUrl,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedVideoListRow(
    video: YouTubeVideoItem,
    isNowPlaying: Boolean,
    watchProgress: Float = 0f,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    var showChannelPicker by remember { mutableStateOf(false) }
    val channels = rememberVideoChannels(video.videoId, video.channels, video.channelName)
    val menuState = LocalMenuState.current
    val showMenu: () -> Unit = {
        menuState.show {
            YouTubeVideoMenu(
                video = video,
                channels = channels,
                onChannelClick = onChannelClick,
                onDismiss = menuState::dismiss,
            )
        }
    }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = showMenu,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            val thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (video.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.live),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (video.durationText != null) {
                val durationText = video.durationText
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = durationText.orEmpty(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (watchProgress > 0f) {
                VideoWatchBar(
                    progress = watchProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isNowPlaying) {
                    NowPlayingBars(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                MarqueeTitle(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            val compactViews = video.viewCountText?.let { compactViewCount(it) }
            if (compactViews != null || video.publishedTimeText != null) {
                Text(
                    text = listOfNotNull(compactViews, video.publishedTimeText).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (video.channelName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val channelClick: () -> Unit = {
                        if (channels.size > 1) showChannelPicker = true
                        else video.channelId?.let(onChannelClick)
                    }
                    ChannelAvatarStack(
                        channels = channels,
                        primaryThumbnailUrl = video.channelThumbnailUrl,
                        size = 28.dp,
                        onClick = channelClick,
                    )
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = channelClick)
                    )
                }
            }
        }
        VideoMenuButton(onClick = showMenu)
        }
        if (showChannelPicker) {
            ChannelPickerDialog(
                channels = channels,
                onDismiss = { showChannelPicker = false },
                onSelect = { channel ->
                    showChannelPicker = false
                    channel.id?.let(onChannelClick)
                },
                primaryThumbnailUrl = video.channelThumbnailUrl,
            )
        }
    }
}

/** The three-dot button that opens a video's menu, as music items have. */
@Composable
internal fun VideoMenuButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            painter = painterResource(R.drawable.more_vert),
            contentDescription = stringResource(R.string.more_options),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * How many different queries one scroll goes out with. Rotated queries overlap heavily,
 * so a single one often adds only a row or two before looking like it has stalled;
 * asking for a few at a time fills the page in one update.
 */
private const val QUERIES_PER_FEED_PAGE = 4

/** Consecutive barren feed pages before the feed stops offering more. */
private const val MAX_EMPTY_FEED_FETCHES = 3
