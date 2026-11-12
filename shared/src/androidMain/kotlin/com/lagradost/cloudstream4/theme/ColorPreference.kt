package com.lagradost.cloudstream4.theme

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager

/** TODO: This should be replaced with a better system */

fun perfToMode(perf: String?) =
    when (perf) {
        "Amoled" -> CloudStreamThemeMode.Amoled
        else -> CloudStreamThemeMode.Dark
    }

fun boolToMode(pureBlack: Boolean) =
    if (pureBlack) CloudStreamThemeMode.Amoled else CloudStreamThemeMode.Dark

fun perfToColor(perf: String?) = when (perf) {
    "VelvetNoir" -> CloudStreamPrimaryColor.VELVET_NOIR
    "AuraBrass" -> CloudStreamPrimaryColor.AURA_BRASS
    "SmokedCognac" -> CloudStreamPrimaryColor.SMOKED_COGNAC
    "ObsidianPine" -> CloudStreamPrimaryColor.OBSIDIAN_PINE
    "FincherTeal" -> CloudStreamPrimaryColor.FINCHER_TEAL
    "MidnightLapis" -> CloudStreamPrimaryColor.MIDNIGHT_LAPIS
    "NocturneViolet" -> CloudStreamPrimaryColor.NOCTURNE_VIOLET
    "Monet" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CloudStreamPrimaryColor.DYNAMIC
    } else {
        CloudStreamPrimaryColor.VELVET_NOIR
    }

    "Monet2" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CloudStreamPrimaryColor.DYNAMIC_TWO
    } else {
        CloudStreamPrimaryColor.VELVET_NOIR
    }

    else -> CloudStreamPrimaryColor.VELVET_NOIR
}

fun Context.loadThemeMode(): CloudStreamThemeMode {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return perfToMode(prefs.getString("theme_key", "AmoledLight"))
}

fun Context.loadPrimaryColor(): CloudStreamPrimaryColor {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return perfToColor(prefs.getString("primary_color_key", "VelvetNoir"))
}