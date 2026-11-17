package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.R

internal object PlayerHudIcons {
    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayerPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(7.25f, 5f)
                horizontalLineTo(9.75f)
                curveTo(10.44f, 5f, 11f, 5.56f, 11f, 6.25f)
                verticalLineTo(17.75f)
                curveTo(11f, 18.44f, 10.44f, 19f, 9.75f, 19f)
                horizontalLineTo(7.25f)
                curveTo(6.56f, 19f, 6f, 18.44f, 6f, 17.75f)
                verticalLineTo(6.25f)
                curveTo(6f, 5.56f, 6.56f, 5f, 7.25f, 5f)
                close()
                moveTo(14.25f, 5f)
                horizontalLineTo(16.75f)
                curveTo(17.44f, 5f, 18f, 5.56f, 18f, 6.25f)
                verticalLineTo(17.75f)
                curveTo(18f, 18.44f, 17.44f, 19f, 16.75f, 19f)
                horizontalLineTo(14.25f)
                curveTo(13.56f, 19f, 13f, 18.44f, 13f, 17.75f)
                verticalLineTo(6.25f)
                curveTo(13f, 5.56f, 13.56f, 5f, 14.25f, 5f)
                close()
            }
        }.build()
    }

    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayerLock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 11f)
                horizontalLineTo(17f)
                curveTo(18.105f, 11f, 19f, 11.895f, 19f, 13f)
                verticalLineTo(19f)
                curveTo(19f, 20.105f, 18.105f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(5.895f, 21f, 5f, 20.105f, 5f, 19f)
                verticalLineTo(13f)
                curveTo(5f, 11.895f, 5.895f, 11f, 7f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 11f)
                verticalLineTo(7.5f)
                curveTo(8f, 5.291f, 9.791f, 3.5f, 12f, 3.5f)
                curveTo(14.209f, 3.5f, 16f, 5.291f, 16f, 7.5f)
                verticalLineTo(11f)
            }
        }.build()
    }

    val LockOpen: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayerLockOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 11f)
                horizontalLineTo(17f)
                curveTo(18.105f, 11f, 19f, 11.895f, 19f, 13f)
                verticalLineTo(19f)
                curveTo(19f, 20.105f, 18.105f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(5.895f, 21f, 5f, 20.105f, 5f, 19f)
                verticalLineTo(13f)
                curveTo(5f, 11.895f, 5.895f, 11f, 7f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 11f)
                verticalLineTo(7.5f)
                curveTo(8f, 5.291f, 9.791f, 3.5f, 12f, 3.5f)
                curveTo(13.864f, 3.5f, 15.431f, 4.774f, 15.875f, 6.5f)
            }
        }.build()
    }

    fun volumeDrawable(level: Float): Int = when {
        level <= 0.02f -> R.drawable.ic_baseline_volume_mute_24
        level < 0.5f -> R.drawable.ic_baseline_volume_down_24
        else -> R.drawable.ic_baseline_volume_up_24
    }

    private val brightnessDrawables = intArrayOf(
        R.drawable.sun_1,
        R.drawable.sun_2,
        R.drawable.sun_3,
        R.drawable.sun_4,
        R.drawable.sun_5,
        R.drawable.sun_6,
        R.drawable.sun_7,
    )

    fun brightnessDrawable(level: Float): Int {
        val clamped = level.coerceIn(0f, 1f)
        val index = (clamped * brightnessDrawables.lastIndex).toInt().coerceIn(0, brightnessDrawables.lastIndex)
        return brightnessDrawables[index]
    }
}
