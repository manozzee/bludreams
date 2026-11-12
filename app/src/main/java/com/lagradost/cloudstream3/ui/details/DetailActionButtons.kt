package com.lagradost.cloudstream3.ui.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.ui.liquidglass.CircularActionButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.home.PosterProgressBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    primaryPlayLabel: String,
    resumeSubLabel: String?,
    resumeProgressFraction: Float?,
    resumeTimeLeftLabel: String?,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Primary Play / Resume pill button
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .combinedClickable(
                    onClick = onPrimaryPlayClick,
                    onLongClick = onPrimaryPlayLongClick,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = primaryPlayLabel,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = primaryPlayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!resumeSubLabel.isNullOrBlank()) {
                        Text(
                            text = resumeSubLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Resume Progress Bar if in-progress
        if (resumeProgressFraction != null && resumeProgressFraction > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PosterProgressBar(
                    progress = resumeProgressFraction,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!resumeTimeLeftLabel.isNullOrBlank()) {
                    Text(
                        text = resumeTimeLeftLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Secondary action row (Watchlist / Favorite / Subscribe / Download / Trailer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isBookmarked = watchType != WatchType.NONE
            DetailSecondaryActionChip(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (isBookmarked) {
                    stringResource(watchType.stringRes)
                } else {
                    stringResource(R.string.action_add_to_bookmarks)
                },
                isActive = isBookmarked,
                onClick = onBookmarkClick,
                modifier = Modifier.weight(1f),
            )

            if (isFavorite != null) {
                CircularActionButton(
                    drawableRes = if (isFavorite) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24,
                    contentDescription = "Favorite",
                    isActive = isFavorite,
                    onClick = onFavoriteClick,
                )
            }

            if (isSubscribed != null) {
                CircularActionButton(
                    icon = if (isSubscribed) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = "Notify",
                    isActive = isSubscribed,
                    onClick = onSubscribeClick,
                )
            }

            if (showDownloadButton) {
                CircularActionButton(
                    icon = Icons.Default.Download,
                    contentDescription = downloadLabel,
                    isActive = false,
                    onClick = onDownloadClick,
                    onLongClick = onDownloadLongClick,
                )
            }

            if (hasTrailers) {
                CircularActionButton(
                    icon = Icons.Default.Movie,
                    contentDescription = "Trailer",
                    isActive = false,
                    onClick = onTrailerClick,
                )
            }
        }
    }
}

@Composable
private fun DetailSecondaryActionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (isActive) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else null,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailMetaInfo(
    yearText: String?,
    durationText: String?,
    contentRatingText: String?,
    ratingText: String?,
    statusText: String?,
    typeText: String?,
    apiNameText: String?,
    nextAiringEpisodeText: String?,
    nextAiringDateText: String?,
    vpnText: String?,
    metaText: String?,
    tags: List<String>,
    onTagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Primary metadata row: Year · Duration · Certification · IMDb Rating · Status
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val tokens = buildList {
                typeText?.takeIf { it.isNotBlank() }?.let(::add)
                yearText?.takeIf { it.isNotBlank() }?.let(::add)
                durationText?.takeIf { it.isNotBlank() }?.let(::add)
                statusText?.takeIf { it.isNotBlank() }?.let(::add)
            }
            tokens.forEachIndexed { index, token ->
                Text(
                    text = token,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                if (index < tokens.lastIndex) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            if (!contentRatingText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Text(
                        text = contentRatingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }

            if (!ratingText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF5C518).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color(0xFFF5C518).copy(alpha = 0.48f)),
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFF5C518),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF5C518),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (!apiNameText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Text(
                        text = apiNameText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Next airing info banner if available
        if (!nextAiringEpisodeText.isNullOrBlank() || !nextAiringDateText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Text(
                    text = listOfNotNull(nextAiringEpisodeText, nextAiringDateText).joinToString(" — "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // VPN / Meta warning if present
        if (!vpnText.isNullOrBlank() || !metaText.isNullOrBlank()) {
            Text(
                text = listOfNotNull(vpnText, metaText).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Genre / Tag pills
        if (tags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tags, key = { it }) { tag ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(enabled = onTagClick != null) {
                                onTagClick?.invoke(tag)
                            },
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
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
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = cleanDescription,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

