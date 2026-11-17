package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import com.lagradost.cloudstream3.ui.player.SubtitleCue

private val SelectedCueShape = RoundedCornerShape(LiquidGlassTokens.Radius.lg)
private val PillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

private enum class SubtitleStepKind {
    CoarseDecrease,
    FineDecrease,
    FineIncrease,
    CoarseIncrease,
}

@Composable
fun PlayerSubtitleOffsetDialog(
    visible: Boolean,
    state: PlayerSubtitleOffsetDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onInputChanged: (String) -> Unit,
    onStep: (Long) -> Unit,
    onCueClick: (SubtitleCue) -> Unit,
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

    val cueListState = rememberLazyListState()

    LaunchedEffect(visible, state.scrollTriggerToken) {
        if (!visible || state.cues.isEmpty()) return@LaunchedEffect
        val targetIndex = state.activeCueIndex.coerceIn(0, state.cues.lastIndex)
        cueListState.scrollToItem(targetIndex)
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

                        if (useSplitLayout) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                SubtitleCueTimelinePane(
                                    cues = state.cues,
                                    timelinePositionMs = state.timelinePositionMs,
                                    listState = cueListState,
                                    onCueClick = onCueClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = LiquidGlassTokens.Space.s4)
                                        .width(LiquidGlassTokens.Border.thin)
                                        .background(PlayerGlassWhiteDivider),
                                )

                                SubtitleDelayControlsPane(
                                    state = state,
                                    onInputChanged = onInputChanged,
                                    onStep = onStep,
                                    onReset = onReset,
                                    onDismiss = onDismiss,
                                    onApply = onApply,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubtitleCueTimelinePane(
                                    cues = state.cues,
                                    timelinePositionMs = state.timelinePositionMs,
                                    listState = cueListState,
                                    onCueClick = onCueClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(LiquidGlassTokens.Border.thin)
                                        .background(PlayerGlassWhiteDivider),
                                )

                                SubtitleDelayControlsPane(
                                    state = state,
                                    onInputChanged = onInputChanged,
                                    onStep = onStep,
                                    onReset = onReset,
                                    onDismiss = onDismiss,
                                    onApply = onApply,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
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
private fun SubtitleCueTimelinePane(
    cues: List<SubtitleCue>,
    timelinePositionMs: Long,
    listState: LazyListState,
    onCueClick: (SubtitleCue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (cues.isEmpty()) {
            Text(
                text = stringResource(R.string.no_subtitles_loaded),
                style = MaterialTheme.liquidGlassTypeScale.bodyLg.copy(
                    fontStyle = FontStyle.Italic,
                ),
                color = glassTokens.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = LiquidGlassTokens.Space.s16),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .subtitleCueEdgeScrollFade(maskColor),
                contentPadding = PaddingValues(
                    horizontal = LiquidGlassTokens.Space.s8,
                    vertical = LiquidGlassTokens.Space.s12,
                ),
                verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s4),
            ) {
                itemsIndexed(
                    items = cues,
                    key = { index, cue -> "${index}_${cue.startTimeMs}" },
                ) { _, cue ->
                    val startTime = cue.startTimeMs
                    val endTime = cue.endTimeMs
                    val isActive = timelinePositionMs in startTime..<endTime
                    val isReached = timelinePositionMs >= startTime
                    val targetProgress = if (isActive && cue.durationMs > 0L) {
                        ((timelinePositionMs - startTime).toFloat() / cue.durationMs.toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    SubtitleCueGlassRow(
                        cue = cue,
                        isActive = isActive,
                        isReached = isReached,
                        progress = targetProgress,
                        onClick = { onCueClick(cue) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleCueGlassRow(
    cue: SubtitleCue,
    isActive: Boolean,
    isReached: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = LiquidGlassTokens.Motion.fastMillis),
        label = "cueProgress",
    )

    val rowModifier = if (isActive) {
        Modifier
            .fillMaxWidth()
            .clip(SelectedCueShape)
            .background(glassTokens.colors.overlaySelected)
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteActiveRowBorder,
                shape = SelectedCueShape,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .clip(SelectedCueShape)
    }

    val textColor = when {
        isActive -> glassTokens.colors.textPrimary
        isReached -> glassTokens.colors.textSecondary
        else -> glassTokens.colors.textMuted
    }

    Column(
        modifier = rowModifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = LiquidGlassTokens.Space.s14,
                vertical = LiquidGlassTokens.Space.s10,
            ),
    ) {
        Text(
            text = cue.text.joinToString("\n"),
            style = MaterialTheme.liquidGlassTypeScale.bodyLg.copy(
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = textColor,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        if (isActive) {
            Spacer(modifier = Modifier.height(LiquidGlassTokens.Space.s8))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LiquidGlassTokens.Space.s4)
                    .clip(PillShape)
                    .background(glassTokens.colors.playerTimelineTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .background(glassTokens.colors.textPrimary.copy(alpha = 0.90f)),
                )
            }
        }
    }
}

private fun Modifier.subtitleCueEdgeScrollFade(maskColor: Color): Modifier =
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
private fun SubtitleDelayControlsPane(
    state: PlayerSubtitleOffsetDialogState,
    onInputChanged: (String) -> Unit,
    onStep: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(start = LiquidGlassTokens.Space.s16),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.subtitle_offset_title),
                    style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = glassTokens.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(LiquidGlassTokens.Space.s6))

                Text(
                    text = state.statusText,
                    style = MaterialTheme.liquidGlassTypeScale.bodySm,
                    color = glassTokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(LiquidGlassTokens.Space.s24))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
                ) {
                    FrostedSubtitleStepCircle(
                        kind = SubtitleStepKind.CoarseDecrease,
                        contentDescription = "-1000 ms",
                        onClick = {
                            focusManager.clearFocus()
                            onStep(-1000L)
                        },
                    )

                    FrostedSubtitleStepCircle(
                        kind = SubtitleStepKind.FineDecrease,
                        contentDescription = "-100 ms",
                        onClick = {
                            focusManager.clearFocus()
                            onStep(-100L)
                        },
                    )

                    Box(
                        modifier = Modifier
                            .height(LiquidGlassTokens.Space.s40)
                            .widthIn(
                                min = LiquidGlassTokens.Space.s96,
                                max = LiquidGlassTokens.Space.s96 + LiquidGlassTokens.Space.s32,
                            )
                            .clip(PillShape)
                            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
                            .border(
                                width = LiquidGlassTokens.Border.thin,
                                brush = PlayerGlassWhiteBorderBrush,
                                shape = PillShape,
                            )
                            .padding(horizontal = LiquidGlassTokens.Space.s14),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicTextField(
                            value = state.inputText,
                            onValueChange = onInputChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.liquidGlassTypeScale.bodyLg.copy(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                                color = glassTokens.colors.textPrimary,
                                textAlign = TextAlign.Center,
                            ),
                            cursorBrush = SolidColor(glassTokens.colors.textPrimary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.inputText.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.subtitle_offset_hint),
                                            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                                fontFeatureSettings = "tnum",
                                            ),
                                            color = glassTokens.colors.textMuted,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }

                    FrostedSubtitleStepCircle(
                        kind = SubtitleStepKind.FineIncrease,
                        contentDescription = "+100 ms",
                        onClick = {
                            focusManager.clearFocus()
                            onStep(100L)
                        },
                    )

                    FrostedSubtitleStepCircle(
                        kind = SubtitleStepKind.CoarseIncrease,
                        contentDescription = "+1000 ms",
                        onClick = {
                            focusManager.clearFocus()
                            onStep(1000L)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LiquidGlassTokens.Space.s10),
            horizontalArrangement = Arrangement.spacedBy(
                space = LiquidGlassTokens.Space.s8,
                alignment = Alignment.End,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrostedSubtitleActionPill(
                label = stringResource(R.string.reset_btn),
                isPrimary = false,
                onClick = {
                    focusManager.clearFocus()
                    onReset()
                },
            )
            FrostedSubtitleActionPill(
                label = stringResource(R.string.sort_cancel),
                isPrimary = false,
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
            )
            FrostedSubtitleActionPill(
                label = stringResource(R.string.sort_apply),
                isPrimary = true,
                onClick = {
                    focusManager.clearFocus()
                    onApply()
                },
            )
        }
    }
}

@Composable
private fun FrostedSubtitleStepCircle(
    kind: SubtitleStepKind,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val iconColor = glassTokens.colors.textPrimary.copy(alpha = 0.92f)
    Box(
        modifier = Modifier
            .size(LiquidGlassTokens.Space.s40)
            .clip(CircleShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.thin,
                brush = PlayerGlassWhiteBorderBrush,
                shape = CircleShape,
            )
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(LiquidGlassTokens.Icon.sm)) {
            val stroke = LiquidGlassTokens.Space.s2.toPx()
            val inset = size.minDimension * 0.18f
            val cx = size.width / 2f
            val cy = size.height / 2f

            when (kind) {
                SubtitleStepKind.FineDecrease -> {
                    drawLine(
                        color = iconColor,
                        start = Offset(inset, cy),
                        end = Offset(size.width - inset, cy),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }

                SubtitleStepKind.FineIncrease -> {
                    drawLine(
                        color = iconColor,
                        start = Offset(inset, cy),
                        end = Offset(size.width - inset, cy),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(cx, inset),
                        end = Offset(cx, size.height - inset),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }

                SubtitleStepKind.CoarseDecrease -> {
                    val horizontalReach = size.width * 0.18f
                    val verticalReach = size.height * 0.30f
                    val path = Path().apply {
                        moveTo(cx + horizontalReach, cy - verticalReach)
                        lineTo(cx - horizontalReach, cy)
                        lineTo(cx + horizontalReach, cy + verticalReach)
                    }
                    drawPath(
                        path = path,
                        color = iconColor,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                SubtitleStepKind.CoarseIncrease -> {
                    val horizontalReach = size.width * 0.18f
                    val verticalReach = size.height * 0.30f
                    val path = Path().apply {
                        moveTo(cx - horizontalReach, cy - verticalReach)
                        lineTo(cx + horizontalReach, cy)
                        lineTo(cx - horizontalReach, cy + verticalReach)
                    }
                    drawPath(
                        path = path,
                        color = iconColor,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrostedSubtitleActionPill(
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
            .padding(horizontal = LiquidGlassTokens.Space.s18),
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
