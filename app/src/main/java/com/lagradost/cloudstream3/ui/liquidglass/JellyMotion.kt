package com.lagradost.cloudstream3.ui.liquidglass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

internal data class JellyFrame(
    val position: Float,
    val pillScaleX: Float = 1f,
    val pillScaleY: Float = 1f,
    val contentScale: Float = 1f,
    val panelOffset: Float = 0f,
    val trackScale: Float = 1f,
    val trackScaleX: Float = 1f,
    val trackOffsetY: Float = 0f,
    val originX: Float = 0f,
    val glowY: Float = 0f,
    val glowOpacity: Float = 0f,
)

@Immutable
internal data class TabSlotBounds(
    val leftDp: Float = 0f,
    val widthDp: Float = 0f,
) {
    val centerDp: Float get() = leftDp + widthDp / 2f
    val rightDp: Float get() = leftDp + widthDp
}

internal class TabSlotRegistry(count: Int) {
    private var slots: Array<TabSlotBounds> = Array(count.coerceAtLeast(1)) { TabSlotBounds() }

    fun resize(newCount: Int) {
        val safeCount = newCount.coerceAtLeast(1)
        if (slots.size != safeCount) {
            slots = Array(safeCount) { TabSlotBounds() }
        }
    }

    fun update(visualIndex: Int, leftDp: Float, widthDp: Float) {
        if (visualIndex in slots.indices) {
            slots[visualIndex] = TabSlotBounds(leftDp, widthDp)
        }
    }

    fun isReady(expectedCount: Int): Boolean {
        if (expectedCount <= 0 || slots.size != expectedCount) return false
        return slots.all { it.widthDp > 0f }
    }

    fun get(visualIndex: Int): TabSlotBounds =
        slots.getOrElse(visualIndex) { TabSlotBounds() }

    fun indexAt(xDp: Double): Int {
        if (slots.isEmpty()) return 0
        for (i in slots.indices) {
            val slot = slots[i]
            if (xDp <= slot.rightDp || i == slots.lastIndex) {
                return i
            }
        }
        return slots.lastIndex
    }

    fun fractionalIndexAt(xDp: Double): Double {
        if (slots.size <= 1) return 0.0
        val firstCenter = slots.first().centerDp.toDouble()
        val lastCenter = slots.last().centerDp.toDouble()
        if (xDp <= firstCenter) return 0.0
        if (xDp >= lastCenter) return (slots.size - 1).toDouble()
        for (i in 0 until slots.lastIndex) {
            val c0 = slots[i].centerDp.toDouble()
            val c1 = slots[i + 1].centerDp.toDouble()
            if (xDp <= c1) {
                val span = (c1 - c0).coerceAtLeast(1.0)
                return i + ((xDp - c0) / span).coerceIn(0.0, 1.0)
            }
        }
        return (slots.size - 1).toDouble()
    }
}

@Stable
internal class JellyMotion(initialIndex: Int, count: Int) {
    private val position = JellySpring(initialIndex.coerceAtLeast(0).toDouble(), 1000.0, 1.0)
    private val velocity = JellySpring(0.0, 300.0, 0.5)
    private val press = JellySpring(0.0, 1000.0, 1.0)
    private val scaleX = JellySpring(1.0, 250.0, 0.6)
    private val scaleY = JellySpring(1.0, 250.0, 0.7)
    private val panel = JellySpring(0.0, 300.0, 1.0)
    private val distortionDamping = 18.0 / (2 * sqrt(240.0 * 0.9))
    private val trackY = JellySpring(0.0, 240.0 / 0.9, distortionDamping)
    private val trackX = JellySpring(1.0, 240.0 / 0.9, distortionDamping)
    private val trackPress = JellySpring(1.0, 240.0 / 0.9, distortionDamping)
    private val glow = JellySpring(0.0, 240.0 / 0.9, distortionDamping)
    private var target = position.value
    private var pressTarget = 0.0
    private var shapeTarget = 1.0
    private var releasePending = false
    private var downX = 0.0
    private var downY = 0.0
    private var dragStartTarget = target
    private var dragStartCenter = 0.0
    private var dragStartPanel = 0.0
    private var dragStartY = 0.0
    private var movedDistance = 0.0
    private var originX = 0.0
    private var width = 0.0
    private var height = 54.0
    private var tabCount = count
    private val maxIndex get() = (tabCount - 1).coerceAtLeast(0)
    private val tabWidth get() = ((width - 16) / tabCount.coerceAtLeast(1)).coerceAtLeast(0.0)

    internal val slotRegistry = TabSlotRegistry(count)

    var dragging = false
        private set
    var running by mutableStateOf(false)
        private set
    var frame by mutableStateOf(JellyFrame(position.value.toFloat()))
        private set

    fun resize(width: Float, height: Float, count: Int) {
        this.width = width.toDouble()
        this.height = height.toDouble()
        tabCount = count
        slotRegistry.resize(count)
        target = target.coerceIn(0.0, maxIndex.toDouble())
        if (!dragging) originX = this.width / 2
        publish()
    }

    fun visualIndexAt(xDp: Float): Int = indexAt(xDp.toDouble())

