package com.lagradost.cloudstream3.ui.liquidglass

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.lagradost.cloudstream3.ui.poster.CardDepthRepository
import com.lagradost.cloudstream3.ui.poster.LocalCardDepthStyle
import com.lagradost.cloudstream3.ui.poster.LocalPosterCardStyle
import com.lagradost.cloudstream3.ui.poster.PosterCardStyleRepository
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.perfToColor
import com.mihon.presentation.settings.collectAsState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalLiquidGlassBottomNavigationOverlayPadding = compositionLocalOf { 0.dp }

object LiquidGlassTokens {
    object Space {
        val none = 0.dp
        val hairline = 0.5.dp
        val s1 = 1.dp
        val s2 = 2.dp
        val s3 = 3.dp
        val s4 = 4.dp
        val s5 = 5.dp
        val s6 = 6.dp
        val s7 = 7.dp
        val s8 = 8.dp
        val s10 = 10.dp
        val s12 = 12.dp
        val s14 = 14.dp
        val s16 = 16.dp
        val s18 = 18.dp
        val s20 = 20.dp
        val s22 = 22.dp
        val s24 = 24.dp
        val s28 = 28.dp
        val s32 = 32.dp
        val s36 = 36.dp
        val s40 = 40.dp
        val s48 = 48.dp
        val s56 = 56.dp
        val s64 = 64.dp
        val s72 = 72.dp
        val s80 = 80.dp
        val s96 = 96.dp
    }

    object Radius {
        val none = Space.none
        val xs = Space.s4
        val sm = Space.s6
        val md = Space.s8
        val lg = Space.s12
        val xl = Space.s16
        val xxl = Space.s24
        val full = 999.dp

        val card = xxl
        val compactCard = lg
        val sheet = xxl
        val dialog = Space.s28
        val button = xl
        val chip = full
        val poster = lg
        val avatar = full
        val playerPanel = xxl
    }

    object Border {
        val hairline = Space.hairline
        val thin = Space.s1
        val medium = Space.s2
    }

    object Elevation {
        val none = Space.none
        val raised = Space.s2
        val modal = Space.s8
        val overlay = Space.s12
        val playerControls = Space.s4
    }

    object Opacity {
        const val invisible = 0f
        const val disabled = 0.38f
        const val secondary = 0.70f
        const val muted = 0.60f
        const val selected = 0.15f
        const val hover = 0.08f
        const val pressed = 0.12f
        const val subtle = 0.06f
        const val medium = 0.52f
        const val strong = 0.75f
        const val overlayLight = 0.35f
        const val overlayMedium = 0.56f
        const val overlayHeavy = 0.82f
        const val visible = 1f
    }

    object Motion {
        const val instantMillis = 0
        const val fastMillis = 150
        const val normalMillis = 220
        const val sheetEnterMillis = 300
        const val sheetExitMillis = 250
        const val slowMillis = 400
        const val cinematicMillis = 700

        val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
        val accelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    }

    object Icon {
        val xs = Space.s12
        val sm = Space.s16
        val md = Space.s20
        val lg = Space.s24
        val xl = Space.s32
        val xxl = Space.s40
    }

    object Type {
        val labelXs = 11.sp
        val labelSm = 12.sp
        val bodySm = 13.sp
        val bodyMd = 14.sp
        val bodyApp = 15.sp
        val bodyLg = 16.sp
        val titleSm = 18.sp
        val titleMd = 20.sp
        val titleLg = 22.sp
        val headline = 24.sp
        val displaySm = 24.sp
        val pageDisplay = 24.sp
        val displayMd = 28.sp
    }

    object LineHeight {
        val labelXs = 15.sp
        val labelSm = 16.sp
        val bodySm = 19.sp
        val bodyMd = 21.sp
        val bodyApp = 22.sp
        val bodyLg = 24.sp
        val titleSm = 24.sp
        val materialTitleLarge = 24.sp
        val headline = 30.sp
        val titleMd = 26.sp
        val titleLg = 28.sp
        val displaySm = 30.sp
        val pageDisplay = 30.sp
        val displayMd = 34.sp
    }

