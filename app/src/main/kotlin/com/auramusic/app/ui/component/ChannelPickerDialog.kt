/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.auramusic.app.R
import com.auramusic.innertube.models.Artist

/**
 * Channel chooser for videos that credit more than one channel.
 *
 * A credit line like "A & B" carries a link per channel, and a plain text row can only honour
 * one of them. This dialog lists every linked channel so the viewer picks which page to open.
 */
@Composable
fun ChannelPickerDialog(
    channels: List<Artist>,
    onDismiss: () -> Unit,
    onSelect: (Artist) -> Unit,
) {
    ListDialog(onDismiss = onDismiss) {
        items(channels, key = { it.id ?: it.name }) { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = channel.id != null) { onSelect(channel) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
