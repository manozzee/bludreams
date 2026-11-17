package com.lagradost.cloudstream3.ui.liquidglass

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

internal fun DrawScope.drawJellyGlow(frame: JellyFrame, color: Color) {
    if (frame.glowOpacity <= 0f || color.alpha <= 0f) return
    val alpha = 0.24f * frame.glowOpacity * color.alpha
    drawRect(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            0.45f to color.copy(alpha = alpha * 0.43f),
            1f to color.copy(alpha = 0f),
            center = Offset(frame.originX.dp.toPx(), frame.glowY.dp.toPx()),
            radius = 300.dp.toPx(),
        ),
        topLeft = Offset(-48.dp.toPx(), -16.dp.toPx()),
        size = Size(size.width + 96.dp.toPx(), size.height + 32.dp.toPx()),
    )
}

internal fun DrawScope.jellyPillPath(
    frame: JellyFrame,
    count: Int,
    slotRegistry: TabSlotRegistry? = null,
): Path {
    val verticalInset = 6.dp.toPx()
    val itemHeight = size.height - 2 * verticalInset
    val (centerX, baseWidth) = if (slotRegistry != null && slotRegistry.isReady(count)) {
        val pos = frame.position.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())
        val i0 = floor(pos).toInt().coerceIn(0, count - 1)
        val i1 = ceil(pos).toInt().coerceIn(0, count - 1)
        val t = (pos - i0).coerceIn(0f, 1f)
        val slot0 = slotRegistry.get(i0)
        val slot1 = slotRegistry.get(i1)
        val left0 = slot0.leftDp.dp.toPx()
        val width0 = slot0.widthDp.dp.toPx()
        val left1 = slot1.leftDp.dp.toPx()
        val width1 = slot1.widthDp.dp.toPx()
        val left = left0 + (left1 - left0) * t
        val width = width0 + (width1 - width0) * t
        (left + width / 2f) to width
    } else {
        val inset = 8.dp.toPx()
        val tabWidth = (size.width - 2 * inset) / count.coerceAtLeast(1)
        (inset + (frame.position + 0.5f) * tabWidth) to tabWidth
    }
    val halfWidth = baseWidth * frame.pillScaleX / 2f
    val halfHeight = itemHeight * frame.pillScaleY / 2f
    val radius = min(baseWidth, itemHeight) / 2f
    return Path().apply {
        addRoundRect(
            RoundRect(
                left = centerX - halfWidth,
                top = size.height / 2f - halfHeight,
                right = centerX + halfWidth,
                bottom = size.height / 2f + halfHeight,
                cornerRadius = CornerRadius(radius * frame.pillScaleX, radius * frame.pillScaleY),
            ),
        )
    }
}

internal fun DrawScope.drawJellyPill(
    frame: JellyFrame,
    count: Int,
    surfaceColor: Color,
    glowColor: Color,
    slotRegistry: TabSlotRegistry? = null,
    borderColor: Color = Color.White.copy(alpha = 0.18f),
    content: () -> Unit,
) {
    val pillPath = jellyPillPath(frame, count, slotRegistry)
    clipPath(pillPath) {
        drawRect(
            color = surfaceColor,
            topLeft = Offset(-48.dp.toPx(), -16.dp.toPx()),
            size = Size(size.width + 96.dp.toPx(), size.height + 32.dp.toPx()),
        )
        drawJellyGlow(frame, glowColor)
        content()
        drawPath(
            path = pillPath,
            color = borderColor,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