    object LetterSpacing {
        val none = 0.sp
        val pageDisplay = (-0.2).sp
        val headline = (-0.15).sp
        val display = (-0.15).sp
        val label = 0.2.sp
    }

    object Breakpoint {
        val phone = 0.dp
        val largePhone = 420.dp
        val tablet = 600.dp
        val largeTablet = 840.dp
        val desktop = 1024.dp
        val playerWide = 1280.dp
    }
}

@Immutable
data class LiquidGlassColorTokens(
    val background: Color,
    val backgroundAmoled: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceCard: Color,
    val surfaceSheet: Color,
    val surfaceDialog: Color,
    val surfacePopover: Color,
    val nativeChrome: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textInverse: Color,
    val accent: Color,
    val accentStrong: Color,
    val onAccent: Color,
    val focusRing: Color,
    val focusBackground: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val borderSelected: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val neutral: Color,
    val overlayScrim: Color,
    val overlayHover: Color,
    val overlayPressed: Color,
    val overlaySelected: Color,
    val overlayDisabled: Color,
    val shimmer: Color,
    val skeleton: Color,
    val playerControlsBackground: Color,
    val playerControlsForeground: Color,
    val playerTimelineTrack: Color,
    val playerTimelineFill: Color,
    val playerSubtitlePreview: Color,
    val playerBuffering: Color,
    val parentalGuide: Color,
)

@Immutable
data class LiquidGlassShapeTokens(
    val card: Shape,
    val compactCard: Shape,
    val sheet: Shape,
    val dialog: Shape,
    val button: Shape,
    val chip: Shape,
    val poster: Shape,
    val avatar: Shape,
    val playerPanel: Shape,
)

@Immutable
data class LiquidGlassSpacingTokens(
    val screenHorizontal: Dp,
    val screenTop: Dp,
    val screenBottom: Dp,
    val sectionGap: Dp,
    val listGap: Dp,
    val railGap: Dp,
    val cardPadding: Dp,
    val cardPaddingCompact: Dp,
    val controlGap: Dp,
    val dialogPadding: Dp,
    val sheetPadding: Dp,
    val playerOverlayPadding: Dp,
)

@Immutable
data class LiquidGlassBorderTokens(
    val hairline: Dp,
    val thin: Dp,
    val medium: Dp,
)

@Immutable
data class LiquidGlassElevationTokens(
    val flat: Dp,
    val raised: Dp,
    val modal: Dp,
    val overlay: Dp,
    val playerControls: Dp,
)

@Immutable
data class LiquidGlassMotionTokens(
    val instantMillis: Int,
    val fastMillis: Int,
    val normalMillis: Int,
    val sheetEnterMillis: Int,
    val sheetExitMillis: Int,
    val slowMillis: Int,
    val cinematicMillis: Int,
    val standard: Easing,
    val emphasized: Easing,
    val decelerate: Easing,
    val accelerate: Easing,
)

@Immutable
data class LiquidGlassOpacityTokens(
    val invisible: Float,
    val disabled: Float,
    val secondary: Float,
    val muted: Float,
    val selected: Float,
    val hover: Float,
    val pressed: Float,
    val subtle: Float,
    val medium: Float,
    val strong: Float,
    val overlayLight: Float,
    val overlayMedium: Float,
    val overlayHeavy: Float,
    val visible: Float,
)

@Immutable
data class LiquidGlassIconTokens(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
)

@Immutable
data class LiquidGlassBreakpointTokens(
    val phone: Dp,
    val largePhone: Dp,
    val tablet: Dp,
    val largeTablet: Dp,
    val desktop: Dp,
    val playerWide: Dp,
)

@Immutable
data class LiquidGlassComponentTokens(
    val navItemShape: Shape,
    val navIconSize: Dp,
    val navItemMaxWidth: Dp,
    val sheetMaxWidth: Dp,
    val dialogMaxWidth: Dp,
    val chipHorizontalPadding: Dp,
    val chipVerticalPadding: Dp,
    val posterRadius: Dp,
    val avatarSize: Dp,
    val playerPanelMaxWidth: Dp,
)

