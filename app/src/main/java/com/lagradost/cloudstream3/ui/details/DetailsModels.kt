package com.lagradost.cloudstream3.ui.details

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.EpisodeSortType
import com.lagradost.cloudstream3.utils.ExtractorLink

enum class EpisodeLayoutStyle {
    HORIZONTAL,
    LIST,
}

data class TrailerMirror(
    val link: ExtractorLink,
    val originalUrl: String,
    val title: String,
    val thumbnailUrl: String?,
)

data class SeasonOption(
    val label: String,
    val seasonNumber: Int,
)

data class DubOption(
    val label: String,
    val status: DubStatus,
)

data class RangeOption(
    val label: String,
    val range: EpisodeRange,
)

data class SortOption(
    val label: String,
    val sortType: EpisodeSortType,
)

