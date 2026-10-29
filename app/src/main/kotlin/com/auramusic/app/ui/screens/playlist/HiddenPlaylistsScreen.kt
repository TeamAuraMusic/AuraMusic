/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.playlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.ui.component.CollectionArtwork
import com.auramusic.app.ui.component.CollectionHeroBackdrop
import com.auramusic.app.ui.component.CollectionTitleBlock
import com.auramusic.app.viewmodels.HiddenPlaylist
import com.auramusic.app.viewmodels.HiddenPlaylistsViewModel

/**
 * Playlists the user hid, each with a button to show it again. The page opens with the
 * same blurred, accent-tinted hero as the other collection screens, then lists the
 * hidden playlists as cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenPlaylistsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HiddenPlaylistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val accent = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item(key = "hero") {
                CollectionHeroBackdrop(
                    imageUrl = null,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CollectionArtwork(
                            accent = accent,
                            size = 128.dp,
                            shape = CircleShape,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.visibility_off),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp),
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        CollectionTitleBlock(
                            title = stringResource(R.string.hidden_playlists),
                            overline = stringResource(R.string.filter_library),
                            accent = accent,
                            chips = listOf(
                                pluralStringResource(
                                    R.plurals.n_playlist,
                                    playlists.size,
                                    playlists.size,
                                ),
                            ),
                        )
                    }
                }
            }

            if (playlists.isEmpty()) {
                item(key = "empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.visibility_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.hidden_playlists_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    HiddenPlaylistRow(
                        playlist = playlist,
                        onOpen = {
                            if (playlist.localId != null) navController.navigate("local_playlist/${playlist.localId}")
                            else navController.navigate("online_playlist/${playlist.id}")
                        },
                        onUnhide = {
                            viewModel.unhide(playlist)
                            Toast.makeText(context, R.string.playlist_unhidden, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(12.dp)) }
        }

        TopAppBar(
            title = { Text(stringResource(R.string.hidden_playlists)) },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun HiddenPlaylistRow(playlist: HiddenPlaylist, onOpen: () -> Unit, onUnhide: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Icon(
                    painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (playlist.thumbnail != null) {
                    AsyncImage(
                        model = playlist.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            )
            Surface(
                onClick = onUnhide,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.visibility),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.unhide),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
