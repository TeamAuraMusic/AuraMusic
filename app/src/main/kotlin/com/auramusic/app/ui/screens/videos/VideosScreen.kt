/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class VideoCategory(
    val browseId: String,
    val labelRes: Int,
) {
    ForYou("FEwhat_to_watch", R.string.for_you),
    Trending("FEtrending", R.string.trending),
    Music("FEmusic", R.string.filter_music),
    Gaming("FEgaming", R.string.video_category_gaming),
}

@Composable
fun VideosScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(VideoCategory.ForYou) }
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
        val browseId = selectedCategory.browseId
        withContext(Dispatchers.IO) {
            val result = when (browseId) {
                "FEwhat_to_watch" -> {
                    YouTube.youtubeHomeFeed().getOrNull()
                        ?.takeIf { it.items.isNotEmpty() }
                        ?: YouTube.youtubeTrending().getOrNull()
                }
                else -> YouTube.youtubeCategoryFeed(browseId).getOrNull()
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
        val browseId = selectedCategory.browseId
        val cont = continuation ?: return
        isLoadingMore = true
        withContext(Dispatchers.IO) {
            val result = when (browseId) {
                "FEwhat_to_watch" -> YouTube.youtubeHomeFeed(cont).getOrNull()
                    ?: YouTube.youtubeTrending(cont).getOrNull()
                else -> YouTube.youtubeCategoryFeed(browseId, cont).getOrNull()
            }
            result?.let {
                feed = feed + it.items
                continuation = it.continuation
            }
        }
        isLoadingMore = false
    }

    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = insets.calculateBottomPadding())
        ) {
            FeedFilterBar(
                selected = selectedCategory,
                gridView = gridView,
                onCategorySelected = { selectedCategory = it },
                onToggleView = { gridView = !gridView },
            )

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
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val err = error
                        Text(
                            text = err.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                        navController.navigate("youtube_browse/$channelId")
                    }
                    if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            items(
                                items = feed,
                                key = { "video_${it.videoId}" }
                            ) { video ->
                                FeedVideoGridCard(
                                    video = video,
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
private fun FeedFilterBar(
    selected: VideoCategory,
    gridView: Boolean,
    onCategorySelected: (VideoCategory) -> Unit,
    onToggleView: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
        IconButton(onClick = onToggleView) {
            Icon(
                painter = painterResource(if (gridView) R.drawable.list else R.drawable.grid_view),
                contentDescription = stringResource(
                    if (gridView) R.string.videos_list_view else R.string.videos_grid_view
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkeletonFeed(columns: Int) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize())
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

private fun isCurrentlyPlaying(videoId: String): Boolean {
    val session = VideoPlaybackManager.uiState.value.session ?: return false
    return session.videoId == videoId && VideoPlaybackManager.uiState.value.isPlaying
}

@Composable
private fun ChannelMonogram(channelName: String, modifier: Modifier = Modifier) {
    val initial = channelName.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeedVideoGridCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    val playing = isCurrentlyPlaying(video.videoId)

    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (playing) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(18.dp)
                    )
                } else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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

            if (video.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
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

            if (playing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAYING",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChannelMonogram(video.channelName, Modifier.size(24.dp))
            Text(
                text = video.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (video.viewCountText != null) {
            Text(
                text = listOfNotNull(video.viewCountText, video.publishedTimeText).joinToString(" • "),
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
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(150.dp)
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
                        text = "LIVE",
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (video.viewCountText != null || video.publishedTimeText != null) {
                Text(
                    text = listOfNotNull(video.viewCountText, video.publishedTimeText).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (video.channelName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChannelMonogram(video.channelName, Modifier.size(22.dp))
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}