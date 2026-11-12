package com.lagradost.cloudstream3.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.home.rememberHeroStretchState
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.ExtractedTrailerData
import com.lagradost.cloudstream3.ui.result.ResultData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.ResumeWatchingStatus
import com.lagradost.cloudstream3.utils.AppContextUtils.getNameFull
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.UiText
import kotlinx.coroutines.delay

@Composable
fun DetailsScreen(
    pageResource: Resource<ResultData>?,
    episodesResource: Resource<List<ResultEpisode>>?,
    movieResource: Resource<Pair<UiText, ResultEpisode>>?,
    episodesCountText: UiText?,
    resumeWatching: ResumeWatchingStatus?,
    trailers: List<ExtractedTrailerData>,
    trailersEnabled: Boolean,
    showCastSetting: Boolean,
    watchStatus: WatchType,
    isFavorite: Boolean?,
    isSubscribed: Boolean?,
    hasDownloadSupport: Boolean,
    showSyncButton: Boolean,
    seasonSelections: List<Pair<UiText?, Int>>,
    selectedSeason: UiText?,
    dubSubSelections: List<Pair<UiText?, DubStatus>>,
    selectedDubStatus: UiText?,
    rangeSelections: List<Pair<UiText?, EpisodeRange>>,
    selectedRange: UiText?,
    sortSelectionsCount: Int,
    selectedSorting: UiText?,
    recommendations: List<SearchResponse>,
    onBackClick: () -> Unit,
    onTitleLongClick: (String) -> Unit,
    onSyncClick: () -> Unit,
    onQuickSearchClick: (String) -> Unit = {},
    onShareClick: (ResultData) -> Unit,
    onOpenInBrowserClick: () -> Unit = {},
    onReloadClick: () -> Unit,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    onMovieDownloadClick: (ResultEpisode) -> Unit,
    onMovieDownloadLongClick: (ResultEpisode) -> Unit,
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
    onActorClick: (ActorData) -> Unit,
    onRecommendationClick: (SearchResponse) -> Unit,
    onRecommendationLongClick: (SearchResponse) -> Unit,
    onTrailerClick: (TrailerMirror) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentContentId = (pageResource as? Resource.Success)?.value?.let {
        it.url.ifBlank { it.title }
    } ?: ""
    val listState = rememberSaveable(
        currentContentId,
        saver = androidx.compose.foundation.lazy.LazyListState.Saver,
    ) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val heroStretch = rememberHeroStretchState()
    val density = LocalDensity.current
    val heroHeightPx = with(density) { DetailHeroBaseHeight.toPx() }

    val scrollOffsetPx by remember(listState, heroHeightPx) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> heroHeightPx
                else -> listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }
    val collapseProgress by remember(scrollOffsetPx, heroHeightPx) {
        derivedStateOf {
            (scrollOffsetPx / (heroHeightPx * 0.72f)).coerceIn(0f, 1f)
        }
    }
    val isHeroVisible by remember(listState, scrollOffsetPx, heroHeightPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && scrollOffsetPx < heroHeightPx * 0.82f
        }
    }

    val resultData = (pageResource as? Resource.Success)?.value
    val fallbackBackdrop = resultData?.posterBackgroundImage?.takeIf { it.isNotBlank() }
        ?: resultData?.backgroundPosterUrl?.takeIf { it.isNotBlank() }
        ?: resultData?.posterImage

    val trailerMirrors: List<TrailerMirror> = remember(trailers, fallbackBackdrop) {
        trailers
            .flatMap { it.mirros }
            .mapIndexed { index, (extractorLink, originalUrl) ->
                TrailerMirror(
                    link = extractorLink,
                    originalUrl = originalUrl,
                    title = extractorLink.name.ifBlank { "Trailer ${index + 1}" },
                    thumbnailUrl = fallbackBackdrop,
                )
            }
    }

    var activeTrailerIndex by remember(trailerMirrors) { mutableIntStateOf(0) }
    var isTrailerAutoPlaying by remember(trailerMirrors) { mutableStateOf(false) }
    var isTrailerReady by remember(trailerMirrors, activeTrailerIndex) { mutableStateOf(false) }
    var isTrailerMuted by rememberSaveable { mutableStateOf(true) }
    var hasTrailerFinishedOrFailedAll by remember(trailerMirrors) { mutableStateOf(false) }
    var episodeLayoutStyle by rememberSaveable { mutableStateOf(EpisodeLayoutStyle.HORIZONTAL) }

    val currentBackgroundTrailer = trailerMirrors.getOrNull(activeTrailerIndex)

    // Auto-start background hero trailer after 900ms when hero is visible and trailers are enabled
    LaunchedEffect(
        trailersEnabled,
        currentBackgroundTrailer?.link?.url,
        isHeroVisible,
        hasTrailerFinishedOrFailedAll,
    ) {
        if (
            !trailersEnabled ||
            currentBackgroundTrailer == null ||
            !isHeroVisible ||
            hasTrailerFinishedOrFailedAll
        ) {
            isTrailerAutoPlaying = false
            return@LaunchedEffect
        }
        delay(900L)
        isTrailerAutoPlaying = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(heroStretch.nestedScrollConnection),
    ) {
        when (pageResource) {
            is Resource.Loading, null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is Resource.Failure -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = pageResource.errorString.ifBlank { stringResource(R.string.reload_error) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onReloadClick),
                        ) {
                            Text(
                                text = stringResource(R.string.reload_error),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onOpenInBrowserClick),
                        ) {
                            Text(
                                text = stringResource(R.string.result_open_in_browser),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            is Resource.Success -> {
                val data = pageResource.value
                val titleStr = data.titleText.asStringNull(context)?.takeIf { it.isNotBlank() } ?: data.title
                val yearStr = data.yearText?.asStringNull(context)
                val durationStr = data.durationText?.asStringNull(context)
                val contentRatingStr = data.contentRatingText?.asStringNull(context)
                val ratingStr = data.ratingText?.asStringNull(context)
                val statusStr = data.onGoingText?.asStringNull(context)
                val typeStr = data.typeText.asStringNull(context)
                val apiNameStr = data.apiName.asStringNull(context)
                val nextAiringEpStr = data.nextAiringEpisode?.asStringNull(context)
                val nextAiringDateStr = data.nextAiringDate?.asStringNull(context)
                val vpnStr = data.vpnText?.asStringNull(context)
                val metaStr = data.metaText?.asStringNull(context)
                val plotStr = data.plotText.asStringNull(context)?.html()?.toString()
                val actorsStr = data.actorsText?.asStringNull(context)
                val noEpisodesStr = data.noEpisodesFoundText?.asStringNull(context)
                val episodesCountStr = episodesCountText?.asStringNull(context)

                val moviePair = (movieResource as? Resource.Success)?.value
                val movieEpisode = moviePair?.second
                val moviePlayText = moviePair?.first?.asStringNull(context)

                val resumeString = stringResource(R.string.resume)
                val playEpisodeString = stringResource(R.string.play_episode)
                val primaryPlayLabel = remember(resumeWatching, moviePlayText, resumeString, playEpisodeString) {
                    when {
                        resumeWatching != null -> {
                            if (resumeWatching.isMovie) {
                                resumeString
                            } else {
                                val s = resumeWatching.result.season
                                val e = resumeWatching.result.episode
                                val epBadge = if (s != null && s > 0) "S$s:E$e" else "E$e"
                                "$resumeString $epBadge"
                            }
                        }
                        !moviePlayText.isNullOrBlank() -> moviePlayText
                        else -> playEpisodeString
                    }
                }
                val resumeSubLabel = remember(resumeWatching, context) {
                    if (resumeWatching != null && !resumeWatching.isMovie) {
                        context.getNameFull(
                            resumeWatching.result.name,
                            resumeWatching.result.episode,
                            resumeWatching.result.season,
                        )
                    } else {
                        null
                    }
                }
                val resumeProgressFraction = remember(resumeWatching) {
                    val prog = resumeWatching?.progress
                    if (prog != null && prog.maxProgress > 0) {
                        (prog.progress.toFloat() / prog.maxProgress.toFloat()).coerceIn(0f, 1f)
                    } else {
                        null
                    }
                }
                val resumeTimeLeftLabel = remember(resumeWatching, context) {
                    resumeWatching?.progress?.progressLeft?.asStringNull(context)
                }

                val mappedSeasonOptions = remember(seasonSelections, context) {
                    seasonSelections.mapNotNull { (uiText, seasonNum) ->
                        val label = uiText?.asStringNull(context) ?: return@mapNotNull null
                        SeasonOption(label = label, seasonNumber = seasonNum)
                    }
                }
                val selectedSeasonStr = selectedSeason?.asStringNull(context)

                val mappedDubOptions = remember(dubSubSelections, context) {
                    dubSubSelections.mapNotNull { (uiText, status) ->
                        val label = uiText?.asStringNull(context) ?: return@mapNotNull null
                        DubOption(label = label, status = status)
                    }
                }
                val selectedDubStr = selectedDubStatus?.asStringNull(context)

                val mappedRangeOptions = remember(rangeSelections, context) {
                    rangeSelections.mapNotNull { (uiText, range) ->
                        val label = uiText?.asStringNull(context) ?: return@mapNotNull null
                        RangeOption(label = label, range = range)
                    }
                }
                val selectedRangeStr = selectedRange?.asStringNull(context)
                val selectedSortStr = selectedSorting?.asStringNull(context)

                val episodesList = (episodesResource as? Resource.Success)?.value.orEmpty()
                val showSeriesContent = movieEpisode == null && (
                    episodesResource is Resource.Loading ||
                        episodesList.isNotEmpty() ||
                        mappedSeasonOptions.isNotEmpty() ||
                        mappedDubOptions.isNotEmpty() ||
                        !noEpisodesStr.isNullOrBlank()
                    )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. HERO SECTION (with Background Trailer Surface)
                    item(key = "detail_hero") {
                        DetailHero(
                            title = titleStr,
                            backdropUrl = fallbackBackdrop,
                            posterUrl = data.posterImage,
                            logoUrl = data.logoUrl,
                            trailerMirror = currentBackgroundTrailer,
                            isTrailerPlaying = isTrailerAutoPlaying,
                            isTrailerReady = isTrailerReady,
                            isTrailerMuted = isTrailerMuted,
                            stretchPx = { heroStretch.stretchPx },
                            scrollOffsetPx = scrollOffsetPx,
                            onTrailerReady = {
                                isTrailerReady = true
                            },
                            onTrailerEnded = {
                                isTrailerReady = false
                                isTrailerAutoPlaying = false
                                hasTrailerFinishedOrFailedAll = true
                            },
                            onTrailerError = {
                                isTrailerReady = false
                                if (activeTrailerIndex + 1 < trailerMirrors.size) {
                                    activeTrailerIndex++
                                } else {
                                    isTrailerAutoPlaying = false
                                    hasTrailerFinishedOrFailedAll = true
                                }
                            },
                            onToggleTrailerMute = {
                                isTrailerMuted = !isTrailerMuted
                            },
                            onTitleLongClick = {
                                onTitleLongClick(titleStr)
                            },
                        )
                    }

                    // 2. METADATA CHIPS & TAGS
                    item(key = "detail_meta_info") {
                        DetailMetaInfo(
                            yearText = yearStr,
                            durationText = durationStr,
                            contentRatingText = contentRatingStr,
                            ratingText = ratingStr,
                            statusText = statusStr,
                            typeText = typeStr,
                            apiNameText = apiNameStr,
                            nextAiringEpisodeText = nextAiringEpStr,
                            nextAiringDateText = nextAiringDateStr,
                            vpnText = vpnStr,
                            metaText = metaStr,
                            tags = data.tags,
                        )
                    }

                    // 3. ACTION BUTTONS
                    if (!data.comingSoon) {
                        item(key = "detail_action_buttons") {
                            DetailActionButtons(
                                primaryPlayLabel = primaryPlayLabel,
                                resumeSubLabel = resumeSubLabel,
                                resumeProgressFraction = resumeProgressFraction,
                                resumeTimeLeftLabel = resumeTimeLeftLabel,
                                watchType = watchStatus,
                                isFavorite = isFavorite,
                                isSubscribed = isSubscribed,
                                showDownloadButton = hasDownloadSupport && (movieEpisode != null || episodesList.isNotEmpty()),
                                downloadLabel = stringResource(R.string.download),
                                hasTrailers = trailerMirrors.isNotEmpty(),
                                onPrimaryPlayClick = onPrimaryPlayClick,
                                onPrimaryPlayLongClick = onPrimaryPlayLongClick,
                                onBookmarkClick = onBookmarkClick,
                                onFavoriteClick = onFavoriteClick,
                                onSubscribeClick = onSubscribeClick,
                                onDownloadClick = {
                                    if (movieEpisode != null) {
                                        onMovieDownloadClick(movieEpisode)
                                    } else if (episodesList.isNotEmpty()) {
                                        onBatchDownloadClick(episodesList)
                                    }
                                },
                                onDownloadLongClick = {
                                    if (movieEpisode != null) {
                                        onMovieDownloadLongClick(movieEpisode)
                                    } else {
                                        onBatchDownloadLongClick()
                                    }
                                },
                                onTrailerClick = {
                                    val targetTrailer = currentBackgroundTrailer ?: trailerMirrors.firstOrNull()
                                    if (targetTrailer != null) {
                                        onTrailerClick(targetTrailer)
                                    }
                                },
                            )
                        }
                    } else {
                        item(key = "detail_coming_soon") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                ) {
                                    Text(
                                        text = stringResource(R.string.coming_soon),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 4. EXPANDABLE SYNOPSIS
                    if (!plotStr.isNullOrBlank()) {
                        item(key = "detail_synopsis") {
                            ExpandableDescription(
                                description = plotStr,
                            )
                        }
                    }

                    // 5. CAST & CREW CAROUSEL (Required to be before Episodes)
                    val actorsList = data.actors.orEmpty()
                    if (showCastSetting && actorsList.isNotEmpty()) {
                        item(key = "detail_cast_section") {
                            DetailCastSection(
                                actors = actorsList,
                                onActorClick = onActorClick,
                            )
                        }
                    }

                    // 6. SEASONS & EPISODES (Series / Anime - Virtualized)
                    if (!data.comingSoon && showSeriesContent) {
                        item(key = "detail_episodes_header") {
                            DetailEpisodesHeader(
                                seasonOptions = mappedSeasonOptions,
                                selectedSeasonLabel = selectedSeasonStr,
                                dubOptions = mappedDubOptions,
                                selectedDubLabel = selectedDubStr,
                                rangeOptions = mappedRangeOptions,
                                selectedRangeLabel = selectedRangeStr,
                                selectedSortLabel = selectedSortStr,
                                hasSortOptions = sortSelectionsCount > 1,
                                hasDownloadSupport = hasDownloadSupport,
                                episodesCountText = episodesCountStr,
                                episodesList = episodesList,
                                episodeLayoutStyle = episodeLayoutStyle,
                                onToggleEpisodeLayoutStyle = {
                                    episodeLayoutStyle =
                                        if (episodeLayoutStyle == EpisodeLayoutStyle.HORIZONTAL) {
                                            EpisodeLayoutStyle.LIST
                                        } else {
                                            EpisodeLayoutStyle.HORIZONTAL
                                        }
                                },
                                onSeasonSelected = onSeasonSelected,
                                onDubSelected = onDubSelected,
                                onRangeSelected = onRangeSelected,
                                onSortClick = onSortClick,
                                onBatchDownloadClick = onBatchDownloadClick,
                                onBatchDownloadLongClick = onBatchDownloadLongClick,
                            )
                        }

                        when (episodesResource) {
                            is Resource.Loading -> {
                                item(key = "detail_episodes_loading") {
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
                            }

                            is Resource.Success -> {
                                if (episodesList.isEmpty()) {
                                    if (!noEpisodesStr.isNullOrBlank()) {
                                        item(key = "detail_episodes_empty") {
                                            Text(
                                                text = noEpisodesStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            )
                                        }
                                    }
                                } else if (episodeLayoutStyle == EpisodeLayoutStyle.HORIZONTAL) {
                                    item(key = "detail_episodes_carousel") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            items(
                                                items = episodesList,
                                                key = { ep -> "ep_horiz_${ep.id}_${ep.episode}_${ep.index}" },
                                            ) { episode ->
                                                EpisodeHorizontalCard(
                                                    episode = episode,
                                                    fallbackBackdropUrl = fallbackBackdrop,
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
                                } else {
                                    // Flattened items in parent LazyColumn for smooth virtualization
                                    items(
                                        items = episodesList,
                                        key = { ep -> "ep_list_${ep.id}_${ep.episode}_${ep.index}" },
                                    ) { episode ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                        ) {
                                            EpisodeListCard(
                                                episode = episode,
                                                fallbackBackdropUrl = fallbackBackdrop,
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

                    // 7. TRAILERS & EXTRAS SECTION
                    if (trailerMirrors.isNotEmpty()) {
                        item(key = "detail_trailers_section") {
                            DetailTrailersSection(
                                trailers = trailerMirrors,
                                fallbackBackdropUrl = fallbackBackdrop,
                                onTrailerClick = onTrailerClick,
                            )
                        }
                    }

                    // 8. SHOW DETAILS TABLE
                    item(key = "detail_additional_info_section") {
                        DetailAdditionalInfoSection(
                            typeText = typeStr,
                            yearText = yearStr,
                            durationText = durationStr,
                            statusText = statusStr,
                            contentRatingText = contentRatingStr,
                            ratingText = ratingStr,
                            apiNameText = apiNameStr,
                            actorsText = actorsStr,
                        )
                    }

                    // 9. MORE LIKE THIS (RECOMMENDATIONS) - at the absolute bottom
                    if (recommendations.isNotEmpty()) {
                        item(key = "detail_recommendations_section") {
                            DetailPosterRailSection(
                                recommendations = recommendations,
                                onItemClick = onRecommendationClick,
                                onItemLongClick = onRecommendationLongClick,
                            )
                        }
                    }
                }
            }
        }

        // Collapsing Floating Top Header
        val headerTitle = (pageResource as? Resource.Success)?.value?.let {
            it.titleText.asStringNull(context)?.takeIf { s -> s.isNotBlank() } ?: it.title
        }.orEmpty()
        DetailFloatingHeader(
            title = headerTitle,
            collapseProgress = collapseProgress,
            showTrailerMuteButton = currentBackgroundTrailer != null && isTrailerAutoPlaying && isTrailerReady,
            isTrailerMuted = isTrailerMuted,
            showSyncButton = showSyncButton,
            onBackClick = onBackClick,
            onToggleTrailerMute = { isTrailerMuted = !isTrailerMuted },
            onSyncClick = onSyncClick,
            onShareClick = {
                resultData?.let(onShareClick)
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
