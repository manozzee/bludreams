package com.lagradost.cloudstream3.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository.Companion.noneApi
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.getDisplayName
import com.lagradost.cloudstream3.ui.liquidglass.HomeHeroSection
import com.lagradost.cloudstream3.ui.liquidglass.MetaPreview
import com.lagradost.cloudstream3.ui.liquidglass.MetaPreviewAdapter
import com.lagradost.cloudstream3.ui.liquidglass.PosterShape
import com.lagradost.cloudstream3.ui.poster.rememberPosterCardStyleUiState
import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val STRETCH_DAMPING = 0.45f
private const val MAX_STRETCH_PX = 380f

@Stable
class HeroStretchState(private val scope: CoroutineScope) {
    private val _stretch = Animatable(0f)
    val stretchPx: Float get() = _stretch.value

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && _stretch.value > 0f) {
                val consumed = available.y.coerceAtLeast(-_stretch.value / STRETCH_DAMPING)
                val next = (_stretch.value + consumed * STRETCH_DAMPING).coerceAtLeast(0f)
                scope.launch { _stretch.snapTo(next) }
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (available.y > 0f && source == NestedScrollSource.UserInput) {
                val damped = available.y * STRETCH_DAMPING *
                    (1f - (_stretch.value / MAX_STRETCH_PX).coerceIn(0f, 0.85f))
                val next = (_stretch.value + damped).coerceAtMost(MAX_STRETCH_PX)
                scope.launch { _stretch.snapTo(next) }
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (_stretch.value > 0f) {
                scope.launch {
                    _stretch.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
                return available
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (_stretch.value > 0f) {
                scope.launch {
                    _stretch.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberHeroStretchState(): HeroStretchState {
    val scope = rememberCoroutineScope()
    return remember(scope) { HeroStretchState(scope) }
}

@Composable
fun HomeScreen(
    apiName: String?,
    pageResource: Resource<Map<String, HomeViewModel.ExpandableHomepageList>>?,
    previewResource: Resource<Pair<Boolean, List<LoadResponse>>>?,
    resumeWatching: List<SearchResponse>,
    currentAccount: DataStoreHelper.Account? = null,
    onSelectProviderClick: () -> Unit = {},
    onReloadProviderClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onPosterStyleClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onAccountLongClick: () -> Unit = {},
    onHeroItemClick: (MetaPreview, LoadResponse?, SearchResponse?) -> Unit,
    onHeroPlayClick: (MetaPreview, LoadResponse?, SearchResponse?) -> Unit,
    onHeroBookmarkClick: (MetaPreview, LoadResponse?, SearchResponse?) -> Unit,
    isHeroBookmarked: (MetaPreview, LoadResponse?, SearchResponse?) -> Boolean,
    onHeroShareClick: (MetaPreview, LoadResponse?, SearchResponse?) -> Unit = { _, _, _ -> },
    onLoadMoreHero: () -> Unit,
    onContinueWatchingClick: (SearchResponse) -> Unit,
    onContinueWatchingPlayClick: (SearchResponse) -> Unit,
    onContinueWatchingRemoveClick: (SearchResponse) -> Unit,
    onContinueWatchingViewAll: () -> Unit,
    onCatalogItemClick: (SearchResponse) -> Unit,
    onCatalogViewAllClick: (HomeViewModel.ExpandableHomepageList) -> Unit,
    onCatalogExpand: (String) -> Unit,
    onOpenInBrowserClick: () -> Unit,
    onGoToDownloadsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState: LazyListState = rememberLazyListState()
    val heroStretch = rememberHeroStretchState()
    val posterStyle = rememberPosterCardStyleUiState()

    val catalogSections = remember(pageResource) {
        (pageResource as? Resource.Success)?.value?.values?.filter {
            it.list.list.isNotEmpty()
        }.orEmpty()
    }

    val heroItems: List<MetaPreview> = remember(previewResource, catalogSections) {
        val loadedPreviews = (previewResource as? Resource.Success)?.value?.second.orEmpty()
        if (loadedPreviews.isNotEmpty()) {
            loadedPreviews.map { MetaPreviewAdapter.fromLoadResponse(it) }
        } else {
            catalogSections
                .flatMap { it.list.list }
                .distinctBy { it.url }
                .take(6)
                .map { MetaPreviewAdapter.fromSearchResponse(it) }
        }
    }

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(heroStretch.nestedScrollConnection),
    ) {
        val viewportHeight = maxHeight

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 96.dp else 118.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 1. HERO SECTION (or top spacer if no hero items)
            item(key = "home_hero_item") {
                if (heroItems.isNotEmpty()) {
                    HomeHeroSection(
                        items = heroItems,
                        viewportHeight = viewportHeight,
                        mobileBelowSectionHeightHint = 128.dp,
                        listState = listState,
                        stretchPx = { heroStretch.stretchPx },
                        showImdbRating = posterStyle.showRating,
                        onItemClick = { meta ->
                            onHeroItemClick(meta, meta.originalLoadResponse, meta.originalResponse)
                        },
                        onPlayClick = { meta ->
                            onHeroPlayClick(meta, meta.originalLoadResponse, meta.originalResponse)
                        },
                        onBookmarkClick = { meta ->
                            onHeroBookmarkClick(meta, meta.originalLoadResponse, meta.originalResponse)
                        },
                        isBookmarked = { meta ->
                            isHeroBookmarked(meta, meta.originalLoadResponse, meta.originalResponse)
                        },
                        onShareClick = { meta ->
                            onHeroShareClick(meta, meta.originalLoadResponse, meta.originalResponse)
                        },
                        onNearEndReached = onLoadMoreHero,
                    )
                } else {
                    Spacer(modifier = Modifier.height(statusBarTopPadding + 16.dp))
                }
            }

            // 2. CONTINUE WATCHING SECTION
            if (resumeWatching.isNotEmpty()) {
                item(key = "home_continue_watching") {
                    HomeContinueWatchingSection(
                        title = stringResource(R.string.continue_watching),
                        items = resumeWatching,
                        onItemClick = onContinueWatchingClick,
                        onPlayClick = onContinueWatchingPlayClick,
                        onRemoveClick = onContinueWatchingRemoveClick,
                        onViewAllClick = onContinueWatchingViewAll,
                    )
                }
            }

            // 3. LOADING / ERROR / EMPTY PROVIDER STATES
            when (pageResource) {
                is Resource.Loading -> {
                    if (catalogSections.isEmpty()) {
                        item(key = "home_loading_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                is Resource.Failure -> {
                    item(key = "home_error_state") {
                        HomeErrorSection(
                            errorMessage = pageResource.errorString,
                            onReloadProviderClick = onSelectProviderClick,
                            onOpenInBrowserClick = onOpenInBrowserClick,
                            onGoToDownloadsClick = onGoToDownloadsClick,
                        )
                    }
                }

                is Resource.Success, null -> {
                    if (catalogSections.isEmpty() && (apiName == null || apiName == noneApi.name)) {
                        item(key = "home_empty_provider_state") {
                            HomeEmptyProviderSection(
                                onSelectProviderClick = onSelectProviderClick,
                            )
                        }
                    } else {
                        // 5. CATALOG SHELF ROWS
                        items(
                            items = catalogSections,
                            key = { section -> "catalog_${section.list.name}" },
                        ) { section ->
                            val forceShape = if (section.list.isHorizontalImages) {
                                PosterShape.Landscape
                            } else {
                                null
                            }
                            ShelfSection(
                                title = section.list.name,
                                actionLabel = "View All",
                                onActionClick = { onCatalogViewAllClick(section) },
                                onNearEndReached = if (section.hasNext) {
                                    { onCatalogExpand(section.list.name) }
                                } else {
                                    null
                                },
                            ) {
                                items(
                                    items = section.list.list,
                                    key = { item -> "${section.list.name}_${item.url}_${item.name}" },
                                ) { item ->
                                    PosterCardFromSearchResponse(
                                        item = item,
                                        forceShape = forceShape,
                                        onClick = { onCatalogItemClick(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun HomeEmptyProviderSection(
    onSelectProviderClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No Home Provider Selected",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Select a provider or Random to populate the featured Hero carousel and catalog shelves.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onSelectProviderClick),
        ) {
            Text(
                text = "Select Provider",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun HomeErrorSection(
    errorMessage: String,
    onReloadProviderClick: () -> Unit,
    onOpenInBrowserClick: () -> Unit,
    onGoToDownloadsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = errorMessage.ifBlank { stringResource(R.string.reload_error) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
                    .clickable(onClick = onReloadProviderClick),
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
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onGoToDownloadsClick),
        ) {
            Text(
                text = stringResource(R.string.go_to_downloads),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

