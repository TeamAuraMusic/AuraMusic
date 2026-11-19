package com.auramusic.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuraMusicApp() {
    val state = remember { DesktopAppState() }
    DisposableEffect(Unit) {
        onDispose { state.close() }
    }

    val query by state.query.collectAsState()
    val results by state.results.collectAsState()
    val searching by state.searching.collectAsState()
    val resolving by state.resolving.collectAsState()
    val notice by state.notice.collectAsState()
    val title by state.player.title.collectAsState()
    val playing by state.player.isPlaying.collectAsState()
    val position by state.player.position.collectAsState()
    val duration by state.player.duration.collectAsState()
    val playerError by state.player.error.collectAsState()

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("AuraMusic Desktop", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "v${DesktopBuildInfo.VERSION_LABEL} · Compose Desktop, InnerTube search, libmpv audio",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = state::onQueryChange,
                        label = { Text("Search songs") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = state::search, enabled = !searching) {
                        Text(if (searching) "Searching…" else "Search")
                    }
                }

                if (notice != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(notice!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Text("Results", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(results, key = { it.id }) { song ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    song.artists.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(onClick = { state.play(song) }, enabled = !resolving) {
                                Text(if (resolving) "Loading…" else "Play")
                            }
                        }
                        HorizontalDivider()
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                NowPlaying(
                    title = title,
                    playing = playing,
                    position = position,
                    duration = duration,
                    error = playerError,
                    state = state,
                )
            }
        }
    }
}

@Composable
private fun NowPlaying(
    title: String?,
    playing: Boolean,
    position: Double,
    duration: Double,
    error: String?,
    state: DesktopAppState,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Now playing", style = MaterialTheme.typography.titleMedium)
        Text(title ?: "Nothing playing")
        Text("${formatTime(position)} / ${formatTime(duration)}", style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = {
                if (duration > 0.0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = state.player::togglePause) {
                Text(if (playing) "Pause" else "Play / resume")
            }
            Button(onClick = { state.player.seekBy(-10) }) { Text("−10s") }
            Button(onClick = { state.player.seekBy(10) }) { Text("+10s") }
            Button(onClick = state.player::stop) { Text("Stop") }
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTime(seconds: Double): String {
    if (seconds <= 0.0 || seconds.isNaN()) return "0:00"
    val total = seconds.toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
