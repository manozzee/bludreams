package com.lagradost.cloudstream3.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ui.home.PosterCardFromSearchResponse
import com.lagradost.cloudstream3.ui.home.SectionHeader
import com.lagradost.cloudstream3.ui.home.ShelfSection
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.cardDepth

@Composable
fun DetailCastSection(
    actors: List<ActorData>,
    onActorClick: (ActorData) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actors.isEmpty()) return

    val castKey = remember(actors) { actors.firstOrNull()?.actor?.name ?: "" }
    val castListState = rememberSaveable(
        castKey,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }

    ShelfSection(
        title = "Cast & Crew",
        actionLabel = null,
        listState = castListState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = actors,
            key = { index, actor -> "cast_${index}_${actor.actor.name}" },
        ) { _, actorData ->
            val cardShape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .clip(cardShape)
                    .clickable { onActorClick(actorData) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
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
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actorData.actor.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val roleLabel = actorData.roleString?.takeIf { it.isNotBlank() }
                    ?: actorData.role?.name
                if (!roleLabel.isNullOrBlank()) {
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun DetailTrailersSection(
    trailers: List<TrailerMirror>,
    fallbackBackdropUrl: String?,
    onTrailerClick: (TrailerMirror) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (trailers.isEmpty()) return

    ShelfSection(
        title = "Trailers & Extras",
        actionLabel = null,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = trailers,
            key = { index, trailer -> "trailer_${index}_${trailer.link.url}" },
        ) { index, trailer ->
            val shape = RoundedCornerShape(14.dp)
            val thumbUrl = trailer.thumbnailUrl?.takeIf { it.isNotBlank() } ?: fallbackBackdropUrl
            val label = trailer.title.ifBlank { "Trailer ${index + 1}" }

            Box(
                modifier = Modifier
                    .width(224.dp)
                    .height(126.dp)
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
                                    Color.Black.copy(alpha = 0.16f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                        ),
                )

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Trailer",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
fun DetailAdditionalInfoSection(
    typeText: String?,
    yearText: String?,
    durationText: String?,
    statusText: String?,
    contentRatingText: String?,
    ratingText: String?,
    apiNameText: String?,
    actorsText: String?,
    modifier: Modifier = Modifier,
) {
    val rows = remember(
        typeText,
        yearText,
        durationText,
        statusText,
        contentRatingText,
        ratingText,
        apiNameText,
        actorsText,
    ) {
        buildList {
            typeText?.takeIf { it.isNotBlank() }?.let { add("Format" to it) }
            yearText?.takeIf { it.isNotBlank() }?.let { add("Release Year" to it) }
            statusText?.takeIf { it.isNotBlank() }?.let { add("Status" to it) }
            durationText?.takeIf { it.isNotBlank() }?.let { add("Runtime" to it) }
            contentRatingText?.takeIf { it.isNotBlank() }?.let { add("Certification" to it) }
            ratingText?.takeIf { it.isNotBlank() }?.let { add("Score" to it) }
            apiNameText?.takeIf { it.isNotBlank() }?.let { add("Source" to it) }
            actorsText?.takeIf { it.isNotBlank() }?.let { add("Starring" to it) }
        }
    }
    if (rows.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Show Details",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(110.dp),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPosterRailSection(
    recommendations: List<SearchResponse>,
    onItemClick: (SearchResponse) -> Unit,
    onItemLongClick: (SearchResponse) -> Unit,
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
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }
}