    fun select(index: Int) {
        dragging = false
        if (index >= 0) target = index.coerceAtMost(maxIndex).toDouble()
        releasePending = true
        pressTarget = 0.0
        shapeTarget = 1.0
        running = true
    }

    fun begin(x: Float, y: Float) {
        downX = x.toDouble()
        downY = y.toDouble().coerceIn(0.0, height)
        originX = downX.coerceIn(0.0, width)
        dragStartY = trackY.value
        movedDistance = 0.0
        if (slotRegistry.isReady(tabCount) || tabWidth > 0) {
            target = indexAt(downX).toDouble()
        }
        dragStartTarget = target
        dragStartCenter = if (slotRegistry.isReady(tabCount)) {
            slotRegistry.get(dragStartTarget.toInt().coerceIn(0, maxIndex)).centerDp.toDouble()
        } else {
            8.0 + (dragStartTarget + 0.5) * tabWidth
        }
        dragStartPanel = panel.value
        dragging = true
        releasePending = false
        pressTarget = 1.0
        shapeTarget = 1.3
        panel.velocity = 0.0
        running = true
    }

    fun drag(x: Float, y: Float) {
        if (!dragging) return
        val dx = x.toDouble()
        val dy = y.toDouble()
        if (slotRegistry.isReady(tabCount)) {
            target = slotRegistry.fractionalIndexAt(dragStartCenter + dx).coerceIn(0.0, maxIndex.toDouble())
        } else if (tabWidth > 0) {
            target = (dragStartTarget + dx / tabWidth).coerceIn(0.0, maxIndex.toDouble())
        } else {
            return
        }
        panel.snapTo(dragStartPanel + dx)
        trackY.snapTo(dragStartY + jellyRubberBand(dy, height) * 0.25)
        trackX.snapTo(1 - (abs(dy) / 700).coerceAtMost(1.0) * 0.08)
        originX = (downX + dx).coerceIn(0.0, width)
        movedDistance = max(movedDistance, max(abs(dx), abs(dy)))
        publish()
    }

    fun finish(): Int {
        val index = if (movedDistance < 4 && (slotRegistry.isReady(tabCount) || tabWidth > 0)) {
            indexAt(downX)
        } else {
            floor(target + 0.5).toInt().coerceIn(0, maxIndex)
        }
        dragging = false
        panel.velocity = 0.0
        target = index.toDouble()
        releasePending = true
        running = true
        return index
    }

    fun cancel(selectedIndex: Int) {
        dragging = false
        panel.velocity = 0.0
        select(selectedIndex)
    }

    fun advance(seconds: Double) {
        val delta = seconds.coerceIn(0.0, 0.064)
        target = target.coerceIn(0.0, maxIndex.toDouble())
        position.advance(target, delta)
        velocity.advance(if (dragging && maxIndex > 0) position.velocity / maxIndex else 0.0, delta)
        if (!dragging) panel.advance(0.0, delta)
        if (releasePending && abs(position.value - target) < max(1, maxIndex) * 0.025) {
            releasePending = false
            pressTarget = 0.0
            shapeTarget = 1.0
        }
        press.advance(pressTarget, delta)
        scaleX.advance(shapeTarget, delta)
        scaleY.advance(shapeTarget, delta)
        trackPress.advance(if (dragging) 1.025 else 1.0, delta)
        glow.advance(if (dragging) 1.0 else 0.0, delta)
        if (!dragging) {
            trackY.advance(0.0, delta)
            trackX.advance(1.0, delta)
            if (trackX.isAtRest(1.0)) originX = width / 2
        }
        publish()
        running = dragging || releasePending || !position.isAtRest(target) || !velocity.isAtRest(0.0) ||
            !press.isAtRest(0.0) || !scaleX.isAtRest(1.0) || !scaleY.isAtRest(1.0) ||
            !panel.isAtRest(0.0) || !trackY.isAtRest(0.0) || !trackX.isAtRest(1.0) ||
            !trackPress.isAtRest(1.0) || !glow.isAtRest(0.0)
    }

    private fun indexAt(x: Double): Int =
        if (slotRegistry.isReady(tabCount)) {
            slotRegistry.indexAt(x)
        } else {
            floor((x - 8) / tabWidth.coerceAtLeast(1.0)).toInt().coerceIn(0, maxIndex)
        }

    private fun publish() {
        val speed = velocity.value / 10
        frame = JellyFrame(
            position = position.value.toFloat(),
            pillScaleX = (scaleX.value / (1 - (speed * 0.75).coerceIn(-0.2, 0.2))).toFloat(),
            pillScaleY = (scaleY.value * (1 - (speed * 0.25).coerceIn(-0.2, 0.2))).toFloat(),
            contentScale = (1 + 0.2 * press.value).toFloat(),
            panelOffset = jellyPanelOffset(panel.value, width),
            trackScale = trackPress.value.toFloat(),
            trackScaleX = trackX.value.toFloat(),
            trackOffsetY = trackY.toFloatSafe(),
            originX = originX.toFloat(),
            glowY = downY.toFloat(),
            glowOpacity = glow.value.toFloat().coerceIn(0f, 1f),
        )
    }

    private fun JellySpring.toFloatSafe(): Float = value.toFloat()
}
