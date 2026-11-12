package com.lagradost.cloudstream3.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.isMovieType
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.cardDepth
import com.lagradost.cloudstream3.ui.poster.rememberPosterCardStyleUiState
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.fixVisual
import kotlin.math.roundToInt

private val ContinueWatchingCardWidth = 224.dp
private val ContinueWatchingCardHeight = 126.dp

@Composable
fun HomeContinueWatchingSection(
    title: String,
    items: List<SearchResponse>,
    onItemClick: (SearchResponse) -> Unit,
    onItemLongClick: (SearchResponse) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val posterStyle = rememberPosterCardStyleUiState()
    val cornerRadius = posterStyle.cornerRadiusDp.coerceIn(6, 16).dp

    ShelfSection(
        title = title,
        actionLabel = "View All",
        onActionClick = onViewAllClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { item -> "cw_${item.id ?: item.url}_${item.name}" },
        ) { item ->
            ContinueWatchingCard(
                item = item,
                cornerRadius = cornerRadius,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    item: SearchResponse,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
    val episodeBadge = remember(resumeItem) {
        if (resumeItem == null) return@remember null
        if (resumeItem.type?.isMovieType() == true) {
            "Movie"
        } else {
            val s = resumeItem.season
            val e = resumeItem.episode
            when {
                s != null && s > 0 && e != null -> "S${s}E${e}"
                e != null -> "E${e}"
                else -> "Resume"
            }
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { ContinueWatchingCardWidth.roundToPx() }
    val heightPx = with(density) { ContinueWatchingCardHeight.roundToPx() }
    val imageRequest = remember(context, item.posterUrl, widthPx, heightPx) {
        item.posterUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(widthPx, heightPx)
                .crossfade(true)
                .build()
        }
    }

    Box(
        modifier = Modifier
            .width(ContinueWatchingCardWidth)
            .height(ContinueWatchingCardHeight)
            .cardDepth(
                shape = shape,
                surface = CardDepthSurface.POSTER,
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Dark gradient overlay for ContinueWatchingCard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )

        // Top row: remaining time badge + more options button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (remainingText != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                ) {
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.56f),
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLongClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        // Bottom content: Episode badge + title + play button + progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (!episodeBadge.isNullOrBlank()) {
                        Text(
                            text = episodeBadge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    contentColor = Color.Black,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (progressFraction > 0f) {
                Spacer(modifier = Modifier.height(2.dp))
                PosterProgressBar(
                    progress = progressFraction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

