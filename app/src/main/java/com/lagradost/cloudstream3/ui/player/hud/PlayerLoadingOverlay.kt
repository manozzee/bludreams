package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.liquidglass.LocalLiquidGlassDrawTick
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.isLiquidGlassBackdropSupported
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassSource

private val LoadingFallbackBorder = Color.White.copy(alpha = 0.22f)

@Immutable
data class PlayerLoadingOverlayState(
    val canSkip: Boolean = false,
    val usableLinks: Int = 0,
)

@Composable
fun PlayerLoadingOverlay(
    state: PlayerLoadingOverlayState,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useLiquidGlass = isLiquidGlassBackdropSupported()
    val drawTickState = remember { mutableIntStateOf(0) }
    val layerBackdrop = rememberLayerBackdrop {
        @Suppress("UNUSED_VARIABLE")
        val tick = drawTickState.intValue
        drawRect(Color.Black)
    }
    val activeBackdrop: PlatformLayerBackdrop? = if (useLiquidGlass) layerBackdrop else null

    LaunchedEffect(useLiquidGlass, state.canSkip) {
        if (useLiquidGlass) {
            repeat(2) {
                withFrameNanos {
                    drawTickState.intValue++
                }
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val startInset = safePadding.calculateStartPadding(layoutDirection)
    val endInset = safePadding.calculateEndPadding(layoutDirection)
    val symmetricalHorizontalPadding = maxOf(startInset, endInset) + 16.dp
    val topEdgePadding = safePadding.calculateTopPadding() + 12.dp
    val backgroundInteraction = remember { MutableInteractionSource() }

    CompositionLocalProvider(LocalLiquidGlassDrawTick provides drawTickState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = backgroundInteraction,
                    indication = null,
                ) {},
        ) {
            if (useLiquidGlass) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlassSource(layerBackdrop),
                )
            }

            // Top-Left Kyant Liquid Glass Back Button (matches main HUD position)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = symmetricalHorizontalPadding,
                        top = topEdgePadding,
                    ),
            ) {
                LoadingGlassSurface(
                    backdrop = activeBackdrop,
                    shape = CircleShape,
                    interactive = true,
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.go_back_img_des),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Center Kyant Liquid Glass Buffering Orb
            LoadingGlassSurface(
                backdrop = activeBackdrop,
                shape = CircleShape,
                interactive = false,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp),
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Kyant Liquid Glass Skip Loading Pill
            AnimatedVisibility(
                visible = state.canSkip,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.94f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 70.dp),
            ) {
                val skipText = if (state.usableLinks > 0) {
                    "${stringResource(R.string.skip_loading)} (${state.usableLinks})"
                } else {
                    stringResource(R.string.skip_loading)
                }
                LoadingGlassSurface(
                    backdrop = activeBackdrop,
                    shape = CircleShape,
                    interactive = true,
                    onClick = onSkipClick,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_skip_next_24),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = skipText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingGlassSurface(
    backdrop: PlatformLayerBackdrop?,
    shape: Shape,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isSupported = (backdrop != null) && isLiquidGlassBackdropSupported()
    val glassModifier = modifier
        .liquidGlass(
            backdrop = backdrop,
            shape = shape,
            interactive = interactive,
        )
        .then(
            if (!isSupported) {
                Modifier.border(0.75.dp, LoadingFallbackBorder, shape)
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
