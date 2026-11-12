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
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.ui.liquidglass.CircularActionButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.home.AnimatedWatchedBadge
import com.lagradost.cloudstream3.ui.home.PosterProgressBar
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
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Season Selector Chips
        if (seasonOptions.size > 1) {
            val seasonKey = remember(seasonOptions) { seasonOptions.firstOrNull()?.seasonNumber?.toString() ?: "" }
            val seasonListState = rememberSaveable(
                seasonKey,
                saver = LazyListState.Saver,
            ) {
                LazyListState()
            }
            LazyRow(
                state = seasonListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(seasonOptions, key = { "season_${it.seasonNumber}_${it.label}" }) { option ->
                    val isSelected = option.label == selectedSeasonLabel
                    DetailFilterPill(
                        label = option.label,
                        selected = isSelected,
                        onClick = { onSeasonSelected(option.seasonNumber) },
                    )
                }
            }
        }

        // 2. Dub/Sub + Episode Range Chips
        if (dubOptions.size > 1 || rangeOptions.size > 1) {
            val filterKey = remember(dubOptions, rangeOptions) {
                "${dubOptions.firstOrNull()?.label}_${rangeOptions.firstOrNull()?.label}"
            }
            val filterListState = rememberSaveable(
                filterKey,
                saver = LazyListState.Saver,
            ) {
                LazyListState()
            }
            LazyRow(
                state = filterListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(dubOptions, key = { "dub_${it.status.name}" }) { dub ->
                    val isSelected = dub.label == selectedDubLabel
                    DetailFilterPill(
                        label = dub.label,
                        selected = isSelected,
                        isAccent = true,
                        onClick = { onDubSelected(dub.status) },
                    )
                }
                items(rangeOptions, key = { "range_${it.range.startIndex}_${it.label}" }) { rangeOpt ->
                    val isSelected = rangeOpt.label == selectedRangeLabel
                    DetailFilterPill(
                        label = rangeOpt.label,
                        selected = isSelected,
                        onClick = { onRangeSelected(rangeOpt.range) },
                    )
                }
            }
        }

        // 3. Episodes Header Row (Title + Count + Sort + Batch Download + Layout Toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.episodes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal,
                )
                if (!episodesCountText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = episodesCountText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasSortOptions) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onSortClick),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Episodes",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            if (!selectedSortLabel.isNullOrBlank()) {
                                Text(
                                    text = selectedSortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (hasDownloadSupport && episodesList.isNotEmpty()) {
                    CircularActionButton(
                        icon = Icons.Default.Download,
                        contentDescription = "Download All",
                        size = 36.dp,
                        iconSize = 18.dp,
                        onClick = { onBatchDownloadClick(episodesList) },
                        onLongClick = onBatchDownloadLongClick,
                    )
                }

                // Toggle between Horizontal Episode Carousel vs Vertical Episode List
                CircularActionButton(
                    icon = if (episodeLayoutStyle == EpisodeLayoutStyle.HORIZONTAL) {
                        Icons.AutoMirrored.Filled.ViewList
                    } else {
                        Icons.Default.ViewCarousel
                    },
                    contentDescription = "Switch Episode Layout",
                    size = 36.dp,
                    iconSize = 18.dp,
                    onClick = onToggleEpisodeLayoutStyle,
                )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        episodesList.forEach { episode ->
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
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun DetailFilterPill(
    label: String,
    selected: Boolean,
    isAccent: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = when {
            selected && isAccent -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.onBackground
            else -> Color.White.copy(alpha = 0.08f)
        },
        contentColor = when {
            selected -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.onBackground
        },
        border = if (!selected) {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        } else {
            null
        },
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
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
                            imageVector = Icons.Default.MoreVert,
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
) {
    val cardShape = RoundedCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(10.dp)
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
            .fillMaxWidth()
            .cardDepth(shape = cardShape, surface = CardDepthSurface.CARD)
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left Thumbnail
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(76.dp)
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
                        .background(Color.Black.copy(alpha = 0.28f)),
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.56f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                AnimatedWatchedBadge(
                    visible = isWatched,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
                if (watchProgress > 0f) {
                    PosterProgressBar(
                        progress = watchProgress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            // Middle Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${episode.episode}. $episodeTitle",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val metaTokens = buildList {
                    if (episode.isFiller == true) add("Filler")
                    episode.runTime?.takeIf { it > 0 }?.let { add("${it}m") }
                    formattedDate?.let(::add)
                    ratingText?.let { add("★ $it") }
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

            // Right Actions (Watched + Download)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isWatched) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleWatched),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark Watched",
                            tint = if (isWatched) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (hasDownloadSupport) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onDownloadClick,
                                onLongClick = onDownloadLongClick,
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        if (!episode.description.isNullOrBlank()) {
            Text(
                text = episode.description.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expandSynopsis) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expandSynopsis = !expandSynopsis },
            )
        }
    }
}