@Immutable
data class LiquidGlassThemeTokens(
    val colors: LiquidGlassColorTokens,
    val spacing: LiquidGlassSpacingTokens,
    val shapes: LiquidGlassShapeTokens,
    val borders: LiquidGlassBorderTokens,
    val elevation: LiquidGlassElevationTokens,
    val motion: LiquidGlassMotionTokens,
    val opacity: LiquidGlassOpacityTokens,
    val icons: LiquidGlassIconTokens,
    val breakpoints: LiquidGlassBreakpointTokens,
    val components: LiquidGlassComponentTokens,
)

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF131417),
    val backgroundElevated: Color = Color(0xFF2A2D36),
    val backgroundCard: Color = Color(0xFF21232A),
)

fun ThemeColorPalette.accentBrush(): Brush =
    if (accentGradient.size > 1) Brush.linearGradient(accentGradient) else Brush.linearGradient(listOf(secondary, secondary))

fun computeAccentContainerColor(accent: Color, baseBg: Color): Color {
    val isAmoled = baseBg == Color.Black || baseBg.luminance() < 0.005f
    val containerBase = if (isAmoled) Color(0xFF14151A) else Color(0xFF21232A)
    return accent.copy(alpha = 0.07f).compositeOver(containerBase)
}

fun computeAccentElevatedColor(accent: Color, baseBg: Color): Color {
    val isAmoled = baseBg == Color.Black || baseBg.luminance() < 0.005f
    val elevatedBase = if (isAmoled) Color(0xFF1E2027) else Color(0xFF2A2D36)
    return accent.copy(alpha = 0.10f).compositeOver(elevatedBase)
}

object ThemeColors {
    val VelvetNoir = ThemeColorPalette(
        secondary = Color(0xFF991B1B),
        secondaryVariant = Color(0xFF450A10),
        nativeAccentHex = "#991B1B",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFDC2626),
        focusBackground = Color(0x33991B1B),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val AuraBrass = ThemeColorPalette(
        secondary = Color(0xFFB98A28),
        secondaryVariant = Color(0xFF59410D),
        nativeAccentHex = "#B98A28",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFD9A74A),
        focusBackground = Color(0x33B98A28),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val SmokedCognac = ThemeColorPalette(
        secondary = Color(0xFF9A3412),
        secondaryVariant = Color(0xFF4D1A09),
        nativeAccentHex = "#9A3412",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFBC4712),
        focusBackground = Color(0x339A3412),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val ObsidianPine = ThemeColorPalette(
        secondary = Color(0xFF065F46),
        secondaryVariant = Color(0xFF022B20),
        nativeAccentHex = "#065F46",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF047857),
        focusBackground = Color(0x33065F46),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val FincherTeal = ThemeColorPalette(
        secondary = Color(0xFF0E7490),
        secondaryVariant = Color(0xFF062E2C),
        nativeAccentHex = "#0E7490",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF0D7972),
        focusBackground = Color(0x330E7490),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val MidnightLapis = ThemeColorPalette(
        secondary = Color(0xFF1E40AF),
        secondaryVariant = Color(0xFF0E1E5B),
        nativeAccentHex = "#1E40AF",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF3B82F6),
        focusBackground = Color(0x331E40AF),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    val NocturneViolet = ThemeColorPalette(
        secondary = Color(0xFF581C87),
        secondaryVariant = Color(0xFF2A0844),
        nativeAccentHex = "#581C87",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF7022AC),
        focusBackground = Color(0x33581C87),
        background = Color(0xFF131417),
        backgroundElevated = Color(0xFF2A2D36),
        backgroundCard = Color(0xFF21232A),
    )

    // Backward-compatible alias for default theme
    val White = VelvetNoir
}

