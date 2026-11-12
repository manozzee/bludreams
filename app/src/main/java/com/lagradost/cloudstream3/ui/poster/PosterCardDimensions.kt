package com.lagradost.cloudstream3.ui.poster

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class PosterCardStyleUiState(
    val widthDp: Int = PosterCardDefaults.widthDp,
    val heightDp: Int = PosterCardDefaults.heightDp,
    val cornerRadiusDp: Int = PosterCardDefaults.cornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = PosterCardDefaults.catalogLandscapeModeEnabled,
    val hideLabelsEnabled: Boolean = PosterCardDefaults.hideLabelsEnabled,
    val alwaysShowLandscapeClearlogo: Boolean = PosterCardDefaults.alwaysShowLandscapeClearlogo,
    val showRating: Boolean = PosterCardDefaults.showRating,
    val showQuality: Boolean = PosterCardDefaults.showQuality,
    val showDub: Boolean = PosterCardDefaults.showDub,
    val showSub: Boolean = PosterCardDefaults.showSub,
    val showEpisode: Boolean = PosterCardDefaults.showEpisode,
) {
    val landscapePostersEnabled: Boolean get() = catalogLandscapeModeEnabled
    val hideLabels: Boolean get() = hideLabelsEnabled
    val alwaysShowClearlogoOnLandscape: Boolean get() = alwaysShowLandscapeClearlogo
}

object PosterCardDefaults {
    const val widthDp: Int = 126
    const val heightDp: Int = 189
    const val cornerRadiusDp: Int = 12
    const val catalogLandscapeModeEnabled: Boolean = false
    const val hideLabelsEnabled: Boolean = false
    const val alwaysShowLandscapeClearlogo: Boolean = false
    const val showRating: Boolean = true
    const val showQuality: Boolean = true
    const val showDub: Boolean = true
    const val showSub: Boolean = true
    const val showEpisode: Boolean = true

    val DefaultState = PosterCardStyleUiState()

    val widthRange: IntRange = 96..168
    val cornerRadiusRange: IntRange = 0..28

    fun calculateHeightDp(widthDp: Int): Int = ((widthDp * 3f) / 2f).roundToInt()
}

const val PosterPortraitAspectRatio: Float = 2f / 3f
const val PosterLandscapeAspectRatio: Float = 16f / 9f
private const val PosterLandscapeWidthMultiplier: Float = 1.65f

fun landscapePosterWidth(widthDp: Int): Dp = (widthDp * PosterLandscapeWidthMultiplier).roundToInt().dp

fun landscapePosterHeight(widthDp: Int): Dp {
    val landscapeWidth = (widthDp * PosterLandscapeWidthMultiplier).roundToInt()
    return (landscapeWidth / PosterLandscapeAspectRatio).roundToInt().dp
}

enum class PosterWidthPreset(val label: String, val widthDp: Int) {
    Compact("Compact", 104),
    Dense("Dense", 112),
    Standard("Standard", 120),
    Balanced("Balanced", 126),
    Comfort("Comfort", 134),
    Large("Large", 140),
}

enum class PosterRadiusPreset(val label: String, val radiusDp: Int) {
    Sharp("Sharp", 0),
    Subtle("Subtle", 4),
    Classic("Classic", 8),
    Rounded("Rounded", 12),
    Pill("Pill", 16),
}

