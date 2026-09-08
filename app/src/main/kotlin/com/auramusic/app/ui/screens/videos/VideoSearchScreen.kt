/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeSearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSearchScreen(
    navController: NavController,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val initialQuery = remember {
        val encoded = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
        URLDecoder.decode(encoded, "UTF-8")
    }

    var query by remember { mutableStateOf(initialQuery) }
    var searchResults by remember { mutableStateOf<List<YouTubeSearchResultItem>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var continuation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(initialQuery.isNotEmpty()) }

    val performSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                coroutineScope.launch {
                    isLoading = true
                    isLoadingMore = false
                    error = null
                    suggestions = emptyList()
                    withContext(Dispatchers.IO) {
                        YouTube.youtubeSearch(searchQuery).fold(
                            onSuccess = { result ->
                                searchResults = result.items
                                continuation = result.continuation
                                hasSearched = true
                            },
                            onFailure = { e ->
                                error = e.message ?: "Search failed"
                            }
                        )
                    }
                    isLoading = false
                }
            }
        }
    }

    val loadSuggestions: (String) -> Unit = remember {
        { prefix ->
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.youtubeSearchSuggestions(prefix).fold(
                        onSuccess = { suggestions = it },
                        onFailure = { /* ignore suggestion failures */ }
                    )
                }
            }
        }
    }

    // Empty initial query => show suggestions instead of searching
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            performSearch(initialQuery)
        } else {
            focusRequester.requestFocus()
            loadSuggestions("")
        }
    }

    // Live suggestions while typing a new query (before searching)
    LaunchedEffect(query) {
        if (query.isNotEmpty() && query != initialQuery) {
            loadSuggestions(query)
        }
    }

    // Load more on scroll
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && continuation != null && !isLoadingMore && hasSearched) {
                isLoadingMore = true
                withContext(Dispatchers.IO) {
                    continuation?.let { cont ->
                        YouTube.youtubeSearchContinuation(cont).fold(
                            onSuccess = { result ->
                                searchResults = searchResults + result.items
                                continuation = result.continuation
                            },
                            onFailure = { /* ignore pagination errors */ }
                        )
                    }
                }
                isLoadingMore = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_youtube),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                searchResults = emptyList()
                                continuation = null
                                performSearch(query)
                            }
                        )
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

        val screenModifier = Modifier
            .padding(paddingValues)
            .padding(bottom = bottomPadding)
            .fillMaxSize()

        when {
            isLoading -> {
                Box(
                    modifier = screenModifier,
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && !hasSearched -> {
                Box(
                    modifier = screenModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_youtube),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            !hasSearched && suggestions.isNotEmpty() -> {
                LazyColumn(modifier = screenModifier) {
                    items(items = suggestions, key = { it }) { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            onClick = {
                                query = suggestion
                                focusManager.clearFocus()
                                performSearch(suggestion)
                            }
                        )
                    }
                }
            }
            searchResults.isEmpty() && hasSearched -> {
                Box(
                    modifier = screenModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = screenModifier,
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.key() }
                    ) { result ->
                        when (result) {
                            is YouTubeSearchResultItem.Video -> SearchVideoItem(
                                video = result.video,
                                onClick = {
                                    navController.navigate("video_player/${result.video.videoId}")
                                }
                            )
                            is YouTubeSearchResultItem.Channel -> SearchChannelRow(
                                channel = result,
                                onClick = { navController.navigate("youtube_browse/${result.channelId}") }
                            )
                            is YouTubeSearchResultItem.Playlist -> SearchPlaylistRow(
                                playlist = result,
                                onClick = {}
                            )
                        }
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

private fun YouTubeSearchResultItem.key(): String =
    when (this) {
        is YouTubeSearchResultItem.Video -> "video:${video.videoId}"
        is YouTubeSearchResultItem.Channel -> "channel:$channelId"
        is YouTubeSearchResultItem.Playlist -> "playlist:$playlistId"
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SearchVideoItem(
    video: com.auramusic.innertube.models.YouTubeVideoItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
private fun SearchChannelRow(
    channel: YouTubeSearchResultItem.Channel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val avatar = channel.thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = channel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = listOfNotNull(channel.subscriberCountText, channel.videoCountText).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val thumb = playlist.thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(6.dp))
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
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = listOfNotNull(playlist.channelName, playlist.itemCountText).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}