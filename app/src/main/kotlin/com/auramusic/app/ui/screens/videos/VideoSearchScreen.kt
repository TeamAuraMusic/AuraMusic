/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeSearchResultItem
import com.auramusic.innertube.models.YouTubeVideoItem
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

private enum class SearchFilter(val labelRes: Int, val iconRes: Int, val spParams: String?) {
    All(R.string.all_results, R.drawable.manage_search, null),
    Videos(R.string.video_section, R.drawable.slow_motion_video, YouTube.SEARCH_FILTER_VIDEOS),
    Channels(R.string.channels, R.drawable.ic_person, YouTube.SEARCH_FILTER_CHANNELS),
    Playlists(R.string.playlists, R.drawable.playlist_play, YouTube.SEARCH_FILTER_PLAYLISTS),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSearchScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val initialQuery = remember {
        val encoded = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
        URLDecoder.decode(encoded, "UTF-8")
    }

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var allResults by remember { mutableStateOf<List<YouTubeSearchResultItem>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var continuation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(initialQuery.isNotEmpty()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(initialQuery.isNotEmpty()) }
    var activeFilter by remember { mutableStateOf(SearchFilter.All) }

    suspend fun performSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) return
        isLoading = true
        isLoadingMore = false
        error = null
        suggestions = emptyList()
        val params = activeFilter.spParams
        withContext(Dispatchers.IO) {
            YouTube.youtubeSearch(searchQuery, params = params).fold(
                onSuccess = { result ->
                    allResults = result.items
                    continuation = result.continuation
                },
                onFailure = {
                    error = context.getString(R.string.search_failed)
                }
            )
        }
        isLoading = false
    }

    suspend fun loadSuggestions(prefix: String) {
        withContext(Dispatchers.IO) {
            YouTube.youtubeSearchSuggestions(prefix).fold(
                onSuccess = { suggestions = it },
                onFailure = { /* keep current suggestions */ }
            )
        }
    }

    suspend fun performLoadMore() {
        if (isLoadingMore || isLoading) return
        val cont = continuation ?: return
        isLoadingMore = true
        withContext(Dispatchers.IO) {
            YouTube.youtubeSearchContinuation(cont).fold(
                onSuccess = { result ->
                    allResults = allResults + result.items
                    continuation = result.continuation
                },
                onFailure = { /* keep what we have */ }
            )
        }
        isLoadingMore = false
    }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotEmpty()) {
            performSearch(initialQuery)
        } else {
            focusRequester.requestFocus()
            loadSuggestions("")
        }
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty() && query != initialQuery && !hasSearched) {
            delay(250)
            loadSuggestions(query)
        } else if (query.isEmpty() && !hasSearched) {
            loadSuggestions("")
        }
    }

    LaunchedEffect(lazyListState, activeFilter, hasSearched) {
        snapshotFlow {
            val last = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= lazyListState.layoutInfo.totalItemsCount - 3
        }.collect { nearEnd ->
            if (nearEnd && hasSearched) performLoadMore()
        }
    }

    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchField(
                        query = query,
                        onQueryChange = {
                            query = it
                            if (hasSearched) {
                                hasSearched = false
                                allResults = emptyList()
                                continuation = null
                                error = null
                                activeFilter = SearchFilter.All
                            }
                        },
                        onClear = {
                            query = ""
                            hasSearched = false
                            allResults = emptyList()
                            continuation = null
                            error = null
                            focusRequester.requestFocus()
                        },
                        onSubmit = {
                            focusManager.clearFocus()
                            query = query.trim()
                            hasSearched = true
                            coroutineScope.launch { performSearch(query) }
                        },
                        focusRequester = focusRequester,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(bottom = bottomInset)
                .fillMaxSize()
        ) {
            if (hasSearched) {
                SearchFilterBar(
                    active = activeFilter,
                    enabled = query.isNotEmpty(),
                    onSelect = { selected ->
                        if (selected == activeFilter) return@SearchFilterBar
                        activeFilter = selected
                        if (hasSearched && query.isNotBlank()) {
                            coroutineScope.launch { performSearch(query) }
                        }
                    },
                )
            }

            val showSuggestions = !hasSearched && suggestions.isNotEmpty()
            when {
                isLoading -> SearchSkeleton()

                !hasSearched && showSuggestions -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "suggest_title") {
                            Text(
                                text = stringResource(R.string.search_suggestions),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                        items(items = suggestions, key = { it }) { suggestion ->
                            SuggestionRow(
                                suggestion = suggestion,
                                onClick = {
                                    query = suggestion
                                    hasSearched = true
                                    focusManager.clearFocus()
                                    coroutineScope.launch { performSearch(suggestion) }
                                }
                            )
                        }
                    }
                }

                !hasSearched && error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                hasSearched && allResults.isEmpty() && error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                hasSearched && allResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.search_off),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.no_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                else -> {
                    val visibleResults = filterResults(allResults, activeFilter)
                    val grouped = remember(visibleResults, activeFilter) {
                        groupResults(visibleResults, activeFilter)
                    }
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        grouped.forEach { section ->
                            if (section.titleRes != R.string.search_section_top_result) {
                                item(key = "section_${section.titleRes}") {
                                    SearchSectionHeader(titleRes = section.titleRes)
                                }
                            }
                            section.items.forEachIndexed { index, result ->
                                item(
                                    key = result.key() + (if (section.titleRes == R.string.search_section_top_result) "_${index}" else "")
                                ) {
                                    SearchResultRow(
                                        result = result,
                                        isHero = section.titleRes == R.string.search_section_top_result,
                                        onVideoClick = { video ->
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
                                        },
                                        onChannelClick = { channelId ->
                                            navController.navigate("youtube_channel/$channelId")
                                        },
                                        onPlaylistClick = { playlistId ->
                                            navController.navigate("youtube_browse/$playlistId")
                                        }
                                    )
                                }
                            }
                        }

                        if (isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
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
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = {
            Text(
                text = stringResource(R.string.search_youtube),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.dismiss),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() })
    )
}

