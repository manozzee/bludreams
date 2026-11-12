package com.lagradost.cloudstream3.ui.details

import android.graphics.Matrix
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import com.lagradost.cloudstream3.AudioFile

@OptIn(UnstableApi::class)
@Composable
fun HeroTrailerPlayerSurface(
    videoUrl: String?,
    audioTracks: List<AudioFile> = emptyList(),
    headers: Map<String, String> = emptyMap(),
    referer: String? = null,
    isPlaying: Boolean,
    isMuted: Boolean,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    cropToFill: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnError by rememberUpdatedState(onError)

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = if (isMuted) 0f else 1f
            }
    }

    var currentSourceUrl by remember { mutableStateOf<String?>(null) }
    var currentVideoWidth by remember { mutableIntStateOf(0) }
    var currentVideoHeight by remember { mutableIntStateOf(0) }
    var attachedTextureView by remember { mutableStateOf<TextureView?>(null) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                currentOnReady()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                currentVideoWidth = videoSize.width
                currentVideoHeight = videoSize.height
                attachedTextureView?.let { view ->
                    applyTextureScaling(
                        textureView = view,
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height,
                        cropToFill = cropToFill,
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> currentOnReady()
                    Player.STATE_ENDED -> currentOnEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("HeroTrailer", "Hero trailer player error: ${error.errorCodeName}", error)
                currentOnError()
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            attachedTextureView?.let(exoPlayer::clearVideoTextureView)
            attachedTextureView = null
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer, isPlaying) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying && !currentSourceUrl.isNullOrBlank()) {
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(videoUrl, audioTracks, headers, referer, isPlaying) {
        val normalizedUrl = videoUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedUrl == null || !isPlaying) {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            if (normalizedUrl == null) {
                currentSourceUrl = null
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
            return@LaunchedEffect
        }

        if (currentSourceUrl != normalizedUrl) {
            currentSourceUrl = normalizedUrl
            val mergedHeaders = buildMap {
                putAll(headers)
                if (!referer.isNullOrBlank() && !containsKey("Referer")) {
                    put("Referer", referer)
                }
            }
            val httpFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mergedHeaders)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val videoMediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(normalizedUrl))

            val primaryAudio = audioTracks.firstOrNull()
            val finalMediaSource: MediaSource = if (primaryAudio != null) {
                try {
                    val audioHeaders = buildMap {
                        primaryAudio.headers?.let { putAll(it) }
                        if (!referer.isNullOrBlank() && !containsKey("Referer")) {
                            put("Referer", referer)
                        }
                    }
                    val audioHttpFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(audioHeaders)
                    val audioDataFactory = DefaultDataSource.Factory(context, audioHttpFactory)
                    val audioSource = DefaultMediaSourceFactory(audioDataFactory)
                        .createMediaSource(MediaItem.fromUri(primaryAudio.url))
                    MergingMediaSource(videoMediaSource, audioSource)
                } catch (t: Throwable) {
                    videoMediaSource
                }
            } else {
                videoMediaSource
            }

            exoPlayer.setMediaSource(finalMediaSource)
            exoPlayer.prepare()
        }

        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(cropToFill, currentVideoWidth, currentVideoHeight, attachedTextureView) {
        attachedTextureView?.let { view ->
            applyTextureScaling(
                textureView = view,
                videoWidth = currentVideoWidth,
                videoHeight = currentVideoHeight,
                cropToFill = cropToFill,
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextureView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isOpaque = false
                addOnLayoutChangeListener(
                    View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        applyTextureScaling(
                            textureView = this,
                            videoWidth = currentVideoWidth,
                            videoHeight = currentVideoHeight,
                            cropToFill = cropToFill,
                        )
                    },
                )
                exoPlayer.setVideoTextureView(this)
                attachedTextureView = this
            }
        },
        update = { view ->
            if (attachedTextureView !== view) {
                attachedTextureView?.let(exoPlayer::clearVideoTextureView)
                exoPlayer.setVideoTextureView(view)
                attachedTextureView = view
            }
            applyTextureScaling(
                textureView = view,
                videoWidth = currentVideoWidth,
                videoHeight = currentVideoHeight,
                cropToFill = cropToFill,
            )
        },
    )
}

private fun applyTextureScaling(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    cropToFill: Boolean,
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
        textureView.setTransform(null)
        return
    }

    val viewAspect = viewWidth / viewHeight
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val scaleX: Float
    val scaleY: Float

    if (cropToFill) {
        if (videoAspect > viewAspect) {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        }
    } else {
        if (videoAspect > viewAspect) {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        } else {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        }
    }

    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    }
    textureView.setTransform(matrix)
    textureView.invalidate()
}

