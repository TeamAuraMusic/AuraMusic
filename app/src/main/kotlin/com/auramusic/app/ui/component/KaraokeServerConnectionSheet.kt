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
import io.ktor.client.request.head
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay

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
        // expectSuccess = false: Ktor 3.x defaults to true and would throw
        // ClientRequestException on any non-2xx (e.g. 404), bypassing our status check.
        val client = HttpClient(CIO) {
            expectSuccess = false
        }
        try {
            val response = client.head("https://karaoke.auramusic.site/")
            if (response.status.isSuccess() || response.status.value == 404) {
                delay(300)
                connectionState = ConnectionState.CONNECTED
                onUseServerChange(true)
                onConnected()
            } else {
                connectionState = ConnectionState.ERROR
            }
        } catch (e: Exception) {
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
                    Text("https://karaoke.auramusic.site/", style = MaterialTheme.typography.bodySmall)
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
                    Text("Could not reach https://karaoke.auramusic.site/")
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
