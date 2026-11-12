package com.lagradost.cloudstream4.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Maps to the XML custom attrs declared in attrs.xml:
 * TODO: Remove this comment when we migrate fully
 *  and attrs.xml will no longer be used at all.
 *
 * | XML ?attr                | Here               |
 * |--------------------------|--------------------|
 * | primaryBlackBackground   | [background]       |
 * | primaryGrayBackground    | [surfaceVariant]   |
 * | iconGrayBackground       | [surface]          |
 * | boxItemBackground        | [surfaceContainer] |
 * | textColor                | [onBackground]     |
 * | grayTextColor            | [onSurfaceVariant] |
 * | iconColor                | [icon]             |
 * | colorPrimary             | [primary]          |
 * | colorOngoing             | [ongoing]          |
 *
 * All fields are [MutableState] so Compose recomposes automatically
 * if the scheme is swapped at runtime (e.g. user changes theme without restart).
 */
@Stable
class CloudStreamColorScheme(
    background: Color,
    surfaceVariant: Color,
    surface: Color,
    surfaceContainer: Color,
    onBackground: Color,
    onSurfaceVariant: Color,
    icon: Color,
    primary: Color,
    ongoing: Color,
    isLight: Boolean,
) {
    /** primaryBlackBackground */
    var background by mutableStateOf(background)

    /** primaryGrayBackground */
    var surfaceVariant by mutableStateOf(surfaceVariant)

    /** iconGrayBackground */
    var surface by mutableStateOf(surface)

    /** boxItemBackground */
    var surfaceContainer by mutableStateOf(surfaceContainer)

    /** textColor */
    var onBackground by mutableStateOf(onBackground)

    /** grayTextColor */
    var onSurfaceVariant by mutableStateOf(onSurfaceVariant)

    /** iconColor */
    var icon by mutableStateOf(icon)

    /** colorPrimary */
    var primary by mutableStateOf(primary)

    /** colorOngoing */
    var ongoing by mutableStateOf(ongoing)
    var isLight by mutableStateOf(isLight)

    fun copy(
        background: Color = this.background,
        surfaceVariant: Color = this.surfaceVariant,
        surface: Color = this.surface,
        surfaceContainer: Color = this.surfaceContainer,
        onBackground: Color = this.onBackground,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        icon: Color = this.icon,
        primary: Color = this.primary,
        ongoing: Color = this.ongoing,
        isLight: Boolean = this.isLight,
    ) = CloudStreamColorScheme(
        background, surfaceVariant, surface, surfaceContainer,
        onBackground, onSurfaceVariant, icon, primary, ongoing, isLight,
    )
}

internal fun darkScheme() = CloudStreamColorScheme(
    background = CloudStreamPalette.DarkBlackBg,
    surfaceVariant = CloudStreamPalette.DarkPrimaryGrayBg,
    surface = CloudStreamPalette.DarkIconGrayBg,
    surfaceContainer = CloudStreamPalette.DarkBoxItemBg,
    onBackground = CloudStreamPalette.DarkText,
    onSurfaceVariant = CloudStreamPalette.DarkGrayText,
    icon = CloudStreamPalette.DarkIcon,
    primary = CloudStreamPalette.Primary,
    ongoing = CloudStreamPalette.Ongoing,
    isLight = false,
)


internal fun amoledScheme() = darkScheme().copy(
    background = CloudStreamPalette.AmoledBlack,
    surface = CloudStreamPalette.AmoledElevated,
    surfaceVariant = CloudStreamPalette.AmoledCard,
    surfaceContainer = CloudStreamPalette.AmoledElevated,
)

internal fun amoledLightScheme() = amoledScheme()

internal fun lightScheme() = darkScheme()

internal fun draculaScheme() = darkScheme()

internal fun lavenderScheme() = darkScheme()

internal fun silentBlueScheme() = darkScheme()