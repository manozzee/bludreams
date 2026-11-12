package com.lagradost.cloudstream3.extractors

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.newAudioFile
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType

actual open class YoutubeExtractor actual constructor() : ExtractorApi() {

    actual override val mainUrl = "https://www.youtube.com"
    actual override val name = "YouTube"
    actual override val requiresReferer = false

    actual override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val videoId = extractYouTubeId(url)

        // Try direct InnerTube extraction first (fast, reliable, bypasses NewPipe's bot-detection issues)
        val directSuccess = extractInnerTube(videoId, subtitleCallback, callback)
        if (directSuccess) return

        // Fall back to NewPipeExtractor if direct InnerTube returns no streams
        val watchUrl = "$mainUrl/watch?v=$videoId"
        try {
            val info = StreamInfo.getInfo(watchUrl)
            val isLive = info.streamType == StreamType.LIVE_STREAM
                || info.streamType == StreamType.AUDIO_LIVE_STREAM
                || info.streamType == StreamType.POST_LIVE_STREAM
                || info.streamType == StreamType.POST_LIVE_AUDIO_STREAM

            if (isLive && info.hlsUrl != null) {
                callback(
                    newExtractorLink(
                        source = name,
                        name = "YouTube Live",
                        url = info.hlsUrl
                    ) {
                        type = ExtractorLinkType.M3U8
                    }
                )
            } else {
                processVideo(info, subtitleCallback, callback)
            }
        } catch (e: Throwable) {
            logError(e)
        }
    }

    private suspend fun extractInnerTube(
        videoId: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val clients = listOf(
            mapOf(
                "clientName" to "ANDROID_VR",
                "clientVersion" to "1.61.4",
                "deviceMake" to "Oculus",
                "deviceModel" to "Quest 3",
                "osName" to "Android",
                "osVersion" to "12",
                "hl" to "en",
                "gl" to "US",
            ),
            mapOf(
                "clientName" to "TVHTML5",
                "clientVersion" to "5.20220918",
                "deviceMake" to "Samsung",
                "deviceModel" to "UKS9800",
                "osName" to "Tizen",
                "osVersion" to "2.4",
                "hl" to "en",
                "gl" to "US",
            ),
        )

        for (clientContext in clients) {
            try {
                val payload = mapOf(
                    "context" to mapOf("client" to clientContext),
                    "videoId" to videoId,
                )
                val response = app.post(
                    "https://www.youtube.com/youtubei/v1/player",
                    json = payload,
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                    )
                ).parsedSafe<InnerTubePlayerResponse>() ?: continue

                val playability = response.playabilityStatus?.status
                if (playability != null && !playability.equals("OK", ignoreCase = true)) {
                    continue
                }

                val streamingData = response.streamingData ?: continue
                var linksFound = false

                // HLS Live stream
                val hlsUrl = streamingData.hlsManifestUrl
                if (!hlsUrl.isNullOrBlank()) {
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "YouTube Live",
                            url = hlsUrl
                        ) {
                            type = ExtractorLinkType.M3U8
                        }
                    )
                    linksFound = true
                }

                // Muxed formats (video + audio together, e.g. 360p mp4)
                val muxedFormats = streamingData.formats.orEmpty().filter { !it.url.isNullOrBlank() }
                for (format in muxedFormats) {
                    val streamUrl = format.url ?: continue
                    val height = format.height ?: 360
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "YouTube ${height}p",
                            url = streamUrl
                        ) {
                            quality = height
                        }
                    )
                    linksFound = true
                }

                // Adaptive formats (video only + audio only)
                val adaptiveFormats = streamingData.adaptiveFormats.orEmpty()
                val bestAudio = adaptiveFormats
                    .filter { it.mimeType?.startsWith("audio/") == true && !it.url.isNullOrBlank() }
                    .maxByOrNull { it.bitrate ?: 0L }
                val audioTracks = bestAudio?.url?.let { listOf(newAudioFile(it)) }.orEmpty()

                val videoAdaptive = adaptiveFormats
                    .filter { it.mimeType?.startsWith("video/") == true && !it.url.isNullOrBlank() }
                for (format in videoAdaptive) {
                    val streamUrl = format.url ?: continue
                    val height = format.height ?: (if (format.qualityLabel?.contains("1080") == true) 1080 else 720)
                    callback(
                        newExtractorLink(
                            source = name,
                            name = "YouTube ${height}p",
                            url = streamUrl
                        ) {
                            quality = height
                            this.audioTracks = audioTracks
                        }
                    )
                    linksFound = true
                }

                // Captions
                response.captions?.playerCaptionsTracklistRenderer?.captionTracks?.forEach { track ->
                    val trackUrl = track.baseUrl ?: return@forEach
                    val lang = track.name?.runs?.firstOrNull()?.text
                        ?: track.languageCode
                        ?: "Unknown"
                    subtitleCallback(newSubtitleFile(lang, trackUrl))
                }

                if (linksFound) return true
            } catch (t: Throwable) {
                logError(t)
            }
        }
        return false
    }

    private suspend fun processVideo(
        info: StreamInfo,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val muxedStreams = info.videoStreams.orEmpty()
        val videoOnlyStreams = info.videoOnlyStreams.orEmpty()
        if (muxedStreams.isEmpty() && videoOnlyStreams.isEmpty()) return false

        muxedStreams.forEach { video ->
            callback(
                newExtractorLink(
                    source = name,
                    name = "YouTube ${normalizeCodec(video.codec)}",
                    url = video.content
                ) {
                    quality = video.height
                }
            )
        }

        val audioStreams = info.audioStreams.orEmpty()
        videoOnlyStreams.forEach { video ->
            callback(
                newExtractorLink(
                    source = name,
                    name = "YouTube ${normalizeCodec(video.codec)}",
                    url = video.content
                ) {
                    quality = video.height
                    audioTracks = audioStreams.map { newAudioFile(it.content) }
                }
            )
        }

        info.subtitles.forEach { subtitle ->
            subtitleCallback(
                newSubtitleFile(
                    lang = subtitle.displayLanguageName
                        ?: subtitle.languageTag
                        ?: "Unknown",
                    url = subtitle.content
                )
            )
        }

        return true
    }

    private fun extractYouTubeId(url: String): String {
        val regex = Regex(
            "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:.*v=|v/|u/\\w/|embed/|shorts/|live/))([\\w-]{11})"
        )

        return regex.find(url)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Invalid YouTube URL: $url")
    }

    private fun normalizeCodec(codec: String?): String {
        if (codec.isNullOrBlank()) return ""
        val c = codec.lowercase()
        return when {
            c.startsWith("av01") -> "AV1"
            c.startsWith("vp9") -> "VP9"
            c.startsWith("avc1") || c.startsWith("h264") -> "H264"
            c.startsWith("hev1") || c.startsWith("hvc1") || c.startsWith("hevc") -> "H265"
            else -> codec.substringBefore('.').uppercase()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubePlayerResponse(
    @JsonProperty("playabilityStatus") val playabilityStatus: InnerTubePlayabilityStatus? = null,
    @JsonProperty("streamingData") val streamingData: InnerTubeStreamingData? = null,
    @JsonProperty("videoDetails") val videoDetails: InnerTubeVideoDetails? = null,
    @JsonProperty("captions") val captions: InnerTubeCaptions? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubePlayabilityStatus(
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("reason") val reason: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeStreamingData(
    @JsonProperty("formats") val formats: List<InnerTubeFormat>? = null,
    @JsonProperty("adaptiveFormats") val adaptiveFormats: List<InnerTubeFormat>? = null,
    @JsonProperty("hlsManifestUrl") val hlsManifestUrl: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeFormat(
    @JsonProperty("itag") val itag: Int? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("mimeType") val mimeType: String? = null,
    @JsonProperty("bitrate") val bitrate: Long? = null,
    @JsonProperty("width") val width: Int? = null,
    @JsonProperty("height") val height: Int? = null,
    @JsonProperty("qualityLabel") val qualityLabel: String? = null,
    @JsonProperty("audioQuality") val audioQuality: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeVideoDetails(
    @JsonProperty("videoId") val videoId: String? = null,
    @JsonProperty("title") val title: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeCaptions(
    @JsonProperty("playerCaptionsTracklistRenderer") val playerCaptionsTracklistRenderer: InnerTubePlayerCaptionsTracklistRenderer? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubePlayerCaptionsTracklistRenderer(
    @JsonProperty("captionTracks") val captionTracks: List<InnerTubeCaptionTrack>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeCaptionTrack(
    @JsonProperty("baseUrl") val baseUrl: String? = null,
    @JsonProperty("name") val name: InnerTubeCaptionName? = null,
    @JsonProperty("languageCode") val languageCode: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeCaptionName(
    @JsonProperty("runs") val runs: List<InnerTubeCaptionRun>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnerTubeCaptionRun(
    @JsonProperty("text") val text: String? = null,
)
