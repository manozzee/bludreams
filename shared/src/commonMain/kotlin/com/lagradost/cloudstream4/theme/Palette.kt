package com.lagradost.cloudstream4.theme

import androidx.compose.ui.graphics.Color

enum class CloudStreamPrimaryColor(val color: Color) {
    VELVET_NOIR(Color(0xFF991B1B)),
    AURA_BRASS(Color(0xFFB98A28)),
    SMOKED_COGNAC(Color(0xFF9A3412)),
    OBSIDIAN_PINE(Color(0xFF065F46)),
    FINCHER_TEAL(Color(0xFF0E7490)),
    MIDNIGHT_LAPIS(Color(0xFF1E40AF)),
    NOCTURNE_VIOLET(Color(0xFF581C87)),
    DYNAMIC(Color(0xFF991B1B)),
    DYNAMIC_TWO(Color(0xFF991B1B)),
}


internal object CloudStreamPalette {
    // Default dark (AppTheme / Black)
    val Primary = Color(0xFF3D50FA)
    val PrimaryDark = Color(0xFF3700B3)
    val Ongoing = Color(0xFFF53B66)

    // Default luxury dark charcoal / slate profile (AMOLED Off)
    val DarkPrimaryGrayBg = Color(0xFF21232A) // Elevated cards
    val DarkBlackBg = Color(0xFF131417)        // Root window / canvas
    val DarkIconGrayBg = Color(0xFF2A2D36)     // Elevated dialogs / surfaces
    val DarkBoxItemBg = Color(0xFF21232A)      // Elevated containers / bottom sheets
    val DarkText = Color(0xFFE9EAEE)
    val DarkGrayText = Color(0xFFA4AAB3)
    val DarkIcon = Color(0xFFA4AAB3)

    // Pure Black / AMOLED (AMOLED On)
    val AmoledBlack = Color(0xFF000000)
    val AmoledElevated = Color(0xFF1E2027)
    val AmoledCard = Color(0xFF14151A)
    val AmoledNearBlack = Color(0xFF14151A)

    // Light
    val LightPrimaryGrayBg = Color(0xFFF1F1F1)
    val LightBlackBg = Color(0xFFFFFFFF)
    val LightIconGrayBg = Color(0xFFEEEEEE)
    val LightBoxItemBg = Color(0xFFEEEEEE)
    val LightText = Color(0xFF202125)
    val LightGrayText = Color(0xFF5F6267)
    val LightIcon = Color(0xFF5F6267)

    // Dracula
    val DraculaPrimaryGrayBg = Color(0xFF414450)
    val DraculaBlackBg = Color(0xFF282A36)
    val DraculaIconGrayBg = Color(0xFF44475A)
    val DraculaBoxItemBg = Color(0xFF373844)
    val DraculaText = Color(0xFFF8F8F2)
    val DraculaGrayText = Color(0xFF6272A4)
    val DraculaIcon = Color(0xFF6272A4)

    // Lavender Dreams
    val LavenderPrimaryGrayBg = Color(0xFFF7EEFC)
    val LavenderBlackBg = Color(0xFFFDF0FB)
    val LavenderIconGrayBg = Color(0xFFB794F6)
    val LavenderBoxItemBg = Color(0xFFF8F5FF)
    val LavenderText = Color(0xFF2D1B47)
    val LavenderGrayText = Color(0xFF9AB3FF)
    val LavenderIcon = Color(0xFF7C3AED)

    // Silent Blue
    val SilentBluePrimaryGrayBg = Color(0xFF282F49)
    val SilentBlueBlackBg = Color(0xFF151A30)
    val SilentBlueIconGrayBg = Color(0xFF3A446A)
    val SilentBlueBoxItemBg = Color(0xFF3A446A)
    val SilentBlueText = Color(0xFFE0E1F3)
    val SilentBlueGrayText = Color(0xFF7B83B0)
    val SilentBlueIcon = Color(0xFF7B83B0)
}
