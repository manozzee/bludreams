package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastFirstOrNull
import com.lagradost.cloudstream3.ui.liquidglass.GlassBarSurface
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassPreset
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.isLiquidGlassBackdropSupported
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassChrome
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import kotlin.math.roundToInt

private val FloatingPillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

internal val PlayerGlassWhiteDivider = Color.White.copy(alpha = 0.14f)
internal val PlayerGlassWhiteBorderSubtle = Color.White.copy(alpha = 0.14f)
internal val PlayerGlassWhiteBorder = Color.White.copy(alpha = 0.22f)
internal val PlayerGlassWhiteBorderSelected = Color.White.copy(alpha = 0.36f)
internal val PlayerGlassWhiteActiveRowBorder = Color.White.copy(alpha = 0.20f)
internal val PlayerGlassWhiteBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.10f),
    ),
)
internal val PlayerGlassWhiteSelectedBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.40f),
        Color.White.copy(alpha = 0.14f),
    ),
)

@Composable
internal fun KyantPlayerGlassChromeLayer(
    shape: Shape,
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
    fallbackCornerRadius: Dp = LiquidGlassTokens.Radius.sheet,
) {
    val isGlassSupported = backdrop != null && isLiquidGlassBackdropSupported()
    if (isGlassSupported) {
        Box(
            modifier = modifier
                .liquidGlassChrome(
                    shape = shape,
                    enabled = true,
                    preset = LiquidGlassPreset.BottomNavigation,
                    backdrop = backdrop,
                ),
        )
    } else {
        GlassBarSurface(
            hazeState = null,
            modifier = modifier,
            fixedCornerRadius = fallbackCornerRadius,
        )
    }
}

@Composable
internal fun CenteredDarkGlassOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cardContent: @Composable BoxWithConstraintsScope.(Modifier) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val glassTokens = MaterialTheme.liquidGlass

    val startInset = safePadding.calculateStartPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val endInset = safePadding.calculateEndPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val topInset = safePadding.calculateTopPadding() + LiquidGlassTokens.Space.s12
    val bottomInset = safePadding.calculateBottomPadding() + LiquidGlassTokens.Space.s12

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
                scaleIn(tween(LiquidGlassTokens.Motion.normalMillis), initialScale = 0.94f),
            exit = fadeOut(tween(LiquidGlassTokens.Motion.fastMillis)) +
                scaleOut(tween(LiquidGlassTokens.Motion.fastMillis), targetScale = 0.94f),
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = startInset,
                        top = topInset,
                        end = endInset,
                        bottom = bottomInset,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val clickGuardModifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )

                cardContent(clickGuardModifier)
            }
        }
    }
}

@Composable
internal fun FloatingCardWrapper(
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.liquidGlass.shapes.sheet,
    fallbackCornerRadius: Dp = LiquidGlassTokens.Radius.sheet,
    content: @Composable BoxScope.() -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .clip(shape),
    ) {
        KyantPlayerGlassChromeLayer(
            shape = shape,
            backdrop = backdrop,
            fallbackCornerRadius = fallbackCornerRadius,
            modifier = Modifier.matchParentSize(),
        )

        Box(
            modifier = Modifier
                .background(glassTokens.colors.surfaceSheet.copy(alpha = glassTokens.opacity.overlayLight)),
            content = content,
        )
    }
}

@Composable
internal fun FrostedCircleStepButton(
    isIncrement: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val iconColor = glassTokens.colors.textPrimary.copy(alpha = 0.92f)
    Box(
        modifier = modifier
            .size(LiquidGlassTokens.Space.s32)
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
            val cy = size.height / 2f
            val cx = size.width / 2f
            drawLine(
                color = iconColor,
                start = Offset(inset, cy),
                end = Offset(size.width - inset, cy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (isIncrement) {
                drawLine(
                    color = iconColor,
                    start = Offset(cx, inset),
                    end = Offset(cx, size.height - inset),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
internal fun FrostedGlassPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val bgColor = if (selected) {
        glassTokens.colors.overlaySelected
    } else {
        glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.subtle)
    }
    val borderBrush = if (selected) {
        PlayerGlassWhiteSelectedBorderBrush
    } else {
        PlayerGlassWhiteBorderBrush
    }

    Box(
        modifier = modifier
            .height(LiquidGlassTokens.Space.s28)
            .clip(FloatingPillShape)
            .background(bgColor)
            .border(
                width = if (selected) LiquidGlassTokens.Border.thin else LiquidGlassTokens.Border.hairline,
                brush = borderBrush,
                shape = FloatingPillShape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) glassTokens.colors.textPrimary else glassTokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun FrostedDialogActionPill(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val bgModifier = if (isPrimary) {
        Modifier
            .clip(FloatingPillShape)
            .background(glassTokens.colors.textPrimary)
    } else {
        Modifier
            .clip(FloatingPillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = FloatingPillShape,
            )
    }

    Box(
        modifier = modifier
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

@Composable
internal fun FrostedGlassSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val glassTokens = MaterialTheme.liquidGlass
    val trackColor = glassTokens.colors.playerTimelineTrack
    val thumbColor = glassTokens.colors.textPrimary
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun valueForFraction(rawFraction: Float): Float {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val raw = valueRange.start + clamped * span
        return if (stepSize > 0f) {
            val steps = ((raw - valueRange.start) / stepSize).roundToInt()
            val snapped = valueRange.start + steps * stepSize
            ((snapped * 100f).roundToInt() / 100f).coerceIn(valueRange.start, valueRange.endInclusive)
        } else {
            raw.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    Canvas(
        modifier = modifier
            .height(LiquidGlassTokens.Space.s28)
            .pointerInput(valueRange, stepSize) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    onValueChange(valueForFraction(down.position.x / width))

                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.fastFirstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                            ?: break
                        pointerId = change.id
                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            onValueChange(valueForFraction(change.position.x / width))
                            break
                        }
                        if (change.isConsumed) {
                            break
                        }
                        change.consume()
                        onValueChange(valueForFraction(change.position.x / width))
                    }
                }
            },
    ) {
        val cy = size.height / 2f
        val trackH = LiquidGlassTokens.Space.s4.toPx()
        val thumbR = LiquidGlassTokens.Space.s7.toPx()
        val corner = CornerRadius(trackH / 2f, trackH / 2f)
        val usableWidth = size.width

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size(usableWidth, trackH),
            cornerRadius = corner,
        )

        val activeX = usableWidth * fraction
        if (activeX > 0f) {
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(activeX, trackH),
                cornerRadius = corner,
            )
        }

        drawCircle(
            color = thumbColor,
            radius = thumbR,
            center = Offset(
                activeX.coerceIn(thumbR, (usableWidth - thumbR).coerceAtLeast(thumbR)),
                cy,
            ),
        )
    }
}
