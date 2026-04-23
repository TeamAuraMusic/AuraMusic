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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
                .clickable(onClick = { onDismiss() })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Modern header with gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.share_song),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "×",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .clickable(onClick = onDismiss)
                                        .padding(8.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Album art with modern styling
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = songData.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        songData.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        songData.artist,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    songData.album?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Scrollable content area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            "Share to",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Grid layout for social media icons (2 columns)
                        val rows = platforms.chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rows.forEach { rowPlatforms ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPlatforms.forEach { platformItem ->
                                        SharePlatformButton(
                                            platformItem,
                                            isGenerating = isGenerating,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                ShareUtils.shareToSocialMedia(context, songData, platformItem.platform, cardFile)
                                                onDismiss()
                                            }
                                        )
                                    }
                                    // Fill remaining space if odd number of items
                                    if (rowPlatforms.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SharePlatformButton(
    platformItem: SharePlatformItem,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isGenerating, onClick = onClick)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        platformItem.color.copy(alpha = 0.15f),
                        platformItem.color.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(platformItem.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = platformItem.color
                )
            } else {
                Image(
                    painter = painterResource(platformItem.iconRes),
                    contentDescription = platformItem.platform.displayName,
                    modifier = Modifier.size(32.dp),
                    colorFilter = if (platformItem.platform == ShareUtils.SharePlatform.SNAPCHAT) null else ColorFilter.tint(Color.White)
                )
            }
        }

        Text(
            platformItem.platform.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

data class SharePlatformItem(val platform: ShareUtils.SharePlatform, val iconRes: Int, val color: Color)
