package com.lagradost.cloudstream3.ui.liquidglass

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

internal const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f

private const val HERO_AUTO_SCROLL_INTERVAL_MS = 6_000L
private const val HERO_PARALLAX_PX = 22f
private const val HERO_TITLE_REVEAL_DURATION_MS = 200

private const val MOBILE_HERO_VIEWPORT_RATIO = 0.70f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 306f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 646f

private val HeroSurfaceDark = Color(0xFF17171A)
private val HeroTextPrimary = Color(0xFFF5F5F7)

private val heroPaletteColorCache = LruCache<String, Int>(24)

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
)

fun Modifier.heroStretchHeight(baseHeight: Dp, stretchPx: () -> Float): Modifier =
    layout { measurable, constraints ->
        val height = baseHeight.roundToPx() + (stretchPx().coerceAtLeast(0f)).toInt()
        val placeable = measurable.measure(
            constraints.copy(minHeight = height, maxHeight = height),
        )
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

fun Modifier.heroStretchZoom(stretchPx: () -> Float): Modifier = graphicsLayer {
    val zoom = 1f + (stretchPx().coerceAtLeast(0f) / size.height.coerceAtLeast(1f))
    transformOrigin = TransformOrigin(0.5f, 0f)
    scaleX = zoom
    scaleY = zoom
}

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") viewportHeight: Dp? = null,
    @Suppress("UNUSED_PARAMETER") mobileBelowSectionHeightHint: Dp? = null,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    @Suppress("UNUSED_PARAMETER") showImdbRating: Boolean = true,
    @Suppress("UNUSED_PARAMETER") showMdbListRatings: Boolean = false,
    onItemClick: ((MetaPreview) -> Unit)? = null,
    onPlayClick: ((MetaPreview) -> Unit)? = null,
    onBookmarkClick: ((MetaPreview) -> Unit)? = null,
    isBookmarked: ((MetaPreview) -> Boolean)? = null,
    @Suppress("UNUSED_PARAMETER") onShareClick: ((MetaPreview) -> Unit)? = null,
    onNearEndReached: (() -> Unit)? = null,
) {
    HeroPosterCarouselSection(
        items = items,
        modifier = modifier,
        listState = listState,
        stretchPx = stretchPx,
        onDetailsClick = onItemClick,
        onPlayClick = onPlayClick,
        onBookmarkClick = onBookmarkClick,
        isBookmarked = isBookmarked,
        onNearEndReached = onNearEndReached,
    )
}

