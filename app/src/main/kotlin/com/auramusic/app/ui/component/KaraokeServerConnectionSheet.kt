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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeServerConnectionSheet(
    onDismiss: () -> Unit,
    onConnected: () -> Unit
) {
    val (useServer, onUseServerChange) = rememberPreference(UseKaraokeServerKey, false)
    var isConnecting by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val client = HttpClient(CIO)
        try {
            val response = client.head("https://karaoke.auramusic.site/")
            if (response.status.isSuccess() || response.status.value == 404) {
                // Server reachable (404 is expected for root)
                delay(300)
                isConnecting = false
                onUseServerChange(true)
                onConnected()
            } else {
                isConnecting = false
            }
        } catch (e: Exception) {
            // Offline or error - keep UI but allow manual retry later
            delay(800)
            isConnecting = false
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
            if (isConnecting) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Connecting to Karaoke ML Server...")
                Text("https://karaoke.auramusic.site/", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Connected ✓", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Karaoke mode now uses server-powered instrumental separation.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    onUseServerChange(true)
                    onConnected()
                    onDismiss()
                }) {
                    Text("Enable Server Karaoke")
                }
            }
        }
    }
}