@Composable
private fun SearchFilterBar(
    active: SearchFilter,
    enabled: Boolean,
    onSelect: (SearchFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilter.entries.forEach { filter ->
            val isSelected = filter == active
            Surface(
                onClick = { onSelect(filter) },
                enabled = enabled,
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(filter.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(filter.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun filterResults(
    items: List<YouTubeSearchResultItem>,
    filter: SearchFilter,
): List<YouTubeSearchResultItem> = when (filter) {
    SearchFilter.All -> items
    SearchFilter.Videos -> items.filter { it is YouTubeSearchResultItem.Video }
    SearchFilter.Channels -> items.filter { it is YouTubeSearchResultItem.Channel }
    SearchFilter.Playlists -> items.filter { it is YouTubeSearchResultItem.Playlist }
}

private data class SearchSection(
    val titleRes: Int,
    val items: List<YouTubeSearchResultItem>,
)

/**
 * Groups the flat result stream into YouTube-style sections. In "All" the first video
 * becomes the Top result; the rest are grouped by type so the list scans like YouTube's.
 * Filtered views collapse into a single titled section.
 */
private fun groupResults(
    items: List<YouTubeSearchResultItem>,
    filter: SearchFilter,
): List<SearchSection> {
    if (items.isEmpty()) return emptyList()
    return when (filter) {
        SearchFilter.All -> {
            val firstVideo = items.indexOfFirst { it is YouTubeSearchResultItem.Video }
            if (firstVideo < 0) {
                return listOf(SearchSection(R.string.search_section_results, items))
            }
            val top = items[firstVideo]
            val rest = items.filterIndexed { i, _ -> i != firstVideo }
            val sections = mutableListOf<SearchSection>(
                SearchSection(R.string.search_section_top_result, listOf(top)),
            )
            val videos = rest.filterIsInstance<YouTubeSearchResultItem.Video>()
            val channels = rest.filterIsInstance<YouTubeSearchResultItem.Channel>()
            val playlists = rest.filterIsInstance<YouTubeSearchResultItem.Playlist>()
            if (videos.isNotEmpty()) sections += SearchSection(R.string.search_section_videos, videos)
            if (channels.isNotEmpty()) sections += SearchSection(R.string.search_section_channels, channels)
            if (playlists.isNotEmpty()) sections += SearchSection(R.string.search_section_playlists, playlists)
            sections
        }
        SearchFilter.Videos -> listOf(SearchSection(R.string.search_section_videos, items))
        SearchFilter.Channels -> listOf(SearchSection(R.string.search_section_channels, items))
        SearchFilter.Playlists -> listOf(SearchSection(R.string.search_section_playlists, items))
    }
}

@Composable
private fun SearchSectionHeader(titleRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun YouTubeSearchResultItem.key(): String =
    when (this) {
        is YouTubeSearchResultItem.Video -> "video:${video.videoId}"
        is YouTubeSearchResultItem.Channel -> "channel:$channelId"
        is YouTubeSearchResultItem.Playlist -> "playlist:$playlistId"
    }

@Composable
private fun SearchSkeleton() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .shimmer()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmer()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmer()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmer()
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SearchResultRow(
    result: YouTubeSearchResultItem,
    isHero: Boolean,
    onVideoClick: (YouTubeVideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
) {
    when (result) {
        is YouTubeSearchResultItem.Video -> if (isHero) SearchHeroVideoCard(
            video = result.video,
            onClick = { onVideoClick(result.video) }
        ) else SearchVideoRow(
            video = result.video,
            onClick = { onVideoClick(result.video) }
        )
        is YouTubeSearchResultItem.Channel -> SearchChannelRow(
            channel = result,
            onClick = { onChannelClick(result.channelId) }
        )
        is YouTubeSearchResultItem.Playlist -> SearchPlaylistRow(
            playlist = result,
            onClick = { onPlaylistClick(result.playlistId) }
        )
    }
}

@Composable
private fun SearchHeroVideoCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )
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
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(
                video.channelName.takeIf { it.isNotEmpty() },
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

@Composable
private fun SearchAvatar(channelName: String, url: String?, modifierSize: Int) {
    Box(
        modifier = Modifier
            .size(modifierSize.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = channelName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = channelName.trim().firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SearchVideoRow(
    video: YouTubeVideoItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(150.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
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
            Text(
                text = listOfNotNull(
                    video.channelName.takeIf { it.isNotEmpty() },
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

@Composable
private fun SearchChannelRow(
    channel: YouTubeSearchResultItem.Channel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val avatar = channel.thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
            SearchAvatar(channel.title, avatar, 52)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(
                        channel.subscriberCountText,
                        channel.videoCountText
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!channel.description.isNullOrBlank()) {
                    val description = channel.description
                    Text(
                        text = description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.navigate_next),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPlaylistRow(
    playlist: YouTubeSearchResultItem.Playlist,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(96.dp)) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            val thumb = playlist.thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (thumb != null) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = playlist.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOfNotNull(
                    playlist.channelName,
                    playlist.itemCountText
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}