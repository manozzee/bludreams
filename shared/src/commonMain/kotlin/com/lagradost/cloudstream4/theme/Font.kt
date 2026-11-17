package com.lagradost.cloudstream4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.inter_black
import com.lagradost.cloudstream4.generated.resources.inter_blackitalic
import com.lagradost.cloudstream4.generated.resources.inter_bold
import com.lagradost.cloudstream4.generated.resources.inter_bolditalic
import com.lagradost.cloudstream4.generated.resources.inter_extrabold
import com.lagradost.cloudstream4.generated.resources.inter_extrabolditalic
import com.lagradost.cloudstream4.generated.resources.inter_italic
import com.lagradost.cloudstream4.generated.resources.inter_light
import com.lagradost.cloudstream4.generated.resources.inter_lightitalic
import com.lagradost.cloudstream4.generated.resources.inter_medium
import com.lagradost.cloudstream4.generated.resources.inter_mediumitalic
import com.lagradost.cloudstream4.generated.resources.inter_regular
import com.lagradost.cloudstream4.generated.resources.inter_semibold
import com.lagradost.cloudstream4.generated.resources.inter_semibolditalic
import com.lagradost.cloudstream4.generated.resources.inter_variable
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_bold
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_extrabold
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_extralight
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_light
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_medium
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_regular
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_semibold
import com.lagradost.cloudstream4.generated.resources.plus_jakarta_sans_variable
import org.jetbrains.compose.resources.Font

object AppFont {
    /**
     * Complete Plus Jakarta Sans font family containing weights from ExtraLight (200) to ExtraBold (800).
     */
    val plusJakartaSans @Composable get() = FontFamily(
        Font(Res.font.plus_jakarta_sans_extralight, weight = FontWeight.ExtraLight, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_light, weight = FontWeight.Light, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.plus_jakarta_sans_extrabold, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
    )

    /**
     * Variable font representation of Plus Jakarta Sans (weights 200..800).
     */
    val plusJakartaSansVariable @Composable get() = FontFamily(
        Font(Res.font.plus_jakarta_sans_variable)
    )

    /**
     * Complete Inter font family containing weights from Light (300) to Black (900)
     * including matching italic styles.
     */
    val inter @Composable get() = FontFamily(
        Font(Res.font.inter_light, weight = FontWeight.Light, style = FontStyle.Normal),
        Font(Res.font.inter_lightitalic, weight = FontWeight.Light, style = FontStyle.Italic),
        Font(Res.font.inter_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.inter_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.inter_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
        Font(Res.font.inter_mediumitalic, weight = FontWeight.Medium, style = FontStyle.Italic),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
        Font(Res.font.inter_semibolditalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
        Font(Res.font.inter_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.inter_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),
        Font(Res.font.inter_extrabold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
        Font(Res.font.inter_extrabolditalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
        Font(Res.font.inter_black, weight = FontWeight.Black, style = FontStyle.Normal),
        Font(Res.font.inter_blackitalic, weight = FontWeight.Black, style = FontStyle.Italic)
    )

    /**
     * Variable font representation of Inter.
     */
    val interVariable @Composable get() = FontFamily(
        Font(Res.font.inter_variable)
    )

    val googleSans @Composable get() = plusJakartaSans

    private val defaultTypography = androidx.compose.material3.Typography()
    val typography: Typography @Composable get() {
        val displayFont = plusJakartaSans
        val bodyFont = inter
        val lineHeight = 1.3.em
        return Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            titleLarge = defaultTypography.titleLarge.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = bodyFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = bodyFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = bodyFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),

            labelLarge = defaultTypography.labelLarge.copy(fontFamily = displayFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = displayFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = displayFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight)
        )
    }
}