@Composable
fun HeroPosterCarouselSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    onDetailsClick: ((MetaPreview) -> Unit)? = null,
    onPlayClick: ((MetaPreview) -> Unit)? = null,
    onBookmarkClick: ((MetaPreview) -> Unit)? = null,
    isBookmarked: ((MetaPreview) -> Boolean)? = null,
    onNearEndReached: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val pagerState = key(items.size) {
        rememberPagerState(
            initialPage = if (items.size > 1) {
                (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size)
            } else {
                0
            },
            pageCount = { if (items.size > 1) Int.MAX_VALUE else 1 },
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    var isUserTouching by remember { mutableStateOf(false) }

    val currentItemIndex = (((pagerState.currentPage % items.size) + items.size) % items.size)
        .coerceIn(0, items.lastIndex)
    val targetItemIndex = (((pagerState.targetPage % items.size) + items.size) % items.size)
        .coerceIn(0, items.lastIndex)
    val displayItemIndex = if (pagerState.isScrollInProgress) targetItemIndex else currentItemIndex
    val displayItem = items[displayItemIndex]
    val activeImageUrl = resolvePosterImageUrl(displayItem)

    val isScrolledPastHero by remember(listState) {
        derivedStateOf {
            listState != null && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280)
        }
    }
    val isFeedScrolling by remember(listState) {
        derivedStateOf { listState?.isScrollInProgress == true }
    }
    val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val isAutoAdvancePaused by rememberUpdatedState(
        isUserTouching || isPagerDragged || isFeedScrolling || isScrolledPastHero,
    )

    LaunchedEffect(pagerState.settledPage, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        val settledItemIndex = (((pagerState.settledPage % items.size) + items.size) % items.size)
            .coerceIn(0, items.lastIndex)
        if (settledItemIndex >= items.size - 1) {
            onNearEndReached?.invoke()
        }
    }

    LaunchedEffect(pagerState, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        var lastObservedPage = pagerState.currentPage
        var idleElapsedMs = 0L
        val stepMs = 200L
        while (true) {
            delay(stepMs)
            val currentPage = pagerState.currentPage
            if (currentPage != lastObservedPage) {
                lastObservedPage = currentPage
                idleElapsedMs = 0L
            }
            if (isAutoAdvancePaused || pagerState.isScrollInProgress) {
                idleElapsedMs = 0L
                continue
            }
            idleElapsedMs += stepMs
            if (idleElapsedMs >= HERO_AUTO_SCROLL_INTERVAL_MS) {
                idleElapsedMs = 0L
                val nextPage = (currentPage + 1).coerceAtMost(Int.MAX_VALUE - 1)
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                )
                lastObservedPage = pagerState.currentPage
            }
        }
    }

    var extractedPosterColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(activeImageUrl) {
        if (activeImageUrl.isNullOrBlank()) {
            extractedPosterColor = null
            return@LaunchedEffect
        }
        heroPaletteColorCache.get(activeImageUrl)?.let { cachedArgb ->
            extractedPosterColor = Color(cachedArgb)
            return@LaunchedEffect
        }
        extractedPosterColor = extractPosterGradientColor(context, activeImageUrl)
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val fallbackAccentColor = MaterialTheme.colorScheme.primary
    val animatedBackdropColor by animateColorAsState(
        targetValue = extractedPosterColor ?: fallbackAccentColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "heroBackdropExtractedColor",
    )

    val view = LocalView.current
    val fallbackTopInsetDp = remember(view, density, context) {
        val rootTopPx = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            ?.top ?: 0
        val dimenTopPx = context.getStatusBarHeight()
        with(density) { maxOf(rootTopPx, dimenTopPx).toDp() }
    }
    val composeStatusBarTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val effectiveTopInsetDp = maxOf(fallbackTopInsetDp, composeStatusBarTopDp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to animatedBackdropColor.copy(alpha = 0.55f),
                        0.48f to animatedBackdropColor.copy(alpha = 0.22f),
                        0.86f to animatedBackdropColor.copy(alpha = 0.04f),
                        1.0f to Color.Transparent,
                    ),
                )
            }
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            isUserTouching = event.changes.any { it.pressed }
                        }
                    }
                } finally {
                    isUserTouching = false
                }
            },
    ) {
        val pagerHorizontalPadding = when {
            maxWidth >= 840.dp -> ((maxWidth - 280.dp) / 2f).coerceAtLeast(64.dp)
            maxWidth >= 600.dp -> ((maxWidth - 260.dp) / 2f).coerceAtLeast(64.dp)
            else -> (maxWidth * 0.175f).coerceIn(60.dp, 76.dp)
        }
        val belowCardHorizontalPadding = if (maxWidth >= 600.dp) pagerHorizontalPadding else 24.dp
        val stretchScale =
            1f + (stretchPx().coerceAtLeast(0f) / with(density) { 2600.dp.toPx() }).coerceIn(0f, 0.05f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = effectiveTopInsetDp + 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Minimal Top Header ("Home" only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                    ),
                    color = HeroTextPrimary,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Frameless Portrait Poster Carousel
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = pagerHorizontalPadding),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleX = stretchScale
                        scaleY = stretchScale
                    },
            ) { page ->
                val itemIndex = ((page % items.size) + items.size) % items.size
                val item = items[itemIndex]

                FramelessHeroPosterCard(
                    item = item,
                    pagerState = pagerState,
                    page = page,
                    onDetailsClick = onDetailsClick?.let { { it(item) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Clean Under-Poster Title & Action Row (Zero Liquid Glass)
//            HeroUnderPosterFooter(
//                item = displayItem,
//                itemCount = items.size,
//                activeItemIndex = displayItemIndex,
//                isBookmarked = isBookmarked?.invoke(displayItem) == true,
//                onDetailsClick = onDetailsClick?.let { { it(displayItem) } },
//                onPlayClick = onPlayClick?.let { { it(displayItem) } }
//                    ?: onDetailsClick?.let { { it(displayItem) } },
//                onBookmarkClick = onBookmarkClick?.let { { it(displayItem) } },
//                onDotClick = { targetIndex ->
//                    if (items.size > 1) {
//                        coroutineScope.launch {
//                            pagerState.animateScrollToPage(
//                                heroPageForItem(pagerState.currentPage, targetIndex, items.size),
//                            )
//                        }
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = belowCardHorizontalPadding),
//            )
        }
    }
}

