package com.lagradost.cloudstream3.ui.details

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size as CoilSize
import coil3.transform.Transformation
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.home.rememberHeroStretchState
import com.lagradost.cloudstream3.ui.liquidglass.extractPosterGradientColor
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.ExtractedTrailerData
import com.lagradost.cloudstream3.ui.result.ResultData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.ResumeWatchingStatus
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Random

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
    onRecommendationLongClick: ((SearchResponse) -> Unit)? = null,
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
    val heroLogoThresholdPx = with(density) { (DetailHeroBaseHeight - 102.dp).toPx() }

    val scrollOffsetPx by remember(listState, heroHeightPx) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> heroHeightPx
                else -> listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }
    val isScrolledPastHeroLogo by remember(listState, scrollOffsetPx, heroLogoThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || scrollOffsetPx >= heroLogoThresholdPx
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
    var episodeLayoutStyle by rememberSaveable { mutableStateOf(EpisodeLayoutStyle.LIST) }
    var selectedInfoTab by rememberSaveable(currentContentId) { mutableStateOf(DetailInfoTab.DETAILS) }

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

    val posterSourceUrl = resultData?.posterImage?.takeIf { it.isNotBlank() }
    var extractedPosterColor by remember(posterSourceUrl) { mutableStateOf<Color?>(null) }
    LaunchedEffect(posterSourceUrl) {
        extractedPosterColor = if (posterSourceUrl == null) {
            null
        } else {
            extractPosterGradientColor(context, posterSourceUrl)
        }
    }

    var filmGrainBitmap by remember { mutableStateOf(cachedFilmGrainBitmap) }
    LaunchedEffect(Unit) {
        if (filmGrainBitmap == null) {
            val generated = withContext(Dispatchers.Default) {
                generateFilmGrainNoiseBitmap()
            }
            cachedFilmGrainBitmap = generated
            filmGrainBitmap = generated
        }
    }
    val filmGrainBrush = remember(filmGrainBitmap) {
        filmGrainBitmap?.let { bitmap ->
            ShaderBrush(
                ImageShader(
                    image = bitmap,
                    tileModeX = TileMode.Repeated,
                    tileModeY = TileMode.Repeated,
                ),
            )
        }
    }

    val fallbackAccent = MaterialTheme.colorScheme.primary
    val targetPalette = remember(extractedPosterColor, fallbackAccent) {
        computePosterDuotonePalette(extractedPosterColor, fallbackAccent)
    }
    val topTint by animateColorAsState(
        targetValue = targetPalette.topTint,
        animationSpec = tween(durationMillis = 480),
        label = "detailsTopTint",
    )
    val bottomTint by animateColorAsState(
        targetValue = targetPalette.bottomTint,
        animationSpec = tween(durationMillis = 480),
        label = "detailsBottomTint",
    )

    val blurredPosterModel = remember(posterSourceUrl, context) {
        posterSourceUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        size(240)
                    } else {
                        size(120)
                        transformations(StaticPosterBlurTransformation())
                    }
                }
                .build()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bottomTint)
            .nestedScroll(heroStretch.nestedScrollConnection),
    ) {
        if (blurredPosterModel != null) {
            AsyncImage(
                model = blurredPosterModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let { baseModifier ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            baseModifier.blur(
                                radius = 90.dp,
                                edgeTreatment = BlurredEdgeTreatment.Rectangle,
                            )
                        } else {
                            baseModifier
                        }
                    },
            )
        }

        // Poster-Tinted Atmospheric Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                topTint.copy(alpha = 0.72f),
                                bottomTint.copy(alpha = 0.84f),
                            ),
                        ),
                    )
                },
        )

        // Subtle OLED Film-Grain Overlay (3% opacity)
        if (filmGrainBrush != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = filmGrainBrush,
                            alpha = 0.03f,
                        )
                    },
            )
        }
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
                                val epBadge = if (s != null && s > 0) "S$s E$e" else "E$e"
                                "$resumeString $epBadge"
                            }
                        }
                        !moviePlayText.isNullOrBlank() -> moviePlayText
                        else -> playEpisodeString
                    }
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
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                    ),
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

                    // 2. ACTION BUTTONS
                    if (!data.comingSoon) {
                        item(key = "detail_action_buttons") {
                            DetailActionButtons(
                                primaryPlayLabel = primaryPlayLabel,
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

                    // 3. EXPANDABLE SYNOPSIS
                    if (!plotStr.isNullOrBlank()) {
                        item(key = "detail_synopsis") {
                            ExpandableDescription(
                                description = plotStr,
                            )
                        }
                    }

                    // 4. DETAILS / CAST & CREW TAB SWITCHER
                    val actorsList = data.actors.orEmpty()
                    item(key = "detail_info_tabs") {
                        DetailInfoTabsSection(
                            selectedTab = selectedInfoTab,
                            onSelectTab = { selectedInfoTab = it },
                            typeText = typeStr,
                            yearText = yearStr,
                            durationText = durationStr,
                            statusText = statusStr,
                            contentRatingText = contentRatingStr,
                            ratingText = ratingStr,
                            apiNameText = apiNameStr,
                            nextAiringEpisodeText = nextAiringEpStr,
                            nextAiringDateText = nextAiringDateStr,
                            vpnText = vpnStr,
                            metaText = metaStr,
                            tags = data.tags,
                            trailers = trailerMirrors,
                            fallbackBackdropUrl = fallbackBackdrop,
                            onTrailerClick = onTrailerClick,
                            actors = actorsList,
                            showCastSetting = showCastSetting,
                            onActorClick = onActorClick,
                        )
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
                                    itemsIndexed(
                                        items = episodesList,
                                        key = { _, ep -> "ep_list_${ep.id}_${ep.episode}_${ep.index}" },
                                    ) { index, episode ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp),
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
                                            if (index < episodesList.lastIndex) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 14.dp)
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

                    // 6. MORE LIKE THIS (RECOMMENDATIONS) - at the absolute bottom
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
            isScrolledPastHeroLogo = isScrolledPastHeroLogo,
            showTrailerMuteButton = currentBackgroundTrailer != null && isTrailerAutoPlaying && isTrailerReady,
            isTrailerMuted = isTrailerMuted,
            showSyncButton = showSyncButton,
            onBackClick = onBackClick,
            onToggleTrailerMute = { isTrailerMuted = !isTrailerMuted },
            onSyncClick = onSyncClick,
            onShareClick = {
                resultData?.let(onShareClick)
            },
            logoUrl = resultData?.logoUrl,
            stickyBackgroundColor = topTint,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private data class DetailsDuotonePalette(
    val topTint: Color,
    val bottomTint: Color,
)

private fun computePosterDuotonePalette(
    extracted: Color?,
    fallbackAccent: Color,
): DetailsDuotonePalette {
    val sourceColor = extracted ?: fallbackAccent
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(sourceColor.toArgb(), hsv)
    var hue = hsv[0]
    val saturation = if (hsv[1] >= 0.08f) {
        if (hue in 48f..75f) {
            hue = 36f + (hue - 48f) * 0.25f
        }
        hsv[1].coerceIn(0.50f, 0.70f)
    } else {
        hsv[1]
    }
    return DetailsDuotonePalette(
        topTint = Color.hsv(hue = hue, saturation = saturation, value = 0.24f),
        bottomTint = Color.hsv(hue = hue, saturation = saturation, value = 0.12f),
    )
}

@Volatile
private var cachedFilmGrainBitmap: ImageBitmap? = null

private fun generateFilmGrainNoiseBitmap(): ImageBitmap {
    val size = 128
    val pixels = IntArray(size * size)
    val random = Random(0x5EED128L)
    for (i in pixels.indices) {
        val luma = random.nextInt(256)
        val alpha = 120 + random.nextInt(136)
        pixels[i] = (alpha shl 24) or (luma shl 16) or (luma shl 8) or luma
    }
    return createBitmap(size, size).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }.asImageBitmap()
}

private const val POSTER_BLUR_TARGET_WIDTH = 96

private class StaticPosterBlurTransformation(
    private val radius: Int = 14,
) : Transformation() {
    override val cacheKey: String = "static_poster_blur_$radius"

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap =
        withContext(Dispatchers.Default) {
            val targetH = ((input.height.toFloat() / input.width.coerceAtLeast(1)) * POSTER_BLUR_TARGET_WIDTH)
                .toInt()
                .coerceIn(72, 160)
            val scaled = input.scale(POSTER_BLUR_TARGET_WIDTH, targetH)
            val pixelCount = POSTER_BLUR_TARGET_WIDTH * targetH
            val pixels = IntArray(pixelCount)
            scaled.getPixels(pixels, 0, POSTER_BLUR_TARGET_WIDTH, 0, 0, POSTER_BLUR_TARGET_WIDTH, targetH)
            if (scaled !== input) {
                scaled.recycle()
            }

            val temp = IntArray(pixelCount)
            repeat(2) {
                boxBlurPass(src = pixels, dst = temp, height = targetH, radius = radius, horizontal = true)
                boxBlurPass(src = temp, dst = pixels, height = targetH, radius = radius, horizontal = false)
            }

            createBitmap(POSTER_BLUR_TARGET_WIDTH, targetH).apply {
                setPixels(pixels, 0, POSTER_BLUR_TARGET_WIDTH, 0, 0, POSTER_BLUR_TARGET_WIDTH, targetH)
            }
        }
}

private fun boxBlurPass(
    src: IntArray,
    dst: IntArray,
    height: Int,
    radius: Int,
    horizontal: Boolean,
) {
    val windowSize = radius * 2 + 1
    if (horizontal) {
        for (y in 0 until height) {
            val rowOffset = y * POSTER_BLUR_TARGET_WIDTH
            for (x in 0 until POSTER_BLUR_TARGET_WIDTH) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                for (dx in -radius..radius) {
                    val sx = (x + dx).coerceIn(0, POSTER_BLUR_TARGET_WIDTH - 1)
                    val c = src[rowOffset + sx]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                }
                dst[rowOffset + x] = (0xFF shl 24) or
                    ((rSum / windowSize) shl 16) or
                    ((gSum / windowSize) shl 8) or
                    (bSum / windowSize)
            }
        }
    } else {
        for (y in 0 until height) {
            for (x in 0 until POSTER_BLUR_TARGET_WIDTH) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                for (dy in -radius..radius) {
                    val sy = (y + dy).coerceIn(0, height - 1)
                    val c = src[sy * POSTER_BLUR_TARGET_WIDTH + x]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                }
                dst[y * POSTER_BLUR_TARGET_WIDTH + x] = (0xFF shl 24) or
                    ((rSum / windowSize) shl 16) or
                    ((gSum / windowSize) shl 8) or
                    (bSum / windowSize)
            }
        }
    }
}
