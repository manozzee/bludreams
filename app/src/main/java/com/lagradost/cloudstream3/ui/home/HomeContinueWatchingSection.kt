package com.lagradost.cloudstream3.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.isMovieType
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.cardDepth
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.fixVisual
import kotlin.math.roundToInt

private val ContinueWatchingCardWidth = 170.dp
private val CardPlaceholderColor = Color(0xFF17171A)
private val TitleTextColor = Color(0xFFF5F5F7)
private val ActionTextColor = Color(0xFF9A9AA2)
private val SubtitleTextColor = Color(0xFF8E8E93)

@Composable
fun HomeContinueWatchingSection(
    title: String,
    items: List<SearchResponse>,
    onItemClick: (SearchResponse) -> Unit,
    onPlayClick: (SearchResponse) -> Unit,
    onRemoveClick: (SearchResponse) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleTextColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            if (onViewAllClick != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onViewAllClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ActionTextColor,
                        ),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View All",
                        tint = ActionTextColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        // 2. High-Performance LazyRow for 10+ items
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = items,
                key = { item -> item.id ?: "cw_${item.url}_${item.name}" },
            ) { item ->
                ContinueWatchingCard(
                    item = item,
                    onDetailsClick = { onItemClick(item) },
                    onPlayClick = { onPlayClick(item) },
                    onRemoveClick = { onRemoveClick(item) },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: SearchResponse,
    onDetailsClick: () -> Unit,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeItem = item as? DataStoreHelper.ResumeWatchingResult
    val fixedPos = remember(resumeItem?.watchPos) { resumeItem?.watchPos?.fixVisual() }
    val progressFraction = remember(fixedPos) {
        if (fixedPos != null && fixedPos.duration > 0L) {
            (fixedPos.position.toFloat() / fixedPos.duration.toFloat()).coerceIn(0.02f, 1f)
        } else {
            0f
        }
    }
    val remainingText = remember(fixedPos) {
        if (fixedPos != null && fixedPos.duration > fixedPos.position) {
            val remainingMin = ((fixedPos.duration - fixedPos.position) / 60_000.0).roundToInt()
            if (remainingMin > 0) "${remainingMin}m left" else null
        } else {
            null
        }
    }

    // Row 2 text formatting: "S1 E1 • 62m left" (or "62m left" for movies)
    val metadataLine = remember(resumeItem, remainingText) {
        val epToken = if (resumeItem != null && resumeItem.type?.isMovieType() != true) {
            val s = resumeItem.season
            val e = resumeItem.episode
            when {
                s != null && s > 0 && e != null -> "S$s E$e"
                e != null -> "E$e"
                else -> null
            }
        } else {
            null
        }

        when {
            epToken != null && remainingText != null -> "$epToken • $remainingText"
            epToken != null -> epToken
            remainingText != null -> remainingText
            else -> "Resume"
        }
    }

    val cardShape = RoundedCornerShape(12.dp)
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { ContinueWatchingCardWidth.roundToPx() }
    val heightPx = with(density) { (ContinueWatchingCardWidth * 9f / 16f).roundToPx() }
    val imageRequest = remember(context, item.posterUrl, widthPx, heightPx) {
        item.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(widthPx, heightPx)
                .crossfade(true)
                .build()
        }
    }

    Column(
        modifier = modifier.width(ContinueWatchingCardWidth),
    ) {
        // Clean 16:9 Thumbnail Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .cardDepth(
                    shape = cardShape,
                    surface = CardDepthSurface.POSTER,
                )
                .clip(cardShape)
                .background(CardPlaceholderColor)
                .clickable(onClick = onDetailsClick),
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Tight bottom scrim gradient covering only bottom 32% of thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(0.32f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.70f),
                        ),
                    ),
            )

            // Small, crisp white Play triangle icon in bottom-left corner
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 10.dp)
                    .size(13.dp),
            )

            // Edge-to-edge flush bottom progress bar (3.5.dp) placed LAST to render on top
            HotstarProgressBar(
                progress = progressFraction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.5.dp),
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Clean 2-Line Metadata Block Below Thumbnail
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDetailsClick),
        ) {
            // Row 1: Title + 3-Dot Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitleTextColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Compact vertical 3-dot overflow icon anchored to modern Material 3 DropdownMenu
                var showOptionsMenu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { showOptionsMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_more_vert_24),
                            contentDescription = "Options",
                            tint = ActionTextColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        containerColor = CardPlaceholderColor,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.home_play),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = TitleTextColor,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = TitleTextColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                onPlayClick()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_open_watching),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = TitleTextColor,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TitleTextColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                onDetailsClick()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_remove_watching),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color(0xFFFF6B6B),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                onRemoveClick()
                            },
                        )
                    }
                }
            }

            // Row 2: Episode & Time Left
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadataLine,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubtitleTextColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HotstarProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.28f),
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "hotstarProgressBar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.5.dp)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.5.dp)
                .background(indicatorColor),
        )
    }
}