@Composable
private fun FramelessHeroPosterCard(
    item: MetaPreview,
    pagerState: PagerState,
    page: Int,
    onDetailsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val posterShape = remember { RoundedCornerShape(22.dp) }
    val imageUrl = resolvePosterImageUrl(item)

    Box(
        modifier = modifier
            .graphicsLayer {
                val pageOffset = heroPageOffset(pagerState, page)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val cardScale = lerpFloat(1.0f, 0.90f, absOffset)
                val cardAlpha = lerpFloat(1.0f, 0.60f, absOffset)
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .shadow(
                elevation = 12.dp,
                shape = posterShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.70f),
            )
            .clip(posterShape)
            .background(HeroSurfaceDark)
            .clickable(enabled = onDetailsClick != null) { onDetailsClick?.invoke() },
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)
                    scaleX = 1.05f
                    scaleY = 1.05f
                    translationX = pageOffset * HERO_PARALLAX_PX
                },
        )
    }
}

@Composable
private fun HeroUnderPosterFooter(
    item: MetaPreview,
    itemCount: Int,
    activeItemIndex: Int,
    isBookmarked: Boolean,
    onDetailsClick: (() -> Unit)?,
    onPlayClick: (() -> Unit)?,
    onBookmarkClick: (() -> Unit)?,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRevealAnim = remember { Animatable(1f) }
    var hasRenderedInitial by remember { mutableStateOf(false) }

    LaunchedEffect(activeItemIndex, item.stableKey()) {
        if (!hasRenderedInitial) {
            hasRenderedInitial = true
            titleRevealAnim.snapTo(1f)
        } else {
            titleRevealAnim.snapTo(0f)
            titleRevealAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HERO_TITLE_REVEAL_DURATION_MS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    var logoLoadFailed by remember(item.stableKey(), item.logo) { mutableStateOf(false) }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = MaterialTheme.colorScheme.onPrimary
    val bookmarkIconTint by animateColorAsState(
        targetValue = if (isBookmarked) accentColor else HeroTextPrimary,
        animationSpec = tween(durationMillis = 180),
        label = "heroMinimalBookmarkTint",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        // Title Logo or 1-Line Fallback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val p = titleRevealAnim.value.coerceIn(0f, 1f)
                    alpha = p
                    translationY = (1f - p) * 6.dp.toPx()
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            if (logoUrl != null && !logoLoadFailed) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    onError = { logoLoadFailed = true },
                    modifier = Modifier
                        .height(34.dp)
                        .widthIn(max = 200.dp)
                        .clickable(enabled = onDetailsClick != null) { onDetailsClick?.invoke() },
                )
            } else {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                    ),
                    color = HeroTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onDetailsClick != null) { onDetailsClick?.invoke() },
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Minimal Action & Indicator Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Solid Accent Play Pill (40.dp height, 20.dp radius)
            val playInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor)
                    .clickable(
                        enabled = onPlayClick != null,
                        interactionSource = playInteractionSource,
                        indication = ripple(color = onAccentColor),
                        onClick = { onPlayClick?.invoke() },
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = onAccentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                    color = onAccentColor,
                    maxLines = 1,
                )
            }

            // Simple Solid Dark Bookmark Circle (40.dp, #17171A, no glass/blur)
            val bookmarkInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HeroSurfaceDark)
                    .clickable(
                        enabled = onBookmarkClick != null,
                        interactionSource = bookmarkInteractionSource,
                        indication = ripple(color = Color.White),
                        onClick = { onBookmarkClick?.invoke() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = bookmarkIconTint,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Minimal Page Indicator Dots
            if (itemCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(itemCount) { index ->
                        val isActive = index == activeItemIndex
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 16.dp else 6.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                            label = "heroMinimalDotWidth",
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isActive) HeroTextPrimary else Color.White.copy(alpha = 0.28f),
                            animationSpec = tween(durationMillis = 200),
                            label = "heroMinimalDotColor",
                        )
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .clickable { onDotClick(index) },
                        )
                    }
                }
            }
        }
    }
}