fun getSignaturePalette(primaryColorKey: String?): ThemeColorPalette = when (primaryColorKey) {
    "VelvetNoir" -> ThemeColors.VelvetNoir
    "AuraBrass" -> ThemeColors.AuraBrass
    "SmokedCognac" -> ThemeColors.SmokedCognac
    "ObsidianPine" -> ThemeColors.ObsidianPine
    "FincherTeal" -> ThemeColors.FincherTeal
    "MidnightLapis" -> ThemeColors.MidnightLapis
    "NocturneViolet" -> ThemeColors.NocturneViolet
    else -> ThemeColors.VelvetNoir
}

internal val LocalLiquidGlassThemeTokens = staticCompositionLocalOf {
    defaultLiquidGlassThemeTokens(ThemeColors.VelvetNoir, amoled = false, colorScheme = null)
}

val MaterialTheme.liquidGlass: LiquidGlassThemeTokens
    @Composable
    @Stable
    get() = LocalLiquidGlassThemeTokens.current

val LocalThemePalette = staticCompositionLocalOf { ThemeColors.VelvetNoir }

val MaterialTheme.themePalette: ThemeColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePalette.current

internal fun defaultLiquidGlassThemeTokens(
    palette: ThemeColorPalette,
    amoled: Boolean,
    colorScheme: ColorScheme?,
): LiquidGlassThemeTokens {
    val background = if (amoled) Color.Black else palette.background
    val textPrimary = Color(0xFFF5F7F8)
    val textSecondary = Color(0xFFC2C8D0)
    val textMuted = Color(0xFFA4AAB3)
    val surface = palette.backgroundElevated
    val surfaceCard = palette.backgroundCard
    val accent = palette.secondary
    val accentStrong = palette.secondaryVariant
    val borderSubtle = if (amoled) Color(0x2EFFFFFF) else Color(0xFF363A45).copy(alpha = 0.72f)
    val borderDefault = if (amoled) Color(0x47FFFFFF) else Color(0xFF414654)
    val overlayScrim = Color.Black.copy(alpha = LiquidGlassTokens.Opacity.overlayMedium)

    return LiquidGlassThemeTokens(
        colors = LiquidGlassColorTokens(
            background = background,
            backgroundAmoled = Color.Black,
            surface = surface,
            surfaceElevated = surface,
            surfaceCard = surfaceCard,
            surfaceSheet = surface,
            surfaceDialog = surface,
            surfacePopover = surfaceCard,
            nativeChrome = background,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            textDisabled = textMuted.copy(alpha = LiquidGlassTokens.Opacity.disabled),
            textInverse = Color(0xFF111111),
            accent = accent,
            accentStrong = accentStrong,
            onAccent = palette.onSecondary,
            focusRing = palette.focusRing,
            focusBackground = palette.focusBackground,
            borderSubtle = borderSubtle,
            borderDefault = borderDefault,
            borderStrong = if (amoled) Color(0x5CFFFFFF) else Color(0xFF4E5464),
            borderFocus = palette.focusRing,
            borderSelected = accent.copy(alpha = LiquidGlassTokens.Opacity.strong),
            success = Color(0xFF66BB6A),
            warning = Color(0xFFFFC857),
            danger = colorScheme?.error ?: Color(0xFFE36A8A),
            info = Color(0xFF42A5F5),
            neutral = textMuted,
            overlayScrim = overlayScrim,
            overlayHover = Color.White.copy(alpha = LiquidGlassTokens.Opacity.hover),
            overlayPressed = Color.White.copy(alpha = LiquidGlassTokens.Opacity.pressed),
            overlaySelected = Color.White.copy(alpha = LiquidGlassTokens.Opacity.selected),
            overlayDisabled = Color.Black.copy(alpha = LiquidGlassTokens.Opacity.disabled),
            shimmer = Color.White.copy(alpha = 0.14f),
            skeleton = Color.White.copy(alpha = 0.09f),
            playerControlsBackground = Color.Black.copy(alpha = 0.72f),
            playerControlsForeground = Color.White,
            playerTimelineTrack = Color.White.copy(alpha = 0.30f),
            playerTimelineFill = accent,
            playerSubtitlePreview = Color.Black.copy(alpha = 0.55f),
            playerBuffering = accent,
            parentalGuide = Color(0xFF5D1F1F),
        ),
        spacing = LiquidGlassSpacingTokens(
            screenHorizontal = LiquidGlassTokens.Space.s16,
            screenTop = LiquidGlassTokens.Space.s10,
            screenBottom = LiquidGlassTokens.Space.s18,
            sectionGap = LiquidGlassTokens.Space.s24,
            listGap = LiquidGlassTokens.Space.s12,
            railGap = LiquidGlassTokens.Space.s14,
            cardPadding = LiquidGlassTokens.Space.s18,
            cardPaddingCompact = LiquidGlassTokens.Space.s14,
            controlGap = LiquidGlassTokens.Space.s8,
            dialogPadding = LiquidGlassTokens.Space.s20,
            sheetPadding = LiquidGlassTokens.Space.s20,
            playerOverlayPadding = LiquidGlassTokens.Space.s24,
        ),
        shapes = LiquidGlassShapeTokens(
            card = RoundedCornerShape(LiquidGlassTokens.Radius.card),
            compactCard = RoundedCornerShape(LiquidGlassTokens.Radius.compactCard),
            sheet = RoundedCornerShape(LiquidGlassTokens.Radius.sheet),
            dialog = RoundedCornerShape(LiquidGlassTokens.Radius.dialog),
            button = RoundedCornerShape(LiquidGlassTokens.Radius.button),
            chip = RoundedCornerShape(LiquidGlassTokens.Radius.chip),
            poster = RoundedCornerShape(LiquidGlassTokens.Radius.poster),
            avatar = RoundedCornerShape(LiquidGlassTokens.Radius.avatar),
            playerPanel = RoundedCornerShape(LiquidGlassTokens.Radius.playerPanel),
        ),
        borders = LiquidGlassBorderTokens(
            hairline = LiquidGlassTokens.Border.hairline,
            thin = LiquidGlassTokens.Border.thin,
            medium = LiquidGlassTokens.Border.medium,
        ),
        elevation = LiquidGlassElevationTokens(
            flat = LiquidGlassTokens.Elevation.none,
            raised = LiquidGlassTokens.Elevation.raised,
            modal = LiquidGlassTokens.Elevation.modal,
            overlay = LiquidGlassTokens.Elevation.overlay,
            playerControls = LiquidGlassTokens.Elevation.playerControls,
        ),
        motion = LiquidGlassMotionTokens(
            instantMillis = LiquidGlassTokens.Motion.instantMillis,
            fastMillis = LiquidGlassTokens.Motion.fastMillis,
            normalMillis = LiquidGlassTokens.Motion.normalMillis,
            sheetEnterMillis = LiquidGlassTokens.Motion.sheetEnterMillis,
            sheetExitMillis = LiquidGlassTokens.Motion.sheetExitMillis,
            slowMillis = LiquidGlassTokens.Motion.slowMillis,
            cinematicMillis = LiquidGlassTokens.Motion.cinematicMillis,
            standard = LiquidGlassTokens.Motion.standard,
            emphasized = LiquidGlassTokens.Motion.emphasized,
            decelerate = LiquidGlassTokens.Motion.decelerate,
            accelerate = LiquidGlassTokens.Motion.accelerate,
        ),
        opacity = LiquidGlassOpacityTokens(
            invisible = LiquidGlassTokens.Opacity.invisible,
            disabled = LiquidGlassTokens.Opacity.disabled,
            secondary = LiquidGlassTokens.Opacity.secondary,
            muted = LiquidGlassTokens.Opacity.muted,
            selected = LiquidGlassTokens.Opacity.selected,
            hover = LiquidGlassTokens.Opacity.hover,
            pressed = LiquidGlassTokens.Opacity.pressed,
            subtle = LiquidGlassTokens.Opacity.subtle,
            medium = LiquidGlassTokens.Opacity.medium,
            strong = LiquidGlassTokens.Opacity.strong,
            overlayLight = LiquidGlassTokens.Opacity.overlayLight,
            overlayMedium = LiquidGlassTokens.Opacity.overlayMedium,
            overlayHeavy = LiquidGlassTokens.Opacity.overlayHeavy,
            visible = LiquidGlassTokens.Opacity.visible,
        ),
        icons = LiquidGlassIconTokens(
            xs = LiquidGlassTokens.Icon.xs,
            sm = LiquidGlassTokens.Icon.sm,
            md = LiquidGlassTokens.Icon.md,
            lg = LiquidGlassTokens.Icon.lg,
            xl = LiquidGlassTokens.Icon.xl,
            xxl = LiquidGlassTokens.Icon.xxl,
        ),
        breakpoints = LiquidGlassBreakpointTokens(
            phone = LiquidGlassTokens.Breakpoint.phone,
            largePhone = LiquidGlassTokens.Breakpoint.largePhone,
            tablet = LiquidGlassTokens.Breakpoint.tablet,
            largeTablet = LiquidGlassTokens.Breakpoint.largeTablet,
            desktop = LiquidGlassTokens.Breakpoint.desktop,
            playerWide = LiquidGlassTokens.Breakpoint.playerWide,
        ),
        components = LiquidGlassComponentTokens(
            navItemShape = RoundedCornerShape(LiquidGlassTokens.Radius.xl),
            navIconSize = LiquidGlassTokens.Icon.xl,
            navItemMaxWidth = 150.dp,
            sheetMaxWidth = 520.dp,
            dialogMaxWidth = 460.dp,
            chipHorizontalPadding = LiquidGlassTokens.Space.s14,
            chipVerticalPadding = LiquidGlassTokens.Space.s8,
            posterRadius = LiquidGlassTokens.Radius.poster,
            avatarSize = LiquidGlassTokens.Space.s48,
            playerPanelMaxWidth = 600.dp,
        ),
    )
}

