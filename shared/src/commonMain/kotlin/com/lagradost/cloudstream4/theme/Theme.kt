package com.lagradost.cloudstream4.theme

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

enum class CloudStreamThemeMode {
    /** "Black" standard dark, #111111 backgrounds */
    Dark,

    /** "Amoled" / "AmoledLight" pure black (#000000) */
    Amoled,

    /** "AmoledLight" pure black (#000000) */
    AmoledLight,

    /** "Light" white/gray backgrounds, dark text */
    Light,

    /** "Dracula" */
    Dracula,

    /** "Lavender" */
    Lavender,

    /** "SilentBlue" */
    SilentBlue,

    /** "System" resolved on each platform via [isSystemInDarkTheme] */
    FollowSystem,

    /**
     * Uses platform dynamic color system, Material You on Android 12+,
     * falls back to [Dark] on unsupported platforms.
     */
    Dynamic,
}

@Composable
fun modeToTheme(mode: CloudStreamThemeMode, primaryColor: CloudStreamPrimaryColor): CloudStreamColorScheme {
    val dynamicPrimary = DeviceTheme.resolveDynamicPrimaryColor()
    val dynamicSecondary = DeviceTheme.resolveDynamicSecondaryColor()
    val color = remember(mode, primaryColor, dynamicPrimary, dynamicSecondary) {
        val base = when (mode) {
            CloudStreamThemeMode.Amoled -> amoledScheme()
            else -> darkScheme()
        }
        val accent = when {
            primaryColor == CloudStreamPrimaryColor.DYNAMIC -> dynamicPrimary
            primaryColor == CloudStreamPrimaryColor.DYNAMIC_TWO -> dynamicSecondary
            else -> primaryColor.color
        }
        val isAmoled = mode == CloudStreamThemeMode.Amoled
        val containerBase = if (isAmoled) Color(0xFF14151A) else Color(0xFF21232A)
        val elevatedBase = if (isAmoled) Color(0xFF1E2027) else Color(0xFF2A2D36)
        val accentContainer = accent.copy(alpha = 0.07f).compositeOver(containerBase)
        val accentElevated = accent.copy(alpha = 0.10f).compositeOver(elevatedBase)
        base.copy(
            primary = accent,
            surfaceVariant = accentContainer,
            surfaceContainer = accentContainer,
            surface = accentElevated,
        )
    }
    return color
}

private fun CloudStreamColorScheme.toMaterial3ColorScheme() = if (isLight) {
    lightColorScheme(
        primary = primary,
        primaryContainer = surfaceVariant,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
} else {
    val isAmoled = background == Color.Black
    darkColorScheme(
        primary = primary,
        primaryContainer = surfaceVariant,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        outline = if (isAmoled) Color(0x47FFFFFF) else Color(0xFF414654),
        outlineVariant = if (isAmoled) Color(0x2EFFFFFF) else Color(0xFF323642),
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
}

internal val LocalSharedInfiniteTransition = staticCompositionLocalOf<InfiniteTransition> { throw NotImplementedError() }

/**
 * Global synchronized animation, so many items in e.g. a LazyList can animate at the same time,
 * even if they appeared at different times
 * */
@Composable
@ReadOnlyComposable
fun MaterialTheme.infiniteSharedTransition() = LocalSharedInfiniteTransition.current

@Composable
fun CloudStreamPreviewTheme(content: @Composable () -> Unit) {
    CloudStreamTheme(content = content)
}

@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.VELVET_NOIR,
    content: @Composable () -> Unit,
) {
    val csColors = modeToTheme(mode, primaryColor)
    val globalTransition = rememberInfiniteTransition(label = "GlobalSharedTransition")
    // We do not provide csColors as a global, because people should use MaterialTheme directly instead
    CompositionLocalProvider(LocalSharedInfiniteTransition provides globalTransition) {
        MaterialTheme(
            colorScheme = csColors.toMaterial3ColorScheme(),
            content = content,
            typography = AppFont.typography
        )
    }
}