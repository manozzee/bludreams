package com.lagradost.cloudstream3.ui.player.hud

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.LocalLiquidGlassDrawTick
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.isLiquidGlassBackdropSupported
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassSource
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import com.lagradost.cloudstream3.ui.player.CSPlayerLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val BoostOrange = Color(0xFFFF9800)
private val BoostRed = Color(0xFFEF4444)
private val FallbackBorder = Color.White.copy(alpha = 0.22f)

@Composable
fun PlayerHudOverlay(
    state: PlayerHudState,
    callbacks: PlayerHudCallbacks,
    contentFrameProvider: () -> View?,
    modifier: Modifier = Modifier,
) {
    val isBuffering = state.status == CSPlayerLoading.IsBuffering
    val hasOverlayOpen = state.hasAnyOverlayOpen
    val isMainHudVisible =
        state.isShowing &&
            !state.isLocked &&
            !hasOverlayOpen
    val isLockPillVisible =
        state.isShowing &&
            state.isLocked &&
            !hasOverlayOpen
    val anyGlassVisible =
        state.isShowing ||
            hasOverlayOpen ||
            isBuffering ||
            state.timestampText != null ||
            state.isSpeedUpActive ||
            state.seekPreviewText != null ||
            state.doubleTapFeedback != null ||
            state.isVolumeVisible ||
            state.isBrightnessVisible

    val useLiquidGlass = isLiquidGlassBackdropSupported()
    val drawTickState = remember { mutableIntStateOf(0) }
    val composeView = LocalView.current
    val contentLoc = remember { IntArray(2) }
    val composeLoc = remember { IntArray(2) }

    val layerBackdrop = rememberLayerBackdrop {
        @Suppress("UNUSED_VARIABLE")
        val tick = drawTickState.intValue
        drawRect(Color.Black)
        val frame = contentFrameProvider()
        if (frame != null && frame.width > 0 && frame.height > 0 && frame.isAttachedToWindow) {
            frame.getLocationInWindow(contentLoc)
            composeView.getLocationInWindow(composeLoc)
            val offsetX = (contentLoc[0] - composeLoc[0]).toFloat()
            val offsetY = (contentLoc[1] - composeLoc[1]).toFloat()
            translate(left = offsetX, top = offsetY) {
                drawIntoCanvas { canvas ->
                    try {
                        frame.draw(canvas.nativeCanvas)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }
    val activeBackdrop: PlatformLayerBackdrop? = if (useLiquidGlass) layerBackdrop else null

    LaunchedEffect(useLiquidGlass, anyGlassVisible, state.isPlaying) {
        if (useLiquidGlass && anyGlassVisible && state.isPlaying) {
            while (isActive) {
                withFrameNanos {
                    drawTickState.intValue++
                }
            }
        }
    }

    LaunchedEffect(
        useLiquidGlass,
        anyGlassVisible,
        state.positionMs,
        state.status,
        state.resizeMode,
        state.isLocked,
        state.isEpisodesDrawerVisible,
        state.isSourceSubsDialogVisible,
        state.isTracksDialogVisible,
        state.isSpeedDialogVisible,
        state.isCompressorDialogVisible,
        state.isSubtitleOffsetDialogVisible,
        state.selectionPopup?.token,
        state.isQualityProfileDialogVisible,
        state.qualityProfileDialog.editingProfileId,
        state.qualityProfileDialog.isEditingProfileSettings,
        state.isOnlineSubtitlesDialogVisible,
        state.isSubtitleSettingsVisible,
        state.seekPreviewText,
        state.doubleTapFeedback?.token,
        state.isVolumeVisible,
        state.volumeLevel,
        state.isBrightnessVisible,
        state.brightnessLevel,
        state.timestampText,
    ) {
        if (useLiquidGlass && anyGlassVisible) {
            repeat(2) {
                withFrameNanos {
                    drawTickState.intValue++
                }
            }
        }
    }

    var polledProgress by remember {
        mutableStateOf(
            PlaybackProgressSnapshot(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedPositionMs = state.bufferedPositionMs,
                isAtLiveEdge = state.isAtLiveEdge,
            ),
        )
    }

    LaunchedEffect(
        state.positionMs,
        state.durationMs,
        state.bufferedPositionMs,
        state.isAtLiveEdge,
    ) {
        polledProgress = PlaybackProgressSnapshot(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            bufferedPositionMs = state.bufferedPositionMs,
            isAtLiveEdge = state.isAtLiveEdge,
        )
    }

    LaunchedEffect(state.isShowing, state.isLocked, state.status) {
        if (state.isShowing && !state.isLocked) {
            while (isActive) {
                polledProgress = callbacks.pollProgress()
                delay(250L)
            }
        }
    }

    var showTorrentPopover by remember { mutableStateOf(false) }
    var showMediaInfoExpanded by remember(state.showMediaInfoByDefault) {
        mutableStateOf(state.showMediaInfoByDefault)
    }

    LaunchedEffect(state.isShowing, state.isLocked, state.isTorrent) {
        if (!state.isShowing || state.isLocked || !state.isTorrent) {
            showTorrentPopover = false
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val startInset = safePadding.calculateStartPadding(layoutDirection)
    val endInset = safePadding.calculateEndPadding(layoutDirection)
    val symmetricalHorizontalPadding = maxOf(startInset, endInset) + 16.dp
    val topEdgePadding = safePadding.calculateTopPadding() + 12.dp
    val bottomEdgePadding = safePadding.calculateBottomPadding() + 12.dp

    CompositionLocalProvider(LocalLiquidGlassDrawTick provides drawTickState) {
        Box(modifier = modifier.fillMaxSize()) {
            if (useLiquidGlass && anyGlassVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlassSource(layerBackdrop),
                )
            }

            // Soft edge vignette behind top and bottom HUD controls for bright scenes
            AnimatedVisibility(
                visible = isMainHudVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.52f),
                                    ),
                                ),
                            ),
                    )
                }
            }

            // Full-bleed HUD overlay container so centered controls lock to true screen center
            Box(modifier = Modifier.fillMaxSize()) {
                // Top corner-split bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(
                            start = symmetricalHorizontalPadding,
                            end = symmetricalHorizontalPadding,
                            top = topEdgePadding,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    // Top-Left: Back + Title + Offline Pin + Filler + Source/MediaInfo
                    AnimatedVisibility(
                        visible = isMainHudVisible,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(180)),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        TopLeftCluster(
                            state = state,
                            backdrop = activeBackdrop,
                            showMediaInfoExpanded = showMediaInfoExpanded,
                            onToggleMediaInfo = {
                                callbacks.onInteraction()
                                showMediaInfoExpanded = !showMediaInfoExpanded
                            },
                            onBackClick = {
                                callbacks.onInteraction()
                                callbacks.onBackClick()
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Top-Right: Torrent (+ Popover) and Episodes
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimatedVisibility(
                            visible = isMainHudVisible,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(180)),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isTorrent) {
                                    GlassCircleIconButton(
                                        backdrop = activeBackdrop,
                                        drawableRes = R.drawable.baseline_downloading_24,
                                        contentDescription = stringResource(R.string.player_torrent_progress_desc),
                                        tint = if (showTorrentPopover) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White
                                        },
                                        onClick = {
                                            callbacks.onInteraction()
                                            showTorrentPopover = !showTorrentPopover
                                        },
                                    )
                                }

                                if (state.hasEpisodes) {
                                    GlassCircleVectorButton(
                                        backdrop = activeBackdrop,
                                        imageVector = AppIcons.ViewList,
                                        contentDescription = stringResource(R.string.episodes),
                                        onClick = {
                                            callbacks.onInteraction()
                                            callbacks.onEpisodesClick()
                                        },
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showTorrentPopover && isMainHudVisible && state.isTorrent,
                            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.94f),
                            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.94f),
                        ) {
                            TorrentPopoverCard(
                                backdrop = activeBackdrop,
                                torrent = state.torrentProgress,
                            )
                        }
                    }
                }

                // Top-Center 2x Hold Speed-Up badge
                AnimatedVisibility(
                    visible = state.isSpeedUpActive,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.92f),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topEdgePadding + 6.dp),
                ) {
                    GlassSurface(
                        backdrop = activeBackdrop,
                        shape = RoundedCornerShape(50),
                        interactive = false,
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.speedup),
                                contentDescription = stringResource(R.string.player_speedup_2x),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.player_speedup_2x),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }

                // Double-Tap Rewind / Fast-Forward Edge Glow + Liquid Glass Pills (behind foreground HUD)
                DoubleTapSeekOverlay(
                    feedback = state.doubleTapFeedback,
                    backdrop = activeBackdrop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Center Play / Pause / Replay or Buffering circle
                if (isBuffering && !hasOverlayOpen) {
                    GlassSurface(
                        backdrop = activeBackdrop,
                        shape = CircleShape,
                        interactive = false,
                        modifier = Modifier
                            .size(if (state.isShowing) 68.dp else 56.dp)
                            .align(Alignment.Center),
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(if (state.isShowing) 30.dp else 26.dp),
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = isMainHudVisible,
                        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.9f),
                        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f),
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        val isEnded = state.status == CSPlayerLoading.IsEnded
                        GlassSurface(
                            backdrop = activeBackdrop,
                            shape = CircleShape,
                            interactive = true,
                            onClick = {
                                callbacks.onInteraction()
                                callbacks.onPlayPauseClick()
                            },
                            modifier = Modifier.size(68.dp),
                        ) {
                            when {
                                isEnded -> Icon(
                                    painter = painterResource(R.drawable.ic_baseline_replay_24),
                                    contentDescription = stringResource(R.string.player_replay_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                                state.isPlaying -> Icon(
                                    imageVector = PlayerHudIcons.Pause,
                                    contentDescription = stringResource(R.string.player_play_pause_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                                else -> Icon(
                                    imageVector = AppIcons.PlayArrow,
                                    contentDescription = stringResource(R.string.player_play_pause_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                    }
                }

                // Center Horizontal-Swipe Seek Preview Pill
                AnimatedVisibility(
                    visible = state.seekPreviewText != null,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(120)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 116.dp),
                ) {
                    GlassSurface(
                        backdrop = activeBackdrop,
                        shape = RoundedCornerShape(50),
                        interactive = false,
                    ) {
                        Text(
                            text = state.seekPreviewText.orEmpty(),
                            style = MaterialTheme.liquidGlassTypeScale.titleMd.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                // Left Vertical Volume Capsule
                AnimatedVisibility(
                    visible = state.isVolumeVisible,
                    enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.92f),
                    exit = fadeOut(tween(240)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = symmetricalHorizontalPadding),
                ) {
                    VerticalLevelCapsule(
                        backdrop = activeBackdrop,
                        level = state.volumeLevel,
                        iconRes = PlayerHudIcons.volumeDrawable(state.volumeLevel.coerceIn(0f, 1f)),
                        boostColor = if (state.volumeBoostError) BoostRed else BoostOrange,
                    )
                }

                // Right Vertical Brightness Capsule
                AnimatedVisibility(
                    visible = state.isBrightnessVisible,
                    enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.92f),
                    exit = fadeOut(tween(240)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = symmetricalHorizontalPadding),
                ) {
                    VerticalLevelCapsule(
                        backdrop = activeBackdrop,
                        level = state.brightnessLevel,
                        iconRes = PlayerHudIcons.brightnessDrawable(state.brightnessLevel),
                        boostColor = BoostOrange,
                    )
                }

                // Bottom-Right Skip OP / Skip Chapter Liquid Glass pills above timestamp
                val skipBottomPadding by animateDpAsState(
                    targetValue = if (isMainHudVisible) 96.dp else 24.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "skipPillBottomPadding",
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = symmetricalHorizontalPadding,
                            bottom = bottomEdgePadding + skipBottomPadding,
                        ),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedVisibility(
                        visible = state.isSkipOpVisible && isMainHudVisible,
                        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.94f),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f),
                    ) {
                        GlassTextPill(
                            backdrop = activeBackdrop,
                            text = stringResource(R.string.video_skip_op),
                            iconRes = R.drawable.ic_baseline_fast_forward_24,
                            onClick = {
                                callbacks.onInteraction()
                                callbacks.onSkipOpClick()
                            },
                        )
                    }

                    AnimatedVisibility(
                        visible = state.timestampText != null &&
                            !state.isLocked &&
                            !hasOverlayOpen,
                        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.94f),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f),
                    ) {
                        val label = state.timestampText.orEmpty()
                        GlassTextPill(
                            backdrop = activeBackdrop,
                            text = label,
                            iconRes = R.drawable.ic_baseline_skip_next_24,
                            onClick = {
                                callbacks.onInteraction()
                                callbacks.onSkipChapterClick()
                            },
                        )
                    }
                }

                // Bottom Corner-Split Scrubber + Action Buttons
                AnimatedVisibility(
                    visible = isMainHudVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = symmetricalHorizontalPadding,
                            end = symmetricalHorizontalPadding,
                            bottom = bottomEdgePadding,
                        ),
                ) {
                    BottomHudDock(
                        state = state,
                        progress = polledProgress,
                        backdrop = activeBackdrop,
                        callbacks = callbacks,
                    )
                }

                // Locked Screen Unlock Pill
                AnimatedVisibility(
                    visible = isLockPillVisible,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f),
                    exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomEdgePadding + 8.dp),
                ) {
                    GlassSurface(
                        backdrop = activeBackdrop,
                        shape = RoundedCornerShape(50),
                        interactive = true,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onLockToggle()
                        },
                        modifier = Modifier.height(44.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = PlayerHudIcons.Lock,
                                contentDescription = stringResource(R.string.player_unlock_label),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.player_unlock_label),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // Floating Kyant Liquid Glass Episodes Side Drawer
            PlayerEpisodesDrawer(
                visible = state.isEpisodesDrawerVisible,
                state = state.episodesDrawer,
                backdrop = activeBackdrop,
                playingPositionMs = polledProgress.positionMs,
                playingDurationMs = polledProgress.durationMs,
                onDismiss = callbacks.onDismissEpisodesDrawer,
                onSeasonSelected = callbacks.onEpisodeDrawerSeasonSelected,
                onDubSelected = callbacks.onEpisodeDrawerDubSelected,
                onRangeSelected = callbacks.onEpisodeDrawerRangeSelected,
                onSortClick = callbacks.onEpisodeDrawerSortClick,
                onEpisodeClick = callbacks.onEpisodeDrawerEpisodeClick,
                onEpisodeToggleWatched = callbacks.onEpisodeDrawerToggleWatched,
            )

            // Centered Floating Kyant Liquid Glass Sources & Subtitles Card
            PlayerSourceSubsDialog(
                visible = state.isSourceSubsDialogVisible,
                state = state.sourceSubsDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onSourceSubsDismiss,
                onApply = callbacks.onSourceSubsApply,
                onSourceSelected = callbacks.onSourceSelected,
                onSourceLongClick = callbacks.onSourceLongClick,
                onProfileSettingsClick = callbacks.onSourceProfileSettingsClick,
                onSubtitleGroupSelected = callbacks.onSubtitleGroupSelected,
                onSubtitleVariantSelected = callbacks.onSubtitleAudioOptionSelected,
                onEncodingClick = callbacks.onSubtitleEncodingClick,
                onSubtitleSettingsClick = callbacks.onSubtitleSettingsClick,
                onLoadFromFile = callbacks.onLoadSubtitlesFromFile,
                onLoadOnline = callbacks.onLoadSubtitlesOnline,
                onLoadFirstOnline = callbacks.onLoadFirstSubtitleOnline,
            )

            // Full-Screen Borderless Dark Kyant Liquid Glass Video & Audio Tracks Dialog
            PlayerTracksDialog(
                visible = state.isTracksDialogVisible,
                state = state.tracksDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onTracksDismiss,
                onApply = callbacks.onTracksApply,
                onVideoTrackSelected = callbacks.onVideoTrackSelected,
                onAudioTrackSelected = callbacks.onAudioTrackSelected,
            )

            // Centered Floating Dark Kyant Liquid Glass Playback Speed Card
            PlayerSpeedDialog(
                visible = state.isSpeedDialogVisible,
                state = state.speedDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onSpeedDismiss,
                onSpeedChanged = callbacks.onSpeedChanged,
                onSpeedStep = callbacks.onSpeedStep,
                onSpeedPresetSelected = callbacks.onSpeedPresetSelected,
            )

            // Centered Floating Wide 3-Row Dark Kyant Liquid Glass DRC Card
            PlayerCompressorDialog(
                visible = state.isCompressorDialogVisible,
                state = state.compressorDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onCompressorDismiss,
                onApply = callbacks.onCompressorApply,
                onReset = callbacks.onCompressorReset,
                onEnabledChanged = callbacks.onCompressorEnabledChanged,
                onThresholdChanged = callbacks.onCompressorThresholdChanged,
                onMakeupGainChanged = callbacks.onCompressorMakeupGainChanged,
                onPresetSelected = callbacks.onCompressorPresetSelected,
            )

            // Full-Screen Borderless Dark Kyant Liquid Glass Subtitle Delay / Sync Dialog
            PlayerSubtitleOffsetDialog(
                visible = state.isSubtitleOffsetDialogVisible,
                state = state.subtitleOffsetDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onSubtitleOffsetDismiss,
                onApply = callbacks.onSubtitleOffsetApply,
                onReset = callbacks.onSubtitleOffsetReset,
                onInputChanged = callbacks.onSubtitleOffsetInputChanged,
                onStep = callbacks.onSubtitleOffsetStep,
                onCueClick = callbacks.onSubtitleOffsetCueClick,
            )

            // Kyant Liquid Glass Online Subtitles Picker
            PlayerOnlineSubtitlesDialog(
                visible = state.isOnlineSubtitlesDialogVisible,
                state = state.onlineSubtitlesDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onOnlineSubtitlesDismiss,
                onQueryChanged = callbacks.onOnlineSubtitlesQueryChanged,
                onSearchSubmit = callbacks.onOnlineSubtitlesSearchSubmit,
                onYearClick = callbacks.onOnlineSubtitlesYearClick,
                onLanguageClick = callbacks.onOnlineSubtitlesLanguageClick,
                onResultSelected = callbacks.onOnlineSubtitlesResultSelected,
                onApply = callbacks.onOnlineSubtitlesApply,
            )

            // Kyant Liquid Glass Quality Profile, Source Priority & Profile Settings
            PlayerQualityProfileDialog(
                visible = state.isQualityProfileDialogVisible,
                state = state.qualityProfileDialog,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onQualityProfileDismiss,
                onUseProfile = callbacks.onQualityProfileUse,
                onSelectProfile = callbacks.onQualityProfileSelected,
                onEditClick = callbacks.onQualityProfileEditClick,
                onSetDefaultClick = callbacks.onQualityProfileSetDefaultClick,
                onEditNameChanged = callbacks.onQualityProfileEditNameChanged,
                onSourcePriorityStep = callbacks.onQualityProfileSourcePriorityStep,
                onQualityPriorityStep = callbacks.onQualityProfileQualityPriorityStep,
                onEditSave = callbacks.onQualityProfileEditSave,
                onEditClose = callbacks.onQualityProfileEditClose,
                onHelpClick = callbacks.onQualityProfileHelpClick,
                onOpenSettings = callbacks.onQualityProfileSettingsOpen,
                onHideNegativeChanged = callbacks.onQualityProfileHideNegativeChanged,
                onHideErrorChanged = callbacks.onQualityProfileHideErrorChanged,
                onSettingsApply = callbacks.onQualityProfileSettingsApply,
                onSettingsCancel = callbacks.onQualityProfileSettingsCancel,
            )

            // Kyant Liquid Glass Subtitle Settings Sheet
            PlayerSubtitleSettingsDialog(
                visible = state.isSubtitleSettingsVisible,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onSubtitleSettingsDismiss,
            )

            // Kyant Liquid Glass Selection / Confirmation Popup (Highest Z-order)
            PlayerSelectionPopupDialog(
                state = state.selectionPopup,
                backdrop = activeBackdrop,
                onDismiss = callbacks.onSelectionPopupDismiss,
                onSingleSelect = callbacks.onSelectionPopupSingleSelect,
                onMultiApply = callbacks.onSelectionPopupMultiApply,
                onConfirm = callbacks.onSelectionPopupConfirm,
            )
        }
    }
}