val OutfitFontFamily = FontFamily(
    Font(R.font.font_outfit_thin, FontWeight.Thin),
    Font(R.font.font_outfit_extralight, FontWeight.ExtraLight),
    Font(R.font.font_outfit_light, FontWeight.Light),
    Font(R.font.font_outfit_regular, FontWeight.Normal),
    Font(R.font.font_outfit_medium, FontWeight.Medium),
    Font(R.font.font_outfit_semibold, FontWeight.SemiBold),
    Font(R.font.font_outfit_bold, FontWeight.Bold),
    Font(R.font.font_outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.font_outfit_black, FontWeight.Black),
)

val FigtreeFontFamily = FontFamily(
    Font(R.font.font_figtree_light, FontWeight.Light),
    Font(R.font.font_figtree_regular, FontWeight.Normal),
    Font(R.font.font_figtree_medium, FontWeight.Medium),
    Font(R.font.font_figtree_semibold, FontWeight.SemiBold),
    Font(R.font.font_figtree_bold, FontWeight.Bold),
    Font(R.font.font_figtree_extrabold, FontWeight.ExtraBold),
    Font(R.font.font_figtree_black, FontWeight.Black),
)

data class LiquidGlassTypeScale(
    val labelXs: TextStyle,
    val labelSm: TextStyle,
    val bodySm: TextStyle,
    val bodyMd: TextStyle,
    val bodyLg: TextStyle,
    val titleSm: TextStyle,
    val titleMd: TextStyle,
    val titleLg: TextStyle,
    val displaySm: TextStyle,
    val displayMd: TextStyle,
    val timer: TextStyle = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    ),
)

