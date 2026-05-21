package com.auramusic.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auramusic.app.constants.UseKaraokeServerKey
import com.auramusic.app.utils.rememberPreference
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class ConnectionState { CONNECTING, CONNECTED, ERROR }

private const val KARAOKE_SERVER_URL = "https://karaoke.auramusic.site/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeServerConnectionSheet(
    onDismiss: () -> Unit,
    onConnected: () -> Unit
) {
    val (_, onUseServerChange) = rememberPreference(UseKaraokeServerKey, false)
    var connectionState by remember { mutableStateOf(ConnectionState.CONNECTING) }
    var errorDetail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // - OkHttp engine: more reliable than CIO on Android with Cloudflare/HTTP-2
        // - expectSuccess = false: any HTTP response means the server is reachable
        // - Generous timeouts: Render's free tier spins the service down after
        //   ~15 min of inactivity and a cold start can take 30–90 s.
        val client = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        try {
            // Use GET (not HEAD): some servers / CDNs / FastAPI routes
            // don't accept HEAD. Receiving ANY HTTP response proves the
            // server is reachable.
            val response = withContext(Dispatchers.IO) {
                client.get(KARAOKE_SERVER_URL)
            }
            // Any HTTP status code (including 404/405) means the host
            // answered — i.e. the network path works.
            errorDetail = "HTTP ${response.status.value}"
            delay(300)
            connectionState = ConnectionState.CONNECTED
            onUseServerChange(true)
            // Show the "Connected ✓" confirmation for a moment so the user
            // actually sees it before the parent dismisses the sheet.
            delay(1200)
            onConnected()
        } catch (e: Exception) {
            e.printStackTrace()
            errorDetail = e.message
            connectionState = ConnectionState.ERROR
        } finally {
            client.close()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (connectionState) {
                ConnectionState.CONNECTING -> {
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
                ConnectionState.CONNECTED -> {
                    Text(
                        "Connected ✓",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Karaoke mode now uses server-powered instrumental separation.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) { Text("Done") }
                }
                ConnectionState.ERROR -> {
                    Text("Connection failed", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text("Could not reach $KARAOKE_SERVER_URL")
                    errorDetail?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
