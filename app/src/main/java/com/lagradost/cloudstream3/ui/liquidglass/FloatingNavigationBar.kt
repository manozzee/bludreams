package com.lagradost.cloudstream3.ui.liquidglass

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

data class FloatingNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
    @DrawableRes val drawableRes: Int? = null,
    @DrawableRes val selectedDrawableRes: Int? = null,
    val content: (@Composable (onClick: () -> Unit) -> Unit)? = null,
)

@Stable
class LiquidGlassNavBarScrollState {
    var labelVisibility by mutableFloatStateOf(1f)
        private set

    private var accumulatedDelta = 0f

    fun expand() {
        labelVisibility = 1f
        accumulatedDelta = 0f
    }

    fun collapse() {
        labelVisibility = 0f
        accumulatedDelta = 0f
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val deltaY = available.y
            if (deltaY == 0f) return Offset.Zero

            accumulatedDelta += deltaY

            if (accumulatedDelta < -SCROLL_THRESHOLD && labelVisibility != 0f) {
                labelVisibility = 0f
                accumulatedDelta = 0f
            } else if (accumulatedDelta > SCROLL_THRESHOLD && labelVisibility != 1f) {
                labelVisibility = 1f
                accumulatedDelta = 0f
            }

            if (deltaY < 0f && accumulatedDelta > 0f) accumulatedDelta = deltaY
            if (deltaY > 0f && accumulatedDelta < 0f) accumulatedDelta = deltaY

            return Offset.Zero
        }
    }

    companion object {
        private const val SCROLL_THRESHOLD = 60f
    }
}

@Composable
fun rememberLiquidGlassNavBarScrollState(): LiquidGlassNavBarScrollState {
    return remember { LiquidGlassNavBarScrollState() }
}

val floatingNavigationGlowSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
fun FloatingNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier = Modifier,
    scrollState: LiquidGlassNavBarScrollState? = null,
    hazeState: HazeState? = null,
    backdrop: PlatformLayerBackdrop? = null,
    contentPadding: PaddingValues = floatingNavigationBarPadding(),
    compactSize: Boolean = false,
    glowEnabled: Boolean = true,
) {
    if (items.isEmpty()) return
    val showGlow = !floatingNavigationGlowSupported || glowEnabled
    val glowStrength by animateFloatAsState(
        targetValue = if (showGlow) 1f else 0f,
        animationSpec = tween(420, easing = LiquidGlassTokens.Motion.standard),
        label = "nav_glow_strength",
    )
    val tokens = MaterialTheme.liquidGlass
    val accentColor = tokens.colors.accent
    val selectedSurface = accentColor.copy(alpha = 0.45f)
    val labelFraction by animateFloatAsState(
        targetValue = scrollState?.labelVisibility ?: 1f,
        animationSpec = tween(LiquidGlassTokens.Motion.sheetEnterMillis, easing = LiquidGlassTokens.Motion.standard),
        label = "jelly_labels",
    )
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val selectedIndex = items.indexOfFirst { it.selected }
    val visualSelectedIndex = visualNavIndex(selectedIndex, items.size, isRtl)
    val motion = remember(items.size, isRtl) { JellyMotion(visualSelectedIndex, items.size) }
    val currentItems by rememberUpdatedState(items)
    val currentIsRtl by rememberUpdatedState(isRtl)
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val trackHeight = 48.dp + (if (compactSize) 8.dp else 16.dp) * labelFraction
    val horizontalPadding = 58.dp - 30.dp * labelFraction

    LaunchedEffect(visualSelectedIndex, items.size) {
        motion.select(visualSelectedIndex)
    }
    LaunchedEffect(motion.running) {
        if (!motion.running) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (motion.running) {
            withFrameNanos { now ->
                motion.advance((now - previous) / 1_000_000_000.0)
                previous = now
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .height(trackHeight)
                .onSizeChanged {
                    motion.resize(it.width / density.density, it.height / density.density, items.size)
                }
                .pointerInput(motion, density, items.size, isRtl) {
                    detectJellyTabGestures(motion, density.density, { currentItems }, { currentIsRtl }, hapticFeedback)
                },
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val frame = motion.frame
                        scaleX = frame.trackScale
                        scaleY = frame.trackScale
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val frame = motion.frame
                            transformOrigin = TransformOrigin(
                                if (size.width > 0) frame.originX * density.density / size.width else 0.5f,
                                0.5f,
                            )
                            scaleX = frame.trackScaleX
                            translationY = frame.trackOffsetY * density.density
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { translationX = motion.frame.panelOffset * density.density },
                    ) {
                        Box(
                            Modifier.matchParentSize()
                                .clip(RoundedCornerShape(50))
                                .drawWithContent {
                                    drawContent()
                                    drawJellyGlow(motion.frame, accentColor.copy(alpha = accentColor.alpha * glowStrength))
                                },
                        ) {
                            if (LocalLiquidGlass.current && backdrop != null && isLiquidGlassBackdropSupported()) {
                                Box(
                                    Modifier.matchParentSize().liquidGlassChrome(
                                        shape = RoundedCornerShape(50),
                                        enabled = true,
                                        preset = LiquidGlassPreset.BottomNavigation,
                                        backdrop = backdrop,
                                    ),
                                )
                            } else {
                                GlassBarSurface(hazeState, Modifier.matchParentSize(), glowStrength)
                            }
                        }
                        Box(
                            Modifier.matchParentSize().drawWithContent {
                                if (selectedIndex >= 0) {
                                    clipPath(jellyPillPath(motion.frame, items.size), ClipOp.Difference) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    drawContent()
                                }
                            },
                        ) {
                            JellyTabRow(items, labelFraction, motion, active = false, compactSize = compactSize, modifier = Modifier.matchParentSize())
                        }
                        if (selectedIndex >= 0) {
                            Box(
                                Modifier.matchParentSize()
                                    .clearAndSetSemantics {}
                                    .drawWithContent {
                                        drawJellyPill(
                                            motion.frame,
                                            items.size,
                                            selectedSurface,
                                            accentColor.copy(alpha = accentColor.alpha * glowStrength),
                                        ) { drawContent() }
                                    },
                            ) {
                                JellyTabRow(items, labelFraction, motion, active = true, compactSize = compactSize, modifier = Modifier.matchParentSize())
                            }
                        }
                        JellyTabTargets(items, labelFraction, motion, compactSize, Modifier.matchParentSize())
                    }
                }
            }
        }
    }
}