internal val LocalLiquidGlassTypeScale = staticCompositionLocalOf {
    LiquidGlassTypeScale(
        labelXs = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelXs,
            lineHeight = LiquidGlassTokens.LineHeight.labelXs,
            fontWeight = FontWeight.Medium,
        ),
        labelSm = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelSm,
            lineHeight = LiquidGlassTokens.LineHeight.labelSm,
            fontWeight = FontWeight.Medium,
        ),
        bodySm = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyMd,
            lineHeight = LiquidGlassTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodyLg = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyLg,
            lineHeight = LiquidGlassTokens.LineHeight.bodyLg,
            fontWeight = FontWeight.Normal,
        ),
        titleSm = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleSm,
            lineHeight = LiquidGlassTokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMd = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleMd,
            lineHeight = LiquidGlassTokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLg = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleLg,
            lineHeight = LiquidGlassTokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySm = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.displaySm,
            lineHeight = LiquidGlassTokens.LineHeight.displaySm,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        displayMd = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.displayMd,
            lineHeight = LiquidGlassTokens.LineHeight.displayMd,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        timer = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum",
        ),
    )
}

val MaterialTheme.liquidGlassTypeScale: LiquidGlassTypeScale
    @Composable
    get() = LocalLiquidGlassTypeScale.current

