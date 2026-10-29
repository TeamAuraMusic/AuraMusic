package com.auramusic.app.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared hero header used by the album, artist and playlist screens: a blurred artwork
 * backdrop tinted with the artwork's accent colour that fades into the screen background.
 */

/** Vibrant accent colour extracted from [url]'s artwork, animated; falls back to [fallback]. */
@Composable
fun rememberArtworkAccent(url: String?, fallback: Color = MaterialTheme.colorScheme.primary): Color {
    val context = LocalContext.current
    var extracted by remember(url) { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val bitmap = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(url).size(128).allowHardware(false).build()
            runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
        } ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) {
            Palette.from(bitmap).maximumColorCount(12).generate()
        }
        val rgb = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
        if (rgb != null) extracted = Color(rgb)
    }
    val color by animateColorAsState(extracted ?: fallback, tween(700), label = "artworkAccent")
    return color
}

/**
 * Full-bleed backdrop: the artwork blurred and tinted with [accent], fading to the
 * background colour at the bottom. [content] is drawn on top.
 */
@Composable
fun CollectionHeroBackdrop(
    imageUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().background(accent.copy(alpha = 0.35f)))
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier.matchParentSize().blur(blurRadius),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.25f),
                        0.45f to background.copy(alpha = 0.35f),
                        0.8f to background.copy(alpha = 0.9f),
                        1f to background,
                    ),
                ),
        )
        content()
    }
}

/** Artwork with a large rounded shape and an accent-coloured glow. */
@Composable
fun CollectionArtwork(
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 232.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 32.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.5f),
                spotColor = accent.copy(alpha = 0.7f),
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        content = content,
    )
}

/** Title, optional overline/subtitle and metadata chips, centred under the artwork. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionTitleBlock(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    chips: List<String> = emptyList(),
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!overline.isNullOrBlank()) {
            Text(
                text = overline.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            subtitle()
        }
        val visibleChips = chips.filter { it.isNotBlank() }
        if (visibleChips.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleChips.forEach { MetaChip(it) }
            }
        }
    }
}

@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Row of hero actions: secondary icons on the sides and pill buttons in the middle. */
@Composable
fun CollectionActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Filled (accent) or tonal pill button with an icon and label. */
@Composable
fun HeroPillButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val container = if (filled) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    val contentColor = if (filled) bestContentColor(accent) else MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = contentColor,
        shadowElevation = if (filled) 6.dp else 0.dp,
        modifier = modifier.height(52.dp),
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 20.dp, end = 24.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

/** Small circular translucent icon button for secondary hero actions. */
@Composable
fun HeroIconButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    iconContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        contentColor = tint,
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (iconContent != null) {
                iconContent()
            } else {
                Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Section header with an accent bar, used for song lists and shelves under the hero. */
@Composable
fun CollectionSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke(this)
    }
}

private fun bestContentColor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color.Black else Color.White
}