@Composable
fun floatingNavigationBarPadding(): PaddingValues {
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return PaddingValues(
        top = if (isLandscape) 8.dp else 24.dp,
        bottom = if (isLandscape) maxOf(14.dp, navBarsBottom + 8.dp) else maxOf(36.dp, navBarsBottom + 16.dp),
    )
}

internal fun visualNavIndex(logicalIndex: Int, count: Int, isRtl: Boolean): Int =
    if (logicalIndex in 0 until count && isRtl) count - 1 - logicalIndex else logicalIndex

internal fun logicalNavIndex(visualIndex: Int, count: Int, isRtl: Boolean): Int =
    if (visualIndex in 0 until count && isRtl) count - 1 - visualIndex else visualIndex

internal suspend fun PointerInputScope.detectJellyTabGestures(
    motion: JellyMotion,
    density: Float,
    currentItems: () -> List<FloatingNavigationItem>,
    isRtl: () -> Boolean,
    hapticFeedback: HapticFeedback,
) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            motion.begin(down.position.x / density, down.position.y / density)
            var claimed = false
            var finished = false
            var longPressed = false
            var longPressJob: Job? = null

            val itemsAtDown = currentItems()
            if (itemsAtDown.isNotEmpty() && size.width > 0) {
                val tabWidth = size.width.toFloat() / itemsAtDown.size
                val downVisualIndex = (down.position.x / tabWidth).toInt().coerceIn(0, itemsAtDown.size - 1)
                val downLogicalIndex = logicalNavIndex(downVisualIndex, itemsAtDown.size, isRtl())
                val targetItem = itemsAtDown.getOrNull(downLogicalIndex)
                if (targetItem?.onLongClick != null) {
                    longPressJob = launch {
                        delay(900L)
                        longPressed = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        targetItem.onLongClick.invoke()
                    }
                }
            }

            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (!motion.dragging) {
                        longPressJob?.cancel()
                        finished = true
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.count { it.pressed } > 1) {
                        longPressJob?.cancel()
                        break
                    }
                    val delta = change.position - down.position
                    if (max(abs(delta.x), abs(delta.y)) > viewConfiguration.touchSlop) {
                        claimed = true
                        longPressJob?.cancel()
                    }
                    if (claimed) change.consume()
                    awaitPointerEvent(PointerEventPass.Main)
                    if (!motion.dragging) {
                        longPressJob?.cancel()
                        finished = true
                        break
                    }
                    if (change.isConsumed && !claimed) {
                        longPressJob?.cancel()
                        break
                    }
                    motion.drag(delta.x / density, delta.y / density)
                    if (!change.pressed) {
                        longPressJob?.cancel()
                        val visualIndex = motion.finish()
                        val items = currentItems()
                        val logicalIndex = logicalNavIndex(visualIndex, items.size, isRtl())
                        finished = true
                        if (!longPressed) {
                            items.getOrNull(logicalIndex)?.onClick?.invoke()
                        }
                        change.consume()
                        break
                    }
                }
            } finally {
                longPressJob?.cancel()
                if (!finished) {
                    val items = currentItems()
                    val selectedIndex = items.indexOfFirst { it.selected }
                    motion.cancel(visualNavIndex(selectedIndex, items.size, isRtl()))
                }
            }
        }
    }
}
