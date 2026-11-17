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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale

private val SelectedRowShape = RoundedCornerShape(LiquidGlassTokens.Radius.lg)
private val PillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

@Composable
fun PlayerTracksDialog(
    visible: Boolean,
    state: PlayerTracksDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onVideoTrackSelected: (Int) -> Unit,
    onAudioTrackSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val glassTokens = MaterialTheme.liquidGlass
    val sheetShape = glassTokens.shapes.sheet

    val startInset = safePadding.calculateStartPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val endInset = safePadding.calculateEndPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val topInset = safePadding.calculateTopPadding() + LiquidGlassTokens.Space.s12
    val bottomInset = safePadding.calculateBottomPadding() + LiquidGlassTokens.Space.s12

    val videoTracksListState = rememberLazyListState()
    val audioTracksListState = rememberLazyListState()

    LaunchedEffect(visible, state.scrollTriggerToken) {
        if (!visible) return@LaunchedEffect
        if (state.selectedVideoTrackIndex >= 0 && state.selectedVideoTrackIndex < state.videoTracks.size) {
            videoTracksListState.scrollToItem(state.selectedVideoTrackIndex)
        }
        if (state.selectedAudioTrackIndex >= 0 && state.selectedAudioTrackIndex < state.audioTracks.size) {
            audioTracksListState.scrollToItem(state.selectedAudioTrackIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LiquidGlassTokens.Motion.normalMillis)) +
                scaleIn(tween(LiquidGlassTokens.Motion.normalMillis), initialScale = 0.96f),
            exit = fadeOut(tween(LiquidGlassTokens.Motion.fastMillis)) +
                scaleOut(tween(LiquidGlassTokens.Motion.fastMillis), targetScale = 0.96f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = startInset,
                        top = topInset,
                        end = endInset,
                        bottom = bottomInset,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(sheetShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    KyantPlayerGlassChromeLayer(
                        shape = sheetShape,
                        backdrop = backdrop,
                        fallbackCornerRadius = LiquidGlassTokens.Radius.sheet,
                        modifier = Modifier.matchParentSize(),
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                glassTokens.colors.surfaceSheet.copy(
                                    alpha = glassTokens.opacity.overlayLight,
                                ),
                            )
                            .padding(LiquidGlassTokens.Space.s12),
                    ) {
                        val useSplitLayout = maxWidth >= LiquidGlassTokens.Breakpoint.largePhone

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (useSplitLayout) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    if (state.hasVideoTracks) {
                                        TracksPane(
                                            title = stringResource(R.string.video_tracks),
                                            items = state.videoTracks,
                                            selectedIndex = state.selectedVideoTrackIndex,
                                            listState = videoTracksListState,
                                            keyPrefix = "vid_track",
                                            onItemSelected = onVideoTrackSelected,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        )
                                    }

                                    if (state.hasVideoTracks && state.hasAudioTracks) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .padding(vertical = LiquidGlassTokens.Space.s4)
                                                .width(LiquidGlassTokens.Border.thin)
                                            .background(PlayerGlassWhiteDivider),
                                        )
                                    }

                                    if (state.hasAudioTracks) {
                                        TracksPane(
                                            title = stringResource(R.string.audio_tracks),
                                            items = state.audioTracks,
                                            selectedIndex = state.selectedAudioTrackIndex,
                                            listState = audioTracksListState,
                                            keyPrefix = "aud_track",
                                            onItemSelected = onAudioTrackSelected,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    if (state.hasVideoTracks) {
                                        TracksPane(
                                            title = stringResource(R.string.video_tracks),
                                            items = state.videoTracks,
                                            selectedIndex = state.selectedVideoTrackIndex,
                                            listState = videoTracksListState,
                                            keyPrefix = "vid_track",
                                            onItemSelected = onVideoTrackSelected,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                        )
                                    }

                                    if (state.hasVideoTracks && state.hasAudioTracks) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(LiquidGlassTokens.Border.thin)
                                            .background(PlayerGlassWhiteDivider),
                                        )
                                    }

                                    if (state.hasAudioTracks) {
                                        TracksPane(
                                            title = stringResource(R.string.audio_tracks),
                                            items = state.audioTracks,
                                            selectedIndex = state.selectedAudioTrackIndex,
                                            listState = audioTracksListState,
                                            keyPrefix = "aud_track",
                                            onItemSelected = onAudioTrackSelected,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                        )
                                    }
                                }
                            }

                            // Bottom action bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = LiquidGlassTokens.Space.s10),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FrostedTrackActionPill(
                                    label = stringResource(R.string.sort_cancel),
                                    isPrimary = false,
                                    onClick = onDismiss,
                                )
                                Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
                                FrostedTrackActionPill(
                                    label = stringResource(R.string.sort_apply),
                                    isPrimary = true,
                                    onClick = onApply,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TracksPane(
    title: String,
    items: List<TrackItemUi>,
    selectedIndex: Int,
    listState: LazyListState,
    keyPrefix: String,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LiquidGlassTokens.Space.s14,
                    vertical = LiquidGlassTokens.Space.s10,
                )
                .heightIn(min = LiquidGlassTokens.Space.s32),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

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
                .tracksEdgeScrollFade(maskColor),
            contentPadding = PaddingValues(
                horizontal = LiquidGlassTokens.Space.s8,
                vertical = LiquidGlassTokens.Space.s12,
            ),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
        ) {
            items(
                items = items,
                key = { item -> "${keyPrefix}_${item.index}" },
            ) { item ->
                val isSelected = item.index == selectedIndex
                SelectableTrackGlassRow(
                    label = item.label,
                    selected = isSelected,
                    onClick = { onItemSelected(item.index) },
                )
            }
        }
    }
}

private fun Modifier.tracksEdgeScrollFade(maskColor: Color): Modifier =
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            if (size.height > 0f) {
                val topFadeFraction =
                    (LiquidGlassTokens.Space.s18.toPx() / size.height).coerceIn(0f, 0.2f)
                val bottomFadeFraction =
                    (LiquidGlassTokens.Space.s24.toPx() / size.height).coerceIn(0f, 0.25f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        topFadeFraction to maskColor,
                        (1f - bottomFadeFraction) to maskColor,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

@Composable
private fun SelectableTrackGlassRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val rowModifier = if (selected) {
        Modifier
            .fillMaxWidth()
            .clip(SelectedRowShape)
            .background(glassTokens.colors.overlaySelected)
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteActiveRowBorder,
                shape = SelectedRowShape,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .clip(SelectedRowShape)
    }

    Row(
        modifier = rowModifier
            .clickable(
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(
                horizontal = LiquidGlassTokens.Space.s12,
                vertical = LiquidGlassTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) glassTokens.colors.textPrimary else glassTokens.colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (selected) {
            Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s8))
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = glassTokens.colors.textPrimary.copy(alpha = 0.90f),
                modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
            )
        }
    }
}

@Composable
private fun FrostedTrackActionPill(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val bgModifier = if (isPrimary) {
        Modifier
            .clip(PillShape)
            .background(glassTokens.colors.textPrimary)
    } else {
        Modifier
            .clip(PillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = PillShape,
            )
    }

    Box(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s36)
            .then(bgModifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s20),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isPrimary) {
                glassTokens.colors.textInverse
            } else {
                glassTokens.colors.textPrimary
            },
            maxLines = 1,
        )
    }
}
