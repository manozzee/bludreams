package com.lagradost.cloudstream3.ui.liquidglass

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType

data class MetaExternalRating(
    val source: String,
    val value: String,
    val votes: String? = null,
)

enum class PosterShape {
    Poster,
    Square,
    Landscape,
}

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val banner: String? = null,
    val logo: String? = null,
    val posterShape: PosterShape = PosterShape.Poster,
    val description: String? = null,
    val releaseInfo: String? = null,
    val rawReleaseDate: String? = null,
    val popularity: Double? = null,
    val voteCount: Int? = null,
    val imdbRating: String? = null,
    val genres: List<String> = emptyList(),
    val rawPosterUrl: String? = null,
    val landscapePoster: String? = null,
    val rawLandscapePosterUrl: String? = null,
    val runtime: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val language: String? = null,
    val externalRatings: List<MetaExternalRating> = emptyList(),
    val originalResponse: SearchResponse? = null,
    val originalLoadResponse: LoadResponse? = null,
)

fun MetaPreview.stableKey(): String = "$type:$id"

object MetaPreviewAdapter {
    fun fromSearchResponse(response: SearchResponse): MetaPreview {
        val typeStr = when (response.type) {
            TvType.Movie, TvType.AnimeMovie, TvType.Torrent -> "movie"
            TvType.TvSeries, TvType.Anime, TvType.OVA -> "series"
            TvType.Documentary -> "documentary"
            TvType.Live -> "live"
            else -> "movie"
        }
        val poster = response.posterUrl
        val rating = response.score?.toStringNull(0.1, 10, 1, false)

        return MetaPreview(
            id = response.id?.toString() ?: response.url,
            type = typeStr,
            name = response.name,
            poster = poster,
            banner = poster,
            rawPosterUrl = poster,
            landscapePoster = poster,
            rawLandscapePosterUrl = poster,
            imdbRating = rating,
            posterShape = PosterShape.Poster,
            originalResponse = response,
        )
    }

    fun fromLoadResponse(loadResponse: LoadResponse): MetaPreview {
        val typeStr = when (loadResponse.type) {
            TvType.Movie, TvType.AnimeMovie, TvType.Torrent -> "movie"
            TvType.TvSeries, TvType.Anime, TvType.OVA -> "series"
            TvType.Documentary -> "documentary"
            TvType.Live -> "live"
            else -> "movie"
        }
        val poster = loadResponse.posterUrl
        val banner = loadResponse.backgroundPosterUrl?.takeIf { it.isNotBlank() } ?: poster
        val logo = loadResponse.logoUrl?.takeIf { it.isNotBlank() }
        val rating = loadResponse.score?.toStringNull(0.1, 10, 1, false)
        val runtimeStr = loadResponse.duration?.takeIf { it > 0 }?.let { "${it}m" }
        val showStatus = (loadResponse as? TvSeriesLoadResponse)?.showStatus?.name
            ?: (loadResponse as? AnimeLoadResponse)?.showStatus?.name

        return MetaPreview(
            id = loadResponse.url,
            type = typeStr,
            name = loadResponse.name,
            poster = poster,
            banner = banner,
            logo = logo,
            rawPosterUrl = poster,
            landscapePoster = banner,
            rawLandscapePosterUrl = banner,
            description = loadResponse.plot,
            releaseInfo = loadResponse.year?.toString(),
            imdbRating = rating,
            genres = loadResponse.tags ?: emptyList(),
            runtime = runtimeStr,
            status = showStatus,
            ageRating = loadResponse.contentRating,
            originalLoadResponse = loadResponse,
        )
    }
}
