package com.mihon.presentation.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

enum class GroupPosition {
    Single,
    First,
    Middle,
    Last
}

fun getGroupPosition(index: Int, total: Int): GroupPosition = when {
    total <= 1 -> GroupPosition.Single
    index == 0 -> GroupPosition.First
    index == total - 1 -> GroupPosition.Last
    else -> GroupPosition.Middle
}

fun getCardShape(position: GroupPosition): RoundedCornerShape = when (position) {
    GroupPosition.Single -> RoundedCornerShape(28.dp)
    GroupPosition.First -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    GroupPosition.Middle -> RoundedCornerShape(12.dp)
    GroupPosition.Last -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
}

val CardVerticalGap = 3.dp

val LocalCardBackgroundColor = compositionLocalOf<Color?> { null }

val CardBackgroundColor: Color
    @Composable
    get() = LocalCardBackgroundColor.current ?: MaterialTheme.colorScheme.surfaceVariant

@Composable
fun Modifier.settingsCard(
    position: GroupPosition,
    backgroundColor: Color = CardBackgroundColor,
): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
    .padding(bottom = if (position == GroupPosition.First || position == GroupPosition.Middle) CardVerticalGap else 0.dp)
    .clip(getCardShape(position))
    .background(backgroundColor)

val LocalGroupPosition = compositionLocalOf { GroupPosition.Single }
val LocalTileBackgroundColor = compositionLocalOf<Color?> { null }

val TileSize = 42.dp
val TileCornerRadius = 12.dp

@Composable
fun defaultTileColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

@Composable
fun getTileColorForTitle(title: String?): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

@Composable
fun SettingsIconTile(
    icon: Painter,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    iconTint: Color = Color.White,
) {
    val bg = backgroundColor ?: LocalTileBackgroundColor.current ?: defaultTileColor()
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(RoundedCornerShape(TileCornerRadius))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}
