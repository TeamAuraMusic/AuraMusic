/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.app.utils.VideoThumbnails
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.innertube.pages.YouTubeChannelPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

private enum class ChannelTab(val labelRes: Int) {
    Videos(R.string.channel_tab_videos),
    Shorts(R.string.channel_tab_shorts),
    Live(R.string.channel_tab_live),
    About(R.string.channel_tab_about),
}

/**
 * A regular YouTube channel screen: banner, avatar, subscriber count and
 * Videos/Shorts/Live/About tabs with infinite scroll. Content comes from the
 * YouTube WEB browse endpoint, not YT Music (which can't resolve UC... channels).
 */
@Composable
fun ChannelScreen(
    navController: NavController,
    channelIdOrUrl: String,
) {
    val context = LocalContext.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val listState = rememberLazyListState()

    val channelId = remember(channelIdOrUrl) { extractChannelId(channelIdOrUrl) }

    var header by remember { mutableStateOf<ChannelHeader?>(null) }
    var videos by remember(channelId) { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var continuation by remember(channelId) { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(ChannelTab.Videos.ordinal) }

    suspend fun loadFirstPage(tab: ChannelTab) {
        isLoading = true
        error = null
        videos = emptyList()
        continuation = null
        val params = when (tab) {
            ChannelTab.Videos -> YouTubeChannelPage.VIDEOS_PARAMS
            ChannelTab.Shorts -> YouTubeChannelPage.SHORTS_PARAMS
            ChannelTab.Live -> YouTubeChannelPage.LIVE_PARAMS
            ChannelTab.About -> null
        }
        val result = withContext(Dispatchers.IO) {
            YouTube.youtubeChannel(channelId, params).getOrNull()
        }
        if (result != null) {
            header = ChannelHeader(
                channelId = channelId,
                title = result.title,
                avatarUrl = result.avatarUrl,
                bannerUrl = result.bannerUrl,
                subscriberCountText = result.subscriberCountText,
                videosCountText = result.videosCountText,
                description = result.description,
            )
            videos = result.videos
            continuation = result.continuation
        } else {
            error = context.getString(R.string.videos_feed_error)
        }
        isLoading = false
    }

    suspend fun loadMore() {
        if (isLoadingMore || isLoading || selectedTab == ChannelTab.About.ordinal) return
        val cont = continuation ?: return
        isLoadingMore = true
        val result = withContext(Dispatchers.IO) {
            YouTube.youtubeChannelContinuation(channelId, cont).getOrNull()
        }
        result?.let {
            val existing = videos.map { it.videoId }.toSet()
            videos = videos + it.videos.filter { v -> v.videoId !in existing }
            continuation = it.continuation
        }
        isLoadingMore = false
    }

    // Header + first page of the active tab. A tab switch reloads; the channel id
    // stays in the key so back navigation re-uses the same screen instance.
    LaunchedEffect(channelId, selectedTab) {
        loadFirstPage(ChannelTab.entries[selectedTab])
    }

    // Infinite scroll.
    LaunchedEffect(listState, selectedTab) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4
        }.collect { nearEnd -> if (nearEnd) loadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = insets.calculateBottomPadding() + 16.dp),
    ) {
        item(key = "header") {
            ChannelHeaderSection(
                header = header,
                isLoading = isLoading,
                onBackClick = { navController.navigateUp() },
            )
        }

        item(key = "tabs") {
            ChannelTabBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
            )
        }

        when (ChannelTab.entries[selectedTab]) {
            ChannelTab.About -> {
                item(key = "about") {
                    ChannelAboutSection(header = header)
                }
            }
            else -> {
                if (isLoading && videos.isEmpty()) {
                    items(count = 4, key = { "skeleton_$it" }) {
                        ChannelVideoSkeleton()
                    }
                } else if (error != null && videos.isEmpty()) {
                    item(key = "error") {
                        ChannelErrorState(message = error.orEmpty())
                    }
                } else if (videos.isEmpty()) {
                    item(key = "empty") {
                        ChannelErrorState(message = stringResource(R.string.no_videos_found))
                    }
                } else {
                    items(count = videos.size, key = { "video_${videos[it].videoId}" }) { index ->
                        val video = videos[index]
                        ChannelVideoRow(
                            video = video,
                            onClick = {
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
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading_more") {
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
}

@Composable
private fun ChannelHeaderSection(
    header: ChannelHeader?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f / 1f),
        ) {
            if (header?.bannerUrl != null) {
                AsyncImage(
                    model = header.bannerUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                val avatar = header?.avatarUrl
                if (avatar != null) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = header.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header?.title.orEmpty().ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(
                    header?.subscriberCountText,
                    header?.videosCountText,
                ).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelTabBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        edgePadding = 16.dp,
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
                        color = if (selectedTab == tab.ordinal) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun ChannelAboutSection(header: ChannelHeader?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val about = listOfNotNull(
            header?.description?.takeIf { it.isNotBlank() },
            header?.subscriberCountText,
            header?.videosCountText,
        )
        if (about.isEmpty()) {
            Text(
                text = stringResource(R.string.channel_no_about),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            Text(
                text = header?.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (header?.subscriberCountText != null) {
                Text(
                    text = header.subscriberCountText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (header?.videosCountText != null) {
                Text(
                    text = header.videosCountText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ChannelVideoRow(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        ThumbnailWithBadge(
            thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url
                ?: VideoThumbnails.highQuality(video.videoId),
            durationText = video.durationText,
            isLive = video.isLive,
            modifier = Modifier
                .width(150.dp)
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
                    video.viewCountText,
                    video.publishedTimeText,
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                .width(150.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
            .clip(RoundedCornerShape(12.dp))
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
            .clip(RoundedCornerShape(4.dp))
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
    return when {
        decoded.startsWith("UC") -> decoded
        decoded.startsWith("@") -> decoded
        decoded.contains("/channel/") -> decoded.substringAfter("/channel/")
        decoded.contains("/@") -> "@" + decoded.substringAfter("/@").substringBefore("/")
        decoded.contains("/c/") -> decoded.substringAfter("/c/").substringBefore("/")
        decoded.contains("/user/") -> decoded.substringAfter("/user/").substringBefore("/")
        else -> decoded
    }
}