@Composable
private fun TopLeftCluster(
    state: PlayerHudState,
    backdrop: PlatformLayerBackdrop?,
    showMediaInfoExpanded: Boolean,
    onToggleMediaInfo: () -> Unit,
    onBackClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassCircleVectorButton(
            backdrop = backdrop,
            imageVector = AppIcons.ArrowBack,
            contentDescription = stringResource(R.string.go_back_img_des),
            onClick = onBackClick,
        )

        Column(
            modifier = Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.title.isNotBlank()) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                if (state.showOfflinePin) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline_pin_24),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                if (state.isFiller) {
                    GlassSurface(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(50),
                        interactive = false,
                    ) {
                        Text(
                            text = stringResource(R.string.filler),
                            style = MaterialTheme.liquidGlassTypeScale.labelXs.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            if (state.sourceMetaText.isNotBlank() ||
                (state.mediaInfoText.isNotBlank() && showMediaInfoExpanded)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    if (state.sourceMetaText.isNotBlank()) {
                        GlassSurface(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(50),
                            interactive = state.mediaInfoText.isNotBlank(),
                            onClick = if (state.mediaInfoText.isNotBlank()) onToggleMediaInfo else null,
                        ) {
                            Text(
                                text = state.sourceMetaText,
                                style = MaterialTheme.liquidGlassTypeScale.labelSm,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (showMediaInfoExpanded && state.mediaInfoText.isNotBlank()) {
                        GlassSurface(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(50),
                            interactive = false,
                        ) {
                            Text(
                                text = state.mediaInfoText,
                                style = MaterialTheme.liquidGlassTypeScale.labelSm,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomHudDock(
    state: PlayerHudState,
    progress: PlaybackProgressSnapshot,
    backdrop: PlatformLayerBackdrop?,
    callbacks: PlayerHudCallbacks,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(state.isShowing, state.isLocked) {
        if ((!state.isShowing || state.isLocked) && isScrubbing) {
            isScrubbing = false
            previewBitmap = null
            callbacks.onScrubStateChanged(false)
        }
    }

    val durationMs = progress.durationMs.coerceAtLeast(0L)
    val playedFraction = if (durationMs > 0L) {
        (progress.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bufferedFraction = if (durationMs > 0L) {
        (progress.bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val activeFraction = if (isScrubbing) scrubFraction else playedFraction
    val displayedPosMs = if (isScrubbing && durationMs > 0L) {
        (scrubFraction * durationMs).roundToLong()
    } else {
        progress.positionMs
    }

    val previewBucket = (scrubFraction.coerceIn(0f, 1f) * 200f).roundToInt()
    LaunchedEffect(isScrubbing, previewBucket) {
        if (isScrubbing && callbacks.hasPreview()) {
            val bmp = withContext(Dispatchers.IO) {
                callbacks.getPreviewBitmap(scrubFraction.coerceIn(0f, 1f))
            }
            if (isScrubbing) {
                previewBitmap = bmp
            }
        } else if (!isScrubbing) {
            previewBitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { callbacks.onBottomDockHeightChanged(it.height) },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Timeline Scrubber Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatPlayerTime(displayedPosMs),
                style = MaterialTheme.liquidGlassTypeScale.timer.copy(fontSize = 13.sp),
                color = Color.White,
                modifier = Modifier.widthIn(min = 44.dp),
            )

            ScrubberTrackWithPreview(
                activeFraction = activeFraction,
                bufferedFraction = bufferedFraction,
                durationMs = durationMs,
                isScrubbing = isScrubbing,
                previewBitmap = previewBitmap,
                backdrop = backdrop,
                onScrubStart = { fraction ->
                    isScrubbing = true
                    scrubFraction = fraction
                    callbacks.onInteraction()
                    callbacks.onScrubStateChanged(true)
                },
                onScrubMove = { fraction ->
                    scrubFraction = fraction
                    callbacks.onInteraction()
                },
                onScrubEnd = { fraction ->
                    val targetMs = (fraction * durationMs).roundToLong().coerceIn(0L, durationMs)
                    isScrubbing = false
                    previewBitmap = null
                    callbacks.onSeekTo(targetMs)
                    callbacks.onScrubStateChanged(false)
                    callbacks.onInteraction()
                },
                onScrubCancel = {
                    isScrubbing = false
                    previewBitmap = null
                    callbacks.onScrubStateChanged(false)
                    callbacks.onInteraction()
                },
                modifier = Modifier.weight(1f),
            )

            if (progress.isAtLiveEdge) {
                GlassSurface(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(50),
                    interactive = false,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BoostRed),
                        )
                        Text(
                            text = stringResource(R.string.player_is_live),
                            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                    }
                }
            } else {
                val endLabel = if (state.durationModeRemaining && durationMs > 0L) {
                    "-${formatPlayerTime((durationMs - displayedPosMs).coerceAtLeast(0L))}"
                } else {
                    formatPlayerTime(durationMs)
                }
                Text(
                    text = endLabel,
                    style = MaterialTheme.liquidGlassTypeScale.timer.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            callbacks.onInteraction()
                            callbacks.onToggleDurationMode()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }

        // Bottom Corner-Split Action Clusters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bottom-Left: Lock, Rotate, Speed, Compressor
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleVectorButton(
                    backdrop = backdrop,
                    imageVector = PlayerHudIcons.LockOpen,
                    contentDescription = stringResource(R.string.video_lock),
                    size = 42.dp,
                    onClick = {
                        callbacks.onInteraction()
                        callbacks.onLockToggle()
                    },
                )

                if (state.isRotateEnabled) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.screen_rotation,
                        contentDescription = stringResource(R.string.rotate_video),
                        size = 42.dp,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onRotateClick()
                        },
                    )
                }

                if (state.isSpeedEnabled) {
                    val isDefaultSpeed = kotlin.math.abs(state.playbackSpeed - 1.0f) < 0.01f
                    if (isDefaultSpeed) {
                        GlassCircleIconButton(
                            backdrop = backdrop,
                            drawableRes = R.drawable.ic_baseline_speed_24,
                            contentDescription = formatSpeedLabel(state.playbackSpeed),
                            size = 42.dp,
                            onClick = {
                                callbacks.onInteraction()
                                callbacks.onSpeedClick()
                            },
                        )
                    } else {
                        GlassTextPill(
                            backdrop = backdrop,
                            text = formatSpeedLabel(state.playbackSpeed),
                            iconRes = R.drawable.ic_baseline_speed_24,
                            height = 42.dp,
                            onClick = {
                                callbacks.onInteraction()
                                callbacks.onSpeedClick()
                            },
                        )
                    }
                }

                if (state.isCompressorEnabled) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.ic_compressor_24,
                        contentDescription = stringResource(R.string.compressor),
                        size = 42.dp,
                        tint = if (state.isCompressorActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White
                        },
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onCompressorClick()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Bottom-Right: Resize, Subtitle Sync, Tracks, Sources, Next Episode
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isResizeEnabled) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.ic_baseline_aspect_ratio_24,
                        contentDescription = stringResource(R.string.video_aspect_ratio_resize),
                        size = 42.dp,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onResizeClick()
                        },
                    )
                }

                if (state.canOffsetSubtitles) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.ic_outline_subtitles_24,
                        contentDescription = stringResource(R.string.subtitle_offset),
                        size = 42.dp,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onSubtitleOffsetClick()
                        },
                    )
                }

                if (state.hasMultipleTracks) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.ic_baseline_equalizer_24,
                        contentDescription = stringResource(R.string.tracks),
                        size = 42.dp,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onTracksClick()
                        },
                    )
                }

                GlassCircleIconButton(
                    backdrop = backdrop,
                    drawableRes = R.drawable.ic_baseline_playlist_play_24,
                    contentDescription = stringResource(R.string.video_source),
                    size = 42.dp,
                    onClick = {
                        callbacks.onInteraction()
                        callbacks.onSourcesClick()
                    },
                )

                if (state.hasNextEpisode) {
                    GlassCircleIconButton(
                        backdrop = backdrop,
                        drawableRes = R.drawable.ic_baseline_skip_next_24,
                        contentDescription = stringResource(R.string.next_episode),
                        size = 42.dp,
                        onClick = {
                            callbacks.onInteraction()
                            callbacks.onNextEpisodeClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrubberTrackWithPreview(
    activeFraction: Float,
    bufferedFraction: Float,
    durationMs: Long,
    isScrubbing: Boolean,
    previewBitmap: Bitmap?,
    backdrop: PlatformLayerBackdrop?,
    onScrubStart: (Float) -> Unit,
    onScrubMove: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onScrubCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 5.dp else 3.dp,
        animationSpec = tween(120),
        label = "scrubberTrackHeight",
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isScrubbing) 8.dp else 6.dp,
        animationSpec = tween(120),
        label = "scrubberThumbRadius",
    )
    val accentColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.height(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val previewWidthDp = (maxWidth * 0.25f)
            .coerceIn(
                minimumValue = LiquidGlassTokens.Space.s96 + LiquidGlassTokens.Space.s48,
                maximumValue = LiquidGlassTokens.Space.s96 + LiquidGlassTokens.Space.s96,
            )
            .coerceAtMost(maxWidth)
        val previewWidthPx = with(density) { previewWidthDp.toPx() }
        val previewLiftPx = with(density) {
            (LiquidGlassTokens.Space.s32 + LiquidGlassTokens.Space.s10).roundToPx()
        }
        val targetX = (activeFraction * trackWidthPx - previewWidthPx / 2f)
            .coerceIn(0f, (trackWidthPx - previewWidthPx).coerceAtLeast(0f))

        if (isScrubbing && previewBitmap != null) {
            GlassSurface(
                backdrop = backdrop,
                shape = RoundedCornerShape(LiquidGlassTokens.Radius.lg),
                interactive = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        IntOffset(
                            x = targetX.roundToInt(),
                            y = -previewLiftPx,
                        )
                    }
                    .wrapContentHeight(unbounded = true, align = Alignment.Bottom)
                    .width(previewWidthDp),
            ) {
                Column(
                    modifier = Modifier.padding(LiquidGlassTokens.Space.s4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s3),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(LiquidGlassTokens.Radius.md)),
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = formatPlayerTime((activeFraction * durationMs).roundToLong()),
                        style = MaterialTheme.liquidGlassTypeScale.timer.copy(
                            fontSize = LiquidGlassTokens.Type.labelXs,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var fraction = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrubStart(fraction)

                        var pointerId = down.id
                        var completed = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.fastFirstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            pointerId = change.id
                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                onScrubEnd(fraction)
                                completed = true
                                break
                            }
                            if (change.isConsumed) {
                                break
                            }
                            change.consume()
                            fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onScrubMove(fraction)
                        }
                        if (!completed) {
                            onScrubCancel()
                        }
                    }
                },
        ) {
            val cy = size.height / 2f
            val h = trackHeight.toPx()
            val radius = thumbRadius.toPx()
            val corner = CornerRadius(h / 2f, h / 2f)
            val usableWidth = size.width

            drawRoundRect(
                color = Color.White.copy(alpha = 0.24f),
                topLeft = Offset(0f, cy - h / 2f),
                size = Size(usableWidth, h),
                cornerRadius = corner,
            )

            val bufferedW = (usableWidth * bufferedFraction.coerceIn(0f, 1f))
            if (bufferedW > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(0f, cy - h / 2f),
                    size = Size(bufferedW, h),
                    cornerRadius = corner,
                )
            }

            val playedX = (usableWidth * activeFraction.coerceIn(0f, 1f))
            if (playedX > 0f) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(0f, cy - h / 2f),
                    size = Size(playedX, h),
                    cornerRadius = corner,
                )
            }

            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(playedX.coerceIn(radius, (usableWidth - radius).coerceAtLeast(radius)), cy),
            )
        }
    }
}

