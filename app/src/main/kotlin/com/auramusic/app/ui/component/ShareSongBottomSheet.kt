/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareSongBottomSheet(
    songData: ShareUtils.SongShareData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val platforms = listOf(
        SharePlatformItem(ShareUtils.SharePlatform.INSTAGRAM, R.drawable.instagram, Color(0xFFE4405F)),
        SharePlatformItem(ShareUtils.SharePlatform.FACEBOOK, R.drawable.facebook, Color(0xFF1877F2)),
        SharePlatformItem(ShareUtils.SharePlatform.WHATSAPP, R.drawable.whatsapp, Color(0xFF25D366)),
        SharePlatformItem(ShareUtils.SharePlatform.TWITTER, R.drawable.twitter, Color(0xFF1DA1F2)),
        SharePlatformItem(ShareUtils.SharePlatform.TELEGRAM, R.drawable.telegram, Color(0xFF0088CC)),
        SharePlatformItem(ShareUtils.SharePlatform.SNAPCHAT, R.drawable.snapchat, Color(0xFFFFFC00)),
        SharePlatformItem(ShareUtils.SharePlatform.TIKTOK, R.drawable.tiktok, Color(0xFF000000)),
        SharePlatformItem(ShareUtils.SharePlatform.GENERIC, R.drawable.share, MaterialTheme.colorScheme.primary)
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.share_song), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = songData.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(songData.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(songData.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))

                Text("Share to", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(platforms) { platformItem ->
                        SharePlatformButton(platformItem, onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val cardFile = ShareUtils.generateShareCard(context, songData)
                                withContext(Dispatchers.Main) {
                                    ShareUtils.shareToSocialMedia(context, songData, platformItem.platform, cardFile)
                                    onDismiss()
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SharePlatformButton(platformItem: SharePlatformItem, isGenerating: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = !isGenerating) { onClick() }) {
        Box(
            modifier = Modifier.size(56.dp).background(color = platformItem.color.copy(alpha = 0.15f), shape = CircleShape).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                androidx.compose.foundation.Image(painter = painterResource(platformItem.iconRes), contentDescription = platformItem.platform.displayName, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(platformItem.platform.displayName, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, fontSize = 11.sp)
    }
}

data class SharePlatformItem(val platform: ShareUtils.SharePlatform, val iconRes: Int, val color: Color)
