package com.lagradost.cloudstream3.ui.details

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ui.home.PosterCardFromSearchResponse
import com.lagradost.cloudstream3.ui.home.SectionHeader
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.cardDepth

enum class DetailInfoTab(val label: String) {
    DETAILS("Details"),
    CAST_AND_CREW("Cast & Crew"),
    TRAILERS("Trailers"),
}

private data class ShowDetailGridItem(
    val label: String,
    val value: AnnotatedString,
    val isSource: Boolean = false,
)

@Composable
fun DetailInfoTabsSection(
    selectedTab: DetailInfoTab,
    onSelectTab: (DetailInfoTab) -> Unit,
    typeText: String?,
    yearText: String?,
    durationText: String?,
    statusText: String?,
    contentRatingText: String?,
    ratingText: String?,
    apiNameText: String?,
    nextAiringEpisodeText: String?,
    nextAiringDateText: String?,
    vpnText: String?,
    metaText: String?,
    tags: List<String>,
    trailers: List<TrailerMirror>,
    fallbackBackdropUrl: String?,
    onTrailerClick: (TrailerMirror) -> Unit,
    actors: List<ActorData>,
    showCastSetting: Boolean,
    onActorClick: (ActorData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleTabs = remember(showCastSetting, trailers.isNotEmpty()) {
        buildList {
            add(DetailInfoTab.DETAILS)
            if (showCastSetting) {
                add(DetailInfoTab.CAST_AND_CREW)
            }
            if (trailers.isNotEmpty()) {
                add(DetailInfoTab.TRAILERS)
            }
        }
    }
    val effectiveTab = if (selectedTab in visibleTabs) selectedTab else DetailInfoTab.DETAILS

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        // Horizontal 3-tab underline switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            visibleTabs.forEach { tab ->
                val isSelected = tab == effectiveTab
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelectTab(tab) },
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) Color(0xFFF5F5F7) else Color(0xFF8E8E93),
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Crossfade(
            targetState = effectiveTab,
            animationSpec = tween(durationMillis = 220),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            label = "detailInfoTabCrossfade",
        ) { tab ->
            when (tab) {
                DetailInfoTab.DETAILS -> {
                    DetailTabDetailsContent(
                        typeText = typeText,
                        yearText = yearText,
                        durationText = durationText,
                        statusText = statusText,
                        contentRatingText = contentRatingText,
                        ratingText = ratingText,
                        apiNameText = apiNameText,
                        nextAiringEpisodeText = nextAiringEpisodeText,
                        nextAiringDateText = nextAiringDateText,
                        vpnText = vpnText,
                        metaText = metaText,
                        tags = tags,
                    )
                }

                DetailInfoTab.CAST_AND_CREW -> {
                    DetailTabCastContent(
                        actors = actors,
                        onActorClick = onActorClick,
                    )
                }

                DetailInfoTab.TRAILERS -> {
                    DetailTabTrailersContent(
                        trailers = trailers,
                        fallbackBackdropUrl = fallbackBackdropUrl,
                        onTrailerClick = onTrailerClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTabDetailsContent(
    typeText: String?,
    yearText: String?,
    durationText: String?,
    statusText: String?,
    contentRatingText: String?,
    ratingText: String?,
    apiNameText: String?,
    nextAiringEpisodeText: String?,
    nextAiringDateText: String?,
    vpnText: String?,
    metaText: String?,
    tags: List<String>,
) {
    val cleanAiringEp = nextAiringEpisodeText?.trim()?.takeIf { it.isNotEmpty() }
    val cleanAiringDate = nextAiringDateText?.trim()?.takeIf { it.isNotEmpty() }
    val hasNextAiring = cleanAiringEp != null || cleanAiringDate != null

    val items = remember(
        typeText,
        yearText,
        durationText,
        statusText,
        contentRatingText,
        ratingText,
        apiNameText,
        tags,
    ) {
        buildList {
            typeText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ShowDetailGridItem(label = "Format", value = AnnotatedString(it)))
            }
            yearText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ShowDetailGridItem(label = "Release Year", value = AnnotatedString(it)))
            }
            statusText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ShowDetailGridItem(label = "Status", value = AnnotatedString(it)))
            }
            durationText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ShowDetailGridItem(label = "Runtime", value = AnnotatedString(it)))
            }
            contentRatingText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(ShowDetailGridItem(label = "Certification", value = AnnotatedString(it)))
            }
            ratingText?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                val cleanScore = raw.substringBefore('/').trim()
                val scoreValue = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFF5C518))) {
                        append("★")
                    }
                    append(" ")
                    append(cleanScore)
                    append(" / 10")
                }
                add(ShowDetailGridItem(label = "Score", value = scoreValue))
            }
            apiNameText?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(
                    ShowDetailGridItem(
                        label = "Source",
                        value = AnnotatedString(it),
                        isSource = true,
                    ),
                )
            }
            val genres = tags.asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(4)
                .joinToString(", ")
            if (genres.isNotEmpty()) {
                add(ShowDetailGridItem(label = "Genres", value = AnnotatedString(genres)))
            }
        }
    }

    val noticeText = remember(vpnText, metaText) {
        listOfNotNull(
            vpnText?.trim()?.takeIf { it.isNotEmpty() },
            metaText?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ")
    }

    val rows = remember(items) { items.chunked(2) }
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Next Airing first at top of Details tab
        if (hasNextAiring) {
            val nextAiringValue = buildAnnotatedString {
                if (cleanAiringEp != null) {
                    append(cleanAiringEp)
                }
                if (cleanAiringEp != null && cleanAiringDate != null) {
                    append(" — ")
                }
                if (cleanAiringDate != null) {
                    withStyle(SpanStyle(color = accentColor)) {
                        append(cleanAiringDate)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Next Airing",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nextAiringValue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFFF5F5F7),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        rowItems.forEach { item ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (item.isSource) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    ) {
                                        Text(
                                            text = item.value,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = Color(0xFFF5F5F7),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                } else {
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = Color(0xFFF5F5F7),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (noticeText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = noticeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun DetailTabCastContent(
    actors: List<ActorData>,
    onActorClick: (ActorData) -> Unit,
) {
    if (actors.isEmpty()) {
        Text(
            text = "No cast information available",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        return
    }

    val castKey = remember(actors) { actors.firstOrNull()?.actor?.name ?: "" }
    val castListState = rememberSaveable(
        castKey,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }

    LazyRow(
        state = castListState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(
            items = actors,
            key = { index, actorData -> "cast_${index}_${actorData.actor.name}" },
        ) { _, actorData ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onActorClick(actorData) }
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .cardDepth(shape = CircleShape, surface = CardDepthSurface.CARD)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val imageUrl = actorData.actor.image?.takeIf { it.isNotBlank() }
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = actorData.actor.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = actorData.actor.name,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actorData.actor.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFFF5F5F7),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val roleLabel = actorData.roleString?.takeIf { it.isNotBlank() }
                    ?: actorData.role?.name
                if (!roleLabel.isNullOrBlank()) {
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun cleanTrailerLabel(rawTitle: String?, index: Int): String {
    val trimmed = rawTitle?.trim().orEmpty()
    val isRawSourceOrCodec = trimmed.isEmpty() ||
        trimmed.contains("youtube", ignoreCase = true) ||
        trimmed.contains("h264", ignoreCase = true) ||
        trimmed.contains("h.264", ignoreCase = true) ||
        trimmed.contains("avc", ignoreCase = true) ||
        trimmed.contains("vp9", ignoreCase = true) ||
        trimmed.contains("mp4", ignoreCase = true) ||
        trimmed.contains("webm", ignoreCase = true)
    return if (isRawSourceOrCodec) "Trailer ${index + 1}" else trimmed
}

@Composable
private fun DetailTabTrailersContent(
    trailers: List<TrailerMirror>,
    fallbackBackdropUrl: String?,
    onTrailerClick: (TrailerMirror) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = trailers,
            key = { index, trailer -> "trailer_${index}_${trailer.link.url}" },
        ) { index, trailer ->
            val shape = RoundedCornerShape(12.dp)
            val thumbUrl = trailer.thumbnailUrl?.takeIf { it.isNotBlank() } ?: fallbackBackdropUrl
            val label = cleanTrailerLabel(trailer.title, index)

            Box(
                modifier = Modifier
                    .width(220.dp)
                    .aspectRatio(16f / 9f)
                    .cardDepth(shape = shape, surface = CardDepthSurface.CARD)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                    .clickable { onTrailerClick(trailer) },
            ) {
                if (!thumbUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.24f),
                                    Color.Black.copy(alpha = 0.80f),
                                ),
                            ),
                        ),
                )

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.54f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Trailer",
                            tint = Color(0xFFF5F5F7),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFFF5F5F7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
fun DetailPosterRailSection(
    recommendations: List<SearchResponse>,
    onItemClick: (SearchResponse) -> Unit,
    onItemLongClick: ((SearchResponse) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) return

    val apiNames = remember(recommendations) {
        recommendations.map { it.apiName }.distinct()
    }
    var selectedApiName by remember(apiNames) {
        mutableStateOf(apiNames.firstOrNull())
    }
    val filteredItems = remember(recommendations, selectedApiName) {
        if (selectedApiName == null || apiNames.size <= 1) {
            recommendations
        } else {
            recommendations.filter { it.apiName == selectedApiName }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            title = "More Like This",
            actionLabel = null,
        )

        if (apiNames.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(apiNames, key = { it }) { api ->
                    val isSelected = api == selectedApiName
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.06f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { selectedApiName = api },
                    ) {
                        Text(
                            text = api,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        val recKey = remember(filteredItems) { filteredItems.firstOrNull()?.url ?: "" }
        val recListState = rememberSaveable(
            recKey,
            saver = LazyListState.Saver,
        ) {
            LazyListState()
        }

        LazyRow(
            state = recListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = filteredItems,
                key = { item -> "rec_${item.apiName}_${item.url}_${item.name}" },
            ) { item ->
                PosterCardFromSearchResponse(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongClick = onItemLongClick?.let { cb -> { cb(item) } },
                )
            }
        }
    }
}

