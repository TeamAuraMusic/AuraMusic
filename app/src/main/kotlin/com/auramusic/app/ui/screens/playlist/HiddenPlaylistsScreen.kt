/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.playlist

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.ui.component.PlaylistListItem
import com.auramusic.app.viewmodels.HiddenPlaylistsViewModel

/** Playlists the user hid from the library, each with a button to show it again. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenPlaylistsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HiddenPlaylistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    trailingContent = {
                        IconButton(onClick = {
                            viewModel.unhide(playlist)
                            Toast.makeText(context, R.string.playlist_unhidden, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.visibility),
                                contentDescription = stringResource(R.string.unhide_playlist),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("local_playlist/${playlist.id}") },
                )
            }
        }

        if (playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.hidden_playlists_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
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
