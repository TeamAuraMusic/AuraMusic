/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.auramusic.app.constants.VideoFeedGridViewKey
import com.auramusic.app.ui.component.shimmer.ShimmerHost
import com.auramusic.app.utils.compactViewCount
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class VideoCategory(
    val labelRes: Int,
) {
    Music(R.string.filter_music),
    ForYou(R.string.for_you),
    Trending(R.string.trending),
    Gaming(R.string.video_category_gaming),
}

@Composable
fun VideosScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(VideoCategory.Music) }
    var gridView by rememberPreference(VideoFeedGridViewKey, true)

    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val configuration = LocalConfiguration.current

    var feed by remember { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var continuation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    suspend fun loadFirstPage() {
        if (!isLoading) {
            isLoading = true
            error = null
            feed = emptyList()
            continuation = null
        }
        withContext(Dispatchers.IO) {
            val result = when (selectedCategory) {
                VideoCategory.ForYou -> YouTube.youtubeHomeFeed().getOrNull()
                VideoCategory.Trending -> YouTube.youtubeTrending().getOrNull()
                VideoCategory.Music -> YouTube.youtubeCategoryFeed(context.getString(R.string.video_category_music_query)).getOrNull()
                VideoCategory.Gaming -> YouTube.youtubeCategoryFeed(context.getString(R.string.video_category_gaming_query)).getOrNull()
            }
            if (result != null && result.items.isNotEmpty()) {
                feed = result.items
                continuation = result.continuation
                error = null
            } else {
                error = context.getString(R.string.videos_feed_error)
            }
        }
        isLoading = false
        isRefreshing = false
    }

    suspend fun refresh() {
        isRefreshing = true
        loadFirstPage()
    }

    suspend fun loadMore() {
        if (isLoadingMore || isLoading) return
        val cont = continuation ?: return
        isLoadingMore = true
        withContext(Dispatchers.IO) {
            val result = when (selectedCategory) {
                VideoCategory.ForYou -> YouTube.youtubeHomeFeed(cont).getOrNull()
                VideoCategory.Trending -> YouTube.youtubeTrending(cont).getOrNull()
                VideoCategory.Music -> YouTube.youtubeCategoryFeed(context.getString(R.string.video_category_music_query), cont).getOrNull()
                VideoCategory.Gaming -> YouTube.youtubeCategoryFeed(context.getString(R.string.video_category_gaming_query), cont).getOrNull()
            }
            result?.let {
                feed = feed + it.items
                continuation = it.continuation
            }
        }
        isLoadingMore = false
    }

    val columns = when {
        configuration.screenWidthDp >= 550 -> 3
        else -> 2
    }

    LaunchedEffect(selectedCategory) {
        loadFirstPage()
    }

    val gridListState = rememberLazyGridState()
    val listListState = rememberLazyListState()

    LaunchedEffect(gridListState, selectedCategory, gridView) {
        snapshotFlow {
            val last = gridListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridListState.layoutInfo.totalItemsCount - 8
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }
    LaunchedEffect(listListState, selectedCategory, gridView) {
        snapshotFlow {
            val last = listListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listListState.layoutInfo.totalItemsCount - 3
        }.collect { nearEnd -> if (nearEnd) loadMore() }
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

        FeedFilterBar(
            selected = selectedCategory,
            onCategorySelected = { selectedCategory = it },
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        scope.launch {
                            loadFirstPage()
                        }
                    },
                ),
            contentAlignment = Alignment.TopStart
        ) {
            when {
                isLoading && feed.isEmpty() -> SkeletonFeed(columns = columns)
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
                        )
                    }
                    val openChannel: (String) -> Unit = { channelId ->
                        navController.navigate("youtube_channel/$channelId")
                    }
                    if (gridView) {
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
                                    onClick = { playVideo(video) },
                                    onChannelClick = openChannel,
                                )
                            }
                            item(
                                key = "feed_footer",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                GridListFooter(isLoadingMore = isLoadingMore, hasMore = continuation != null)
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
                                    onClick = { playVideo(video) },
                                    onChannelClick = openChannel,
                                )
                            }
                            item(key = "feed_footer") {
                                GridListFooter(isLoadingMore = isLoadingMore, hasMore = continuation != null)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
private fun FeedFilterBar(
    selected: VideoCategory,
    onCategorySelected: (VideoCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(vertical = 6.dp)
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        VideoCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = stringResource(category.labelRes),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = RoundedCornerShape(16.dp),
                border = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SkeletonFeed(columns: Int) {
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

@Composable
private fun GridListFooter(isLoadingMore: Boolean, hasMore: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
            else -> Text(
                text = stringResource(R.string.no_more_content),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun NowPlayingBars(color: Color, modifier: Modifier = Modifier) {
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

private fun isCurrentlyPlaying(videoId: String): Boolean {
    val state = VideoPlaybackManager.uiState.value
    return state.session?.videoId == videoId
}

@Composable
private fun ChannelAvatar(
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
        } else {
            val initial = channelName.trim().firstOrNull()?.uppercase() ?: "?"
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FeedVideoGridCard(
    video: YouTubeVideoItem,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
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
            .clickable(onClick = onClick),
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
        }

        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val channelClick: () -> Unit = { video.channelId?.let(onChannelClick) }
            ChannelAvatar(
                channelName = video.channelName,
                channelThumbnailUrl = video.channelThumbnailUrl,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = channelClick)
            )
            Text(
                text = video.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = channelClick)
            )
        }
        val compactViews = video.viewCountText?.let { compactViewCount(it) }
        if (compactViews != null) {
            Text(
                text = listOfNotNull(compactViews, video.publishedTimeText).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedVideoListRow(
    video: YouTubeVideoItem,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
                    val channelClick: () -> Unit = { video.channelId?.let(onChannelClick) }
                    ChannelAvatar(
                        channelName = video.channelName,
                        channelThumbnailUrl = video.channelThumbnailUrl,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = channelClick)
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
    }
}