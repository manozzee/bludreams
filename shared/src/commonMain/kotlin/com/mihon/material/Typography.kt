package com.mihon.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

val Typography.header: TextStyle
    @Composable
    get() = titleSmall.copy(
        fontFamily = com.lagradost.cloudstream4.theme.AppFont.outfit,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
