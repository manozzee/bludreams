package com.lagradost.cloudstream3.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.lagradost.cloudstream3.ui.liquidglass.CircularActionButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.ui.liquidglass.HERO_BACKGROUND_SCALE
import com.lagradost.cloudstream3.ui.liquidglass.heroBackgroundScrollScale
import com.lagradost.cloudstream3.ui.liquidglass.heroBackgroundScrollTranslationY
import com.lagradost.cloudstream3.ui.liquidglass.heroStretchHeight
import com.lagradost.cloudstream3.ui.liquidglass.heroStretchZoom
import com.lagradost.cloudstream3.ui.liquidglass.homeHeroLayout
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight

internal val DetailHeroBaseHeight: Dp = 420.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailHero(
    title: String,
    backdropUrl: String?,
    posterUrl: String?,
    logoUrl: String?,
    trailerMirror: TrailerMirror?,
    isTrailerPlaying: Boolean,
    isTrailerReady: Boolean,
    isTrailerMuted: Boolean,
    stretchPx: () -> Float,
    scrollOffsetPx: Float,
    onTrailerReady: () -> Unit,
    onTrailerEnded: () -> Unit,
    onTrailerError: () -> Unit,
    onToggleTrailerMute: () -> Unit,
    onTitleLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showVideo = trailerMirror != null && isTrailerPlaying && isTrailerReady
    val videoAlpha by animateFloatAsState(
        targetValue = if (showVideo) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "detailHeroTrailerAlpha",
    )
    val backdropAlpha by animateFloatAsState(
        targetValue = if (showVideo) 0f else 1f,
        animationSpec = tween(durationMillis = 520),
        label = "detailHeroBackdropAlpha",
    )

    var logoLoadFailed by remember(logoUrl, title) { mutableStateOf(false) }
    val resolvedBackdrop = backdropUrl?.takeIf { it.isNotBlank() } ?: posterUrl
    val context = LocalContext.current
    val density = LocalDensity.current
    val insetsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = remember(insetsPadding, context, density) {
        if (insetsPadding > 0.dp) insetsPadding else with(density) { context.getStatusBarHeight().toDp() }
    }

    val baseHeroHeight = DetailHeroBaseHeight + statusBarPadding

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .heroStretchHeight(baseHeroHeight, stretchPx),
    ) {
        val layout = remember(maxWidth) {
            homeHeroLayout(maxWidthDp = maxWidth.value)
        }
        val bottomFadeHeight = layout.bottomFadeHeight
        val scrollScale = heroBackgroundScrollScale(scrollOffsetPx)
        val scrollTranslationY = heroBackgroundScrollTranslationY(scrollOffsetPx)

        // Background media layer (Backdrop + Background Trailer ExoPlayer Surface)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .heroStretchZoom(stretchPx),
        ) {
            if (!resolvedBackdrop.isNullOrBlank()) {
                AsyncImage(
                    model = resolvedBackdrop,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = backdropAlpha
                            translationY = scrollTranslationY
                            scaleX = HERO_BACKGROUND_SCALE * scrollScale
                            scaleY = HERO_BACKGROUND_SCALE * scrollScale
                        },
                    contentScale = ContentScale.Crop,
                    alignment = if (layout.isTablet) Alignment.TopCenter else Alignment.Center,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.background,
                                ),
                            ),
                        ),
                )
            }

            if (trailerMirror != null) {
                HeroTrailerPlayerSurface(
                    videoUrl = trailerMirror.link.url,
                    audioTracks = trailerMirror.link.audioTracks,
                    headers = trailerMirror.link.headers,
                    referer = trailerMirror.link.referer,
                    isPlaying = isTrailerPlaying,
                    isMuted = isTrailerMuted,
                    onReady = onTrailerReady,
                    onEnded = onTrailerEnded,
                    onError = onTrailerError,
                    cropToFill = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = videoAlpha
                            translationY = scrollTranslationY
                            scaleX = HERO_BACKGROUND_SCALE * scrollScale
                            scaleY = HERO_BACKGROUND_SCALE * scrollScale
                        },
                )
            }
        }

        // Top subtle vignette for floating buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding + 110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.54f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Multi-stop vertical gradient for smooth background transition (matching HomeHeroSection)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        // Bottom fade layer matching HomeHeroSection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        // Foreground Clearlogo or Title at bottom of Hero
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val validLogo = logoUrl?.takeIf { it.isNotBlank() && !logoLoadFailed }
            if (validLogo != null) {
                AsyncImage(
                    model = validLogo,
                    contentDescription = title,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .fillMaxWidth(0.68f)
                        .height(84.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onTitleLongClick,
                        ),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                    onError = { logoLoadFailed = true },
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onTitleLongClick,
                    ),
                )
            }
        }
    }
}

@Composable
fun DetailFloatingHeader(
    title: String,
    collapseProgress: Float,
    showTrailerMuteButton: Boolean,
    isTrailerMuted: Boolean,
    showSyncButton: Boolean,
    onBackClick: () -> Unit,
    onToggleTrailerMute: () -> Unit,
    onSyncClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val insetsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = remember(insetsPadding, context, density) {
        if (insetsPadding > 0.dp) insetsPadding else with(density) { context.getStatusBarHeight().toDp() }
    }
    val clampedProgress = collapseProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.background.copy(alpha = clampedProgress * 0.94f),
            )
            .padding(top = statusBarPadding + 14.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBackClick,
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        alpha = ((clampedProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedVisibility(
                    visible = showTrailerMuteButton,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150)),
                ) {
                    CircularActionButton(
                        icon = if (isTrailerMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (isTrailerMuted) "Unmute Trailer" else "Mute Trailer",
                        onClick = onToggleTrailerMute,
                    )
                }

                if (showSyncButton) {
                    CircularActionButton(
                        icon = Icons.Default.Sync,
                        contentDescription = "Sync",
                        onClick = onSyncClick,
                    )
                }

                CircularActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    onClick = onShareClick,
                )
            }
        }
    }
}

