package com.lagradost.cloudstream3.ui.liquidglass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.abs

fun Modifier.gradientMask(brush: Brush): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcIn)
            }
        }

@Composable
internal fun JellyTabRow(
    items: List<FloatingNavigationItem>,
    labelFraction: Float,
    motion: JellyMotion,
    active: Boolean,
    compactSize: Boolean,
    modifier: Modifier,
) {
    val tokens = MaterialTheme.liquidGlass
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val containerHorizontalPaddingDp = 8f
    val color = Color.White
    val iconSize = if (compactSize) 18.dp else 20.dp
    val iconModifier = Modifier.size(iconSize)
    val iconTint = Color.White

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val visualIndex = visualNavIndex(index, items.size, isRtl)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (!active) {
                            Modifier.onPlaced { coords ->
                                motion.slotRegistry.update(
                                    visualIndex = visualIndex,
                                    leftDp = coords.positionInParent().x / density.density + containerHorizontalPaddingDp,
                                    widthDp = coords.size.width / density.density,
                                )
                            }
                        } else {
                            Modifier
                        },
                    )
                    .graphicsLayer {
                        val scale = if (active) motion.frame.contentScale else 1f
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        val displayIcon = if (active) (item.selectedIcon ?: item.icon) else item.icon
                        val displayDrawable = if (active) (item.selectedDrawableRes ?: item.drawableRes) else item.drawableRes
                        when {
                            item.avatarUri != null -> {
                                val avatarBorderColor = if (active) Color.White else tokens.colors.accent
                                AsyncImage(
                                    model = item.avatarUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = iconModifier
                                        .clip(CircleShape)
                                        .border(1.5.dp, avatarBorderColor, CircleShape),
                                )
                            }
                            displayIcon != null -> Icon(displayIcon, null, iconModifier, tint = iconTint)
                            displayDrawable != null -> Icon(painterResource(displayDrawable), null, iconModifier, tint = iconTint)
                        }
                    }

                    AnimatedVisibility(
                        visible = item.selected && labelFraction > 0.5f,
                        enter = expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) + fadeIn(animationSpec = tween(180)),
                        exit = shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ) + fadeOut(animationSpec = tween(120)),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.label,
                                color = color,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false,
                                modifier = Modifier.widthIn(max = 112.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun JellyTabTargets(
    items: List<FloatingNavigationItem>,
    motion: JellyMotion,
    compactSize: Boolean,
    modifier: Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    Layout(
        content = {
            items.forEachIndexed { index, item ->
                val visualIndex = visualNavIndex(index, items.size, isRtl)
                val onClick = {
                    motion.select(visualIndex)
                    item.onClick()
                }
                Box(
                    modifier = Modifier
                        .selectable(
                            selected = item.selected,
                            role = Role.Tab,
                            interactionSource = null,
                            indication = null,
                            onClick = onClick,
                        )
                        .clearAndSetSemantics {
                            role = Role.Tab
                            selected = item.selected
                            contentDescription = item.label
                            onClick { onClick(); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.content != null) {
                        Box(
                            modifier = Modifier
                                .then(if (compactSize) Modifier.size(18.dp) else Modifier.size(20.dp))
                                .graphicsLayer {
                                    val frame = motion.frame
                                    val coverage = (1f - abs(frame.position - visualIndex)).coerceIn(0f, 1f)
                                    val scale = 1f + (frame.contentScale - 1f) * coverage
                                    scaleX = scale
                                    scaleY = scale
                                },
                        ) {
                            item.content(onClick)
                        }
                    }
                }
            }
        },
        modifier = modifier.selectableGroup(),
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val totalHeight = constraints.maxHeight
        val ready = motion.slotRegistry.isReady(items.size)
        val placeables = measurables.mapIndexed { index, measurable ->
            val visualIndex = visualNavIndex(index, items.size, isRtl)
            val widthPx = if (ready) {
                with(density) { motion.slotRegistry.get(visualIndex).widthDp.dp.roundToPx() }
                    .coerceIn(0, totalWidth)
            } else {
                (totalWidth / items.size.coerceAtLeast(1)).coerceAtLeast(0)
            }
            measurable.measure(Constraints.fixed(width = widthPx, height = totalHeight))
        }
        layout(totalWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val visualIndex = visualNavIndex(index, items.size, isRtl)
                val xPx = if (ready) {
                    with(density) { motion.slotRegistry.get(visualIndex).leftDp.dp.roundToPx() }
                } else {
                    visualIndex * (totalWidth / items.size.coerceAtLeast(1))
                }
                placeable.place(x = xPx, y = 0)
            }
        }
    }
}
