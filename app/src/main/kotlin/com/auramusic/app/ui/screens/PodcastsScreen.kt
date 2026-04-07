/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auramusic.app.R
import com.auramusic.app.ui.screens.artist.ArtistItemsScreen
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.pages.RelatedPage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {
    val podcastsPage = MutableStateFlow<YouTube.PodcastsPage?>(null)
    val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadPodcasts()
        }
    }

    private suspend fun loadPodcasts() {
        isLoading.value = true
        YouTube.podcasts().onSuccess { page ->
            podcastsPage.value = page
        }
        isLoading.value = false
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) { loadPodcasts() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior?,
    viewModel: PodcastsViewModel = hiltViewModel()
) {
    val podcastsPage by viewModel.podcastsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.podcasts)) },
            scrollBehavior = scrollBehavior
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                podcastsPage?.featured?.forEach { section ->
                    item(key = section.title) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(section.items.size) { index ->
                        val item = section.items[index]
                        ArtistItemsScreen.ArtistItem(
                            item = item,
                            index = index,
                            isPlaying = false,
                            playWhenReady = false,
                            artistId = if (item is RelatedPage) item.id else null,
                            onClick = { /* Navigate to podcast */ },
                            onLongClick = { /* Add to queue */ }
                        )
                    }
                }
            }
        }
    }
}