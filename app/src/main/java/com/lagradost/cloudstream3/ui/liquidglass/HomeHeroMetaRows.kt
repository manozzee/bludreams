package com.lagradost.cloudstream3.ui.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val ImdbYellow = Color(0xFFF5C518)

internal fun formatReleaseDateForDisplay(releaseInfo: String?): String? {
    if (releaseInfo.isNullOrBlank()) return null
    return releaseInfo.trim()
}

internal fun formatRuntimeForDisplay(runtime: String?): String? {
    if (runtime.isNullOrBlank()) return null
    val minutes = runtime.filter { it.isDigit() }.toIntOrNull() ?: return runtime
    val hours = minutes / 60
    val remMinutes = minutes % 60
    return when {
        hours > 0 && remMinutes > 0 -> "${hours}h ${remMinutes}m"
        hours > 0 -> "${hours}h"
        else -> "${remMinutes}m"
    }
}

internal fun localizedMediaStatusLabel(status: String): String {
    return status.trim()
}

internal fun formatOneDecimal(value: Double): String {
    return ((value * 10).roundToInt() / 10.0).toString()
}

@Composable
internal fun HomeHeroMetaRows(
    item: MetaPreview,
    isTablet: Boolean,
    showImdbRating: Boolean = true,
    showMdbListRatings: Boolean = false,
) {
    val type = item.type.trim().lowercase()
    val isSeries = type == "series" || type == "tv"
    val typeLabel = when (type) {
        "movie" -> "Movie"
        "series" -> "Series"
        else -> item.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    val genre = item.genres.firstOrNull()?.takeIf { it.isNotBlank() }
    val runtime = formatRuntimeForDisplay(item.runtime)?.takeIf { it.isNotBlank() }
    val release = item.releaseInfo?.takeIf { it.isNotBlank() }?.let(::formatReleaseDateForDisplay)
    val status = item.status?.trim()?.takeIf { it.isNotBlank() }
        ?.let { localizedMediaStatusLabel(it).uppercase() }
    val badges = listOfNotNull(item.ageRating?.trim()?.takeIf { it.isNotBlank() }, status)
    val language = item.language?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
    val ratings = if (showMdbListRatings) item.externalRatings else emptyList()
    val imdbRating = item.imdbRating?.trim()
        ?.takeIf { showImdbRating && ratings.isEmpty() }
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
        ?.let(::formatOneDecimal)
    val primaryImdbRating = imdbRating?.takeIf { !isSeries && badges.isEmpty() }
    val secondaryImdbRating = imdbRating?.takeIf { primaryImdbRating == null }
    val horizontalAlignment = if (isTablet) Alignment.Start else Alignment.CenterHorizontally
    val rowArrangement = Arrangement.spacedBy(8.dp, horizontalAlignment)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = rowArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = typeLabel)
            genre?.let {
                HeroMetaDot()
                HeroMetaText(text = it, modifier = Modifier.weight(1f, fill = false))
            }
            runtime?.let {
                HeroMetaDot()
                HeroMetaText(text = it)
            }
            release?.let {
                HeroMetaDot()
                HeroMetaText(text = it)
            }
            primaryImdbRating?.let {
                HeroMetaDot()
                HeroImdbRating(rating = it)
            }
        }

        if (badges.isNotEmpty() || secondaryImdbRating != null || language != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = rowArrangement,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (badges.isNotEmpty()) {
                    DetailHeroMetaBadge(
                        text = badges.first(),
                        secondaryText = badges.getOrNull(1),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    )
                }
                secondaryImdbRating?.let {
                    if (badges.isNotEmpty()) HeroMetaDot()
                    HeroImdbRating(rating = it)
                }
                language?.let {
                    if (badges.isNotEmpty() || secondaryImdbRating != null) HeroMetaDot()
                    HeroMetaText(text = it)
                }
            }
        }
    }
}

@Composable
private fun DetailHeroMetaBadge(
    text: String,
    secondaryText: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        val label = if (secondaryText != null) "$text • $secondaryText" else text
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = contentColor,
        )
    }
}

@Composable
private fun HeroImdbRating(rating: String) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        ImdbRatingSourceLabel(storeTextStyle = textStyle)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rating,
            style = textStyle,
            color = ImdbYellow,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImdbRatingSourceLabel(storeTextStyle: TextStyle) {
    Box(
        modifier = Modifier
            .background(ImdbYellow, shape = RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "IMDb",
            style = storeTextStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
            ),
        )
    }
}

@Composable
private fun HeroMetaText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroMetaDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
    )
}
