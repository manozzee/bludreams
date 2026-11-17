package com.lagradost.cloudstream3.ui.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.WatchType

private val HeroTextPrimary = Color(0xFFF5F5F7)
private val HeroSynopsisText = Color(0xFFB4B4BC)
private val HeroActionButtonSurface = Color(0xFF17171A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    primaryPlayLabel: String,
    watchType: WatchType,
    isFavorite: Boolean?,
    isSubscribed: Boolean?,
    showDownloadButton: Boolean,
    downloadLabel: String,
    hasTrailers: Boolean,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadLongClick: () -> Unit,
    onTrailerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val hasOverflowActions = isFavorite != null || isSubscribed != null || hasTrailers
    val isBookmarked = watchType != WatchType.NONE
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Primary Play / Resume pill button
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = primaryColor,
            contentColor = onPrimaryColor,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = onPrimaryPlayClick,
                    onLongClick = onPrimaryPlayLongClick,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = primaryPlayLabel,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryPlayLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 1. Watch Status / Bookmark circle button
        HeroCompactCircleButton(
            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = if (isBookmarked) {
                stringResource(watchType.stringRes)
            } else {
                stringResource(R.string.action_add_to_bookmarks)
            },
            isActive = isBookmarked,
            onClick = onBookmarkClick,
        )

        // 2. Download circle button
        if (showDownloadButton) {
            HeroCompactCircleButton(
                icon = Icons.Default.Download,
                contentDescription = downloadLabel,
                isActive = false,
                onClick = onDownloadClick,
                onLongClick = onDownloadLongClick,
            )
        }

        // 3. Overflow / More (⋮) circle button
        if (hasOverflowActions) {
            Box {
                HeroCompactCircleButton(
                    painter = painterResource(R.drawable.ic_baseline_more_vert_24),
                    contentDescription = "More actions",
                    isActive = false,
                    onClick = { showOverflowMenu = true },
                )

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    containerColor = HeroActionButtonSurface,
                ) {
                    if (isFavorite != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (isFavorite) {
                                            R.string.action_remove_from_favorites
                                        } else {
                                            R.string.action_add_to_favorites
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = HeroTextPrimary,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) {
                                        Icons.Default.Favorite
                                    } else {
                                        Icons.Default.FavoriteBorder
                                    },
                                    contentDescription = null,
                                    tint = if (isFavorite) primaryColor else HeroTextPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onFavoriteClick()
                            },
                        )
                    }

                    if (isSubscribed != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (isSubscribed) {
                                            R.string.action_unsubscribe
                                        } else {
                                            R.string.action_subscribe
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = HeroTextPrimary,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSubscribed) {
                                        Icons.Default.Notifications
                                    } else {
                                        Icons.Default.NotificationsNone
                                    },
                                    contentDescription = null,
                                    tint = if (isSubscribed) primaryColor else HeroTextPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onSubscribeClick()
                            },
                        )
                    }

                    if (hasTrailers) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.play_trailer_button),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = HeroTextPrimary,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = HeroTextPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onTrailerClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCompactCircleButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    HeroCompactCircleButton(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        isActive = isActive,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCompactCircleButton(
    painter: Painter,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        shape = CircleShape,
        color = HeroActionButtonSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) {
                primaryColor.copy(alpha = 0.55f)
            } else {
                Color.White.copy(alpha = 0.12f)
            },
        ),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = if (isActive) primaryColor else HeroTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun ExpandableDescription(
    description: String?,
    modifier: Modifier = Modifier,
) {
    val cleanDescription = remember(description) {
        description?.trim()?.takeIf { it.isNotEmpty() }
    } ?: return

    var isExpanded by remember(cleanDescription) { mutableStateOf(false) }
    var isEllipsized by remember(cleanDescription) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = cleanDescription,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = HeroSynopsisText,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!isExpanded) {
                    isEllipsized = result.hasVisualOverflow
                }
            },
        )
        if (isEllipsized || isExpanded) {
            Text(
                text = if (isExpanded) "Show Less" else "Read More",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

