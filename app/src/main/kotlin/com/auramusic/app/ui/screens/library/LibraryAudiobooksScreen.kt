/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.CONTENT_TYPE_HEADER
import com.auramusic.app.constants.CONTENT_TYPE_SONG
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.ui.component.SongListItem
import com.auramusic.app.utils.AUDIOBOOK_RESUME_THRESHOLD_MS
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.viewmodels.LibraryAudiobooksViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAudiobooksScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibraryAudiobooksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.audiobooks)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null
                            )
                        },
                    )
                }
            }

            item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = pluralStringResource(R.plurals.n_audiobook, audiobooks.size, audiobooks.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.audiobooks_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (audiobooks.isEmpty()) {
                item(key = "empty", contentType = CONTENT_TYPE_HEADER) {
                    Text(
                        text = stringResource(R.string.no_audiobooks_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            itemsIndexed(
                items = audiobooks,
                key = { _, item -> item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { _, item ->
                val song = item.song
                val durationMs = (song.song.duration * 1000L).coerceAtLeast(0L)
                val resumePosition = item.resumePositionMs.coerceIn(0L, durationMs)
                val hasResume = resumePosition >= AUDIOBOOK_RESUME_THRESHOLD_MS && resumePosition < durationMs - AUDIOBOOK_RESUME_THRESHOLD_MS
                val startPosition = if (hasResume) resumePosition else 0L

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = context.getString(R.string.audiobooks),
                                        items = listOf(song.toMediaItem()),
                                        startIndex = 0,
                                        position = startPosition,
                                    )
                                )
                            }
                        }
                        .animateItem()
                ) {
                    SongListItem(
                        song = song,
                        showLikedIcon = false,
                        showInLibraryIcon = item.pinned,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(onClick = { viewModel.setPinned(song.id, !item.pinned) }) {
                                Icon(
                                    painter = painterResource(if (item.pinned) R.drawable.library_add_check else R.drawable.library_add),
                                    contentDescription = stringResource(
                                        if (item.pinned) R.string.remove_from_audiobooks else R.string.add_to_audiobooks
                                    )
                                )
                            }
                            if (hasResume) {
                                IconButton(onClick = { viewModel.clearResumePosition(song.id) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.refresh),
                                        contentDescription = stringResource(R.string.clear_resume_position)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (hasResume && durationMs > 0L) {
                        Column(
                            modifier = Modifier.padding(start = 88.dp, end = 24.dp, bottom = 12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { (resumePosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(
                                    R.string.resume_at,
                                    makeTimeString(resumePosition),
                                    makeTimeString(durationMs)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
