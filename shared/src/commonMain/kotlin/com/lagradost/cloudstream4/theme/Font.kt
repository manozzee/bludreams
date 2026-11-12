package com.lagradost.cloudstream4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.figtree_black
import com.lagradost.cloudstream4.generated.resources.figtree_blackitalic
import com.lagradost.cloudstream4.generated.resources.figtree_bold
import com.lagradost.cloudstream4.generated.resources.figtree_bolditalic
import com.lagradost.cloudstream4.generated.resources.figtree_extrabold
import com.lagradost.cloudstream4.generated.resources.figtree_extrabolditalic
import com.lagradost.cloudstream4.generated.resources.figtree_italic
import com.lagradost.cloudstream4.generated.resources.figtree_light
import com.lagradost.cloudstream4.generated.resources.figtree_lightitalic
import com.lagradost.cloudstream4.generated.resources.figtree_medium
import com.lagradost.cloudstream4.generated.resources.figtree_mediumitalic
import com.lagradost.cloudstream4.generated.resources.figtree_regular
import com.lagradost.cloudstream4.generated.resources.figtree_semibold
import com.lagradost.cloudstream4.generated.resources.figtree_semibolditalic
import com.lagradost.cloudstream4.generated.resources.figtree_variable
import com.lagradost.cloudstream4.generated.resources.outfit_black
import com.lagradost.cloudstream4.generated.resources.outfit_bold
import com.lagradost.cloudstream4.generated.resources.outfit_extrabold
import com.lagradost.cloudstream4.generated.resources.outfit_extralight
import com.lagradost.cloudstream4.generated.resources.outfit_light
import com.lagradost.cloudstream4.generated.resources.outfit_medium
import com.lagradost.cloudstream4.generated.resources.outfit_regular
import com.lagradost.cloudstream4.generated.resources.outfit_semibold
import com.lagradost.cloudstream4.generated.resources.outfit_thin
import com.lagradost.cloudstream4.generated.resources.outfit_variable
import org.jetbrains.compose.resources.Font

object AppFont {
    /**
     * Complete Outfit font family containing all available weights from Thin (100) to Black (900).
     */
    val outfit @Composable get() = FontFamily(
        Font(Res.font.outfit_thin, weight = FontWeight.Thin, style = FontStyle.Normal),
        Font(Res.font.outfit_extralight, weight = FontWeight.ExtraLight, style = FontStyle.Normal),
        Font(Res.font.outfit_light, weight = FontWeight.Light, style = FontStyle.Normal),
        Font(Res.font.outfit_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.outfit_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
        Font(Res.font.outfit_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
        Font(Res.font.outfit_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.outfit_extrabold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
        Font(Res.font.outfit_black, weight = FontWeight.Black, style = FontStyle.Normal)
    )

    /**
     * Variable font representation of Outfit (weights 100..900).
     */
    val outfitVariable @Composable get() = FontFamily(
        Font(Res.font.outfit_variable)
    )

    /**
     * Complete Figtree font family containing all available weights from Light (300) to Black (900)
     * including matching italic styles.
     */
    val figtree @Composable get() = FontFamily(
        Font(Res.font.figtree_light, weight = FontWeight.Light, style = FontStyle.Normal),
        Font(Res.font.figtree_lightitalic, weight = FontWeight.Light, style = FontStyle.Italic),
        Font(Res.font.figtree_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.figtree_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.figtree_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
        Font(Res.font.figtree_mediumitalic, weight = FontWeight.Medium, style = FontStyle.Italic),
        Font(Res.font.figtree_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
        Font(Res.font.figtree_semibolditalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
        Font(Res.font.figtree_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.figtree_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),
        Font(Res.font.figtree_extrabold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
        Font(Res.font.figtree_extrabolditalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
        Font(Res.font.figtree_black, weight = FontWeight.Black, style = FontStyle.Normal),
        Font(Res.font.figtree_blackitalic, weight = FontWeight.Black, style = FontStyle.Italic)
    )

    /**
     * Variable font representation of Figtree (weights 300..900).
     */
    val figtreeVariable @Composable get() = FontFamily(
        Font(Res.font.figtree_variable)
    )

    val googleSans @Composable get() = outfit

    private val defaultTypography = androidx.compose.material3.Typography()
    val typography: Typography @Composable get() {
        val outfitFont = outfit
        val figtreeFont = figtree
        val lineHeight = 1.3.em
        return Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            titleLarge = defaultTypography.titleLarge.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = outfitFont, fontWeight = FontWeight.SemiBold, lineHeight = lineHeight),

            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = figtreeFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = figtreeFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = figtreeFont, fontWeight = FontWeight.Normal, lineHeight = lineHeight),

            labelLarge = defaultTypography.labelLarge.copy(fontFamily = outfitFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = outfitFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = outfitFont, fontWeight = FontWeight.Medium, lineHeight = lineHeight)
        )
    }
}
