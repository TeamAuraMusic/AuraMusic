/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import java.io.File

@Composable
fun ShareSongBottomSheet(
    songData: ShareUtils.SongShareData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var cardFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    // Pre-generate the share card when the sheet opens
    LaunchedEffect(songData) {
        isGenerating = true
        cardFile = ShareUtils.generateShareCard(context, songData)
        isGenerating = false
    }

    val platforms = listOf(
        SharePlatformItem(ShareUtils.SharePlatform.INSTAGRAM, R.drawable.instagram, Color(0xFFE4405F)),
        SharePlatformItem(ShareUtils.SharePlatform.FACEBOOK, R.drawable.facebook, Color(0xFF1877F2)),
        SharePlatformItem(ShareUtils.SharePlatform.WHATSAPP, R.drawable.whatsapp, Color(0xFF25D366)),
        SharePlatformItem(ShareUtils.SharePlatform.X, R.drawable.x_logo, Color(0xFF000000)),
        SharePlatformItem(ShareUtils.SharePlatform.TELEGRAM, R.drawable.telegram, Color(0xFF0088CC)),
        SharePlatformItem(ShareUtils.SharePlatform.SNAPCHAT, R.drawable.snapchat, Color(0xFFFFFC00)),
        SharePlatformItem(ShareUtils.SharePlatform.TIKTOK, R.drawable.tiktok, Color(0xFFEE1D52)),
        SharePlatformItem(ShareUtils.SharePlatform.GENERIC, R.drawable.share, MaterialTheme.colorScheme.primary)
    )

    // Dimmed background for the dialog to prevent seeing content behind
    val backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.32f)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Background that dims the content behind
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clickable { onDismiss() }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.share_song),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

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

                    // Vertical layout for social media icons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        platforms.forEach { platformItem ->
                            SharePlatformButton(platformItem, isGenerating = isGenerating, onClick = {
                                ShareUtils.shareToSocialMedia(context, songData, platformItem.platform, cardFile)
                                onDismiss()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharePlatformButton(platformItem: SharePlatformItem, isGenerating: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isGenerating) { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(platformItem.color.copy(alpha = 0.15f)).padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Image(
                    painter = painterResource(platformItem.iconRes),
                    contentDescription = platformItem.platform.displayName,
                    modifier = Modifier.size(28.dp),
                    colorFilter = if (platformItem.platform == ShareUtils.SharePlatform.SNAPCHAT) null else ColorFilter.tint(platformItem.color)
                )
            }
        }
        Text(platformItem.platform.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

data class SharePlatformItem(val platform: ShareUtils.SharePlatform, val iconRes: Int, val color: Color)
