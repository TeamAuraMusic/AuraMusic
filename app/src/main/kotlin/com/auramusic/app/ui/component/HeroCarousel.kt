/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.auramusic.app.R
import com.auramusic.innertube.models.YTItem
import kotlinx.coroutines.delay

private data class CarouselDimens(
    val height: Dp,
    val horizontalPadding: Dp,
    val cornerRadius: Dp,
    val playButtonSize: Dp,
    val playIconSize: Dp,
    val pageSpacing: Dp,
    val indicatorWidth: Dp,
    val indicatorDot: Dp,
    val indicatorSpacing: Dp,
)

@Composable
private fun rememberCarouselDimens(screenWidth: Dp): CarouselDimens {
    val isSmallScreen = screenWidth < 360.dp
    val isTablet = screenWidth >= 600.dp
    return CarouselDimens(
        height = when {
            isTablet -> 500.dp
            isSmallScreen -> 280.dp
            else -> 280.dp
        },
        horizontalPadding = when {
            isTablet -> 24.dp
            isSmallScreen -> 12.dp
            else -> 16.dp
        },
        cornerRadius = when {
            isTablet -> 20.dp
            isSmallScreen -> 12.dp
            else -> 16.dp
        },
        playButtonSize = when {
            isTablet -> 56.dp
            isSmallScreen -> 36.dp
            else -> 44.dp
        },
        playIconSize = when {
            isTablet -> 28.dp
            isSmallScreen -> 16.dp
            else -> 22.dp
        },
        pageSpacing = if (isTablet) 16.dp else 12.dp,
        indicatorWidth = if (isTablet) 24.dp else 20.dp,
        indicatorDot = if (isTablet) 8.dp else 6.dp,
        indicatorSpacing = if (isTablet) 14.dp else 10.dp,
    )
}

@Composable
fun HeroCarouselShimmer(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val dimens = rememberCarouselDimens(maxWidth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.horizontalPadding)
                .height(dimens.height)
                .clip(RoundedCornerShape(dimens.cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun HeroCarousel(
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit,
    onPlayClick: (YTItem) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = 4000L,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState, items.size) {
        if (items.size > 1) {
            while (true) {
                delay(autoScrollInterval)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val dimens = rememberCarouselDimens(maxWidth)
        val isSmallScreen = maxWidth < 360.dp
        val isTablet = maxWidth >= 600.dp

        val subtitleStyle = when {
            isTablet -> MaterialTheme.typography.bodyMedium
            isSmallScreen -> MaterialTheme.typography.labelSmall
            else -> MaterialTheme.typography.bodySmall
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.horizontalPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.height)
                    .clip(RoundedCornerShape(dimens.cornerRadius)),
                pageSpacing = dimens.pageSpacing,
            ) { page ->
                val item = items[page]
                HeroCarouselCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onPlayClick = { onPlayClick(item) },
                    cornerRadius = dimens.cornerRadius,
                    playButtonSize = dimens.playButtonSize,
                    playIconSize = dimens.playIconSize,
                    subtitleStyle = subtitleStyle,
                )
            }

            if (items.size > 1) {
                Spacer(modifier = Modifier.height(dimens.indicatorSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) dimens.indicatorWidth else dimens.indicatorDot,
                            animationSpec = tween(300),
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(dimens.indicatorDot)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCarouselCard(
    item: YTItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    cornerRadius: Dp,
    playButtonSize: Dp,
    playIconSize: Dp,
    subtitleStyle: androidx.compose.ui.text.TextStyle,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )

            IconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(playButtonSize),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(playIconSize),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val subtitle = item.subtitle
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = subtitleStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val YTItem.subtitle: String?
    get() = when (this) {
        is com.auramusic.innertube.models.SongItem -> artists.joinToString(", ") { it.name }
        is com.auramusic.innertube.models.AlbumItem -> artists?.joinToString(", ") { it.name }
        is com.auramusic.innertube.models.ArtistItem -> null
        is com.auramusic.innertube.models.PlaylistItem -> author?.name
        is com.auramusic.innertube.models.PodcastItem -> author?.name
        is com.auramusic.innertube.models.EpisodeItem -> author?.name
    }
