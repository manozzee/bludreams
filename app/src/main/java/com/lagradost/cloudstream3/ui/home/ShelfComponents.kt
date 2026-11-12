package com.lagradost.cloudstream3.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ui.liquidglass.MetaPreview
import com.lagradost.cloudstream3.ui.liquidglass.PosterShape
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.PosterCardStyleUiState
import com.lagradost.cloudstream3.ui.poster.PosterLandscapeAspectRatio
import com.lagradost.cloudstream3.ui.poster.PosterPortraitAspectRatio
import com.lagradost.cloudstream3.ui.poster.cardDepth
import com.lagradost.cloudstream3.ui.poster.landscapePosterHeight
import com.lagradost.cloudstream3.ui.poster.landscapePosterWidth
import com.lagradost.cloudstream3.ui.poster.rememberPosterCardStyleUiState

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = "View All",
    onActionClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (onActionClick != null && !actionLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = actionLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun ShelfSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = "View All",
    onActionClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    onNearEndReached: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    if (onNearEndReached != null) {
        val shouldLoadMore by remember(listState) {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 4
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) {
                onNearEndReached()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            actionLabel = actionLabel,
            onActionClick = onActionClick,
            trailingContent = trailingContent,
        )
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    landscapePosterUrl: String? = null,
    logoUrl: String? = null,
    subtitle: String? = null,
    ratingText: String? = null,
    badgeText: String? = null,
    isWatched: Boolean = false,
    progressFraction: Float? = null,
    forceShape: PosterShape? = null,
    posterStyleOverride: PosterCardStyleUiState? = null,
    customWidth: Dp? = null,
) {
    val posterStyle = posterStyleOverride ?: rememberPosterCardStyleUiState()
    val useLandscape = when (forceShape) {
        PosterShape.Landscape -> true
        PosterShape.Poster -> false
        PosterShape.Square -> false
        null -> posterStyle.landscapePostersEnabled
    }
    val cardWidth: Dp = customWidth ?: if (useLandscape) {
        landscapePosterWidth(posterStyle.widthDp)
    } else {
        posterStyle.widthDp.dp
    }
    val cardHeight: Dp = if (customWidth != null) {
        if (useLandscape) {
            (customWidth / PosterLandscapeAspectRatio)
        } else {
            (customWidth / PosterPortraitAspectRatio)
        }
    } else when {
        forceShape == PosterShape.Square -> posterStyle.widthDp.dp
        useLandscape -> landscapePosterHeight(posterStyle.widthDp)
        else -> posterStyle.heightDp.dp
    }
    val cardShape = RoundedCornerShape(posterStyle.cornerRadiusDp.dp)
    val showClearlogoOverlay = useLandscape && posterStyle.alwaysShowClearlogoOnLandscape
    val showBelowLabels = !posterStyle.hideLabels && !showClearlogoOverlay

    val resolvedImageUrl = if (useLandscape) {
        landscapePosterUrl?.takeIf { it.isNotBlank() } ?: posterUrl
    } else {
        posterUrl ?: landscapePosterUrl
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { cardWidth.roundToPx().coerceAtLeast(1) }
    val heightPx = with(density) { cardHeight.roundToPx().coerceAtLeast(1) }
    val imageRequest = remember(context, resolvedImageUrl, widthPx, heightPx) {
        resolvedImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(widthPx, heightPx)
                .crossfade(true)
                .build()
        }
    }

    var logoLoadFailed by remember(logoUrl, title) { mutableStateOf(false) }

    Column(
        modifier = modifier.width(cardWidth),
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .cardDepth(
                    shape = cardShape,
                    surface = CardDepthSurface.POSTER,
                )
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = cardShape,
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.background,
                                ),
                            ),
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showClearlogoOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight * 0.65f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.48f),
                                    Color.Black.copy(alpha = 0.86f),
                                ),
                            ),
                        ),
                )
                val validLogo = logoUrl?.takeIf { it.isNotBlank() && !logoLoadFailed }
                if (validLogo != null) {
                    AsyncImage(
                        model = validLogo,
                        contentDescription = title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .fillMaxWidth(0.68f)
                            .height((cardHeight * 0.32f).coerceIn(24.dp, 46.dp)),
                        alignment = Alignment.BottomStart,
                        contentScale = ContentScale.Fit,
                        onError = { logoLoadFailed = true },
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            // Top-left badge (Dub/Sub/Episodes or quality)
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Top-right watched or rating badge
            if (isWatched) {
                AnimatedWatchedBadge(
                    visible = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            } else if (!ratingText.isNullOrBlank() && posterStyle.showRating) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF5C518),
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (progressFraction != null && progressFraction > 0f) {
                PosterProgressBar(
                    progress = progressFraction,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        if (showBelowLabels) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PosterCardFromSearchResponse(
    item: SearchResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    forceShape: PosterShape? = null,
    customWidth: Dp? = null,
    progressFraction: Float? = null,
) {
    val posterStyle = rememberPosterCardStyleUiState()
    val ratingText = remember(item.score, posterStyle.showRating) {
        if (posterStyle.showRating) {
            item.score?.toStringNull(0.1, 10, 1, false)
        } else null
    }
    val badgeText = remember(item, posterStyle.showDub, posterStyle.showSub, posterStyle.showEpisode, posterStyle.showQuality) {
        when (item) {
            is AnimeSearchResponse -> {
                val dubEp = if (posterStyle.showEpisode) item.episodes[DubStatus.Dubbed] else null
                val subEp = if (posterStyle.showEpisode) item.episodes[DubStatus.Subbed] else null
                buildList {
                    if (posterStyle.showDub) {
                        if (dubEp != null && dubEp > 0) add("DUB $dubEp")
                        else if (item.dubStatus?.contains(DubStatus.Dubbed) == true) add("DUB")
                    }
                    if (posterStyle.showSub) {
                        if (subEp != null && subEp > 0) add("SUB $subEp")
                        else if (item.dubStatus?.contains(DubStatus.Subbed) == true) add("SUB")
                    }
                }.joinToString(" · ").takeIf { it.isNotBlank() }
            }
            else -> if (posterStyle.showQuality) item.quality?.name else null
        }
    }

    val computedProgress = progressFraction ?: remember(item) {
        if (item is com.lagradost.cloudstream3.syncproviders.SyncAPI.LibraryItem) {
            val completed = item.episodesCompleted
            val total = item.episodesTotal
            if (completed != null && total != null && total > 0 && completed > 0) {
                (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            } else null
        } else null
    }

    PosterCard(
        title = item.name,
        posterUrl = item.posterUrl,
        landscapePosterUrl = item.posterUrl,
        ratingText = ratingText,
        badgeText = badgeText,
        forceShape = forceShape,
        progressFraction = computedProgress,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        posterStyleOverride = posterStyle,
        customWidth = customWidth,
    )
}

@Composable
fun PosterCardFromMetaPreview(
    item: MetaPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    forceShape: PosterShape? = null,
) {
    val posterStyle = rememberPosterCardStyleUiState()
    PosterCard(
        title = item.name,
        posterUrl = item.poster ?: item.rawPosterUrl,
        landscapePosterUrl = item.landscapePoster ?: item.banner ?: item.poster,
        logoUrl = item.logo,
        subtitle = item.releaseInfo,
        ratingText = if (posterStyle.showRating) item.imdbRating else null,
        forceShape = forceShape,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        posterStyleOverride = posterStyle,
    )
}

@Composable
fun PosterProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.24f),
    indicatorColor: Color = Color.White,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "posterProgressBar",
    )
    Box(
        modifier = modifier
            .height(3.5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(indicatorColor),
        )
    }
}

@Composable
fun AnimatedWatchedBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.8f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f),
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.72f),
            modifier = Modifier
                .size(22.dp)
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Watched",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

