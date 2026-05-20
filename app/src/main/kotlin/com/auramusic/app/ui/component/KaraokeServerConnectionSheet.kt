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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import timber.log.Timber

enum class ConnectionState { CONNECTING, CONNECTED, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeServerConnectionSheet(
    onDismiss: () -> Unit,
    onConnected: () -> Unit
) {
    val (useServer, onUseServerChange) = rememberPreference(UseKaraokeServerKey, false)
    var connectionState by remember { mutableStateOf(ConnectionState.CONNECTING) }

    LaunchedEffect(Unit) {
        val client = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10000
                requestTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
        }
        var attempt = 0
        val maxAttempts = 6
        var lastError: Exception? = null
        while (attempt < maxAttempts) {
            try {
                Timber.tag("KaraokeServer").d("Connection attempt ${attempt + 1}/$maxAttempts")
                val response = client.get("https://karaoke.auramusic.site/health")
                Timber.tag("KaraokeServer").d("Server responded: ${response.status.value}")
                if (response.status.isSuccess()) {
                    delay(300)
                    connectionState = ConnectionState.CONNECTED
                    delay(300)
                    connectionState = ConnectionState.CONNECTED
                    onUseServerChange(true)
                    onConnected()
                    client.close()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                lastError = e
            }
            attempt++
            if (attempt < maxAttempts) {
                val delayMs = minOf(2000L * (1 shl (attempt - 1)), 30000L)
                delay(delayMs)
            }
        }
        connectionState = ConnectionState.ERROR
        client.close()
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
                    Text("Waking up Karaoke ML Server...")
                    Text("This may take up to 60 seconds on first request", style = MaterialTheme.typography.bodySmall)
                }
                ConnectionState.CONNECTED -> {
                    Text("Connected ✓", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Karaoke mode now uses server-powered instrumental separation.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
                ConnectionState.ERROR -> {
                    Text("Connection failed", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text("Could not reach karaoke.auramusic.site", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Check your internet connection", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss) { Text("Close") }
                    TextButton(onClick = {
                        connectionState = ConnectionState.CONNECTING
                        // Re-trigger connection by resetting LaunchedEffect key
                    }) { Text("Retry") }
                }
            }
        }
    }
}