private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFF5F7F8)

private fun buildColorScheme(palette: ThemeColorPalette, amoled: Boolean = false) = darkColorScheme(
    primary = palette.secondary,
    onPrimary = palette.onSecondary,
    primaryContainer = palette.backgroundCard,
    onPrimaryContainer = Color(0xFFF5F7F8),
    secondary = palette.secondaryVariant,
    onSecondary = palette.onSecondaryVariant,
    background = if (amoled) Color.Black else palette.background,
    onBackground = Color(0xFFF5F7F8),
    surface = palette.backgroundElevated,
    onSurface = Color(0xFFF5F7F8),
    surfaceVariant = palette.backgroundCard,
    onSurfaceVariant = Color(0xFFA4AAB3),
    surfaceContainerLow = palette.backgroundCard,
    surfaceContainer = palette.backgroundCard,
    surfaceContainerHigh = palette.backgroundElevated,
    surfaceContainerHighest = palette.backgroundElevated,
    outline = if (amoled) Color(0x47FFFFFF) else Color(0xFF414654),
    outlineVariant = if (amoled) Color(0x2EFFFFFF) else Color(0xFF323642),
    error = Color(0xFFE36A8A),
    onError = Color(0xFFFCE5EC),
)

private val LiquidGlassTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.pageDisplay,
            lineHeight = LiquidGlassTokens.LineHeight.pageDisplay,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.pageDisplay,
        ),
        displayMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.displaySm,
            lineHeight = LiquidGlassTokens.LineHeight.displaySm,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        displaySmall = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleLg,
            lineHeight = LiquidGlassTokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        headlineLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.headline,
            lineHeight = LiquidGlassTokens.LineHeight.headline,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.headline,
        ),
        headlineMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleMd,
            lineHeight = LiquidGlassTokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineSmall = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleSm,
            lineHeight = LiquidGlassTokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleSm,
            lineHeight = LiquidGlassTokens.LineHeight.materialTitleLarge,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyLg,
            lineHeight = LiquidGlassTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Medium,
        ),
        titleSmall = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyMd,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Medium,
        ),
        bodyLarge = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyApp,
            lineHeight = LiquidGlassTokens.LineHeight.bodyApp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyMd,
            lineHeight = LiquidGlassTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodySmall = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Medium,
        ),
        labelMedium = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelSm,
            lineHeight = LiquidGlassTokens.LineHeight.labelXs,
            fontWeight = FontWeight.Medium,
            letterSpacing = LiquidGlassTokens.LetterSpacing.label,
        ),
        labelSmall = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelXs,
            lineHeight = LiquidGlassTokens.LineHeight.labelXs,
            fontWeight = FontWeight.Medium,
        ),
    )