private fun resolvePosterImageUrl(item: MetaPreview): String? =
    item.poster?.takeIf { it.isNotBlank() }
        ?: item.rawPosterUrl?.takeIf { it.isNotBlank() }
        ?: item.banner?.takeIf { it.isNotBlank() }
        ?: item.landscapePoster

internal suspend fun extractPosterGradientColor(
    context: Context,
    imageUrl: String,
): Color? {
    heroPaletteColorCache.get(imageUrl)?.let { cachedArgb ->
        return Color(cachedArgb)
    }

    return withContext(Dispatchers.IO) {
        runCatching {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(128)
                .build()
            val result = imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
            val bitmap = result.image.asDrawable(context.resources).toBitmap()
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap).maximumColorCount(12).generate()
            }
            val candidateSwatches = listOfNotNull(
                palette.vibrantSwatch,
                palette.mutedSwatch,
                palette.darkVibrantSwatch,
                palette.lightVibrantSwatch,
                palette.dominantSwatch,
            )
            val chosenSwatch = candidateSwatches.firstOrNull { swatch ->
                val color = Color(swatch.rgb)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                color.luminance() >= 0.02f && hsv[1] >= 0.12f
            } ?: candidateSwatches.firstOrNull { Color(it.rgb).luminance() >= 0.015f }
                ?: candidateSwatches.firstOrNull()
            val rgb = chosenSwatch?.rgb ?: return@runCatching null
            heroPaletteColorCache.put(imageUrl, rgb)
            Color(rgb)
        }.getOrNull()
    }
}

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

internal fun heroPageForItem(currentPage: Int, itemIndex: Int, itemCount: Int): Int {
    val page = currentPage.toLong() - currentPage % itemCount + itemIndex
    return listOf(page - itemCount, page, page + itemCount)
        .filter { it in 0L until Int.MAX_VALUE.toLong() }
        .minBy { abs(it - currentPage) }
        .toInt()
}

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
): HomeHeroLayout =
    when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.36f).dp.coerceIn(306.dp, 374.dp),
            contentMaxWidth = 640.dp,
            contentWidthFraction = 0.56f,
            contentHorizontalPadding = 56.dp,
            contentVerticalPadding = 22.dp,
            bottomFadeHeight = 160.dp,
            logoWidthFraction = 0.58f,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.39f).dp.coerceIn(289.dp, 357.dp),
            contentMaxWidth = 560.dp,
            contentWidthFraction = 0.62f,
            contentHorizontalPadding = 40.dp,
            contentVerticalPadding = 20.dp,
            bottomFadeHeight = 153.dp,
            logoWidthFraction = 0.56f,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.49f).dp.coerceIn(272.dp, 323.dp),
            contentMaxWidth = 520.dp,
            contentWidthFraction = 0.72f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 18.dp,
            bottomFadeHeight = 145.dp,
            logoWidthFraction = 0.54f,
        )
        else -> HomeHeroLayout(
            isTablet = false,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 14.dp,
            bottomFadeHeight = 187.dp,
            logoWidthFraction = 0.62f,
        )
    }

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 0.98f).dp
    val baseHeight = if (mobileBelowSectionHeightHintDp == null) {
        viewportDrivenHeight?.coerceAtMost(widthFallbackHeight) ?: widthFallbackHeight
    } else {
        viewportDrivenHeight ?: widthFallbackHeight
    }

    val maxAllowedFromViewportDp = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        viewportHeightDp - mobileBelowSectionHeightHintDp
    } else {
        null
    }
    val cappedHeight = if (maxAllowedFromViewportDp != null) {
        val maxAllowedFromViewport = maxAllowedFromViewportDp.dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }
    val minHeight = if (maxAllowedFromViewportDp != null) {
        minOf(MOBILE_HERO_MIN_HEIGHT_DP, maxAllowedFromViewportDp.coerceAtLeast(0f)).dp
    } else {
        MOBILE_HERO_MIN_HEIGHT_DP.dp
    }

    return cappedHeight.coerceIn(minHeight, MOBILE_HERO_MAX_HEIGHT_DP.dp)
}

internal fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

internal fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}
