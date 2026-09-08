/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VideosScreen(
    navController: NavController,
) {
    var feed by remember { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var continuation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isHomeFeed by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    suspend fun loadFirstPage() {
        isLoading = true
        error = null
        withContext(Dispatchers.IO) {
            // Home feed first; fall back to trending when it returns nothing
            // (home requires sign-in for personalization, so guests get []
            // or an error and we degrade to the worldwide trending page).
            val home = YouTube.youtubeHomeFeed().getOrNull()?.takeIf { it.items.isNotEmpty() }
            if (home != null) {
                feed = home.items
                continuation = home.continuation
                isHomeFeed = true
            } else {
                val trending = YouTube.youtubeTrending().getOrNull()
                if (trending != null) {
                    feed = trending.items
                    continuation = trending.continuation
                    isHomeFeed = false
                } else {
                    error = "Could not load recommendations"
                }
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadFirstPage()
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && continuation != null && !isLoadingMore && !isLoading) {
                isLoadingMore = true
                withContext(Dispatchers.IO) {
                    continuation?.let { cont ->
                        val result = if (isHomeFeed) {
                            YouTube.youtubeHomeFeed(cont).getOrNull()
                        } else {
                            YouTube.youtubeTrending(cont).getOrNull()
                        }
                        result?.let {
                            feed = feed + it.items
                            continuation = it.continuation
                        }
                    }
                }
                isLoadingMore = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
    ) {
        when {
            isLoading && feed.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && feed.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.slow_motion_video),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = feed,
                        key = { it.videoId }
                    ) { video ->
                        FeedVideoCard(
                            video = video,
                            onClick = { navController.navigate("video_player/${video.videoId}") },
                            onChannelClick = { channelId ->
                                try {
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/channel/$channelId")
                                    ).let(navController.context::startActivity)
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedVideoCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            val thumbnailUrl = video.thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            val durationText = video.durationText
            if (durationText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (video.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Red, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (video.channelName.isNotEmpty()) {
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { video.channelId?.let(onChannelClick) }
                )
            }
            Text(
                text = listOfNotNull(
                    video.viewCountText,
                    video.publishedTimeText
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}