private val LiquidGlassTypeTokens: LiquidGlassTypeScale
    @Composable
    get() = LiquidGlassTypeScale(
        labelXs = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelXs,
            lineHeight = LiquidGlassTokens.LineHeight.labelXs,
            fontWeight = FontWeight.Medium,
        ),
        labelSm = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.labelSm,
            lineHeight = LiquidGlassTokens.LineHeight.labelSm,
            fontWeight = FontWeight.Medium,
        ),
        bodySm = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyMd,
            lineHeight = LiquidGlassTokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodyLg = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodyLg,
            lineHeight = LiquidGlassTokens.LineHeight.bodyLg,
            fontWeight = FontWeight.Normal,
        ),
        titleSm = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleSm,
            lineHeight = LiquidGlassTokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMd = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleMd,
            lineHeight = LiquidGlassTokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLg = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.titleLg,
            lineHeight = LiquidGlassTokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySm = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.displaySm,
            lineHeight = LiquidGlassTokens.LineHeight.displaySm,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        displayMd = TextStyle(
            fontFamily = OutfitFontFamily,
            fontSize = LiquidGlassTokens.Type.displayMd,
            lineHeight = LiquidGlassTokens.LineHeight.displayMd,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = LiquidGlassTokens.LetterSpacing.display,
        ),
        timer = TextStyle(
            fontFamily = FigtreeFontFamily,
            fontSize = LiquidGlassTokens.Type.bodySm,
            lineHeight = LiquidGlassTokens.LineHeight.bodySm,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum",
        ),
    )

private val LiquidGlassRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.12f,
        hoveredAlpha = 0.08f,
        pressedAlpha = 0.12f,
    ),
)

@Composable
fun LiquidGlassTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val settings = rememberAppSettings()
    val pureBlack by settings.ui.pureBlack.collectAsState()
    val primaryColorKey by settings.ui.primaryColor.collectAsState()
    val dynamicAccent = remember(primaryColorKey) { perfToColor(primaryColorKey).color }

    val liquidGlassEnabledFlow = remember(context) {
        LiquidGlassSettingsRepository.ensureLoaded(context)
        LiquidGlassSettingsRepository.liquidGlassEnabled
    }
    val posterCardStyleFlow = remember(context) {
        PosterCardStyleRepository.ensureLoaded(context)
        PosterCardStyleRepository.state
    }
    val cardDepthStyleFlow = remember(context) {
        CardDepthRepository.ensureLoaded(context)
        CardDepthRepository.state
    }
    val liquidGlassEnabled by liquidGlassEnabledFlow.collectAsState()
    val posterCardStyle by posterCardStyleFlow.collectAsState()
    val cardDepthStyle by cardDepthStyleFlow.collectAsState()

    val palette = remember(primaryColorKey, dynamicAccent, pureBlack) {
        val baseBg = if (pureBlack) Color.Black else Color(0xFF131417)
        val basePalette = getSignaturePalette(primaryColorKey)
        val subtleAccentContainer = computeAccentContainerColor(dynamicAccent, baseBg)
        val subtleAccentElevated = computeAccentElevatedColor(dynamicAccent, baseBg)

        val isMonet = primaryColorKey == "Monet" || primaryColorKey == "Monet2"
        if (isMonet) {
            basePalette.copy(
                secondary = dynamicAccent,
                secondaryVariant = dynamicAccent,
                nativeAccentHex = String.format("#%06X", (0xFFFFFF and dynamicAccent.value.toInt())),
                focusRing = dynamicAccent,
                focusBackground = dynamicAccent.copy(alpha = 0.20f),
                background = baseBg,
                backgroundElevated = subtleAccentElevated,
                backgroundCard = subtleAccentContainer,
            )
        } else {
            basePalette.copy(
                background = baseBg,
                backgroundElevated = subtleAccentElevated,
                backgroundCard = subtleAccentContainer,
            )
        }
    }
    val colorScheme = buildColorScheme(palette, amoled = pureBlack)
    val tokens = defaultLiquidGlassThemeTokens(palette, amoled = pureBlack, colorScheme = colorScheme)
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f,
        ),
        LocalLiquidGlass provides liquidGlassEnabled,
        LocalPosterCardStyle provides posterCardStyle,
        LocalCardDepthStyle provides cardDepthStyle,
        LocalLiquidGlassThemeTokens provides tokens,
        LocalLiquidGlassTypeScale provides LiquidGlassTypeTokens,
        LocalRippleConfiguration provides LiquidGlassRippleConfiguration,
        LocalThemePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LiquidGlassTypography,
            content = content
        )
    }
}
