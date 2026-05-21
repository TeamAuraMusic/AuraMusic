package com.auramusic.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auramusic.app.constants.UseKaraokeServerKey
import com.auramusic.app.playback.KaraokeProgress
import com.auramusic.app.playback.KaraokeServerHelper
import com.auramusic.app.playback.MusicService
import com.auramusic.app.utils.rememberPreference
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val KARAOKE_SERVER_URL = "https://karaoke.auramusic.site/"

private enum class ReachState { CONNECTING, REACHED, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeServerConnectionSheet(
    service: MusicService?,
    onDismiss: () -> Unit,
    onConnected: () -> Unit
) {
    val (_, onUseServerChange) = rememberPreference(UseKaraokeServerKey, false)
    var reachState by remember { mutableStateOf(ReachState.CONNECTING) }
    var reachError by remember { mutableStateOf<String?>(null) }
    val pipeline by KaraokeServerHelper.progress.collectAsState()

    // Phase 1 – wake up / verify the server is reachable.
    LaunchedEffect(Unit) {
        // Reset any leftover state from a prior session so the user doesn't
        // briefly see an old "Failed" / "Playing" before the new pipeline runs.
        KaraokeServerHelper.setProgress(KaraokeProgress.Connecting)
        // OkHttp engine + generous timeouts handle Render's free-tier cold
        // start (30–90 s). expectSuccess = false so any HTTP response is
        // treated as "reachable".
        val client = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        try {
            val response = withContext(Dispatchers.IO) { client.get(KARAOKE_SERVER_URL) }
            reachError = "HTTP ${response.status.value}"
            reachState = ReachState.REACHED
            onUseServerChange(true)

            // Phase 2 – kick off the ML pipeline if we have a service.
            // KaraokeServerHelper.progress will drive the rest of the UI.
            if (service != null) {
                service.runKaraokeServerPipeline()
            } else {
                // No service available – just signal "connected" and dismiss.
                delay(1200)
                onConnected()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            reachError = e.message
            reachState = ReachState.ERROR
        } finally {
            client.close()
        }
    }

    // Auto-dismiss once the pipeline finishes successfully (Playing).
    LaunchedEffect(pipeline) {
        if (pipeline is KaraokeProgress.Playing) {
            delay(1500)
            onConnected()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (reachState) {
                ReachState.CONNECTING -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting to Karaoke ML Server...")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "First connection can take up to a minute while the server wakes up.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(KARAOKE_SERVER_URL, style = MaterialTheme.typography.bodySmall)
                }

                ReachState.ERROR -> {
                    Text("Connection failed", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text("Could not reach $KARAOKE_SERVER_URL")
                    reachError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) { Text("Close") }
                }

                ReachState.REACHED -> {
                    PipelineStatus(pipeline = pipeline, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun PipelineStatus(pipeline: KaraokeProgress, onDismiss: () -> Unit) {
    when (pipeline) {
        is KaraokeProgress.Idle,
        is KaraokeProgress.Connecting,
        is KaraokeProgress.Connected -> {
            Text(
                "Connected ✓",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Preparing to upload track...")
        }

        is KaraokeProgress.Uploading -> {
            Text("Uploading audio to server", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { pipeline.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("${pipeline.percent}%", style = MaterialTheme.typography.bodySmall)
        }

        is KaraokeProgress.Processing -> {
            Text("Server is removing vocals", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                "MDX-NET separation typically takes 1–5 minutes depending on track length.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        is KaraokeProgress.Downloading -> {
            Text("Downloading instrumental", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { pipeline.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("${pipeline.percent}%", style = MaterialTheme.typography.bodySmall)
        }

        is KaraokeProgress.Preparing -> {
            Text("Preparing playback", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                "Swapping in the instrumental track and disabling local vocal suppression.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        is KaraokeProgress.Playing -> {
            Text(
                "Karaoke ready ✓",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("Now playing the ML-separated instrumental. Sing!")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss) { Text("Done") }
        }

        is KaraokeProgress.Failed -> {
            Text("Karaoke pipeline failed", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(pipeline.reason, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss) { Text("Close") }
        }
    }
}