@Composable
private fun VerticalLevelCapsule(
    backdrop: PlatformLayerBackdrop?,
    level: Float,
    iconRes: Int,
    boostColor: Color,
) {
    val baseFraction by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(70),
        label = "baseLevelFraction",
    )
    val boostFraction by animateFloatAsState(
        targetValue = (level - 1f).coerceIn(0f, 1f),
        animationSpec = tween(70),
        label = "boostLevelFraction",
    )

    GlassSurface(
        backdrop = backdrop,
        shape = RoundedCornerShape(50),
        interactive = false,
        modifier = Modifier
            .width(42.dp)
            .height(164.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (boostFraction > 0.01f) boostColor else Color.White,
                modifier = Modifier.size(18.dp),
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .width(5.dp),
            ) {
                val trackW = size.width
                val trackH = size.height
                val cx = trackW / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(cx, trackH),
                    end = Offset(cx, 0f),
                    strokeWidth = trackW,
                    cap = StrokeCap.Round,
                )
                if (baseFraction > 0.005f) {
                    val fillTop = trackH * (1f - baseFraction)
                    drawLine(
                        color = Color.White,
                        start = Offset(cx, trackH),
                        end = Offset(cx, fillTop),
                        strokeWidth = trackW,
                        cap = StrokeCap.Round,
                    )
                }
                if (boostFraction > 0.005f) {
                    val boostTop = trackH * (1f - boostFraction)
                    drawLine(
                        color = boostColor,
                        start = Offset(cx, trackH),
                        end = Offset(cx, boostTop),
                        strokeWidth = trackW,
                        cap = StrokeCap.Round,
                    )
                }
            }

            Text(
                text = "${(level.coerceIn(0f, 2f) * 100f).roundToInt()}%",
                style = MaterialTheme.liquidGlassTypeScale.labelXs.copy(
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = if (boostFraction > 0.01f) boostColor else Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun TorrentPopoverCard(
    backdrop: PlatformLayerBackdrop?,
    torrent: TorrentProgressState,
) {
    GlassSurface(
        backdrop = backdrop,
        shape = RoundedCornerShape(16.dp),
        interactive = false,
        modifier = Modifier
            .width(220.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (torrent.downloadedText.isNotBlank()) {
                Text(
                    text = torrent.downloadedText,
                    style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (torrent.showSpeed && torrent.speedText.isNotBlank()) {
                Text(
                    text = torrent.speedText,
                    style = MaterialTheme.liquidGlassTypeScale.labelXs.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val fraction = torrent.progressFraction
            if (fraction == null) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                )
            } else {
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Composable
private fun GlassCircleVectorButton(
    backdrop: PlatformLayerBackdrop?,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    tint: Color = Color.White,
) {
    GlassSurface(
        backdrop = backdrop,
        shape = CircleShape,
        interactive = true,
        onClick = onClick,
        modifier = Modifier.size(size),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun GlassCircleIconButton(
    backdrop: PlatformLayerBackdrop?,
    drawableRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    tint: Color = Color.White,
) {
    GlassSurface(
        backdrop = backdrop,
        shape = CircleShape,
        interactive = true,
        onClick = onClick,
        modifier = Modifier.size(size),
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun GlassTextPill(
    backdrop: PlatformLayerBackdrop?,
    text: String,
    onClick: () -> Unit,
    iconRes: Int? = null,
    height: Dp = 38.dp,
) {
    GlassSurface(
        backdrop = backdrop,
        shape = RoundedCornerShape(50),
        interactive = true,
        onClick = onClick,
        modifier = Modifier.height(height),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GlassSurface(
    backdrop: PlatformLayerBackdrop?,
    shape: Shape,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isSupported = backdrop != null && isLiquidGlassBackdropSupported()
    val glassModifier = modifier
        .liquidGlass(
            backdrop = backdrop,
            shape = shape,
            interactive = interactive,
        )
        .then(
            if (!isSupported) {
                Modifier.border(0.75.dp, FallbackBorder, shape)
            } else {
                Modifier
            },
        )
        .then(
            if (onClick != null) {
                Modifier
                    .clip(shape)
                    .clickable(
                        role = Role.Button,
                        onClick = onClick,
                    )
            } else {
                Modifier
            },
        )

    Box(
        modifier = glassModifier,
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun DoubleTapSeekOverlay(
    feedback: DoubleTapSeekFeedback?,
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        DoubleTapSeekSideSlot(
            isForward = false,
            feedback = feedback,
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        DoubleTapSeekSideSlot(
            isForward = true,
            feedback = feedback,
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun DoubleTapSeekSideSlot(
    isForward: Boolean,
    feedback: DoubleTapSeekFeedback?,
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val activeFeedback = feedback?.takeIf { it.isForward == isForward }
    val isActiveForSide = activeFeedback != null
    var lastSeconds by remember { mutableIntStateOf(10) }
    var lastToken by remember { mutableLongStateOf(0L) }
    if (activeFeedback != null) {
        lastSeconds = activeFeedback.seconds
        lastToken = activeFeedback.token
    }

    val density = LocalDensity.current
    val glowProgress = remember { Animatable(1f) }
    val nudgeOffsetPx = remember { Animatable(0f) }

    LaunchedEffect(lastToken) {
        if (lastToken > 0L) {
            glowProgress.snapTo(0f)
            glowProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(lastToken) {
        if (lastToken > 0L) {
            val kickPx = with(density) { (if (isForward) 8.dp else (-8).dp).toPx() }
            nudgeOffsetPx.animateTo(
                targetValue = kickPx,
                animationSpec = tween(durationMillis = 65, easing = FastOutSlowInEasing),
            )
            nudgeOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Subtle wide elliptical radial gradient flash behind the Liquid Glass pill
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = glowProgress.value
            if (t < 1f) {
                val alpha = 0.12f * (1f - t)
                val centerX = if (isForward) size.width else 0f
                val centerY = size.height * 0.5f
                val radius = size.width * (0.55f + 0.45f * t)
                if (radius > 0f && alpha > 0.001f) {
                    val scaleY = (size.height / size.width.coerceAtLeast(1f)).coerceAtLeast(1f) * 1.25f
                    scale(scaleX = 1f, scaleY = scaleY, pivot = Offset(centerX, centerY)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = alpha),
                                    Color.White.copy(alpha = alpha * 0.35f),
                                    Color.Transparent,
                                ),
                                center = Offset(centerX, centerY),
                                radius = radius,
                            ),
                            radius = radius,
                            center = Offset(centerX, centerY),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isActiveForSide,
            enter = fadeIn(tween(150)) + scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
            exit = fadeOut(tween(220)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(220),
            ),
            modifier = Modifier.graphicsLayer {
                translationX = nudgeOffsetPx.value
            },
        ) {
            GlassSurface(
                backdrop = backdrop,
                shape = CircleShape,
                interactive = false,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!isForward) {
                        CascadingSeekChevrons(isForward = false)
                    }

                    AnimatedContent(
                        targetState = lastSeconds,
                        transitionSpec = {
                            (slideInVertically(tween(140)) { height -> -height / 2 } + fadeIn(tween(140)))
                                .togetherWith(
                                    slideOutVertically(tween(110)) { height -> height / 2 } + fadeOut(tween(110)),
                                )
                        },
                        label = "doubleTapSeekSeconds",
                    ) { secs ->
                        Text(
                            text = if (isForward) "+${secs}s" else "-${secs}s",
                            style = MaterialTheme.liquidGlassTypeScale.timer.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = Color.White,
                        )
                    }

                    if (isForward) {
                        CascadingSeekChevrons(isForward = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun CascadingSeekChevrons(
    isForward: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seekChevronWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chevronWavePhase",
    )
    val density = LocalDensity.current
    val maxShiftPx = with(density) { 3.dp.toPx() } * (if (isForward) 1f else -1f)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        for (index in 0..2) {
            val staggerOrder = if (isForward) index else (2 - index)
            val shifted = (wavePhase - staggerOrder * 0.22f + 1f) % 1f
            val pulse = 0.5f - 0.5f * cos(shifted * 2f * PI.toFloat())
            val chevronAlpha = 0.25f + 0.75f * pulse
            val chevronShiftPx = maxShiftPx * pulse

            Canvas(
                modifier = Modifier
                    .size(width = 7.dp, height = 12.dp)
                    .graphicsLayer {
                        alpha = chevronAlpha
                        translationX = chevronShiftPx
                    },
            ) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    if (isForward) {
                        moveTo(w * 0.22f, h * 0.14f)
                        lineTo(w * 0.82f, h * 0.5f)
                        lineTo(w * 0.22f, h * 0.86f)
                    } else {
                        moveTo(w * 0.78f, h * 0.14f)
                        lineTo(w * 0.18f, h * 0.5f)
                        lineTo(w * 0.78f, h * 0.86f)
                    }
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = 1.9.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
