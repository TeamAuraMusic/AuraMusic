/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.Artist
import java.util.concurrent.ConcurrentHashMap

/**
 * Channel avatar URLs by channel id.
 *
 * A video's byline only carries the primary channel's avatar; credited co-channels arrive as
 * a name and an id with no image. The avatar is looked up once per channel and remembered for
 * the process - without this, every card and the channel picker would show a grey person
 * glyph for the second channel.
 */
object ChannelAvatarStore {
    private val avatars = ConcurrentHashMap<String, String?>()

    suspend fun avatarFor(channelId: String): String? {
        if (avatars.containsKey(channelId)) return avatars[channelId]
        val url = runCatching {
            YouTube.youtubeChannel(channelId, params = null).getOrNull()?.avatarUrl
        }.getOrNull()
        avatars[channelId] = url
        return url
    }
}

/**
 * Avatar for one credited channel. Uses [thumbnailUrl] when one was already fetched for this
 * channel (the primary channel's art travels with the video), and looks it up otherwise.
 */
@Composable
fun CreditedChannelAvatar(
    channel: Artist,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val channelId = channel.id
    val url by produceState<String?>(initialValue = thumbnailUrl, channelId, thumbnailUrl) {
        if (value == null && channelId != null) {
            value = ChannelAvatarStore.avatarFor(channelId)
        }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = channel.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            androidx.compose.material3.Text(
                text = channel.name.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The channel art for a video, showing up to two credited channels as an overlapping pair.
 *
 * A single-channel video keeps the plain round avatar it always had. A two-channel credit
 * ("A & B") used to show only the first channel's art; the second channel now peeks out from
 * behind it, so the card reads as a collaboration before it is even tapped.
 */
@Composable
fun ChannelAvatarStack(
    channels: List<Artist>,
    primaryThumbnailUrl: String?,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (channels.size <= 1) {
        CreditedChannelAvatar(
            channel = channels.firstOrNull() ?: Artist("", null),
            thumbnailUrl = primaryThumbnailUrl,
            modifier = modifier
                .size(size)
                .clickable(onClick = onClick),
        )
        return
    }
    val overlap = size * 0.62f
    Box(
        modifier = modifier
            .size(width = size + overlap, height = size)
            .clickable(onClick = onClick),
    ) {
        CreditedChannelAvatar(
            channel = channels[1],
            thumbnailUrl = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(size)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
        CreditedChannelAvatar(
            channel = channels[0],
            thumbnailUrl = primaryThumbnailUrl,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(size)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}
