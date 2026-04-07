/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.pages.RelatedPage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopChartsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {
    val topChartsPage = MutableStateFlow<YouTube.Top100ChartsPage?>(null)
    val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadTopCharts()
        }
    }

    private suspend fun loadTopCharts() {
        isLoading.value = true
        YouTube.getTop100Charts().onSuccess { page ->
            topChartsPage.value = page
        }
        isLoading.value = false
    }

    fun refresh(countryCode: String = "US") = viewModelScope.launch(Dispatchers.IO) {
        loadTopCharts()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopChartsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior?,
    viewModel: TopChartsViewModel = hiltViewModel()
) {
    val topChartsPage by viewModel.topChartsPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Top 100 Charts") },
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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(topChartsPage?.items.orEmpty()) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(32.dp)
                        )
                        item.thumbnailUrl?.let { url ->
                            CoilImage(
                                url = url,
                                modifier = Modifier.size(56.dp),
                                shape = MaterialTheme.shapes.small
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            item.title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            item.artists?.firstOrNull()?.name?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}