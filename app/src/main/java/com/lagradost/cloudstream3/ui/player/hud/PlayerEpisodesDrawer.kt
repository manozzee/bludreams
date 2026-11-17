package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.ui.details.DetailEpisodesHeader
import com.lagradost.cloudstream3.ui.details.EpisodeLayoutStyle
import com.lagradost.cloudstream3.ui.details.EpisodeListCard
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.ResultEpisode

private val DrawerCornerRadius = LiquidGlassTokens.Radius.sheet
private val DrawerPanelShape = RoundedCornerShape(
    topStart = DrawerCornerRadius,
    topEnd = LiquidGlassTokens.Radius.none,
    bottomEnd = LiquidGlassTokens.Radius.none,
    bottomStart = DrawerCornerRadius,
)

@Composable
fun PlayerEpisodesDrawer(
    visible: Boolean,
    state: PlayerEpisodesDrawerState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onDubSelected: (DubStatus) -> Unit,
    onRangeSelected: (EpisodeRange) -> Unit,
    onSortClick: () -> Unit,
    onEpisodeClick: (ResultEpisode) -> Unit,
    onEpisodeToggleWatched: (ResultEpisode) -> Unit,
    modifier: Modifier = Modifier,
    playingPositionMs: Long = 0L,
    playingDurationMs: Long = 0L,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val endSafeInset = safePadding.calculateEndPadding(layoutDirection)
    val topSafeInset = safePadding.calculateTopPadding() + LiquidGlassTokens.Space.s12
    val bottomSafeInset = safePadding.calculateBottomPadding()

    val glassTokens = MaterialTheme.liquidGlass

    val listState = rememberLazyListState()
    LaunchedEffect(visible, state.scrollTriggerToken) {
        if (visible && state.playingEpisodeId != null) {
            val targetIndex = state.episodes.indexOfFirst { it.id == state.playingEpisodeId }
            if (targetIndex >= 0) {
                val comfortableIndex = (targetIndex - 1).coerceAtLeast(0)
                listState.scrollToItem(comfortableIndex)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val drawerWidth = if (maxWidth >= LiquidGlassTokens.Breakpoint.tablet) {
            (maxWidth * 0.40f).coerceIn(
                LiquidGlassTokens.Space.s80 * 4f,
                glassTokens.components.dialogMaxWidth,
            )
        } else {
            (maxWidth * 0.80f).coerceAtMost(glassTokens.components.dialogMaxWidth * 0.85f)
        }

        // Fullscreen soft dimmed video scrim with tap-outside dismissal
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LiquidGlassTokens.Motion.normalMillis)),
            exit = fadeOut(tween(LiquidGlassTokens.Motion.fastMillis)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassTokens.colors.overlayScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        // Full-Height Right-Anchored Kyant Liquid Glass Side Drawer
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LiquidGlassTokens.Motion.normalMillis)) + slideInHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = fadeOut(tween(180)) + slideOutHorizontally(
                animationSpec = tween(210, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(drawerWidth)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(DrawerPanelShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                KyantPlayerGlassChromeLayer(
                    shape = DrawerPanelShape,
                    backdrop = backdrop,
                    fallbackCornerRadius = DrawerCornerRadius,
                    modifier = Modifier.matchParentSize(),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(glassTokens.colors.surfaceSheet.copy(alpha = glassTokens.opacity.overlayLight))
                        .padding(end = endSafeInset, top = topSafeInset),
                ) {
                    DetailEpisodesHeader(
                        seasonOptions = state.seasonOptions,
                        selectedSeasonLabel = state.selectedSeasonLabel,
                        dubOptions = state.dubOptions,
                        selectedDubLabel = state.selectedDubLabel,
                        rangeOptions = state.rangeOptions,
                        selectedRangeLabel = state.selectedRangeLabel,
                        selectedSortLabel = state.selectedSortLabel,
                        hasSortOptions = state.hasSortOptions,
                        hasDownloadSupport = false,
                        episodesCountText = state.episodesCountText,
                        episodesList = state.episodes,
                        episodeLayoutStyle = EpisodeLayoutStyle.LIST,
                        onToggleEpisodeLayoutStyle = {},
                        onSeasonSelected = onSeasonSelected,
                        onDubSelected = onDubSelected,
                        onRangeSelected = onRangeSelected,
                        onSortClick = onSortClick,
                        onBatchDownloadClick = {},
                        onBatchDownloadLongClick = {},
                        showLayoutToggle = false,
                        horizontalPadding = LiquidGlassTokens.Space.s14,
                        topRowTrailingContent = null,
                        elevatedGlassContrast = true,
                    )

                    Spacer(modifier = Modifier.height(LiquidGlassTokens.Space.s10))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LiquidGlassTokens.Border.thin)
                            .background(PlayerGlassWhiteDivider),
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                if (size.height > 0f) {
                                    val topFadeFraction =
                                        (LiquidGlassTokens.Space.s18.toPx() / size.height).coerceIn(0f, 0.2f)
                                    val bottomFadeFraction =
                                        (LiquidGlassTokens.Space.s28.toPx() / size.height).coerceIn(0f, 0.25f)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            topFadeFraction to glassTokens.colors.backgroundAmoled,
                                            (1f - bottomFadeFraction) to glassTokens.colors.backgroundAmoled,
                                            1f to Color.Transparent,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            },
                        contentPadding = PaddingValues(
                            top = LiquidGlassTokens.Space.s20,
                            bottom = bottomSafeInset + LiquidGlassTokens.Space.s32,
                        ),
                    ) {
                        itemsIndexed(
                            items = state.episodes,
                            key = { _, ep -> "player_ep_${ep.id}_${ep.episode}_${ep.index}" },
                        ) { index, episode ->
                            val isPlaying = state.playingEpisodeId != null && episode.id == state.playingEpisodeId
                            val nextIsPlaying = index < state.episodes.lastIndex &&
                                state.playingEpisodeId != null &&
                                state.episodes[index + 1].id == state.playingEpisodeId

                            val displayEpisode = remember(
                                episode,
                                isPlaying,
                                playingPositionMs,
                                playingDurationMs,
                            ) {
                                if (isPlaying && playingDurationMs > 0L) {
                                    episode.copy(
                                        position = playingPositionMs,
                                        duration = playingDurationMs,
                                    )
                                } else {
                                    episode
                                }
                            }

                            if (isPlaying) {
                                val activeRowShape = RoundedCornerShape(LiquidGlassTokens.Radius.lg)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = LiquidGlassTokens.Space.s6,
                                            vertical = LiquidGlassTokens.Space.s2,
                                        )
                                        .clip(activeRowShape)
                                        .background(glassTokens.colors.overlaySelected)
                                        .border(
                                            width = LiquidGlassTokens.Border.hairline,
                                            color = PlayerGlassWhiteActiveRowBorder,
                                            shape = activeRowShape,
                                        )
                                        .padding(
                                            horizontal = LiquidGlassTokens.Space.s8,
                                            vertical = LiquidGlassTokens.Space.s8,
                                        ),
                                ) {
                                    EpisodeListCard(
                                        episode = displayEpisode,
                                        fallbackBackdropUrl = state.fallbackBackdropUrl,
                                        hasDownloadSupport = false,
                                        onClick = { onEpisodeClick(episode) },
                                        onLongClick = {},
                                        onDownloadClick = {},
                                        onDownloadLongClick = {},
                                        onToggleWatched = { onEpisodeToggleWatched(episode) },
                                        isPlaying = true,
                                        elevatedGlassContrast = true,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = LiquidGlassTokens.Space.s14,
                                            vertical = LiquidGlassTokens.Space.s10,
                                        ),
                                ) {
                                    EpisodeListCard(
                                        episode = displayEpisode,
                                        fallbackBackdropUrl = state.fallbackBackdropUrl,
                                        hasDownloadSupport = false,
                                        onClick = { onEpisodeClick(episode) },
                                        onLongClick = {},
                                        onDownloadClick = {},
                                        onDownloadLongClick = {},
                                        onToggleWatched = { onEpisodeToggleWatched(episode) },
                                        isPlaying = false,
                                        elevatedGlassContrast = true,
                                    )
                                }
                            }

                            if (index < state.episodes.lastIndex && !isPlaying && !nextIsPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = LiquidGlassTokens.Space.s14)
                                        .height(LiquidGlassTokens.Border.thin)
                                        .background(PlayerGlassWhiteDivider),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
