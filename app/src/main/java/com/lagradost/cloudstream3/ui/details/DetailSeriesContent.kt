package com.lagradost.cloudstream3.ui.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.home.PosterProgressBar
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.cardDepth
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.ui.result.getWatchProgress
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailEpisodesHeader(
    seasonOptions: List<SeasonOption>,
    selectedSeasonLabel: String?,
    dubOptions: List<DubOption>,
    selectedDubLabel: String?,
    rangeOptions: List<RangeOption>,
    selectedRangeLabel: String?,
    selectedSortLabel: String?,
    hasSortOptions: Boolean,
    hasDownloadSupport: Boolean,
    episodesCountText: String?,
    episodesList: List<ResultEpisode>,
    episodeLayoutStyle: EpisodeLayoutStyle,
    onToggleEpisodeLayoutStyle: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onDubSelected: (DubStatus) -> Unit,
    onRangeSelected: (EpisodeRange) -> Unit,
    onSortClick: () -> Unit,
    onBatchDownloadClick: (List<ResultEpisode>) -> Unit,
    onBatchDownloadLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLayoutToggle: Boolean = true,
    horizontalPadding: Dp = 20.dp,
    topRowTrailingContent: (@Composable () -> Unit)? = null,
    elevatedGlassContrast: Boolean = false,
) {
    val hasSeasons = seasonOptions.size > 1
    val hasDubOptions = dubOptions.size > 1
    val hasRangeOptions = rangeOptions.size > 1
    val headingColor = if (elevatedGlassContrast) MaterialTheme.liquidGlass.colors.textPrimary else Color(0xFFF5F5F7)
    val mutedCountColor = if (elevatedGlassContrast) {
        MaterialTheme.liquidGlass.colors.textMuted.copy(alpha = 0.85f)
    } else {
        Color(0xFF8E8E93)
    }
    val actionTintColor = if (elevatedGlassContrast) {
        MaterialTheme.liquidGlass.colors.textSecondary.copy(alpha = 0.88f)
    } else {
        Color(0xFFB0B0B8)
    }
    val inactiveChipTextColor = if (elevatedGlassContrast) {
        MaterialTheme.liquidGlass.colors.textMuted.copy(alpha = 0.85f)
    } else {
        Color(0xFF8E8E93)
    }
    val activeChipTextColor = if (elevatedGlassContrast) {
        MaterialTheme.liquidGlass.colors.textPrimary
    } else {
        Color(0xFFF5F5F7)
    }
    val cleanCount = remember(episodesCountText) {
        episodesCountText?.let { raw ->
            Regex("\\d+").find(raw)?.value ?: raw.trim()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Tier 1: Horizontal Season Pills (no section header)
        if (hasSeasons) {
            val seasonKey = remember(seasonOptions) { seasonOptions.firstOrNull()?.seasonNumber?.toString() ?: "" }
            val seasonListState = rememberSaveable(
                seasonKey,
                saver = LazyListState.Saver,
            ) {
                LazyListState()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    state = seasonListState,
                    modifier = if (topRowTrailingContent != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = if (topRowTrailingContent != null) {
                            if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp
                        } else {
                            horizontalPadding
                        },
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp,
                    ),
                ) {
                    items(seasonOptions, key = { "season_${it.seasonNumber}_${it.label}" }) { option ->
                        val isSelected = option.label == selectedSeasonLabel
                        SeasonGlassPill(
                            label = option.label,
                            selected = isSelected,
                            onClick = { onSeasonSelected(option.seasonNumber) },
                            elevatedGlassContrast = elevatedGlassContrast,
                        )
                    }
                }
                if (topRowTrailingContent != null) {
                    Box(modifier = Modifier.padding(end = horizontalPadding)) {
                        topRowTrailingContent()
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s10 else 14.dp))
        }

        // Tier 2: Episodes Header + Muted Count + Borderless Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.episodes),
                    style = if (elevatedGlassContrast) {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = LiquidGlassTokens.Type.bodyApp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    color = headingColor,
                )
                if (!cleanCount.isNullOrBlank()) {
                    Text(
                        text = cleanCount,
                        style = if (elevatedGlassContrast) {
                            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        color = mutedCountColor,
                        modifier = Modifier.padding(
                            start = if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp,
                        ),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    if (elevatedGlassContrast) LiquidGlassTokens.Space.s4 else 6.dp,
                ),
            ) {
                if (hasSortOptions) {
                    Row(
                        modifier = Modifier
                            .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s28 else 34.dp)
                            .clip(
                                RoundedCornerShape(
                                    if (elevatedGlassContrast) LiquidGlassTokens.Radius.md else 8.dp,
                                ),
                            )
                            .clickable(onClick = onSortClick)
                            .padding(
                                horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (elevatedGlassContrast) LiquidGlassTokens.Space.s4 else 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Episodes",
                            tint = actionTintColor,
                            modifier = Modifier.size(
                                if (elevatedGlassContrast) LiquidGlassTokens.Space.s14 else 16.dp,
                            ),
                        )
                        if (!selectedSortLabel.isNullOrBlank()) {
                            Text(
                                text = selectedSortLabel,
                                style = if (elevatedGlassContrast) {
                                    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                } else {
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                color = actionTintColor,
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (hasDownloadSupport && episodesList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { onBatchDownloadClick(episodesList) },
                                onLongClick = onBatchDownloadLongClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All",
                            tint = Color(0xFFB0B0B8),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                if (showLayoutToggle) {
                    // Toggle between Horizontal Episode Carousel vs Vertical Episode List
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleEpisodeLayoutStyle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (episodeLayoutStyle == EpisodeLayoutStyle.LIST) {
                                Icons.Rounded.ViewAgenda
                            } else {
                                Icons.Rounded.GridView
                            },
                            contentDescription = "Switch Episode Layout",
                            tint = Color(0xFFB0B0B8),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (!hasSeasons && topRowTrailingContent != null) {
                    topRowTrailingContent()
                }
            }
        }

        // Tier 3: Grouped Sub / Dub Segmented Toggle & Episode Range Filter Bar
        if (hasDubOptions || hasRangeOptions) {
            Spacer(modifier = Modifier.height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s8 else 10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasDubOptions) {
                    Row(
                        modifier = Modifier
                            .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s28 else 30.dp)
                            .clip(
                                RoundedCornerShape(
                                    if (elevatedGlassContrast) LiquidGlassTokens.Radius.md else 8.dp,
                                ),
                            )
                            .background(Color.White.copy(alpha = if (elevatedGlassContrast) 0.10f else 0.06f))
                            .padding(if (elevatedGlassContrast) LiquidGlassTokens.Space.s2 else 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dubOptions.forEach { dub ->
                            val isSelected = dub.label == selectedDubLabel
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(
                                        RoundedCornerShape(
                                            if (elevatedGlassContrast) LiquidGlassTokens.Radius.sm else 6.dp,
                                        ),
                                    )
                                    .background(
                                        if (isSelected) {
                                            Color.White.copy(alpha = if (elevatedGlassContrast) 0.22f else 0.16f)
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .clickable { onDubSelected(dub.status) }
                                    .padding(
                                        horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s8 else 10.dp,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dub.label,
                                    style = if (elevatedGlassContrast) {
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        )
                                    } else {
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        )
                                    },
                                    color = if (isSelected) activeChipTextColor else inactiveChipTextColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (hasDubOptions && hasRangeOptions) {
                    Box(
                        modifier = Modifier
                            .padding(
                                horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s8 else 10.dp,
                            )
                            .width(LiquidGlassTokens.Border.thin)
                            .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s14 else 16.dp)
                            .background(Color.White.copy(alpha = if (elevatedGlassContrast) 0.20f else 0.12f)),
                    )
                }

                if (hasRangeOptions) {
                    val rangeKey = remember(rangeOptions) {
                        rangeOptions.firstOrNull()?.label.orEmpty()
                    }
                    val rangeListState = rememberSaveable(
                        rangeKey,
                        saver = LazyListState.Saver,
                    ) {
                        LazyListState()
                    }
                    LazyRow(
                        state = rangeListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (elevatedGlassContrast) LiquidGlassTokens.Space.s5 else 6.dp,
                        ),
                    ) {
                        items(rangeOptions, key = { "range_${it.range.startIndex}_${it.label}" }) { rangeOpt ->
                            val isSelected = rangeOpt.label == selectedRangeLabel
                            Box(
                                modifier = Modifier
                                    .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s28 else 30.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            if (elevatedGlassContrast) LiquidGlassTokens.Radius.md else 8.dp,
                                        ),
                                    )
                                    .background(
                                        if (isSelected) {
                                            Color.White.copy(alpha = if (elevatedGlassContrast) 0.22f else 0.16f)
                                        } else if (elevatedGlassContrast) {
                                            Color.White.copy(alpha = 0.06f)
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .clickable { onRangeSelected(rangeOpt.range) }
                                    .padding(
                                        horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s8 else 10.dp,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = rangeOpt.label,
                                    style = if (elevatedGlassContrast) {
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        )
                                    } else {
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        )
                                    },
                                    color = if (isSelected) activeChipTextColor else inactiveChipTextColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailSeriesContent(
    episodesResource: Resource<List<ResultEpisode>>?,
    episodesCountText: String?,
    noEpisodesFoundText: String?,
    seasonOptions: List<SeasonOption>,
    selectedSeasonLabel: String?,
    dubOptions: List<DubOption>,
    selectedDubLabel: String?,
    rangeOptions: List<RangeOption>,
    selectedRangeLabel: String?,
    selectedSortLabel: String?,
    hasSortOptions: Boolean,
    hasDownloadSupport: Boolean,
    fallbackBackdropUrl: String?,
    episodeLayoutStyle: EpisodeLayoutStyle,
    onToggleEpisodeLayoutStyle: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onDubSelected: (DubStatus) -> Unit,
    onRangeSelected: (EpisodeRange) -> Unit,
    onSortClick: () -> Unit,
    onBatchDownloadClick: (List<ResultEpisode>) -> Unit,
    onBatchDownloadLongClick: () -> Unit,
    onEpisodeClick: (ResultEpisode) -> Unit,
    onEpisodeLongClick: (ResultEpisode) -> Unit,
    onEpisodeDownloadClick: (ResultEpisode) -> Unit,
    onEpisodeDownloadLongClick: (ResultEpisode) -> Unit,
    onEpisodeToggleWatched: (ResultEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val episodesList = (episodesResource as? Resource.Success)?.value.orEmpty()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailEpisodesHeader(
            seasonOptions = seasonOptions,
            selectedSeasonLabel = selectedSeasonLabel,
            dubOptions = dubOptions,
            selectedDubLabel = selectedDubLabel,
            rangeOptions = rangeOptions,
            selectedRangeLabel = selectedRangeLabel,
            selectedSortLabel = selectedSortLabel,
            hasSortOptions = hasSortOptions,
            hasDownloadSupport = hasDownloadSupport,
            episodesCountText = episodesCountText,
            episodesList = episodesList,
            episodeLayoutStyle = episodeLayoutStyle,
            onToggleEpisodeLayoutStyle = onToggleEpisodeLayoutStyle,
            onSeasonSelected = onSeasonSelected,
            onDubSelected = onDubSelected,
            onRangeSelected = onRangeSelected,
            onSortClick = onSortClick,
            onBatchDownloadClick = onBatchDownloadClick,
            onBatchDownloadLongClick = onBatchDownloadLongClick,
        )

        // Episodes Body
        when (episodesResource) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is Resource.Success -> {
                if (episodesList.isEmpty()) {
                    if (!noEpisodesFoundText.isNullOrBlank()) {
                        Text(
                            text = noEpisodesFoundText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                } else if (episodeLayoutStyle == EpisodeLayoutStyle.HORIZONTAL) {
                    val epKey = remember(episodesList) { episodesList.firstOrNull()?.id?.toString() ?: "" }
                    val epListState = rememberSaveable(
                        epKey,
                        saver = LazyListState.Saver,
                    ) {
                        LazyListState()
                    }
                    LazyRow(
                        state = epListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = episodesList,
                            key = { ep -> "ep_horiz_${ep.id}_${ep.episode}_${ep.index}" },
                        ) { episode ->
                            EpisodeHorizontalCard(
                                episode = episode,
                                fallbackBackdropUrl = fallbackBackdropUrl,
                                hasDownloadSupport = hasDownloadSupport,
                                onClick = { onEpisodeClick(episode) },
                                onLongClick = { onEpisodeLongClick(episode) },
                                onDownloadClick = { onEpisodeDownloadClick(episode) },
                                onDownloadLongClick = { onEpisodeDownloadLongClick(episode) },
                                onToggleWatched = { onEpisodeToggleWatched(episode) },
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        episodesList.forEachIndexed { index, episode ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            ) {
                                EpisodeListCard(
                                    episode = episode,
                                    fallbackBackdropUrl = fallbackBackdropUrl,
                                    hasDownloadSupport = hasDownloadSupport,
                                    onClick = { onEpisodeClick(episode) },
                                    onLongClick = { onEpisodeLongClick(episode) },
                                    onDownloadClick = { onEpisodeDownloadClick(episode) },
                                    onDownloadLongClick = { onEpisodeDownloadLongClick(episode) },
                                    onToggleWatched = { onEpisodeToggleWatched(episode) },
                                )
                            }
                            if (index < episodesList.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                )
                            }
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun SeasonGlassPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    elevatedGlassContrast: Boolean = false,
) {
    val shape = RoundedCornerShape(
        if (elevatedGlassContrast) LiquidGlassTokens.Radius.md else 10.dp,
    )
    Surface(
        shape = shape,
        color = when {
            selected -> Color.White.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        border = when {
            selected -> BorderStroke(
                width = LiquidGlassTokens.Border.thin,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (elevatedGlassContrast) 0.36f else 0.34f),
                        Color.White.copy(alpha = if (elevatedGlassContrast) 0.10f else 0.08f),
                    ),
                ),
            )
            else -> null
        },
        modifier = Modifier
            .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s28 else 32.dp)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s12 else 14.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = if (elevatedGlassContrast) {
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                } else {
                    MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                color = when {
                    selected -> MaterialTheme.liquidGlass.colors.textPrimary
                    elevatedGlassContrast -> MaterialTheme.liquidGlass.colors.textMuted
                    else -> Color(0xFF9898A0)
                },
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeHorizontalCard(
    episode: ResultEpisode,
    fallbackBackdropUrl: String?,
    hasDownloadSupport: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadLongClick: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)
    val thumbShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val imageUrl = episode.poster?.takeIf { it.isNotBlank() } ?: fallbackBackdropUrl
    val isWatched = episode.videoWatchState == VideoWatchState.Watched
    val watchProgress = remember(episode.position, episode.duration) {
        if (episode.duration > 0L) episode.getWatchProgress().coerceIn(0f, 1f) else 0f
    }
    val ratingText = remember(episode.score) {
        episode.score?.toStringNull(0.1, 10, 1, false)
    }
    val formattedDate = remember(episode.airDate) {
        episode.airDate?.takeIf { it > 0L }?.let { millis ->
            runCatching {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
            }.getOrNull()
        }
    }
    val episodeTitle = remember(episode.name, episode.episode) {
        episode.name?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episode}"
    }
    var expandSynopsis by remember(episode.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(248.dp)
            .cardDepth(shape = cardShape, surface = CardDepthSurface.CARD)
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .animateContentSize(),
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(thumbShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episodeTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.78f),
                            ),
                        ),
                    ),
            )

            // Top-left Episode Number + Filler badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Text(
                        text = if (episode.season != null && episode.season > 0) {
                            "S${episode.season} · E${episode.episode}"
                        } else {
                            "EP ${episode.episode}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }

                if (episode.isFiller == true) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    ) {
                        Text(
                            text = "FILLER",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // Top-right Watched toggle + Options
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isWatched) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Black.copy(alpha = 0.62f)
                    },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleWatched),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark Watched",
                            tint = if (isWatched) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onLongClick),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_more_vert_24),
                            contentDescription = "Episode Options",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            // Center Play Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (watchProgress > 0f) {
                PosterProgressBar(
                    progress = watchProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        // Bottom Metadata & Description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = episodeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (hasDownloadSupport) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onDownloadClick,
                                onLongClick = onDownloadLongClick,
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Episode",
                                tint = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            // Rating · Runtime · AirDate row
            val metaTokens = buildList {
                episode.runTime?.takeIf { it > 0 }?.let { add("${it}m") }
                formattedDate?.let(::add)
            }
            if (!ratingText.isNullOrBlank() || metaTokens.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!ratingText.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF5C518),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = ratingText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF5C518),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (metaTokens.isNotEmpty()) {
                        Text(
                            text = metaTokens.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!episode.description.isNullOrBlank()) {
                Text(
                    text = episode.description.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expandSynopsis) 8 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expandSynopsis = !expandSynopsis },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeListCard(
    episode: ResultEpisode,
    fallbackBackdropUrl: String?,
    hasDownloadSupport: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadLongClick: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    playingBadgeText: String? = null,
    elevatedGlassContrast: Boolean = false,
) {
    val thumbShape = RoundedCornerShape(
        if (elevatedGlassContrast) LiquidGlassTokens.Radius.md else 10.dp,
    )
    val imageUrl = episode.poster?.takeIf { it.isNotBlank() } ?: fallbackBackdropUrl
    val isWatched = episode.videoWatchState == VideoWatchState.Watched
    val watchProgress = remember(episode.position, episode.duration) {
        if (episode.duration > 0L) episode.getWatchProgress().coerceIn(0f, 1f) else 0f
    }
    val ratingText = remember(episode.score) {
        episode.score?.toStringNull(0.1, 10, 1, false)
    }
    val episodeTitle = remember(episode.name, episode.episode) {
        episode.name?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episode}"
    }
    var expandSynopsis by remember(episode.id) { mutableStateOf(false) }

    val accentColor = MaterialTheme.colorScheme.primary
    val titleColor = when {
        elevatedGlassContrast -> MaterialTheme.liquidGlass.colors.textPrimary
        else -> Color(0xFFF5F5F7)
    }
    val metaColor = when {
        elevatedGlassContrast -> MaterialTheme.liquidGlass.colors.textMuted.copy(alpha = 0.85f)
        else -> Color(0xFF8E8E93)
    }
    val synopsisColor = when {
        elevatedGlassContrast -> MaterialTheme.liquidGlass.colors.textMuted.copy(alpha = 0.90f)
        else -> Color(0xFF9898A0)
    }
    val unwatchedTint = if (elevatedGlassContrast) {
        MaterialTheme.liquidGlass.colors.textMuted
    } else {
        Color(0xFFA0A0A8)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(
            if (elevatedGlassContrast) LiquidGlassTokens.Space.s4 else 6.dp,
        ),
    ) {
        // Row 1: Thumbnail + Title/Meta + Minimal Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail (Left): compact 16:9 inside glass drawer, 124.dp x 70.dp on details
            Box(
                modifier = Modifier
                    .width(if (elevatedGlassContrast) LiquidGlassTokens.Space.s96 else 124.dp)
                    .height(if (elevatedGlassContrast) LiquidGlassTokens.Space.s56 else 70.dp)
                    .clip(thumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = episodeTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = if (isPlaying && !elevatedGlassContrast) {
                                LiquidGlassTokens.Border.thin
                            } else {
                                LiquidGlassTokens.Border.hairline
                            },
                            color = if (isPlaying && !elevatedGlassContrast) {
                                accentColor.copy(alpha = 0.72f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            },
                            shape = thumbShape,
                        ),
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.46f),
                    border = BorderStroke(LiquidGlassTokens.Border.thin, Color.White.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (elevatedGlassContrast) LiquidGlassTokens.Space.s24 else 28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.liquidGlass.colors.textPrimary,
                            modifier = Modifier.size(
                                if (elevatedGlassContrast) LiquidGlassTokens.Icon.xs else 15.dp,
                            ),
                        )
                    }
                }
                if (watchProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(
                                if (elevatedGlassContrast) LiquidGlassTokens.Space.s2 else 3.dp,
                            )
                            .background(Color.White.copy(alpha = if (elevatedGlassContrast) 0.22f else 0.28f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(watchProgress.coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .background(
                                    if (elevatedGlassContrast) {
                                        MaterialTheme.liquidGlass.colors.textPrimary.copy(alpha = 0.86f)
                                    } else {
                                        accentColor
                                    },
                                ),
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(
                    if (elevatedGlassContrast) LiquidGlassTokens.Space.s10 else 14.dp,
                ),
            )

            // Title & Compact Meta
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    if (elevatedGlassContrast) LiquidGlassTokens.Space.s3 else 4.dp,
                ),
            ) {
                Text(
                    text = "${episode.episode}. $episodeTitle",
                    style = if (elevatedGlassContrast) {
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val runtimeText = episode.runTime?.takeIf { it > 0 }?.let { "${it}m" }
                val cleanRating = ratingText?.takeIf { it.isNotBlank() }
                if (isPlaying || runtimeText != null || cleanRating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp,
                        ),
                    ) {
                        if (isPlaying) {
                            Surface(
                                shape = RoundedCornerShape(LiquidGlassTokens.Radius.full),
                                color = if (elevatedGlassContrast) {
                                    Color.White.copy(alpha = 0.10f)
                                } else {
                                    Color.White.copy(alpha = 0.16f)
                                },
                                border = BorderStroke(
                                    width = LiquidGlassTokens.Border.thin,
                                    brush = Brush.linearGradient(
                                        if (elevatedGlassContrast) {
                                            listOf(
                                                Color.White.copy(alpha = 0.24f),
                                                Color.White.copy(alpha = 0.07f),
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.34f),
                                                Color.White.copy(alpha = 0.08f),
                                            )
                                        },
                                    ),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = if (elevatedGlassContrast) LiquidGlassTokens.Space.s7 else 8.dp,
                                        vertical = LiquidGlassTokens.Space.s2,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        if (elevatedGlassContrast) LiquidGlassTokens.Space.s5 else 5.dp,
                                    ),
                                ) {
                                    if (elevatedGlassContrast) {
                                        Box(
                                            modifier = Modifier
                                                .size(LiquidGlassTokens.Space.s5)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.liquidGlass.colors.textPrimary.copy(alpha = 0.90f),
                                                ),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                        )
                                    }
                                    Text(
                                        text = playingBadgeText ?: stringResource(R.string.player_episode_playing),
                                        style = if (elevatedGlassContrast) {
                                            MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                            )
                                        } else {
                                            MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        color = MaterialTheme.liquidGlass.colors.textPrimary.copy(alpha = 0.92f),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        if (runtimeText != null || cleanRating != null) {
                            val compactMeta = buildAnnotatedString {
                                var needsSeparator = false
                                if (runtimeText != null) {
                                    append(runtimeText)
                                    needsSeparator = true
                                }
                                if (cleanRating != null) {
                                    if (needsSeparator) {
                                        append("  •  ")
                                    }
                                    withStyle(SpanStyle(color = Color(0xFFF5C518))) {
                                        append("★")
                                    }
                                    append(" ")
                                    append(cleanRating)
                                }
                            }
                            Text(
                                text = compactMeta,
                                style = if (elevatedGlassContrast) {
                                    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal)
                                } else {
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                    )
                                },
                                color = metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(
                    if (elevatedGlassContrast) LiquidGlassTokens.Space.s6 else 8.dp,
                ),
            )

            // Minimal Action Icons (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (elevatedGlassContrast) LiquidGlassTokens.Space.s32 else 36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleWatched),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Watched",
                        tint = when {
                            isPlaying && elevatedGlassContrast ->
                                MaterialTheme.liquidGlass.colors.textPrimary.copy(alpha = 0.90f)
                            isWatched -> accentColor
                            else -> unwatchedTint
                        },
                        modifier = Modifier.size(
                            if (elevatedGlassContrast) LiquidGlassTokens.Icon.sm else 20.dp,
                        ),
                    )
                }

                if (hasDownloadSupport) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onDownloadClick,
                                onLongClick = onDownloadLongClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFFA0A0A8),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Row 2: Breathable Episode Synopsis
        if (!episode.description.isNullOrBlank()) {
            Text(
                text = episode.description.trim(),
                style = if (elevatedGlassContrast) {
                    MaterialTheme.liquidGlassTypeScale.labelSm
                } else {
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                color = synopsisColor,
                maxLines = if (expandSynopsis) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expandSynopsis = !expandSynopsis },
            )
        }
    }
}

