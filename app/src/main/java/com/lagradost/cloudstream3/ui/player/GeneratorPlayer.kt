package com.lagradost.cloudstream3.ui.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.animation.addListener
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.toSpanned
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format.NO_VALUE
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerNotificationManager.EXTRA_INSTANCE_ID
import androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.getAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.getMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.getTMDbId
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.databinding.DialogOnlineSubtitlesBinding
import com.lagradost.cloudstream3.databinding.FragmentPlayerBinding
import com.lagradost.cloudstream3.databinding.PlayerSelectSourceAndSubsBinding
import com.lagradost.cloudstream3.databinding.PlayerSelectTracksBinding
import com.lagradost.cloudstream3.isAnimeOp
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.isLiveStream
import com.lagradost.cloudstream3.isMovieType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.observe
import com.lagradost.cloudstream3.mvvm.observeNullable
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import com.lagradost.cloudstream3.ui.download.DownloadButtonSetup
import com.lagradost.cloudstream3.ui.player.CS3IPlayer.Companion.preferredAudioTrackLanguage
import com.lagradost.cloudstream3.ui.player.CustomDecoder.Companion.updateForcedEncoding
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.hud.TorrentProgressState
import com.lagradost.cloudstream3.ui.player.source_priority.LinkSource
import com.lagradost.cloudstream3.ui.player.source_priority.ProfileSettings
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper.getLinkPriority
import com.lagradost.cloudstream3.ui.player.source_priority.QualityProfileDialog
import com.lagradost.cloudstream3.ui.result.ACTION_CLICK_DEFAULT
import com.lagradost.cloudstream3.ui.result.EpisodeAdapter
import com.lagradost.cloudstream3.ui.result.FOCUS_SELF
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.ResultFragment
import com.lagradost.cloudstream3.ui.result.ResultFragment.bindLogo
import com.lagradost.cloudstream3.ui.result.ResultViewModel2
import com.lagradost.cloudstream3.ui.result.SyncViewModel
import com.lagradost.cloudstream3.ui.result.setLinearListLayout
import com.lagradost.cloudstream3.ui.setRecycledViewPool
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.subtitles.SUBTITLE_AUTO_SELECT_KEY
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragmentDialog2
import com.lagradost.cloudstream3.utils.AppContextUtils.getShortSeasonText
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.runOnMainThread
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.getViewPos
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showDialog
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToEnglishLanguageName
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToLanguageName
import com.lagradost.cloudstream3.utils.SubtitleHelper.languages
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute
import com.lagradost.cloudstream3.utils.UIHelper.dismissSafe
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.hideProgress
import com.lagradost.cloudstream3.utils.UIHelper.hideSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.popCurrentPage
import com.lagradost.cloudstream3.utils.UIHelper.showProgress
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.downloader.DownloadUtils.getImageBitmapFromUrl
import com.lagradost.cloudstream3.utils.setText
import com.lagradost.cloudstream3.utils.txt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.EpisodeResponse
import com.lagradost.cloudstream3.ui.details.DubOption
import com.lagradost.cloudstream3.ui.details.RangeOption
import com.lagradost.cloudstream3.ui.details.SeasonOption
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.player.hud.OnlineSubtitleItemUi
import com.lagradost.cloudstream3.ui.player.hud.PlayerEpisodesDrawerState
import com.lagradost.cloudstream3.ui.player.hud.PlayerLoadingOverlay
import com.lagradost.cloudstream3.ui.player.hud.PlayerLoadingOverlayState
import com.lagradost.cloudstream3.ui.player.hud.PlayerOnlineSubtitlesDialogState
import com.lagradost.cloudstream3.ui.player.hud.PlayerQualityProfileDialogState
import com.lagradost.cloudstream3.ui.player.hud.PlayerSourceSubsDialogState
import com.lagradost.cloudstream3.ui.player.hud.PlayerTracksDialogState
import com.lagradost.cloudstream3.ui.player.hud.PriorityEntryUi
import com.lagradost.cloudstream3.ui.player.hud.QualityProfileItemUi
import com.lagradost.cloudstream3.ui.player.hud.SourceItemUi
import com.lagradost.cloudstream3.ui.player.hud.SubtitleGroupUi
import com.lagradost.cloudstream3.ui.player.hud.SubtitleOptionUi
import com.lagradost.cloudstream3.ui.player.hud.TrackItemUi
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.EpisodeSortType
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.ui.result.txt as dubTxt
import com.lagradost.cloudstream3.utils.UiText
import com.lagradost.cloudstream3.utils.videoskip.VideoSkipStamp
import com.lagradost.safefile.SafeFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(UnstableApi::class)
class GeneratorPlayer : FullScreenPlayer() {
    companion object {
        const val NOTIFICATION_ID = 2326
        const val CHANNEL_ID = 7340
        const val STOP_ACTION = "stopcs3"

        private val generators = ConcurrentHashMap<String, VideoGenerator<*>>()
        fun newInstance(
            generator: VideoGenerator<*>,
            index: Int,
            syncData: HashMap<String, String>? = null
        ): Bundle {
            Log.i(TAG, "newInstance = $syncData")
            val uuid = UUID.randomUUID().toString()
            generators[uuid] = generator
            return Bundle().apply {
                putString("uuid", uuid)
                putInt("index", index)
                if (syncData != null) putSerializable("syncData", syncData)
            }
        }

        val subsProviders = subtitleProviders
        val subsProvidersIsActive
            get() = subsProviders.isNotEmpty()
    }


    private var limitTitle = 0
    private var showTitle = false
    private var showName = false
    private var showResolution = false
    private var showMediaInfo = false

    private lateinit var viewModel: PlayerGeneratorViewModel //by activityViewModels()
    private lateinit var sync: SyncViewModel

    private var currentSelectedLink: Pair<ExtractorLink?, ExtractorUri?>? = null
    private var currentSelectedSubtitles: SubtitleData? = null
    private val currentMeta: Any? get() = viewModel.state.generatorState?.meta
    private val nextMeta: Any? get() = viewModel.state.generatorState?.nextMeta

    private var isPlayerActive: AtomicBoolean = AtomicBoolean(false)
    private var isNextEpisode: Boolean = false // this is used to reset the watch time
    private val loadingOverlayStateFlow = MutableStateFlow(PlayerLoadingOverlayState())

    private var preferredAutoSelectSubtitles: String? = null // null means do nothing, "" means none
    private var drawerEpisodesByIndexer: Map<ResultViewModel2.EpisodeIndexer, List<ResultEpisode>> = emptyMap()
    private var drawerFullListByDub: Map<DubStatus, List<ResultEpisode>> = emptyMap()
    private var drawerIndexByEpisodeId: Map<Int, Int> = emptyMap()
    private var drawerActiveDubStatus: DubStatus = DubStatus.None
    private var drawerSelectedDub: DubStatus = DubStatus.None
    private var drawerSelectedSeason: Int = 0
    private var drawerSelectedRange: EpisodeRange? = null
    private var drawerScrollToken: Long = 0L

    private var sourceSubsWasPlaying: Boolean = false
    private var sourceSubsStartSource: Int = 0
    private var sourceSubsSelectedSource: Int = 0
    private var sourceSubsFilteredLinks: List<DisplayLink> = emptyList()
    private var sourceSubsSortedLinks: List<DisplayLink> = emptyList()
    private var sourceSubsSubtitlesGroupedList: List<Map.Entry<String, List<SubtitleData>>> = emptyList()
    private var sourceSubsGroupIndexStart: Int = 0
    private var sourceSubsGroupIndex: Int = 0
    private var sourceSubsOptionIndexStart: Int = 0
    private var sourceSubsOptionIndex: Int = 0
    private var sourceSubsScrollToken: Long = 0L

    private var tracksWasPlaying: Boolean = false
    private var tracksCurrentVideoTracks: List<VideoTrack> = emptyList()
    private var tracksCurrentAudioTracks: List<AudioTrack> = emptyList()
    private var tracksSelectedVideoIndex: Int = -1
    private var tracksSelectedAudioIndex: Int = 0
    private var tracksScrollToken: Long = 0L

    private var onlineSubLoadResponse: LoadResponse? = null
    private var onlineSubDismissCallback: (() -> Unit)? = null
    private var onlineSubCurrentEntities: List<AbstractSubtitleEntities.SubtitleEntity> = emptyList()
    private var onlineSubSelectedEntity: AbstractSubtitleEntities.SubtitleEntity? = null
    private var onlineSubLanguageTagIETF: String = ""
    private var onlineSubSearchJob: Job? = null
    private var onlineSubApplyJob: Job? = null
    private val allMeta: List<ResultEpisode>?
        get() = viewModel.state.generatorState?.allMeta?.filterIsInstance<ResultEpisode>()
            ?.map { episode ->
                // Refresh all the episodes watch duration
                getViewPos(episode.id)?.let { data ->
                    episode.copy(position = data.position, duration = data.duration)
                } ?: episode
            }

    private fun setSubtitles(subtitle: SubtitleData?, userInitiated: Boolean): Boolean {
        // If subtitle is changed and user initiated -> Save the language
        if (subtitle != currentSelectedSubtitles && userInitiated) {
            val subtitleLanguageTagIETF = if (subtitle == null) {
                "" // -> No Subtitles
            } else {
                subtitle.getIETF_tag()
            }

            if (subtitleLanguageTagIETF != null) {
                Log.i(TAG, "Set SUBTITLE_AUTO_SELECT_KEY to '$subtitleLanguageTagIETF'")
                setKey(SUBTITLE_AUTO_SELECT_KEY, subtitleLanguageTagIETF)
                preferredAutoSelectSubtitles = subtitleLanguageTagIETF
            }
        }

        currentSelectedSubtitles = subtitle
        //Log.i(TAG, "setSubtitles = $subtitle")
        return player.setPreferredSubtitles(subtitle)
    }

    override fun embeddedSubtitlesFetched(subtitles: List<SubtitleData>) {
        viewModel.addSubtitles(subtitles.toSet())
    }

    override fun onTracksInfoChanged() {
        val tracks = player.getVideoTracks()
        val hasMultiTracks = tracks.allVideoTracks.size > 1 || tracks.allAudioTracks.size > 1
        if (useComposeHud) {
            updateHudState { copy(hasMultipleTracks = hasMultiTracks) }
        } else {
            playerBinding?.playerTracksBtt?.isVisible = hasMultiTracks
        }
        // Only set the preferred language if it is available.
        // Otherwise, it may give some users audio track init failed!
        if (tracks.allAudioTracks.any { it.language == preferredAudioTrackLanguage }) {
            player.setPreferredAudioTrack(preferredAudioTrackLanguage)
        }
        updatePlayerInfo()
    }

    override fun playerStatusChanged() {
        super.playerStatusChanged()
        if (player.getIsPlaying()) {
            viewModel.forceClearCache = false
        }
        syncSkipOpAndNextEpisodeState()
    }

    private fun noSubtitles(): Boolean {
        return setSubtitles(null, true)
    }

    private fun getPos(): Long {
        val durPos = getViewPos(viewModel.state.generatorState?.id) ?: return 0L
        if (durPos.duration == 0L) return 0L
        if (durPos.position * 100L / durPos.duration > 95L) {
            return 0L
        }
        return durPos.position
    }

    private var currentVerifyLink: Job? = null

    private fun loadExtractorJob(extractorLink: ExtractorLink?) {
        currentVerifyLink?.cancel()

        extractorLink?.let { link ->
            currentVerifyLink = ioSafe {
                if (link.extractorData != null) {
                    getApiFromNameNull(link.source)?.extractorVerifierJob(link.extractorData)
                }
            }
        }
    }

    // https://github.com/androidx/media/blob/main/libraries/ui/src/main/java/androidx/media3/ui/PlayerNotificationManager.java#L1517
    private fun createBroadcastIntent(
        action: String,
        context: Context,
        instanceId: Int
    ): PendingIntent {
        val intent: Intent = Intent(action).setPackage(context.packageName)
        intent.putExtra(EXTRA_INSTANCE_ID, instanceId)
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(context, instanceId, intent, pendingFlags)
    }

    private var cachedPlayerNotificationManager: PlayerNotificationManager? = null

    private fun getMediaNotification(context: Context): PlayerNotificationManager {
        val cache = cachedPlayerNotificationManager
        if (cache != null) return cache
        return PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            CHANNEL_ID.toString()
        )
            .setChannelNameResourceId(R.string.player_notification_channel_name)
            .setChannelDescriptionResourceId(R.string.player_notification_channel_description)
            .setMediaDescriptionAdapter(object : MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return when (val meta = currentMeta) {
                        is ResultEpisode -> {
                            meta.headerName
                        }

                        is ExtractorUri -> {
                            meta.headerName ?: meta.name
                        }

                        else -> null
                    } ?: "Unknown"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    // Open the app without creating a new task to resume playback seamlessly
                    return PendingIntentCompat.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        0,
                        false
                    )
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return when (val meta = currentMeta) {
                        is ResultEpisode -> {
                            meta.name
                        }

                        is ExtractorUri -> {
                            if (meta.headerName == null) {
                                null
                            } else {
                                meta.name
                            }
                        }

                        else -> null
                    }
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    ioSafe {
                        val url = when (val meta = currentMeta) {
                            is ResultEpisode -> {
                                meta.poster
                            }

                            else -> null
                        }
                        // if we have a poster url try with it first
                        if (url != null) {
                            val urlBitmap = context.getImageBitmapFromUrl(url)
                            if (urlBitmap != null) {
                                callback.onBitmap(urlBitmap)
                                return@ioSafe
                            }
                        }

                        // retry several times with a preview in case the preview generator is slow
                        repeat(10) {
                            val preview = this@GeneratorPlayer.player.getPreview(0.5f)
                            if (preview != null) {
                                callback.onBitmap(preview)
                                return@repeat
                            }
                            delay(1000L)
                        }
                    }

                    // return null as we want to use the callback
                    return null
                }
            }).setCustomActionReceiver(object : PlayerNotificationManager.CustomActionReceiver {
                // we have to use a custom action for stop if we want to exit the player instead of just stopping playback
                override fun createCustomActions(
                    context: Context,
                    instanceId: Int
                ): MutableMap<String, NotificationCompat.Action> {
                    return mutableMapOf(
                        STOP_ACTION to NotificationCompat.Action(
                            R.drawable.baseline_stop_24,
                            @SuppressLint("PrivateResource")
                            context.getString(androidx.media3.ui.R.string.exo_controls_stop_description),
                            createBroadcastIntent(STOP_ACTION, context, instanceId)
                        )
                    )
                }

                override fun getCustomActions(player: Player): MutableList<String> {
                    return mutableListOf(STOP_ACTION)
                }

                override fun onCustomAction(player: Player, action: String, intent: Intent) {
                    when (action) {
                        STOP_ACTION -> {
                            exitPlayer()
                        }
                    }
                }
            })
            .setPlayActionIconResourceId(R.drawable.ic_baseline_play_arrow_24)
            .setPauseActionIconResourceId(R.drawable.netflix_pause)
            .setSmallIconResourceId(R.drawable.baseline_headphones_24)
            .setStopActionIconResourceId(R.drawable.baseline_stop_24)
            .setRewindActionIconResourceId(R.drawable.go_back_30)
            .setFastForwardActionIconResourceId(R.drawable.go_forward_30)
            .setNextActionIconResourceId(R.drawable.ic_baseline_skip_next_24)
            .setPreviousActionIconResourceId(R.drawable.baseline_skip_previous_24)
            .build().apply {
                setColorized(true) // Color
                setUseChronometer(true) // Seekbar

                // Don't show the prev episode button
                setUsePreviousAction(false)
                setUsePreviousActionInCompactView(false)

                // Don't show the next episode button
                setUseNextAction(false)
                setUseNextActionInCompactView(false)

                // Show the skip 30s in both modes
                setUseFastForwardAction(true)
                setUseFastForwardActionInCompactView(true)

                // Only show rewind in expanded
                setUseRewindAction(true)
                setUseFastForwardActionInCompactView(false)

                // Use custom stop action
                setUseStopAction(false)
            }
            .also { cachedPlayerNotificationManager = it }
    }

    override fun playerUpdated(player: Any?) {
        super.playerUpdated(player)

        // Cancel the notification when released
        if (player == null) {
            cachedPlayerNotificationManager?.setPlayer(null)
            cachedPlayerNotificationManager = null
            return
        }

        // setup the notification when starting the player
        if (player is ExoPlayer) {
            val ctx = context ?: return
            getMediaNotification(ctx).apply {
                setPlayer(player)
                mMediaSession?.platformToken?.let {
                    setMediaSessionToken(it)
                }
            }
        }
    }

    override fun onDownload(event: DownloadEvent) {
        super.onDownload(event)
        showDownloadProgress(event)
    }

    private fun showDownloadProgress(event: DownloadEvent) {
        activity?.runOnUiThread {
            val ctx = context
            val downloadedFormatted = if (ctx != null) {
                txt(
                    R.string.download_size_format,
                    android.text.format.Formatter.formatShortFileSize(ctx, event.downloadedBytes),
                    android.text.format.Formatter.formatShortFileSize(ctx, event.totalBytes),
                ).asString(ctx)
            } else ""
            val downloadSpeed = if (ctx != null) {
                android.text.format.Formatter.formatShortFileSize(ctx, event.downloadSpeed)
            } else ""
            val speedText = event.connections?.let { connections ->
                "%s/s - %d Connections".format(downloadSpeed, connections)
            } ?: downloadSpeed
            val hideSpeed =
                event.downloadedBytes != 0L && event.downloadedBytes - 1024 >= event.totalBytes

            if (useComposeHud) {
                val fraction = if (event.totalBytes > 0L && event.downloadedBytes > 0L) {
                    (event.downloadedBytes.toFloat() / event.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else null
                updateHudState {
                    copy(
                        torrentProgress = TorrentProgressState(
                            downloadedText = downloadedFormatted,
                            speedText = speedText,
                            showSpeed = !hideSpeed,
                            progressFraction = fraction,
                        )
                    )
                }
                return@runOnUiThread
            }

            playerBinding?.downloadedProgress?.apply {
                val indeterminate = event.totalBytes <= 0 || event.downloadedBytes <= 0
                isIndeterminate = indeterminate
                if (!indeterminate) {
                    max = (event.totalBytes / 1000).toInt()
                    progress = (event.downloadedBytes / 1000).toInt()
                }
            }
            playerBinding?.downloadedProgressText?.text = downloadedFormatted
            playerBinding?.downloadedProgressSpeedText?.text = speedText
            // don't display when done
            playerBinding?.downloadedProgressSpeedText?.isGone = hideSpeed
        }
    }

    private fun loadLink(link: VideoLink?, sameEpisode: Boolean) {
        if (link == null) return
        isPlayerActive.set(true)
        // manage UI
        binding?.playerLoadingOverlay?.isVisible = false
        val isTorrent =
            link.first?.type == ExtractorLinkType.MAGNET || link.first?.type == ExtractorLinkType.TORRENT

        if (useComposeHud) {
            updateHudState { copy(isTorrent = isTorrent) }
        } else {
            playerBinding?.downloadHeader?.isVisible = false
            playerBinding?.downloadHeaderToggle?.isVisible = isTorrent
            if (!isLayout(PHONE)) {
                playerBinding?.downloadBothHeader?.isVisible = isTorrent
            }
        }

        showDownloadProgress(DownloadEvent(0, 0, 0, null))

        // uiReset() // Removed due to UX

        currentSelectedLink = link
        //  setEpisodes(viewModel.getAllMeta() ?: emptyList())
        setPlayerDimen(null)
        setTitle()
        if (!sameEpisode)
            hasRequestedStamps = false

        loadExtractorJob(link.first)
        // load player
        context?.let { ctx ->
            val (url, uri) = link
            val subtitles = viewModel.state.subtitles
            player.loadPlayer(
                ctx,
                sameEpisode,
                url,
                uri,
                startPosition = if (sameEpisode) null else {
                    if (isNextEpisode) 0L else getPos()
                },
                subtitles,
                (if (sameEpisode) currentSelectedSubtitles else null) ?: getAutoSelectSubtitle(
                    subtitles, settings = true, downloads = true
                ),
                preview = true
            )
        }

        if (!sameEpisode) {
            player.addTimeStamps(emptyList()) // clear stamps
            // Resets subtitle delay, as we watch some other content
            player.setSubtitleOffset(0)
        }
    }

    data class TempMetaData(
        var episode: Int? = null,
        var season: Int? = null,
        var name: String? = null,
        var imdbId: String? = null,
    )

    private fun getMetaData(): TempMetaData {
        val meta = TempMetaData()

        when (val newMeta = currentMeta) {
            is ResultEpisode -> {
                if (!newMeta.tvType.isMovieType()) {
                    meta.episode = newMeta.episode
                    meta.season = newMeta.season
                }
                meta.name = newMeta.headerName
            }

            is ExtractorUri -> {
                if (newMeta.tvType?.isMovieType() == false) {
                    meta.episode = newMeta.episode
                    meta.season = newMeta.season
                }
                meta.name = newMeta.headerName
            }
        }
        return meta
    }

    fun getName(entry: AbstractSubtitleEntities.SubtitleEntity, withLanguage: Boolean): String {
        if (entry.lang.isBlank() || !withLanguage) {
            return entry.name
        }
        val language = fromTagToLanguageName(entry.lang.trim()) ?: entry.lang
        return "$language ${entry.name}"
    }

    override fun openOnlineSubPicker(
        context: Context, loadResponse: LoadResponse?, dismissCallback: (() -> Unit)
    ) {
        if (useComposeHud) {
            openComposeOnlineSubPicker(context, loadResponse, dismissCallback)
            return
        }
        val providers = subsProviders.toList()
        val isSingleProvider = subsProviders.size == 1

        val dialog = Dialog(context, R.style.DialogFullscreenPlayer)
        val binding =
            DialogOnlineSubtitlesBinding.inflate(LayoutInflater.from(context), null, false)
        dialog.setContentView(binding.root)
        fixSystemBarsPadding(binding.root)

        var currentSubtitles: List<AbstractSubtitleEntities.SubtitleEntity> = emptyList()
        var currentSubtitle: AbstractSubtitleEntities.SubtitleEntity? = null

        val layout = R.layout.sort_bottom_single_choice_double_text
        val arrayAdapter =
            object : ArrayAdapter<AbstractSubtitleEntities.SubtitleEntity>(dialog.context, layout) {
                fun setHearingImpairedIcon(
                    imageViewEnd: ImageView?, position: Int
                ) {
                    if (imageViewEnd == null) return
                    val isHearingImpaired =
                        currentSubtitles.getOrNull(position)?.isHearingImpaired ?: false

                    val drawableEnd = if (isHearingImpaired) {
                        ContextCompat.getDrawable(
                            context, R.drawable.ic_baseline_hearing_24
                        )?.apply {
                            setTint(
                                ContextCompat.getColor(
                                    context, R.color.textColor
                                )
                            )
                        }
                    } else null

                    imageViewEnd.setImageDrawable(drawableEnd)
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(layout, null)

                    val item = getItem(position)

                    val mainTextView = view.findViewById<TextView>(R.id.main_text)
                    val secondaryTextView = view.findViewById<TextView>(R.id.secondary_text)
                    val drawableEnd = view.findViewById<ImageView>(R.id.drawable_end)

                    mainTextView?.text = item?.let { getName(it, false) }

                    val language =
                        item?.let { fromTagToLanguageName(it.lang) ?: it.lang } ?: ""
                    val providerSuffix =
                        if (isSingleProvider || item == null) "" else " · ${item.source}"
                    @SuppressLint("SetTextI18n")
                    secondaryTextView?.text = language + providerSuffix

                    setHearingImpairedIcon(drawableEnd, position)
                    return view
                }
            }

        dialog.show()
        binding.cancelBtt.setOnClickListener {
            dialog.dismissSafe()
        }

        binding.subtitleAdapter.choiceMode = AbsListView.CHOICE_MODE_SINGLE
        binding.subtitleAdapter.adapter = arrayAdapter

        binding.subtitleAdapter.setOnItemClickListener { _, _, position, _ ->
            currentSubtitle = currentSubtitles.getOrNull(position) ?: return@setOnItemClickListener
        }

        var currentLanguageTagIETF: String = getAutoSelectLanguageTagIETF()


        fun setSubtitlesList(list: List<AbstractSubtitleEntities.SubtitleEntity>) {
            currentSubtitles = list
            arrayAdapter.clear()
            arrayAdapter.addAll(currentSubtitles)
        }

        val currentTempMeta = getMetaData()

        // bruh idk why it is not correct
        val color =
            ColorStateList.valueOf(context.colorFromAttribute(androidx.appcompat.R.attr.colorAccent))
        binding.searchLoadingBar.progressTintList = color
        binding.searchLoadingBar.indeterminateTintList = color

        observeNullable(viewModel.currentSubtitleYear) {
            // When year is changed search again
            binding.subtitlesSearch.setQuery(binding.subtitlesSearch.query, true)
            binding.yearBtt.text = it?.toString() ?: txt(R.string.none).asString(context)
        }

        binding.yearBtt.setOnClickListener {
            val none = txt(R.string.none).asString(context)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val earliestYear = 1900

            val years = (currentYear downTo earliestYear).toList()
            val options = listOf(none) + years.map {
                it.toString()
            }

            val selectedIndex = viewModel.currentSubtitleYear.value
                ?.let {
                    // + 1 since none also takes a space
                    years.indexOf(it) + 1
                }
                ?.takeIf { it >= 0 } ?: 0

            activity?.showDialog(
                options,
                selectedIndex,
                txt(R.string.year).asString(context),
                true, {
                }, { index ->
                    viewModel.setSubtitleYear(years.getOrNull(index - 1))
                }
            )
        }

        binding.subtitlesSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchLoadingBar.show()
                ioSafe {
                    val search =
                        SubtitleSearch(
                            query = query ?: return@ioSafe,
                            imdbId = loadResponse?.getImdbId(),
                            tmdbId = loadResponse?.getTMDbId()?.toInt(),
                            malId = loadResponse?.getMalId()?.toInt(),
                            aniListId = loadResponse?.getAniListId()?.toInt(),
                            epNumber = currentTempMeta.episode,
                            seasonNumber = currentTempMeta.season,
                            lang = currentLanguageTagIETF.ifBlank { null },
                            year = viewModel.currentSubtitleYear.value
                        )

                    // TODO Make ui a lot better, like search with tabs
                    val results = providers.amap {
                        when (val response = Resource.fromResult(it.search(search))) {
                            is Resource.Success -> {
                                response.value
                            }

                            is Resource.Loading -> {
                                emptyList()
                            }

                            is Resource.Failure -> {
                                showToast(response.errorString)
                                emptyList()
                            }
                        }
                    }
                    val max = results.maxOfOrNull { it.size } ?: return@ioSafe

                    // very ugly
                    val items = ArrayList<AbstractSubtitleEntities.SubtitleEntity>()
                    val arrays = results.size
                    for (index in 0 until max) {
                        for (i in 0 until arrays) {
                            items.add(results[i].getOrNull(index) ?: continue)
                        }
                    }

                    // ugly ik
                    activity?.runOnUiThread {
                        setSubtitlesList(items)
                        binding.searchLoadingBar.hide()
                    }
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        binding.searchFilter.setOnClickListener { view ->
            val languagesTagName =
                languages
                    .map { Pair(it.IETF_tag, it.nameNextToFlagEmoji()) }
                    .sortedBy {
                        it.second.substringAfter("\u00a0").lowercase()
                    } // name ignoring flag emoji
            val (langTagsIETF, langNames) = languagesTagName.unzip()

            activity?.showDialog(
                langNames,
                langTagsIETF.indexOf(currentLanguageTagIETF),
                view?.context?.getString(R.string.subs_subtitle_languages)
                    ?: return@setOnClickListener,
                true,
                { }) { index ->
                currentLanguageTagIETF = langTagsIETF[index]
                binding.subtitlesSearch.setQuery(binding.subtitlesSearch.query, true)
            }
        }

        binding.applyBtt.setOnClickListener {
            val currentSubtitle = currentSubtitle
            if (currentSubtitle == null) {
                dialog.dismissSafe()
                return@setOnClickListener
            }

            val api = providers.firstOrNull { it.idPrefix == currentSubtitle.idPrefix }
            if (api == null) {
                dialog.dismissSafe()
                return@setOnClickListener
            }

            binding.applyBtt.showProgress()
            ioSafe {
                val apiResource =
                    Resource.fromResult(api.resource(currentSubtitle))
                binding.applyBtt.hideProgress()
                when (apiResource) {
                    is Resource.Success -> {
                        val subtitles = apiResource.value.getSubtitles().map { resource ->
                            SubtitleData(
                                originalName = resource.name ?: getName(
                                    currentSubtitle,
                                    true
                                ),
                                nameSuffix = "",
                                url = resource.url,
                                origin = resource.origin,
                                mimeType = resource.url.toSubtitleMimeType(),
                                headers = currentSubtitle.headers,
                                languageCode = currentSubtitle.lang
                            )
                        }
                        if (subtitles.isEmpty()) {
                            showToast(R.string.no_subtitles)
                            return@ioSafe
                        }
                        dialog.dismissSafe()
                        runOnMainThread {
                            addAndSelectSubtitles(*subtitles.toTypedArray())
                        }
                    }

                    is Resource.Failure -> {
                        showToast(apiResource.errorString)
                    }

                    is Resource.Loading -> {
                        // not possible
                    }
                }
            }
        }

        dialog.setOnDismissListener {
            dismissCallback.invoke()
        }

        dialog.show()
        binding.subtitlesSearch.setQuery(currentTempMeta.name, true)
        //TODO: Set year text from currently loaded movie on Player
        //dialog.subtitles_search_year?.setText(currentTempMeta.year)
    }

    private fun openSubPicker() {
        try {
            subsPathPicker.launch(
                arrayOf(
                    "text/plain",
                    "text/str",
                    "application/octet-stream",
                    MimeTypes.TEXT_UNKNOWN,
                    MimeTypes.TEXT_VTT,
                    MimeTypes.TEXT_SSA,
                    MimeTypes.APPLICATION_TTML,
                    MimeTypes.APPLICATION_MP4VTT,
                    MimeTypes.APPLICATION_SUBRIP,
                )
            )
        } catch (e: Exception) {
            logError(e)
        }
    }

    @MainThread
    private fun addAndSelectSubtitles(
        vararg subtitleData: SubtitleData
    ) {
        if (subtitleData.isEmpty()) return
        val ctx = context ?: return
        val selectedSubtitle = subtitleData.first()
        viewModel.addSubtitles(subtitleData.toSet())

        // this is used instead of observe(viewModel._currentSubs), because observe is too slow
        player.setActiveSubtitles(viewModel.state.subtitles)

        // Save current time as to not reset player to 00:00
        player.saveData()
        player.reloadPlayer(ctx)

        setSubtitles(selectedSubtitle, false)

        selectSourceDialog?.dismissSafe()
        selectSourceDialog = null
        if (useComposeHud) {
            dismissComposeSourceSubsDialog(resumePlayback = true)
        }

        showToast(
            String.format(ctx.getString(R.string.player_loaded_subtitles), selectedSubtitle.name),
            Toast.LENGTH_LONG
        )
    }

    // Open file picker
    private val subsPathPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            safe {
                // It lies, it can be null if file manager quits.
                if (uri == null) return@safe
                val ctx = context ?: CloudStreamApp.context ?: return@safe
                // RW perms for the path
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val file = SafeFile.fromUri(ctx, uri)
                val fileName = file?.name()
                println("Loaded subtitle file. Selected URI path: $uri - Name: $fileName")
                // DO NOT REMOVE THE FILE EXTENSION FROM NAME, IT'S NEEDED FOR MIME TYPES
                val name = fileName ?: uri.toString()

                val subtitleData = SubtitleData(
                    name,
                    "",
                    uri.toString(),
                    SubtitleOrigin.DOWNLOADED_FILE,
                    name.toSubtitleMimeType(),
                    emptyMap(),
                    null
                )

                addAndSelectSubtitles(subtitleData)
            }
        }

    /** Will toast both when an error is found and when a subtitle is selected,
     * so only use from a user click and not a background process */
    private fun addFirstSub(query: SubtitleSearch) =
        viewModel.viewModelScope.launch {
            // async should not have a race condition if they are on the same group
            var hasSelectASubtitle = false

            // first come first served with these subtitles
            // we might want to change it to prefer different sources when used multiple times,
            // however caching might make this random after the first click too
            subsProviders.toList().amap { provider ->
                val success = when (val result = Resource.fromResult(
                    provider.search(
                        query = query
                    )
                )) {
                    is Resource.Failure -> {
                        // scope might cancel, so we do an extra check
                        if (this.isActive) {
                            showToast("${provider.idPrefix}${result.errorString}")
                        }
                        return@amap
                    }

                    is Resource.Loading -> {
                        // unreachable
                        return@amap
                    }

                    is Resource.Success -> {
                        result.value
                    }
                }

                // try to add every subtitle until we have added a new subtitle file
                for (subtitleEntry in success) {
                    if (hasSelectASubtitle || !this.isActive) {
                        break
                    }

                    val subtitleResources = provider.resource(subtitleEntry).getOrNull() ?: continue

                    val subtitles = subtitleResources.getSubtitles().map { resource ->
                        SubtitleData(
                            originalName = resource.name ?: getName(subtitleEntry, true),
                            nameSuffix = "",
                            url = resource.url,
                            origin = resource.origin,
                            mimeType = resource.url.toSubtitleMimeType(),
                            headers = subtitleEntry.headers,
                            languageCode = subtitleEntry.lang,
                        )
                    }

                    // checks for both a race condition and if any of the subs generated is new
                    if (this.isActive && !viewModel.state.subtitles.containsAll(subtitles) && !hasSelectASubtitle) {
                        hasSelectASubtitle = true
                        runOnMainThread {
                            addAndSelectSubtitles(*subtitles.toTypedArray())
                        }
                        break
                    }
                }
            }
            // maybe better error here?
            if (!hasSelectASubtitle && this.isActive) {
                showToast(R.string.no_subtitles)
            }
        }


    override fun showMirrorsDialogue() {
        if (useComposeHud) {
            showComposeSourceSubsDialog()
            return
        }
        try {
            currentSelectedSubtitles = player.getCurrentPreferredSubtitle()
            //println("CURRENT SELECTED :$currentSelectedSubtitles of $currentSubs")
            context?.let { ctx ->
                val isPlaying = player.getIsPlaying()
                player.handleEvent(CSPlayerEvent.Pause, PlayerEventSource.UI)
                val currentSubtitles = sortSubs(viewModel.state.subtitles)

                val sourceDialog = Dialog(ctx, R.style.DialogFullscreenPlayer)
                val binding =
                    PlayerSelectSourceAndSubsBinding.inflate(LayoutInflater.from(ctx), null, false)
                sourceDialog.setContentView(binding.root)

                fixSystemBarsPadding(binding.root)
                selectSourceDialog = sourceDialog

                sourceDialog.show()
                val providerList = binding.sortProviders
                val subtitleList = binding.sortSubtitles
                val subtitleOptionList = binding.sortSubtitlesOptions

                val loadFromFileFooter: TextView =
                    layoutInflater.inflate(R.layout.sort_bottom_footer_add_choice, null) as TextView

                loadFromFileFooter.text = ctx.getString(R.string.player_load_subtitles)
                loadFromFileFooter.setOnClickListener {
                    openSubPicker()
                }
                subtitleList.addFooterView(loadFromFileFooter)

                var shouldDismiss = true

                binding.subtitleSettingsBtt.setOnClickListener {
                    safe {
                        val subtitlesFragment = SubtitlesFragmentDialog2()
                        subtitlesFragment.systemBarsAddPadding = true
                        subtitlesFragment.show(this.parentFragmentManager, "SubtitleSettings")
                    }
                }

                fun dismiss() {
                    if (isPlaying) {
                        player.handleEvent(CSPlayerEvent.Play)
                    }
                    activity?.hideSystemUI()
                }

                if (subsProvidersIsActive) {
                    val currentLoadResponse = viewModel.state.generatorState?.response

                    val loadFromOpenSubsFooter: TextView = layoutInflater.inflate(
                        R.layout.sort_bottom_footer_add_choice, null
                    ) as TextView

                    loadFromOpenSubsFooter.text =
                        ctx.getString(R.string.player_load_subtitles_online)

                    loadFromOpenSubsFooter.setOnClickListener {
                        shouldDismiss = false
                        sourceDialog.dismissSafe(activity)
                        selectSourceDialog = null
                        openOnlineSubPicker(it.context, currentLoadResponse) {
                            dismiss()
                        }
                    }
                    subtitleList.addFooterView(loadFromOpenSubsFooter)

                    // subs from 1 button here
                    val metadata = getMetaData()
                    val queryName = metadata.name ?: currentLoadResponse?.name
                    if (queryName != null) {
                        val currentLanguageTagIETF: String = getAutoSelectLanguageTagIETF()
                        val loadFromFirstSubsFooter: TextView = layoutInflater.inflate(
                            R.layout.sort_bottom_footer_add_choice, null
                        ) as TextView

                        loadFromFirstSubsFooter.text =
                            ctx.getString(R.string.player_load_one_subtitle_online)

                        loadFromFirstSubsFooter.setOnClickListener {
                            sourceDialog.dismissSafe(activity)
                            selectSourceDialog = null
                            showToast(R.string.loading)
                            addFirstSub(
                                SubtitleSearch(
                                    query = queryName,
                                    imdbId = currentLoadResponse?.getImdbId(),
                                    tmdbId = currentLoadResponse?.getTMDbId()?.toInt(),
                                    malId = currentLoadResponse?.getMalId()?.toInt(),
                                    aniListId = currentLoadResponse?.getAniListId()?.toInt(),
                                    epNumber = metadata.episode,
                                    seasonNumber = metadata.season,
                                    lang = currentLanguageTagIETF.ifBlank { null },
                                    year = viewModel.currentSubtitleYear.value
                                )
                            )
                        }
                        subtitleList.addFooterView(loadFromFirstSubsFooter)
                    }
                }

                var sourceIndex = 0
                var startSource = 0
                // Filtered and sorted links
                var currentHiddenFooter: View? = null
                var filteredLinks: List<DisplayLink> = emptyList()

                fun refreshLinks(qualityProfile: Int) {
                    val currentLinkUsed = currentSelectedLink
                    // Always display current linkFooter
                    val sortedLinks = viewModel.state.sortLinks(qualityProfile)

                    filteredLinks = sortedLinks.filter { it.shouldUseLink || it.link == currentLinkUsed }

                    if (sortedLinks.isEmpty()) {
                        sourceDialog.findViewById<LinearLayout>(R.id.sort_sources_holder)?.isGone =
                            true
                    } else {
                        startSource = filteredLinks.indexOfFirst { it.link == currentLinkUsed }
                        sourceIndex = startSource

                        val sourcesArrayAdapter =
                            ArrayAdapter<String>(ctx, R.layout.sort_bottom_single_choice)

                        sourcesArrayAdapter.addAll(filteredLinks.map { displayLink ->
                            val (link, uri) = displayLink.link
                            val name = link?.name ?: uri?.name ?: "NULL"
                            "$name ${Qualities.getStringByInt(link?.quality)}"
                        })

                        providerList.choiceMode = AbsListView.CHOICE_MODE_SINGLE
                        providerList.adapter = sourcesArrayAdapter
                        providerList.setSelection(sourceIndex)
                        providerList.setItemChecked(sourceIndex, true)

                        providerList.setOnItemClickListener { _, _, which, _ ->
                            sourceIndex = which
                            providerList.setItemChecked(which, true)
                        }

                        providerList.setOnItemLongClickListener { _, _, position, _ ->
                            sortedLinks.getOrNull(position)?.link?.first?.url?.let {
                                clipboardHelper(
                                    txt(R.string.video_source),
                                    it
                                )
                            }
                            true
                        }

                        val hiddenLinks = sortedLinks.size - filteredLinks.size
                        providerList.removeFooterView(currentHiddenFooter)

                        if (hiddenLinks > 0) {
                            val hiddenLinksFooter: TextView = layoutInflater.inflate(
                                R.layout.sort_bottom_footer_add_choice, null
                            ) as TextView

                            providerList.addFooterView(hiddenLinksFooter, null, false)
                            currentHiddenFooter = hiddenLinksFooter

                            val hiddenLinksText =
                                ctx.resources.getQuantityString(R.plurals.links_hidden, hiddenLinks)
                                    .format(hiddenLinks)
                            hiddenLinksFooter.text = hiddenLinksText
                            hiddenLinksFooter.setCompoundDrawables(null, null, null, null)
                            hiddenLinksFooter.setTypeface(null, Typeface.ITALIC)
                        }
                    }
                }

                refreshLinks(currentQualityProfile)

                sourceDialog.setOnDismissListener {
                    if (shouldDismiss) dismiss()
                    selectSourceDialog = null
                }


                val subsArrayAdapter =
                    ArrayAdapter<Spanned>(ctx, R.layout.sort_bottom_single_choice)
                subsArrayAdapter.add(ctx.getString(R.string.no_subtitles).html())

                val subtitlesGrouped =
                    currentSubtitles.groupBy { it.originalName }.map { (key, value) ->
                        key to value.sortedBy { it.nameSuffix.toIntOrNull() ?: 0 }
                    }.toMap()
                val subtitlesGroupedList = subtitlesGrouped.entries.toList()

                val subtitles = subtitlesGrouped.map { it.key.html() }

                val subtitleGroupIndexStart =
                    subtitlesGrouped.keys.indexOf(currentSelectedSubtitles?.originalName) + 1
                var subtitleGroupIndex = subtitleGroupIndexStart

                val subtitleOptionIndexStart =
                    subtitlesGrouped[currentSelectedSubtitles?.originalName]?.indexOfFirst { it.nameSuffix == currentSelectedSubtitles?.nameSuffix }
                        ?: 0
                var subtitleOptionIndex = subtitleOptionIndexStart

                subsArrayAdapter.addAll(subtitles)

                subtitleList.adapter = subsArrayAdapter
                subtitleList.choiceMode = AbsListView.CHOICE_MODE_SINGLE

                subtitleList.setSelection(subtitleGroupIndex)
                subtitleList.setItemChecked(subtitleGroupIndex, true)

                val subsOptionsArrayAdapter =
                    ArrayAdapter<Spanned>(ctx, R.layout.sort_bottom_single_choice)

                subtitleOptionList.adapter = subsOptionsArrayAdapter
                subtitleOptionList.choiceMode = AbsListView.CHOICE_MODE_SINGLE

                fun updateSubtitleOptionList() {
                    subsOptionsArrayAdapter.clear()

                    val subtitleOptions =
                        subtitlesGroupedList
                            .getOrNull(subtitleGroupIndex - 1)?.value?.map { subtitle ->
                                val nameSuffix = subtitle.nameSuffix.html()
                                nameSuffix.ifBlank {
                                    when (subtitle.origin) {
                                        SubtitleOrigin.URL -> txt(R.string.subtitles_from_online)
                                        SubtitleOrigin.DOWNLOADED_FILE -> txt(R.string.downloaded)
                                        SubtitleOrigin.EMBEDDED_IN_VIDEO -> txt(R.string.subtitles_from_embedded)
                                    }.asString(ctx).toSpanned()
                                }
                            }
                            ?: emptyList()

                    // Show nothing if there is nothing to select
                    val shouldHide = subtitleOptions.size < 2
                    subtitleOptionList.isGone = shouldHide // Make it easier to click
                    if (shouldHide) return

                    subsOptionsArrayAdapter.addAll(subtitleOptions)

                    subtitleOptionList.setSelection(subtitleOptionIndex)
                    subtitleOptionList.setItemChecked(subtitleOptionIndex, true)
                }

                updateSubtitleOptionList()

                subtitleList.setOnItemClickListener { _, _, which, _ ->
                    if (which > subtitlesGrouped.size) {
                        // Since android TV is funky the setOnItemClickListener will be triggered
                        // instead of setOnClickListener when selecting. To override this we programmatically
                        // click the view when selecting an item outside the list.

                        // Cheeky way of getting the view at that position to click it
                        // to avoid keeping track of the various footers.
                        // getChildAt() gives null :(
                        val child = subtitleList.adapter.getView(which, null, subtitleList)
                        child?.performClick()
                    } else {
                        if (subtitleGroupIndex != which) {
                            subtitleGroupIndex = which
                            subtitleOptionIndex =
                                if (subtitleGroupIndex == subtitleGroupIndexStart) {
                                    subtitleOptionIndexStart
                                } else {
                                    0
                                }
                        }
                        subtitleList.setItemChecked(which, true)
                        updateSubtitleOptionList()
                    }
                }

                subtitleOptionList.setOnItemClickListener { _, _, which, _ ->
                    if (which >= (subtitlesGroupedList.getOrNull(subtitleGroupIndex - 1)?.value?.size
                            ?: -1)
                    ) {
                        val child = subtitleOptionList.adapter.getView(which, null, subtitleList)
                        child?.performClick()
                    } else {
                        subtitleOptionIndex = which
                        subtitleOptionList.setItemChecked(which, true)
                    }
                }

                binding.cancelBtt.setOnClickListener {
                    sourceDialog.dismissSafe(activity)
                    this.selectSourceDialog = null
                }

                fun setProfileName(profile: Int) {
                    binding.sourceSettingsBtt.setText(
                        QualityDataHelper.getProfileName(
                            profile
                        )
                    )
                }
                setProfileName(currentQualityProfile)

                binding.profilesClickSettings.setOnClickListener {
                    val activity = activity ?: return@setOnClickListener
                    val dialog = QualityProfileDialog(
                        activity,
                        R.style.DialogFullscreenPlayer,
                        viewModel.state.links.mapNotNull {
                            it.first?.let { extractorLink ->
                                LinkSource(
                                    extractorLink
                                )
                            }
                        },
                        currentQualityProfile
                    ) { profile ->
                        currentQualityProfile = profile.id
                        setProfileName(profile.id)
                    }

                    dialog.setOnDismissListener {
                        viewModel.state.clearSortedLinksCache()
                        refreshLinks(currentQualityProfile)
                    }

                    dialog.show()
                }

                binding.subtitlesEncodingFormat.apply {
                    val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)

                    val prefNames = ctx.resources.getStringArray(R.array.subtitles_encoding_list)
                    val prefValues = ctx.resources.getStringArray(R.array.subtitles_encoding_values)

                    val value = settingsManager.getString(
                        ctx.getString(R.string.subtitles_encoding_key), null
                    )
                    val index = prefValues.indexOf(value)
                    text = prefNames[if (index == -1) 0 else index]
                }

                binding.subtitlesEncodingFormat.setOnClickListener {
                    val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)

                    val prefNames = ctx.resources.getStringArray(R.array.subtitles_encoding_list)
                    val prefValues = ctx.resources.getStringArray(R.array.subtitles_encoding_values)

                    val currentPrefMedia = settingsManager.getString(
                        ctx.getString(R.string.subtitles_encoding_key), null
                    )

                    shouldDismiss = false
                    sourceDialog.dismissSafe(activity)
                    selectSourceDialog = null

                    val index = prefValues.indexOf(currentPrefMedia)
                    activity?.showDialog(
                        prefNames.toList(),
                        if (index == -1) 0 else index,
                        ctx.getString(R.string.subtitles_encoding),
                        true,
                        {}) {
                        settingsManager.edit {
                            putString(
                                ctx.getString(R.string.subtitles_encoding_key), prefValues[it]
                            )
                        }
                        updateForcedEncoding(ctx)
                        dismiss()
                        player.seekTime(-1) // to update subtitles, a dirty trick
                    }
                }

                binding.applyBtt.setOnClickListener {
                    var init = sourceIndex != startSource
                    if (subtitleGroupIndex != subtitleGroupIndexStart || subtitleOptionIndex != subtitleOptionIndexStart) {
                        init = init or if (subtitleGroupIndex <= 0) {
                            noSubtitles()
                        } else {
                            subtitlesGroupedList.getOrNull(subtitleGroupIndex - 1)?.value?.getOrNull(
                                subtitleOptionIndex
                            )?.let {
                                setSubtitles(it, true)
                            } ?: false
                        }
                    }
                    if (init) {
                        filteredLinks.getOrNull(sourceIndex)?.let {
                            loadLink(it.link, true)
                        }
                    }
                    sourceDialog.dismissSafe(activity)
                    selectSourceDialog = null
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun showComposeSourceSubsDialog() {
        try {
            val ctx = context ?: return
            currentSelectedSubtitles = player.getCurrentPreferredSubtitle()
            sourceSubsWasPlaying = player.getIsPlaying()
            player.handleEvent(CSPlayerEvent.Pause, PlayerEventSource.UI)

            val currentSubtitles = sortSubs(viewModel.state.subtitles)
            val subtitlesGrouped =
                currentSubtitles.groupBy { it.originalName }.map { (key, value) ->
                    key to value.sortedBy { it.nameSuffix.toIntOrNull() ?: 0 }
                }.toMap()

            sourceSubsSubtitlesGroupedList = subtitlesGrouped.entries.toList()
            sourceSubsGroupIndexStart =
                subtitlesGrouped.keys.indexOf(currentSelectedSubtitles?.originalName) + 1
            sourceSubsGroupIndex = sourceSubsGroupIndexStart

            sourceSubsOptionIndexStart =
                subtitlesGrouped[currentSelectedSubtitles?.originalName]?.indexOfFirst {
                    it.nameSuffix == currentSelectedSubtitles?.nameSuffix
                } ?: 0
            sourceSubsOptionIndex = sourceSubsOptionIndexStart

            refreshComposeSourceLinks(currentQualityProfile)
            sourceSubsScrollToken++
            pushComposeSourceSubsState(ctx)
            setSourceSubsOverlayVisible(true)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun refreshComposeSourceLinks(qualityProfile: Int) {
        val currentLinkUsed = currentSelectedLink
        val sortedLinks = viewModel.state.sortLinks(qualityProfile)
        sourceSubsSortedLinks = sortedLinks
        sourceSubsFilteredLinks = sortedLinks.filter { it.shouldUseLink || it.link == currentLinkUsed }

        if (sortedLinks.isEmpty()) {
            sourceSubsStartSource = 0
            sourceSubsSelectedSource = 0
        } else {
            sourceSubsStartSource = sourceSubsFilteredLinks.indexOfFirst { it.link == currentLinkUsed }
            sourceSubsSelectedSource = sourceSubsStartSource
        }
    }

    private fun pushComposeSourceSubsState(ctx: Context? = context) {
        val safeCtx = ctx ?: return
        val hasSources = sourceSubsSortedLinks.isNotEmpty()
        val sourceItems = if (hasSources) {
            sourceSubsFilteredLinks.mapIndexed { index, displayLink ->
                val (link, uri) = displayLink.link
                val name = link?.name ?: uri?.name ?: "NULL"
                val quality = Qualities.getStringByInt(link?.quality)
                val label = if (quality.isBlank()) name else "$name $quality"
                SourceItemUi(index = index, label = label)
            }
        } else {
            emptyList()
        }

        val hiddenLinks = (sourceSubsSortedLinks.size - sourceSubsFilteredLinks.size).coerceAtLeast(0)
        val hiddenText = if (hasSources && hiddenLinks > 0) {
            safeCtx.resources.getQuantityString(R.plurals.links_hidden, hiddenLinks)
                .format(hiddenLinks)
        } else {
            null
        }

        val subtitleGroups = buildList {
            add(SubtitleGroupUi(index = 0, label = safeCtx.getString(R.string.no_subtitles)))
            sourceSubsSubtitlesGroupedList.forEachIndexed { idx, entry ->
                val cleanName = entry.key.html().toString().trim().ifEmpty { entry.key }
                add(SubtitleGroupUi(index = idx + 1, label = cleanName))
            }
        }

        val subtitleVariants = sourceSubsSubtitlesGroupedList
            .getOrNull(sourceSubsGroupIndex - 1)
            ?.value
            ?.mapIndexed { idx, subtitle ->
                val nameSuffix = subtitle.nameSuffix.html().toString().trim()
                val label = nameSuffix.ifBlank {
                    when (subtitle.origin) {
                        SubtitleOrigin.URL -> txt(R.string.subtitles_from_online)
                        SubtitleOrigin.DOWNLOADED_FILE -> txt(R.string.downloaded)
                        SubtitleOrigin.EMBEDDED_IN_VIDEO -> txt(R.string.subtitles_from_embedded)
                    }.asString(safeCtx)
                }
                SubtitleOptionUi(index = idx, label = label)
            }
            .orEmpty()

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(safeCtx)
        val prefNames = safeCtx.resources.getStringArray(R.array.subtitles_encoding_list)
        val prefValues = safeCtx.resources.getStringArray(R.array.subtitles_encoding_values)
        val encodingValue = settingsManager.getString(
            safeCtx.getString(R.string.subtitles_encoding_key),
            null
        )
        val encodingIdx = prefValues.indexOf(encodingValue)
        val encodingLabel = prefNames.getOrNull(if (encodingIdx == -1) 0 else encodingIdx).orEmpty()

        val profileLabel = QualityDataHelper.getProfileName(currentQualityProfile).asString(safeCtx)

        val currentLoadResponse = viewModel.state.generatorState?.response
        val metadata = getMetaData()
        val queryName = metadata.name ?: currentLoadResponse?.name

        val loadOnlineLabel = if (subsProvidersIsActive) {
            safeCtx.getString(R.string.player_load_subtitles_online)
        } else {
            null
        }
        val loadFirstOnlineLabel = if (subsProvidersIsActive && queryName != null) {
            safeCtx.getString(R.string.player_load_one_subtitle_online)
        } else {
            null
        }

        updateHudState {
            copy(
                sourceSubsDialog = PlayerSourceSubsDialogState(
                    hasSources = hasSources,
                    profileLabel = profileLabel,
                    sources = sourceItems,
                    selectedSourceIndex = sourceSubsSelectedSource,
                    hiddenSourcesText = hiddenText,
                    subtitleVariants = subtitleVariants,
                    selectedSubtitleVariantIndex = sourceSubsOptionIndex.coerceAtLeast(0),
                    encodingLabel = encodingLabel,
                    subtitleGroups = subtitleGroups,
                    selectedSubtitleGroupIndex = sourceSubsGroupIndex,
                    loadFromFileLabel = safeCtx.getString(R.string.player_load_subtitles),
                    loadOnlineLabel = loadOnlineLabel,
                    loadFirstOnlineLabel = loadFirstOnlineLabel,
                    scrollTriggerToken = sourceSubsScrollToken,
                )
            )
        }
    }

    private fun dismissComposeSourceSubsDialog(resumePlayback: Boolean) {
        val wasPlaying = sourceSubsWasPlaying
        sourceSubsWasPlaying = false
        setSourceSubsOverlayVisible(false)
        if (resumePlayback && wasPlaying) {
            player.handleEvent(CSPlayerEvent.Play)
        }
        activity?.hideSystemUI()
    }

    override fun onSourceSubsDismiss() {
        dismissComposeSourceSubsDialog(resumePlayback = true)
    }

    override fun onSourceSubsApply() {
        var init = sourceSubsSelectedSource != sourceSubsStartSource
        if (sourceSubsGroupIndex != sourceSubsGroupIndexStart || sourceSubsOptionIndex != sourceSubsOptionIndexStart) {
            init = init or if (sourceSubsGroupIndex <= 0) {
                noSubtitles()
            } else {
                sourceSubsSubtitlesGroupedList
                    .getOrNull(sourceSubsGroupIndex - 1)
                    ?.value
                    ?.getOrNull(sourceSubsOptionIndex)
                    ?.let { setSubtitles(it, true) }
                    ?: false
            }
        }
        if (init) {
            sourceSubsFilteredLinks.getOrNull(sourceSubsSelectedSource)?.let {
                loadLink(it.link, true)
            }
        }
        dismissComposeSourceSubsDialog(resumePlayback = !init)
    }

    override fun onSourceSelected(index: Int) {
        sourceSubsSelectedSource = index
        pushComposeSourceSubsState()
    }

    override fun onSourceLongClick(index: Int) {
        val url = sourceSubsFilteredLinks.getOrNull(index)?.link?.first?.url
            ?: sourceSubsSortedLinks.getOrNull(index)?.link?.first?.url
        url?.let {
            clipboardHelper(txt(R.string.video_source), it)
        }
    }

    override fun onSourceProfileSettingsClick() {
        if (useComposeHud) {
            openComposeQualityProfileDialog()
            return
        }
        val act = activity ?: return
        val dialog = QualityProfileDialog(
            act,
            R.style.DialogFullscreenPlayer,
            viewModel.state.links.mapNotNull {
                it.first?.let { extractorLink ->
                    LinkSource(extractorLink)
                }
            },
            currentQualityProfile
        ) { profile ->
            currentQualityProfile = profile.id
            pushComposeSourceSubsState(act)
        }
        dialog.setOnDismissListener {
            viewModel.state.clearSortedLinksCache()
            refreshComposeSourceLinks(currentQualityProfile)
            pushComposeSourceSubsState(act)
            activity?.hideSystemUI()
        }
        dialog.show()
    }

    private fun buildQualityProfileItems(ctx: Context): List<QualityProfileItemUi> {
        return QualityDataHelper.getProfiles().map { profile ->
            QualityProfileItemUi(
                id = profile.id,
                name = profile.name.asString(ctx),
                isUsed = profile.id == currentQualityProfile,
                types = profile.types,
            )
        }
    }

    private fun openComposeQualityProfileDialog() {
        val ctx = context ?: return
        val items = buildQualityProfileItems(ctx)
        updateHudState {
            copy(
                isQualityProfileDialogVisible = true,
                qualityProfileDialog = PlayerQualityProfileDialogState(
                    usedProfileId = currentQualityProfile,
                    usedProfileName = QualityDataHelper.getProfileName(currentQualityProfile)
                        .asString(ctx),
                    selectedProfileId = currentQualityProfile,
                    profiles = items,
                    useProfileSelection = true,
                ),
            )
        }
        syncOverlayAutoHideAndSubtitles()
    }

    override fun onQualityProfileDismiss() {
        updateHudState {
            copy(
                isQualityProfileDialogVisible = false,
                qualityProfileDialog = PlayerQualityProfileDialogState(),
            )
        }
        viewModel.state.clearSortedLinksCache()
        refreshComposeSourceLinks(currentQualityProfile)
        pushComposeSourceSubsState()
        syncOverlayAutoHideAndSubtitles()
        activity?.hideSystemUI()
    }

    override fun onQualityProfileUse() {
        val selectedId = hudStateFlow.value.qualityProfileDialog.selectedProfileId
        if (selectedId != null) {
            currentQualityProfile = selectedId
        }
        onQualityProfileDismiss()
    }

    override fun onQualityProfileSelected(profileId: Int) {
        val ctx = context ?: return
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    selectedProfileId = profileId,
                    profiles = buildQualityProfileItems(ctx),
                ),
            )
        }
    }

    override fun onQualityProfileEditClick() {
        val ctx = context ?: return
        val selectedId = hudStateFlow.value.qualityProfileDialog.selectedProfileId ?: return
        val linkSources = viewModel.state.links
            .mapNotNull { it.first?.source }
            .distinct()
        val sourceEntries = linkSources.map { src ->
            PriorityEntryUi(
                key = src,
                label = src,
                priority = QualityDataHelper.getSourcePriority(selectedId, src),
            )
        }.sortedBy { -it.priority }
        val qualityEntries = Qualities.entries.mapNotNull { q ->
            val label = Qualities.getStringByIntFull(q.value).ifBlank { return@mapNotNull null }
            PriorityEntryUi(
                key = q.value.toString(),
                label = label,
                priority = QualityDataHelper.getQualityPriority(selectedId, q),
            )
        }.sortedBy { -it.priority }

        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    editingProfileId = selectedId,
                    editingProfileNameInput = QualityDataHelper.getProfileName(selectedId).asString(ctx),
                    editingProfilePlaceholder = ctx.getString(R.string.profile_number, selectedId),
                    sourcePriorities = sourceEntries,
                    qualityPriorities = qualityEntries,
                    isEditingProfileSettings = false,
                ),
            )
        }
    }

    override fun onQualityProfileSetDefaultClick() {
        val ctx = context ?: return
        val selectedId = hudStateFlow.value.qualityProfileDialog.selectedProfileId ?: return
        val currentProfile = QualityDataHelper.getProfiles().firstOrNull { it.id == selectedId }
            ?: return
        val choices = QualityDataHelper.QualityProfileType.entries
            .filter { it != QualityDataHelper.QualityProfileType.None }
        val choiceNames = choices.map { txt(it.stringRes).asString(ctx) }
        val selectedIndices = choices.mapIndexedNotNull { index, type ->
            if (currentProfile.types.contains(type)) index else null
        }.toSet()

        showComposeMultiChoicePopup(
            title = ctx.getString(R.string.set_default),
            items = choiceNames,
            selectedIndices = selectedIndices,
            cancelable = true,
            onDismiss = { activity?.hideSystemUI() },
            onApply = { pickedSet ->
                val pickedChoices = pickedSet.mapNotNull { choices.getOrNull(it) }
                pickedChoices.forEach { pickedChoice ->
                    if (pickedChoice.unique) {
                        QualityDataHelper.getProfiles()
                            .filter { it.types.contains(pickedChoice) }
                            .forEach {
                                QualityDataHelper.removeQualityProfileType(it.id, pickedChoice)
                            }
                    }
                    QualityDataHelper.addQualityProfileType(currentProfile.id, pickedChoice)
                }
                val updatedProfiles = buildQualityProfileItems(ctx)
                updateHudState {
                    copy(
                        qualityProfileDialog = qualityProfileDialog.copy(
                            profiles = updatedProfiles,
                            usedProfileName = QualityDataHelper.getProfileName(currentQualityProfile)
                                .asString(ctx),
                        ),
                    )
                }
                activity?.hideSystemUI()
            },
        )
    }

    override fun onQualityProfileEditNameChanged(name: String) {
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    editingProfileNameInput = name,
                ),
            )
        }
    }

    override fun onQualityProfileSourcePriorityStep(key: String, delta: Int) {
        updateHudState {
            val updatedSources = qualityProfileDialog.sourcePriorities.map { entry ->
                if (entry.key == key) entry.copy(priority = entry.priority + delta) else entry
            }
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    sourcePriorities = updatedSources,
                ),
            )
        }
    }

    override fun onQualityProfileQualityPriorityStep(key: String, delta: Int) {
        updateHudState {
            val updatedQualities = qualityProfileDialog.qualityPriorities.map { entry ->
                if (entry.key == key) entry.copy(priority = entry.priority + delta) else entry
            }
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    qualityPriorities = updatedQualities,
                ),
            )
        }
    }

    override fun onQualityProfileEditSave() {
        val ctx = context ?: return
        val qp = hudStateFlow.value.qualityProfileDialog
        val profileId = qp.editingProfileId ?: return

        qp.qualityPriorities.forEach { entry ->
            val qVal = entry.key.toIntOrNull()
            val quality = Qualities.entries.firstOrNull { it.value == qVal }
            if (quality != null) {
                QualityDataHelper.setQualityPriority(profileId, quality, entry.priority)
            }
        }

        qp.sourcePriorities.forEach { entry ->
            QualityDataHelper.setSourcePriority(profileId, entry.key, entry.priority)
        }

        val savedName = qp.editingProfileNameInput
        if (savedName.isBlank()) {
            QualityDataHelper.setProfileName(profileId, null)
        } else {
            QualityDataHelper.setProfileName(profileId, savedName)
        }

        val sortedSources = qp.sourcePriorities.sortedBy { -it.priority }
        val sortedQualities = qp.qualityPriorities.sortedBy { -it.priority }
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    sourcePriorities = sortedSources,
                    qualityPriorities = sortedQualities,
                    profiles = buildQualityProfileItems(ctx),
                    usedProfileName = QualityDataHelper.getProfileName(currentQualityProfile)
                        .asString(ctx),
                ),
            )
        }
    }

    override fun onQualityProfileEditClose() {
        val ctx = context ?: return
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    editingProfileId = null,
                    isEditingProfileSettings = false,
                    profiles = buildQualityProfileItems(ctx),
                    usedProfileName = QualityDataHelper.getProfileName(currentQualityProfile)
                        .asString(ctx),
                ),
            )
        }
    }

    override fun onQualityProfileHelpClick() {
        val ctx = context ?: return
        showComposeConfirmPopup(
            title = ctx.getString(R.string.help),
            message = ctx.getString(R.string.quality_profile_help),
            confirmLabel = ctx.getString(R.string.ok),
            dismissLabel = null,
            cancelable = true,
            onDismiss = { activity?.hideSystemUI() },
            onConfirm = { activity?.hideSystemUI() },
        )
    }

    override fun onQualityProfileSettingsOpen() {
        val profileId = hudStateFlow.value.qualityProfileDialog.editingProfileId ?: return
        val hideError = QualityDataHelper.getProfileSetting(profileId, ProfileSettings.HideErrorSources)
        val hideNegative = QualityDataHelper.getProfileSetting(profileId, ProfileSettings.HideNegativeSources)
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    isEditingProfileSettings = true,
                    hideErrorSources = hideError,
                    hideNegativeSources = hideNegative,
                ),
            )
        }
    }

    override fun onQualityProfileHideNegativeChanged(checked: Boolean) {
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    hideNegativeSources = checked,
                ),
            )
        }
    }

    override fun onQualityProfileHideErrorChanged(checked: Boolean) {
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    hideErrorSources = checked,
                ),
            )
        }
    }

    override fun onQualityProfileSettingsApply() {
        val qp = hudStateFlow.value.qualityProfileDialog
        val profileId = qp.editingProfileId ?: return
        QualityDataHelper.setProfileSetting(profileId, ProfileSettings.HideErrorSources, qp.hideErrorSources)
        QualityDataHelper.setProfileSetting(profileId, ProfileSettings.HideNegativeSources, qp.hideNegativeSources)
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    isEditingProfileSettings = false,
                ),
            )
        }
    }

    override fun onQualityProfileSettingsCancel() {
        updateHudState {
            copy(
                qualityProfileDialog = qualityProfileDialog.copy(
                    isEditingProfileSettings = false,
                ),
            )
        }
    }

    override fun onSubtitleGroupSelected(index: Int) {
        if (sourceSubsGroupIndex != index) {
            sourceSubsGroupIndex = index
            sourceSubsOptionIndex = if (sourceSubsGroupIndex == sourceSubsGroupIndexStart) {
                sourceSubsOptionIndexStart
            } else {
                0
            }
            pushComposeSourceSubsState()
        }
    }

    override fun onSubtitleAudioOptionSelected(index: Int) {
        val optionCount = sourceSubsSubtitlesGroupedList
            .getOrNull(sourceSubsGroupIndex - 1)
            ?.value
            ?.size
            ?: 0
        if (index in 0 until optionCount) {
            sourceSubsOptionIndex = index
            pushComposeSourceSubsState()
        }
    }

    override fun onSubtitleEncodingClick() {
        val ctx = context ?: return
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)
        val prefNames = ctx.resources.getStringArray(R.array.subtitles_encoding_list)
        val prefValues = ctx.resources.getStringArray(R.array.subtitles_encoding_values)
        val currentPrefMedia = settingsManager.getString(
            ctx.getString(R.string.subtitles_encoding_key),
            null
        )
        val index = prefValues.indexOf(currentPrefMedia)
        val selectedIndex = if (index == -1) 0 else index

        if (useComposeHud) {
            showComposeSingleChoicePopup(
                title = ctx.getString(R.string.subtitles_encoding),
                items = prefNames.toList(),
                selectedIndex = selectedIndex,
                cancelable = true,
                onDismiss = { activity?.hideSystemUI() },
                onSelect = { picked ->
                    settingsManager.edit {
                        putString(
                            ctx.getString(R.string.subtitles_encoding_key),
                            prefValues[picked]
                        )
                    }
                    updateForcedEncoding(ctx)
                    pushComposeSourceSubsState(ctx)
                    player.seekTime(-1)
                    activity?.hideSystemUI()
                },
            )
            return
        }

        activity?.showDialog(
            prefNames.toList(),
            selectedIndex,
            ctx.getString(R.string.subtitles_encoding),
            true,
            { activity?.hideSystemUI() }
        ) {
            settingsManager.edit {
                putString(
                    ctx.getString(R.string.subtitles_encoding_key),
                    prefValues[it]
                )
            }
            updateForcedEncoding(ctx)
            pushComposeSourceSubsState(ctx)
            player.seekTime(-1)
            activity?.hideSystemUI()
        }
    }

    override fun onSubtitleSettingsClick() {
        if (useComposeHud) {
            updateHudState { copy(isSubtitleSettingsVisible = true) }
            syncOverlayAutoHideAndSubtitles()
            return
        }
        safe {
            val subtitlesFragment = SubtitlesFragmentDialog2()
            subtitlesFragment.systemBarsAddPadding = true
            subtitlesFragment.show(this.parentFragmentManager, "SubtitleSettings")
        }
    }

    override fun onSubtitleSettingsDismiss() {
        updateHudState { copy(isSubtitleSettingsVisible = false) }
        pushComposeSourceSubsState()
        syncOverlayAutoHideAndSubtitles()
        activity?.hideSystemUI()
    }

    override fun onLoadSubtitlesFromFile() {
        openSubPicker()
    }

    override fun onLoadSubtitlesOnline() {
        val ctx = context ?: return
        val currentLoadResponse = viewModel.state.generatorState?.response
        val wasPlaying = sourceSubsWasPlaying
        dismissComposeSourceSubsDialog(resumePlayback = false)
        openOnlineSubPicker(ctx, currentLoadResponse) {
            if (wasPlaying) {
                player.handleEvent(CSPlayerEvent.Play)
            }
            activity?.hideSystemUI()
        }
    }

    private fun openComposeOnlineSubPicker(
        ctx: Context,
        loadResponse: LoadResponse?,
        dismissCallback: () -> Unit,
    ) {
        onlineSubSearchJob?.cancel()
        onlineSubApplyJob?.cancel()
        onlineSubLoadResponse = loadResponse
        onlineSubDismissCallback = dismissCallback
        onlineSubCurrentEntities = emptyList()
        onlineSubSelectedEntity = null
        onlineSubLanguageTagIETF = getAutoSelectLanguageTagIETF()

        val currentTempMeta = getMetaData()
        val initialQuery = currentTempMeta.name.orEmpty()
        val yearLabel = viewModel.currentSubtitleYear.value?.toString()
            ?: ctx.getString(R.string.none)
        val langLabel = fromTagToLanguageName(onlineSubLanguageTagIETF)
            ?: onlineSubLanguageTagIETF

        updateHudState {
            copy(
                isOnlineSubtitlesDialogVisible = true,
                onlineSubtitlesDialog = PlayerOnlineSubtitlesDialogState(
                    query = initialQuery,
                    yearLabel = yearLabel,
                    languageLabel = langLabel,
                    isSearching = false,
                    isApplying = false,
                    results = emptyList(),
                    selectedIndex = -1,
                ),
            )
        }
        syncOverlayAutoHideAndSubtitles()
        performComposeOnlineSubSearch(initialQuery)
    }

    private fun performComposeOnlineSubSearch(query: String) {
        onlineSubSearchJob?.cancel()
        val providers = subsProviders.toList()
        val isSingleProvider = providers.size == 1
        val currentTempMeta = getMetaData()
        val loadResponse = onlineSubLoadResponse
        val langTag = onlineSubLanguageTagIETF
        val yearVal = viewModel.currentSubtitleYear.value

        updateHudState {
            copy(
                onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                    isSearching = true,
                ),
            )
        }

        onlineSubSearchJob = ioSafe {
            val search = SubtitleSearch(
                query = query,
                imdbId = loadResponse?.getImdbId(),
                tmdbId = loadResponse?.getTMDbId()?.toInt(),
                malId = loadResponse?.getMalId()?.toInt(),
                aniListId = loadResponse?.getAniListId()?.toInt(),
                epNumber = currentTempMeta.episode,
                seasonNumber = currentTempMeta.season,
                lang = langTag.ifBlank { null },
                year = yearVal,
            )

            val results = providers.amap {
                when (val response = Resource.fromResult(it.search(search))) {
                    is Resource.Success -> response.value
                    is Resource.Loading -> emptyList()
                    is Resource.Failure -> {
                        showToast(response.errorString)
                        emptyList()
                    }
                }
            }
            val max = results.maxOfOrNull { it.size } ?: 0
            val items = ArrayList<AbstractSubtitleEntities.SubtitleEntity>()
            val arrays = results.size
            for (index in 0 until max) {
                for (i in 0 until arrays) {
                    items.add(results[i].getOrNull(index) ?: continue)
                }
            }

            val uiItems = items.mapIndexed { idx, entity ->
                val language = fromTagToLanguageName(entity.lang) ?: entity.lang
                val providerSuffix = if (isSingleProvider) "" else " · ${entity.source}"
                OnlineSubtitleItemUi(
                    index = idx,
                    title = getName(entity, false),
                    subtitle = language + providerSuffix,
                    isHearingImpaired = entity.isHearingImpaired,
                )
            }

            runOnMainThread {
                if (!hudStateFlow.value.isOnlineSubtitlesDialogVisible) return@runOnMainThread
                onlineSubCurrentEntities = items
                onlineSubSelectedEntity = null
                updateHudState {
                    copy(
                        onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                            isSearching = false,
                            results = uiItems,
                            selectedIndex = -1,
                        ),
                    )
                }
            }
        }
    }

    override fun onOnlineSubtitlesDismiss() {
        onlineSubSearchJob?.cancel()
        onlineSubApplyJob?.cancel()
        val callback = onlineSubDismissCallback
        onlineSubDismissCallback = null
        onlineSubLoadResponse = null
        onlineSubCurrentEntities = emptyList()
        onlineSubSelectedEntity = null
        updateHudState {
            copy(
                isOnlineSubtitlesDialogVisible = false,
                onlineSubtitlesDialog = PlayerOnlineSubtitlesDialogState(),
            )
        }
        syncOverlayAutoHideAndSubtitles()
        callback?.invoke()
        activity?.hideSystemUI()
    }

    override fun onOnlineSubtitlesQueryChanged(query: String) {
        updateHudState {
            copy(
                onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                    query = query,
                ),
            )
        }
    }

    override fun onOnlineSubtitlesSearchSubmit() {
        performComposeOnlineSubSearch(hudStateFlow.value.onlineSubtitlesDialog.query)
    }

    override fun onOnlineSubtitlesYearClick() {
        val ctx = context ?: return
        val none = txt(R.string.none).asString(ctx)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val earliestYear = 1900
        val years = (currentYear downTo earliestYear).toList()
        val options = listOf(none) + years.map { it.toString() }
        val selectedIndex = viewModel.currentSubtitleYear.value
            ?.let { years.indexOf(it) + 1 }
            ?.takeIf { it >= 0 } ?: 0

        showComposeSingleChoicePopup(
            title = txt(R.string.year).asString(ctx),
            items = options,
            selectedIndex = selectedIndex,
            cancelable = true,
            onDismiss = { activity?.hideSystemUI() },
            onSelect = { index ->
                val pickedYear = years.getOrNull(index - 1)
                viewModel.setSubtitleYear(pickedYear)
                val yearLabel = pickedYear?.toString() ?: none
                updateHudState {
                    copy(
                        onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                            yearLabel = yearLabel,
                        ),
                    )
                }
                performComposeOnlineSubSearch(hudStateFlow.value.onlineSubtitlesDialog.query)
            },
        )
    }

    override fun onOnlineSubtitlesLanguageClick() {
        val ctx = context ?: return
        val languagesTagName = languages
            .map { Pair(it.IETF_tag, it.nameNextToFlagEmoji()) }
            .sortedBy { it.second.substringAfter("\u00a0").lowercase() }
        val (langTagsIETF, langNames) = languagesTagName.unzip()

        showComposeSingleChoicePopup(
            title = ctx.getString(R.string.subs_subtitle_languages),
            items = langNames,
            selectedIndex = langTagsIETF.indexOf(onlineSubLanguageTagIETF),
            cancelable = true,
            onDismiss = { activity?.hideSystemUI() },
            onSelect = { index ->
                onlineSubLanguageTagIETF = langTagsIETF[index]
                val langLabel = fromTagToLanguageName(onlineSubLanguageTagIETF)
                    ?: onlineSubLanguageTagIETF
                updateHudState {
                    copy(
                        onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                            languageLabel = langLabel,
                        ),
                    )
                }
                performComposeOnlineSubSearch(hudStateFlow.value.onlineSubtitlesDialog.query)
            },
        )
    }

    override fun onOnlineSubtitlesResultSelected(index: Int) {
        val entity = onlineSubCurrentEntities.getOrNull(index) ?: return
        onlineSubSelectedEntity = entity
        updateHudState {
            copy(
                onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                    selectedIndex = index,
                ),
            )
        }
    }

    override fun onOnlineSubtitlesApply() {
        val currentSubtitle = onlineSubSelectedEntity
        if (currentSubtitle == null) {
            onOnlineSubtitlesDismiss()
            return
        }
        val api = subsProviders.firstOrNull { it.idPrefix == currentSubtitle.idPrefix }
        if (api == null) {
            onOnlineSubtitlesDismiss()
            return
        }

        updateHudState {
            copy(
                onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                    isApplying = true,
                ),
            )
        }
        onlineSubApplyJob?.cancel()
        onlineSubApplyJob = ioSafe {
            val apiResource = Resource.fromResult(api.resource(currentSubtitle))
            runOnMainThread {
                updateHudState {
                    copy(
                        onlineSubtitlesDialog = onlineSubtitlesDialog.copy(
                            isApplying = false,
                        ),
                    )
                }
            }
            when (apiResource) {
                is Resource.Success -> {
                    val subtitles = apiResource.value.getSubtitles().map { resource ->
                        SubtitleData(
                            originalName = resource.name ?: getName(currentSubtitle, true),
                            nameSuffix = "",
                            url = resource.url,
                            origin = resource.origin,
                            mimeType = resource.url.toSubtitleMimeType(),
                            headers = currentSubtitle.headers,
                            languageCode = currentSubtitle.lang,
                        )
                    }
                    if (subtitles.isEmpty()) {
                        showToast(R.string.no_subtitles)
                        return@ioSafe
                    }
                    runOnMainThread {
                        onOnlineSubtitlesDismiss()
                        addAndSelectSubtitles(*subtitles.toTypedArray())
                    }
                }

                is Resource.Failure -> {
                    showToast(apiResource.errorString)
                }

                is Resource.Loading -> Unit
            }
        }
    }

    override fun onLoadFirstSubtitleOnline() {
        val currentLoadResponse = viewModel.state.generatorState?.response
        val metadata = getMetaData()
        val queryName = metadata.name ?: currentLoadResponse?.name ?: return
        val currentLanguageTagIETF = getAutoSelectLanguageTagIETF()
        dismissComposeSourceSubsDialog(resumePlayback = true)
        showToast(R.string.loading)
        addFirstSub(
            SubtitleSearch(
                query = queryName,
                imdbId = currentLoadResponse?.getImdbId(),
                tmdbId = currentLoadResponse?.getTMDbId()?.toInt(),
                malId = currentLoadResponse?.getMalId()?.toInt(),
                aniListId = currentLoadResponse?.getAniListId()?.toInt(),
                epNumber = metadata.episode,
                seasonNumber = metadata.season,
                lang = currentLanguageTagIETF.ifBlank { null },
                year = viewModel.currentSubtitleYear.value
            )
        )
    }

    override fun showTracksDialogue() {
        if (useComposeHud) {
            showComposeTracksDialog()
            return
        }
        try {
            //println("CURRENT SELECTED :$currentSelectedSubtitles of $currentSubs")
            context?.let { ctx ->
                val tracks = player.getVideoTracks()

                val isPlaying = player.getIsPlaying()
                player.handleEvent(CSPlayerEvent.Pause)

                val currentVideoTracks = tracks.allVideoTracks.sortedBy {
                    it.height?.times(-1)
                }
                val currentAudioTracks = tracks.allAudioTracks
                val binding: PlayerSelectTracksBinding =
                    PlayerSelectTracksBinding.inflate(LayoutInflater.from(ctx), null, false)
                val trackDialog = Dialog(ctx, R.style.DialogFullscreenPlayer)
                this.selectTrackDialog = trackDialog
                trackDialog.setContentView(binding.root)
                trackDialog.show()

                fixSystemBarsPadding(binding.root)

                // selectTracksDialog = tracksDialog

                val videosList = binding.videoTracksList
                val audioList = binding.autoTracksList

                binding.videoTracksHolder.isVisible = currentVideoTracks.size > 1
                binding.audioTracksHolder.isVisible = currentAudioTracks.size > 1

                fun dismiss() {
                    if (isPlaying) {
                        player.handleEvent(CSPlayerEvent.Play)
                    }
                    activity?.hideSystemUI()
                }

                val videosArrayAdapter =
                    ArrayAdapter<String>(ctx, R.layout.sort_bottom_single_choice)

                videosArrayAdapter.addAll(currentVideoTracks.mapIndexed { index, format ->
                    format.label
                        ?: (if (format.height == NO_VALUE || format.width == NO_VALUE) index else "${format.width}x${format.height}").toString()
                })

                videosList.choiceMode = AbsListView.CHOICE_MODE_SINGLE
                videosList.adapter = videosArrayAdapter

                // Sometimes the data is not the same because some data gets resolved at different stages i think
                var videoIndex = currentVideoTracks.indexOf(tracks.currentVideoTrack).takeIf {
                    it != -1
                } ?: currentVideoTracks.indexOfFirst {
                    tracks.currentVideoTrack?.id == it.id
                }

                videosList.setSelection(videoIndex)
                videosList.setItemChecked(videoIndex, true)

                videosList.setOnItemClickListener { _, _, which, _ ->
                    videoIndex = which
                    videosList.setItemChecked(which, true)
                }

                trackDialog.setOnDismissListener {
                    dismiss()
                    // selectTracksDialog = null
                }

                var audioIndexStart = currentAudioTracks.indexOfFirst { track ->
                    track.id == tracks.currentAudioTrack?.id &&
                            track.formatIndex == tracks.currentAudioTrack?.formatIndex
                }.coerceAtLeast(0)

                val audioArrayAdapter =
                    ArrayAdapter<String>(ctx, R.layout.sort_bottom_single_choice)

                audioArrayAdapter.addAll(
                    currentAudioTracks.mapIndexed { _, track ->

                        val language = (
                                track.language?.trim()?.let { raw ->
                                    fromTagToLanguageName(raw)
                                        ?: fromTagToLanguageName(
                                            raw.replace('_', '-').substringBefore('-').lowercase()
                                        )
                                        ?: raw
                                }
                                    ?: track.label
                                    ?: "Audio"
                                ).replaceFirstChar { it.uppercaseChar() }

                        val codec = audioCodecName(track.sampleMimeType)

                        val channelCount = track.channelCount

                        val channels = when {
                            // May be below 1 or null when unknown
                            channelCount == null || channelCount <= 0 -> ""
                            channelCount == 1 -> "Mono"
                            channelCount == 2 -> "Stereo"
                            channelCount == 6 -> "5.1"
                            channelCount == 8 -> "7.1"
                            else -> "${channelCount}ch"
                        }

                        listOfNotNull(
                            language.takeIf { it.isNotBlank() }
                                ?.replaceFirstChar { it.uppercaseChar() },
                            channels.takeIf { it.isNotBlank() },
                            codec.takeIf { it.isNotBlank() }?.uppercase()
                        ).joinToString(" • ")


                    }
                )

                audioList.adapter = audioArrayAdapter
                audioList.choiceMode = AbsListView.CHOICE_MODE_SINGLE

                audioList.setSelection(audioIndexStart)
                audioList.setItemChecked(audioIndexStart, true)

                audioList.setOnItemClickListener { _, _, which, _ ->
                    audioIndexStart = which
                    audioList.setItemChecked(which, true)
                }

                binding.cancelBtt.setOnClickListener {
                    trackDialog.dismissSafe(activity)
                    this.selectTrackDialog = null
                }

                binding.applyBtt.setOnClickListener {
                    val currentTrack = currentAudioTracks.getOrNull(audioIndexStart)
                    player.setPreferredAudioTrack(
                        currentTrack?.language,
                        currentTrack?.id,
                        currentTrack?.formatIndex,
                    )

                    val currentVideo = currentVideoTracks.getOrNull(videoIndex)
                    val width = currentVideo?.width ?: NO_VALUE
                    val height = currentVideo?.height ?: NO_VALUE
                    if (width != NO_VALUE && height != NO_VALUE) {
                        player.setMaxVideoSize(width, height, currentVideo?.id)
                    }
                    trackDialog.dismissSafe(activity)
                    this.selectTrackDialog = null
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun showComposeTracksDialog() {
        try {
            val tracks = player.getVideoTracks()

            tracksWasPlaying = player.getIsPlaying()
            player.handleEvent(CSPlayerEvent.Pause)

            val currentVideoTracks = tracks.allVideoTracks.sortedBy {
                it.height?.times(-1)
            }
            val currentAudioTracks = tracks.allAudioTracks

            tracksCurrentVideoTracks = currentVideoTracks
            tracksCurrentAudioTracks = currentAudioTracks

            tracksSelectedVideoIndex = currentVideoTracks.indexOf(tracks.currentVideoTrack).takeIf {
                it != -1
            } ?: currentVideoTracks.indexOfFirst {
                tracks.currentVideoTrack?.id == it.id
            }

            tracksSelectedAudioIndex = currentAudioTracks.indexOfFirst { track ->
                track.id == tracks.currentAudioTrack?.id &&
                    track.formatIndex == tracks.currentAudioTrack?.formatIndex
            }.coerceAtLeast(0)

            tracksScrollToken++
            pushComposeTracksState()
            setTracksOverlayVisible(true)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun pushComposeTracksState() {
        val hasVideoTracks = tracksCurrentVideoTracks.size > 1
        val hasAudioTracks = tracksCurrentAudioTracks.size > 1

        val videoItems = if (hasVideoTracks) {
            tracksCurrentVideoTracks.mapIndexed { index, format ->
                val label = format.label
                    ?: (if (format.height == NO_VALUE || format.width == NO_VALUE) index else "${format.width}x${format.height}").toString()
                TrackItemUi(index = index, label = label)
            }
        } else {
            emptyList()
        }

        val audioItems = if (hasAudioTracks) {
            tracksCurrentAudioTracks.mapIndexed { index, track ->
                val language = (
                    track.language?.trim()?.let { raw ->
                        fromTagToLanguageName(raw)
                            ?: fromTagToLanguageName(
                                raw.replace('_', '-').substringBefore('-').lowercase()
                            )
                            ?: raw
                    }
                        ?: track.label
                        ?: "Audio"
                    ).replaceFirstChar { it.uppercaseChar() }

                val codec = audioCodecName(track.sampleMimeType)
                val channelCount = track.channelCount
                val channels = when {
                    channelCount == null || channelCount <= 0 -> ""
                    channelCount == 1 -> "Mono"
                    channelCount == 2 -> "Stereo"
                    channelCount == 6 -> "5.1"
                    channelCount == 8 -> "7.1"
                    else -> "${channelCount}ch"
                }

                val label = listOfNotNull(
                    language.takeIf { it.isNotBlank() }
                        ?.replaceFirstChar { it.uppercaseChar() },
                    channels.takeIf { it.isNotBlank() },
                    codec.takeIf { it.isNotBlank() }?.uppercase()
                ).joinToString(" • ")

                TrackItemUi(index = index, label = label)
            }
        } else {
            emptyList()
        }

        updateHudState {
            copy(
                tracksDialog = PlayerTracksDialogState(
                    hasVideoTracks = hasVideoTracks,
                    videoTracks = videoItems,
                    selectedVideoTrackIndex = tracksSelectedVideoIndex,
                    hasAudioTracks = hasAudioTracks,
                    audioTracks = audioItems,
                    selectedAudioTrackIndex = tracksSelectedAudioIndex,
                    scrollTriggerToken = tracksScrollToken,
                )
            )
        }
    }

    private fun dismissComposeTracksDialog() {
        val wasPlaying = tracksWasPlaying
        tracksWasPlaying = false
        setTracksOverlayVisible(false)
        if (wasPlaying) {
            player.handleEvent(CSPlayerEvent.Play)
        }
        activity?.hideSystemUI()
    }

    override fun onTracksDismiss() {
        dismissComposeTracksDialog()
    }

    override fun onTracksApply() {
        val currentTrack = tracksCurrentAudioTracks.getOrNull(tracksSelectedAudioIndex)
        player.setPreferredAudioTrack(
            currentTrack?.language,
            currentTrack?.id,
            currentTrack?.formatIndex,
        )

        val currentVideo = tracksCurrentVideoTracks.getOrNull(tracksSelectedVideoIndex)
        val width = currentVideo?.width ?: NO_VALUE
        val height = currentVideo?.height ?: NO_VALUE
        if (width != NO_VALUE && height != NO_VALUE) {
            player.setMaxVideoSize(width, height, currentVideo?.id)
        }
        dismissComposeTracksDialog()
    }

    override fun onVideoTrackSelected(index: Int) {
        if (index in tracksCurrentVideoTracks.indices) {
            tracksSelectedVideoIndex = index
            pushComposeTracksState()
        }
    }

    override fun onAudioTrackSelected(index: Int) {
        if (index in tracksCurrentAudioTracks.indices) {
            tracksSelectedAudioIndex = index
            pushComposeTracksState()
        }
    }

    override fun playerError(exception: Throwable) {
        currentSelectedLink?.let { link ->
            viewModel.modifyState { this.addError(link) }
        }

        val currentUrl =
            currentSelectedLink?.let { it.first?.url ?: it.second?.uri?.toString() } ?: "unknown"
        val headers = currentSelectedLink?.first?.headers?.toString() ?: "none"
        val referer = currentSelectedLink?.first?.referer ?: "none"
        Log.e(
            TAG,
            "playerError: $currentSelectedLink, " +
                    "type=${exception::class.qualifiedName}, " +
                    "message=${exception.message}, url=$currentUrl, headers=$headers, " +
                    "referer=$referer, position=${player.getPosition() ?: "unknown"}, " +
                    "duration=${player.getDuration() ?: "unknown"}, " +
                    "isPlaying=${player.getIsPlaying()}", exception
        )

        if (!hasNextMirror()) {
            viewModel.forceClearCache = true
        }
        super.playerError(exception)
    }

    private fun noLinksFound() {
        viewModel.forceClearCache = true
        val hiddenLinks = viewModel.state.sortLinks(currentQualityProfile).count { !it.shouldUseLink }

        context?.let { ctx ->
            // Display that there are hidden links to the user.
            if (hiddenLinks > 0) {
                val noLinksString = ctx.getString(R.string.no_links_found_toast)
                val hiddenString =
                    ctx.resources.getQuantityString(R.plurals.links_hidden, hiddenLinks)
                        .format(hiddenLinks)
                val toastText = "$noLinksString\n($hiddenString)"
                showToast(toastText, Toast.LENGTH_SHORT)
            } else {
                showToast(R.string.no_links_found_toast, Toast.LENGTH_SHORT)
            }
        }

        activity?.popCurrentPage()
    }

    private fun startPlayer() {
        // We don't want double load when you skip loading
        if (isPlayerActive.get()) {
            return
        }

        val links = viewModel.state.sortLinks(currentQualityProfile)

        val firstAvailableLink = links.firstOrNull { it.shouldUseLink }?.link
        if (firstAvailableLink == null) {
            noLinksFound()
            return
        }
        // Atomic operation to prevent double loading
        if (!isPlayerActive.compareAndSet(false, true)) {
            return
        }
        loadLink(firstAvailableLink, false)
        showPlayerMetadata()
    }

    private fun showPlayerMetadata() {
        val overlay = playerBinding?.playerMetadataScrim ?: return

        val titleView = overlay.findViewById<TextView>(R.id.player_movie_title)
        val logoView = overlay.findViewById<ImageView>(R.id.player_movie_logo)
        val metaView = overlay.findViewById<TextView>(R.id.player_movie_meta)
        val descView = overlay.findViewById<TextView>(R.id.player_movie_overview)

        val load = viewModel.state.generatorState?.response ?: return
        val episode = currentMeta as? ResultEpisode
        titleView.text = load.name

        bindLogo(
            url = load.logoUrl,
            headers = load.posterHeaders,
            titleView = titleView,
            logoView = logoView
        )

        val meta = arrayOf(
            load.tags?.takeIf { it.isNotEmpty() }?.take(6)?.joinToString(", "),
            load.year?.toString(),
            if (!load.type.isMovieType())
                context?.getShortSeasonText(
                    episode = episode?.episode,
                    season = episode?.season
                )
            else null,
            load.score?.let { "⭐ $it" }
        ).filterNotNull()
            .joinToString(" • ")

        metaView.text = meta
        metaView.isVisible = meta.isNotBlank()


        val description = load.plot

        if (!description.isNullOrBlank()) {
            descView.isVisible = true
            descView.text = description.html()
        } else {
            descView.isVisible = false

        }
    }

    override fun nextEpisode() {
        if (viewModel.hasNextEpisode() == true) {
            isNextEpisode = true
            releasePlayer()
            viewModel.loadLinksNext()
        }
    }

    override fun prevEpisode() {
        if (viewModel.hasPrevEpisode() == true) {
            isNextEpisode = true
            releasePlayer()
            viewModel.loadLinksPrev()
        }
    }

    private fun getNextLink(): DisplayLink? {
        val links = viewModel.state.sortLinks(currentQualityProfile)
        val currentIndex = links.indexOfFirst { it.link == currentSelectedLink }
        val nextPotentialLink =
            links.withIndex().firstOrNull { it.index > currentIndex && it.value.shouldUseLink }
        return nextPotentialLink?.value
    }

    override fun hasNextMirror(): Boolean {
        return getNextLink() != null
    }

    override fun nextMirror() {
        val nextLink = getNextLink()
        if (nextLink == null) {
            noLinksFound()
            return
        }

        loadLink(nextLink.link, true)
    }

    override fun onDestroy() {
        ResultFragment.updateUI()
        currentVerifyLink?.cancel()
        onlineSubSearchJob?.cancel()
        onlineSubApplyJob?.cancel()
        super.onDestroy()
    }

    var maxEpisodeSet: Int? = null
    var hasRequestedStamps: Boolean = false
    override fun playerPositionChanged(position: Long, duration: Long) {
        // Don't save livestream data
        if ((currentMeta as? ResultEpisode)?.tvType?.isLiveStream() == true) return

        // Don't save NSFW data
        if ((currentMeta as? ResultEpisode)?.tvType == TvType.NSFW) return

        if (duration <= 0L) return // idk how you achieved this, but div by zero crash
        if (!hasRequestedStamps) {
            hasRequestedStamps = true
            val fetchStamps = context?.let { ctx ->
                val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)
                settingsManager.getBoolean(
                    ctx.getString(R.string.enable_skip_op_from_database),
                    true
                )
            } ?: true
            if (fetchStamps)
                viewModel.loadStamps(duration)
        }

        val percentage = position * 100L / duration

        DataStoreHelper.setViewPosAndResume(
            viewModel.state.generatorState?.id,
            position,
            duration,
            currentMeta,
            nextMeta
        )

        var isOpVisible = false
        when (val meta = currentMeta) {
            is ResultEpisode -> {
                if (percentage >= UPDATE_SYNC_PROGRESS_PERCENTAGE && (maxEpisodeSet
                        ?: -1) < meta.episode
                ) {
                    context?.let { ctx ->
                        val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)
                        if (settingsManager.getBoolean(
                                ctx.getString(R.string.episode_sync_enabled_key), true
                            )
                        ) {
                            maxEpisodeSet = meta.episode
                            sync.modifyMaxEpisode(meta.totalEpisodeIndex ?: meta.episode)
                        }
                    }
                }

                if (meta.tvType.isAnimeOp()) isOpVisible = percentage < SKIP_OP_VIDEO_PERCENTAGE
            }
        }

        val hasNextEpisode = viewModel.hasNextEpisode() == true
        if (useComposeHud) {
            updateHudState {
                copy(
                    positionMs = position,
                    durationMs = duration,
                    bufferedPositionMs = player.getBufferedPosition()?.coerceAtLeast(0L) ?: bufferedPositionMs,
                    isAtLiveEdge = player.isAtLiveEdge(),
                    isSkipOpVisible = isOpVisible,
                    hasNextEpisode = !isOpVisible && hasNextEpisode,
                )
            }
        } else {
            playerBinding?.playerSkipOp?.isVisible = isOpVisible

            when {
                isLayout(PHONE) ->
                    playerBinding?.playerSkipEpisode?.isVisible =
                        !isOpVisible && hasNextEpisode

                else -> {
                    playerBinding?.playerGoForward?.isVisible = hasNextEpisode
                    playerBinding?.playerGoForwardRoot?.isVisible = hasNextEpisode
                }
            }
        }

        if (percentage >= PRELOAD_NEXT_EPISODE_PERCENTAGE) {
            viewModel.preLoadNextLinks()
        }
    }

    private fun syncSkipOpAndNextEpisodeState() {
        if (!useComposeHud) return
        val duration = player.getDuration()?.takeIf { it > 0L } ?: 0L
        val position = player.getPosition()?.coerceAtLeast(0L) ?: 0L
        val percentage = if (duration > 0L) position * 100L / duration else 0L
        val isOpVisible = (currentMeta as? ResultEpisode)?.tvType?.isAnimeOp() == true &&
            percentage < SKIP_OP_VIDEO_PERCENTAGE
        val hasNext = viewModel.hasNextEpisode() == true
        updateHudState {
            copy(
                isSkipOpVisible = isOpVisible,
                hasNextEpisode = !isOpVisible && hasNext,
            )
        }
    }

    private fun getAutoSelectSubtitle(
        subtitles: Set<SubtitleData>, settings: Boolean, downloads: Boolean
    ): SubtitleData? {
        val langCode = preferredAutoSelectSubtitles ?: return null
        if (downloads) {
            sortSubs(subtitles).firstOrNull {
                it.origin == SubtitleOrigin.DOWNLOADED_FILE && it.matchesLanguageCode(
                    langCode
                )
            }?.let { return it }
        }

        if (!settings) return null

        return sortSubs(subtitles).firstOrNull { it.matchesLanguageCode(langCode) }
    }

    private fun autoSelectFromSettings(): Boolean {
        // auto select subtitle based on settings
        val langCode = preferredAutoSelectSubtitles
        val current = player.getCurrentPreferredSubtitle()
        Log.i(TAG, "autoSelectFromSettings = $current")
        context?.let { ctx ->
            // Only use the player preferred subtitle if it matches the available language
            if (current != null && (langCode == null || current.matchesLanguageCode(langCode))) {
                if (setSubtitles(current, false)) {
                    player.saveData()
                    player.reloadPlayer(ctx)
                    player.handleEvent(CSPlayerEvent.Play)
                    return true
                }
            } else if (!langCode.isNullOrEmpty()) {
                getAutoSelectSubtitle(
                    viewModel.state.subtitles, settings = true, downloads = false
                )?.let { sub ->
                    if (setSubtitles(sub, false)) {
                        player.saveData()
                        player.reloadPlayer(ctx)
                        player.handleEvent(CSPlayerEvent.Play)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun autoSelectFromDownloads() {
        if (player.getCurrentPreferredSubtitle() != null) {
            return
        }
        val sub =
            getAutoSelectSubtitle(viewModel.state.subtitles, settings = false, downloads = true)
                ?: return
        val ctx = context ?: return
        if (!setSubtitles(sub, false)) {
            return
        }
        player.saveData()
        player.reloadPlayer(ctx)
        player.handleEvent(CSPlayerEvent.Play)
    }

    private fun autoSelectSubtitles() {
        //Log.i(TAG, "autoSelectSubtitles")
        safe {
            if (!autoSelectFromSettings()) {
                autoSelectFromDownloads()
            }
        }
    }

    private fun getHeaderName(): String? {
        return when (val meta = currentMeta) {
            is ResultEpisode -> meta.headerName
            is ExtractorUri -> meta.headerName
            else -> null
        }
    }

    private fun getPlayerVideoTitle(): String {
        var headerName: String? = null
        var subName: String? = null
        var episode: Int? = null
        var season: Int? = null
        var tvType: TvType? = null

        when (val meta = currentMeta) {
            is ResultEpisode -> {
                headerName = meta.headerName
                subName = meta.name
                episode = meta.episode
                season = meta.season
                tvType = meta.tvType
            }

            is ExtractorUri -> {
                headerName = meta.headerName
                subName = meta.name
                episode = meta.episode
                season = meta.season
                tvType = meta.tvType
            }
        }
        context?.let { ctx ->
            //Generate video title
            val playerVideoTitle = if (headerName != null) {
                (headerName + if (tvType.isEpisodeBased() && episode != null) if (season == null) " - ${
                    ctx.getString(
                        R.string.episode
                    )
                } $episode"
                else " \"${ctx.getString(R.string.season_short)}${season}:${
                    ctx.getString(
                        R.string.episode_short
                    )
                }${episode}\""
                else "") + if (subName.isNullOrBlank() || subName == headerName) "" else " - $subName"
            } else {
                ""
            }
            return playerVideoTitle
        }
        return ""
    }

    fun setTitle() {
        var playerVideoTitle = getPlayerVideoTitle()

        //Hide title, if set in setting
        if (limitTitle < 0) {
            playerBinding?.playerVideoTitle?.visibility = View.GONE
        } else {
            //Truncate video title if it exceeds limit
            val differenceInLength = playerVideoTitle.length - limitTitle
            val margin = 3 //If the difference is smaller than or equal to this value, ignore it
            if (limitTitle > 0 && differenceInLength > margin) {
                playerVideoTitle = playerVideoTitle.substring(0, limitTitle - 1) + "..."
            }
        }
        val isFiller: Boolean? = (currentMeta as? ResultEpisode)?.isFiller
        val offlinePin = viewModel.generator is DownloadFileGenerator

        if (useComposeHud) {
            val duration = player.getDuration()?.takeIf { it > 0L } ?: 0L
            val position = player.getPosition()?.coerceAtLeast(0L) ?: 0L
            val percentage = if (duration > 0L) position * 100L / duration else 0L
            val isOpVisible = (currentMeta as? ResultEpisode)?.tvType?.isAnimeOp() == true &&
                percentage < SKIP_OP_VIDEO_PERCENTAGE
            val hasNext = viewModel.hasNextEpisode() == true
            updateHudState {
                copy(
                    title = if (limitTitle < 0) "" else playerVideoTitle,
                    isFiller = isFiller ?: false,
                    showOfflinePin = offlinePin,
                    hasEpisodes = isThereEpisodes(),
                    isSkipOpVisible = isOpVisible,
                    hasNextEpisode = !isOpVisible && hasNext,
                )
            }
        } else {
            playerBinding?.playerEpisodeFillerHolder?.isVisible = isFiller ?: false
            playerBinding?.playerVideoTitle?.text = playerVideoTitle
            playerBinding?.offlinePin?.isVisible = offlinePin
        }
    }

    fun setPlayerDimen(widthHeight: Pair<Int, Int>?) {
        val resolution = widthHeight?.let { "${it.first}x${it.second}" }
        val name = currentSelectedLink?.first?.name ?: currentSelectedLink?.second?.name
        val title = getHeaderName()

        val result = listOfNotNull(
            title?.takeIf { showTitle && it.isNotBlank() },
            name?.takeIf { showName && it.isNotBlank() },
            resolution?.takeIf { showResolution && it.isNotBlank() },
        ).joinToString(" - ")

        if (useComposeHud) {
            updateHudState { copy(sourceMetaText = result) }
        } else {
            playerBinding?.playerVideoTitleRez?.apply {
                text = result
                isVisible = result.isNotBlank()
            }
        }
    }


    private fun videoCodecName(mime: String?): String? {
        val m = mime?.lowercase() ?: return null
        return when {
            m.contains("avc") || m.contains("h264") -> "AVC"
            m.contains("hevc") || m.contains("h265") -> "HEVC"
            m.contains("av1") -> "AV1"
            m.contains("vp9") -> "VP9"
            m.contains("vp8") -> "VP8"
            "/" in m -> m.substringAfter("/").uppercase()
            else -> m.uppercase()
        }
    }

    private fun audioCodecName(mime: String?): String {
        val m = mime?.lowercase()?.trim().orEmpty()
        if (m.isBlank()) return ""
        return when {
            m.contains("eac3-joc") -> "Dolby Atmos"
            m.contains("truehd") -> "TrueHD"
            m.contains("eac3") -> "E-AC3"
            m.contains("ac-3") || m.contains("ac3") -> "AC3"
            m.contains("aac") || m.contains("mp4a") -> "AAC"
            m.contains("opus") -> "Opus"
            m.contains("vorbis") -> "Vorbis"
            m.contains("mp3") -> "MP3"
            m.contains("flac") -> "FLAC"
            m.contains("dts") -> "DTS"
            m.contains("pcm") -> "PCM"
            m.contains("alac") -> "ALAC"
            m.contains("amr") -> "AMR"
            m.contains("/") -> m.substringAfter("/").uppercase().takeIf { it.isNotBlank() } ?: ""
            else -> ""
        }
    }

    private fun updatePlayerInfo() {
        val tracks = player.getVideoTracks()

        val videoTrack = tracks.currentVideoTrack
        val audioTrack = tracks.currentAudioTrack

        val ctx = context ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        showMediaInfo = prefs.getBoolean(ctx.getString(R.string.show_media_info_key), false)

        val videoCodec = videoCodecName(videoTrack?.sampleMimeType)
        val audioCodec = audioCodecName(audioTrack?.sampleMimeType)
        val languageName = fromTagToLanguageName(audioTrack?.language)
        val label = audioTrack?.label

        val channelCount = audioTrack?.channelCount

        val channels = when {
            // May be below 1 or null when unknown
            channelCount == null || channelCount <= 0 -> ""
            channelCount == 1 -> "Mono"
            channelCount == 2 -> "Stereo"
            channelCount == 6 -> "5.1"
            channelCount == 8 -> "7.1"
            else -> "${channelCount}ch"
        }

        val language = languageName?.takeIf { it.isNotBlank() }?.let { lang ->
            label?.takeIf { it.isNotBlank() && !it.equals(lang, true) }
                ?.let { lang }
                ?: lang
        } ?: label?.takeIf { it.isNotBlank() }

        val stats = arrayOf(
            videoCodec,
            language,
            channels,
            audioCodec
        ).filter { !it.isNullOrBlank() }.joinToString(" • ")

        if (useComposeHud) {
            updateHudState {
                copy(
                    mediaInfoText = stats,
                    showMediaInfoByDefault = showMediaInfo,
                )
            }
        } else {
            playerBinding?.playerVideoInfo?.apply {
                text = stats
                isVisible = showMediaInfo && stats.isNotBlank()
            }
        }
    }

    override fun playerDimensionsLoaded(width: Int, height: Int) {
        super.playerDimensionsLoaded(width, height)
        setPlayerDimen(width to height)
    }

    private fun unwrapBundle(savedInstanceState: Bundle?) {
        Log.i(TAG, "unwrapBundle = $savedInstanceState")
        savedInstanceState?.let { bundle ->
            sync.addSyncs(bundle.getSafeSerializable<HashMap<String, String>>("syncData"))
        }
    }

    /**
     * This is used instead of layout-television to follow the
     * settings and some TV devices are not classified as TV
     * for some reason.
     */
    override fun pickLayout(): Int =
        if (isLayout(TV or EMULATOR)) R.layout.fragment_player_tv else R.layout.fragment_player

    var skipAnimator: ValueAnimator? = null
    var skipIndex = 0

    private fun displayTimeStamp(show: Boolean) {
        if (useComposeHud) {
            if (timestampShowState == show) return
            skipIndex++
            timestampShowState = show
            if (!show) {
                updateHudState { copy(timestampText = null) }
            }
            return
        }
        if (timestampShowState == show) return
        skipIndex++
        timestampShowState = show
        playerBinding?.skipChapterButton?.apply {
            val showWidth = 170.toPx
            val noShowWidth = 10.toPx
            //if((show && width == showWidth) || (!show && width == noShowWidth)) {
            //    return
            //}
            val to = if (show) showWidth else noShowWidth
            val from = if (!show) showWidth else noShowWidth

            skipAnimator?.cancel()
            isVisible = true

            /** Focus instantly to make the focus color appear instantly */
            if (show && !isShowing) {
                // Automatically request focus if the menu is not opened
                playerBinding?.skipChapterButton?.requestFocus()
            }

            // just in case
            val lay = layoutParams
            lay.width = from
            layoutParams = lay
            skipAnimator = ValueAnimator.ofInt(
                from, to
            ).apply {
                addListener(onEnd = {
                    if (!show) {
                        playerBinding?.skipChapterButton?.isVisible = false
                        if (!isShowing) {
                            // Automatically return focus to play pause
                            playerBinding?.playerPausePlay?.requestFocus()
                        }
                    }
                })
                addUpdateListener { valueAnimator ->
                    val value = valueAnimator.animatedValue as Int
                    val layoutParams: ViewGroup.LayoutParams = layoutParams
                    layoutParams.width = value
                    setLayoutParams(layoutParams)
                }
                duration = 500
                start()
            }
        }
    }

    override fun onTimestampSkipped(timestamp: VideoSkipStamp) {
        displayTimeStamp(false)
    }

    override fun onTimestamp(timestamp: VideoSkipStamp?) {
        if (useComposeHud) {
            if (timestamp != null) {
                val label = context?.let { timestamp.uiText.asString(it) }
                skipIndex++
                timestampShowState = true
                updateHudState { copy(timestampText = label) }
                val currentIndex = skipIndex
                playerBinding?.playerComposeHud?.postDelayed({
                    if (skipIndex == currentIndex) {
                        displayTimeStamp(false)
                    }
                }, 6000)
            } else {
                displayTimeStamp(false)
            }
            return
        }
        if (timestamp != null) {
            playerBinding?.skipChapterButton?.setText(timestamp.uiText)
            displayTimeStamp(true)
            val currentIndex = skipIndex
            playerBinding?.skipChapterButton?.handler?.postDelayed({
                if (skipIndex == currentIndex)
                    displayTimeStamp(false)
            }, 6000)
        } else {
            displayTimeStamp(false)
        }
    }

    override fun isThereEpisodes(): Boolean {
        // Checks if there is a second episode of type ResultEpisode
        // => There exists more than 1 episode, and they are all ResultEpisode
        return viewModel.state.generatorState?.allMeta?.getOrNull(1) as? ResultEpisode != null
    }

    override fun showEpisodesOverlay() {
        if (useComposeHud) {
            openComposeEpisodesDrawer()
            return
        }
        try {
            playerBinding?.apply {
                playerEpisodeList.setRecycledViewPool(EpisodeAdapter.sharedPool)
                playerEpisodeList.adapter = EpisodeAdapter(
                    false,
                    { episodeClick ->
                        if (episodeClick.action == ACTION_CLICK_DEFAULT) {
                            isNextEpisode = false
                            releasePlayer()
                            playerEpisodeOverlay.isGone = true
                            episodeClick.position?.let { viewModel.loadThisEpisode(it) }
                        }
                    },
                    { downloadClickEvent ->
                        DownloadButtonSetup.handleDownloadClick(downloadClickEvent)
                    }
                )
                playerEpisodeList.setLinearListLayout(
                    isHorizontal = false,
                    nextUp = FOCUS_SELF,
                    nextDown = FOCUS_SELF,
                    nextRight = FOCUS_SELF,
                )
                val episodes = allMeta ?: emptyList()
                (playerEpisodeList.adapter as? EpisodeAdapter)?.submitList(episodes)

                // Scroll to current episode
                viewModel.state.generatorState?.index?.let { index ->
                    playerEpisodeList.scrollToPosition(index)
                    // Ensure focus on tv
                    if (isLayout(TV)) {
                        playerEpisodeList.post {
                            val viewHolder =
                                playerEpisodeList.findViewHolderForAdapterPosition(index)
                            viewHolder?.itemView?.requestFocus()
                            viewHolder?.itemView?.let { itemView ->
                                itemView.isFocusableInTouchMode = true
                                itemView.requestFocus()
                            }
                        }
                    }
                }

                // update overlay season title
                var lastTopIndex = -1
                playerEpisodeList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val layoutManager =
                            recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val topIndex = layoutManager.findFirstCompletelyVisibleItemPosition()
                        if (topIndex != RecyclerView.NO_POSITION && topIndex != lastTopIndex) {
                            @Suppress("AssignedValueIsNeverRead")
                            lastTopIndex = topIndex
                            val topItem = episodes.getOrNull(topIndex)
                            topItem?.let {
                                playerEpisodeOverlayTitle.setText(
                                    ResultViewModel2.seasonToTxt(
                                        topItem.seasonData,
                                        topItem.seasonIndex
                                    )
                                )
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun openComposeEpisodesDrawer() {
        try {
            prepareComposeEpisodesDrawerData()
            rebuildAndPushDrawerState(scrollToPlaying = true)
        } catch (e: Exception) {
            logError(e)
        }
    }

    private fun prepareComposeEpisodesDrawerData() {
        val gen = viewModel.generator
        val genVideos = gen?.videos?.filterIsInstance<ResultEpisode>().orEmpty()
        val activeEpisodes = if (genVideos.isNotEmpty()) genVideos else allMeta.orEmpty()
        val indexById = HashMap<Int, Int>(activeEpisodes.size)
        activeEpisodes.forEachIndexed { idx, ep ->
            indexById[ep.id] = idx
        }
        drawerIndexByEpisodeId = indexById

        val playingEpisode = (currentMeta as? ResultEpisode)
            ?: activeEpisodes.getOrNull(viewModel.episodeIndex)

        val response = viewModel.state.generatorState?.response
            ?: (gen as? RepoLinkGenerator)?.page

        if (response is AnimeLoadResponse && response.episodes.size > 1) {
            val mainId = playingEpisode?.parentId ?: response.getId()
            val detectedDub = response.episodes.entries.firstOrNull { (dubStatus, eps) ->
                val dubOffset = dubStatus.id * 1_000_000
                eps.withIndex().any { (idx, rawEp) ->
                    val epNum = rawEp.episode ?: (idx + 1)
                    val computedId = mainId + epNum + dubOffset + (rawEp.season?.times(10_000) ?: 0)
                    (playingEpisode != null && (computedId == playingEpisode.id || rawEp.data == playingEpisode.data))
                }
            }?.key ?: response.episodes.entries.firstOrNull { it.value.size == activeEpisodes.size }?.key
                ?: response.episodes.keys.firstOrNull()
                ?: DubStatus.None

            drawerActiveDubStatus = detectedDub

            val byIndexer = linkedMapOf<ResultViewModel2.EpisodeIndexer, MutableList<ResultEpisode>>()
            val fullByDub = linkedMapOf<DubStatus, List<ResultEpisode>>()

            response.episodes.keys.forEach { dubStatus ->
                if (dubStatus == detectedDub && activeEpisodes.isNotEmpty()) {
                    activeEpisodes.forEach { ep ->
                        val indexer = ResultViewModel2.EpisodeIndexer(dubStatus, ep.seasonIndex ?: 0)
                        byIndexer.getOrPut(indexer) { mutableListOf() }.add(ep)
                    }
                    fullByDub[dubStatus] = activeEpisodes
                } else {
                    val existingIds = HashSet<Int>()
                    val dubSeasonMap = linkedMapOf<Int, MutableList<ResultEpisode>>()
                    val rawList = response.episodes[dubStatus].orEmpty()
                    rawList.forEachIndexed { idx, rawEp ->
                        val epNum = rawEp.episode ?: (idx + 1)
                        val id = mainId + epNum + dubStatus.id * 1_000_000 + (rawEp.season?.times(10_000) ?: 0)
                        if (existingIds.add(id)) {
                            val seasonData = response.seasonNames?.firstOrNull { it.season == rawEp.season }
                            val totalIndex = rawEp.season?.let { season ->
                                response.getTotalEpisodeIndex(epNum, season)
                            }
                            val built = buildResultEpisode(
                                headerName = response.name,
                                name = filterDrawerEpisodeName(rawEp.name),
                                poster = rawEp.posterUrl,
                                episode = epNum,
                                seasonIndex = rawEp.season,
                                season = seasonData?.displaySeason ?: rawEp.season,
                                data = rawEp.data,
                                apiName = response.apiName,
                                id = id,
                                index = idx,
                                rating = rawEp.score,
                                description = rawEp.description,
                                isFiller = null,
                                tvType = response.type,
                                parentId = mainId,
                                totalEpisodeIndex = totalIndex,
                                airDate = rawEp.date,
                                runTime = rawEp.runTime,
                                seasonData = seasonData,
                            )
                            val seasonKey = built.seasonIndex ?: 0
                            val indexer = ResultViewModel2.EpisodeIndexer(dubStatus, seasonKey)
                            byIndexer.getOrPut(indexer) { mutableListOf() }.add(built)
                            dubSeasonMap.getOrPut(seasonKey) { mutableListOf() }.add(built)
                        }
                    }
                    fullByDub[dubStatus] = dubSeasonMap.entries
                        .sortedBy { it.key }
                        .flatMap { it.value }
                }
            }

            drawerEpisodesByIndexer = byIndexer
            drawerFullListByDub = fullByDub
        } else {
            drawerActiveDubStatus = DubStatus.None
            val byIndexer = linkedMapOf<ResultViewModel2.EpisodeIndexer, MutableList<ResultEpisode>>()
            activeEpisodes.forEach { ep ->
                val indexer = ResultViewModel2.EpisodeIndexer(DubStatus.None, ep.seasonIndex ?: 0)
                byIndexer.getOrPut(indexer) { mutableListOf() }.add(ep)
            }
            drawerEpisodesByIndexer = byIndexer
            drawerFullListByDub = mapOf(DubStatus.None to activeEpisodes)
        }

        drawerSelectedDub = drawerActiveDubStatus
        val seasonsForDub = drawerEpisodesByIndexer.keys
            .filter { it.dubStatus == drawerSelectedDub }
            .map { it.season }
            .distinct()
        drawerSelectedSeason = playingEpisode?.seasonIndex
            ?.takeIf { it in seasonsForDub }
            ?: seasonsForDub.firstOrNull()
            ?: 0

        val initialIndexer = ResultViewModel2.EpisodeIndexer(drawerSelectedDub, drawerSelectedSeason)
        val initialEpisodes = drawerEpisodesByIndexer[initialIndexer].orEmpty()
        val initialRanges = computeDrawerRanges(initialEpisodes)
        drawerSelectedRange = initialRanges.firstOrNull { range ->
            val start = range.startIndex.coerceAtMost(initialEpisodes.size)
            val end = (start + range.length).coerceAtMost(initialEpisodes.size)
            playingEpisode != null && initialEpisodes.subList(start, end).any { it.id == playingEpisode.id }
        } ?: initialRanges.firstOrNull { range ->
            val epNum = playingEpisode?.episode ?: return@firstOrNull false
            epNum in range.startEpisode..range.endEpisode
        } ?: initialRanges.firstOrNull()
    }

    private fun rebuildAndPushDrawerState(scrollToPlaying: Boolean) {
        val ctx = context ?: return
        val gen = viewModel.generator
        val response = viewModel.state.generatorState?.response
            ?: (gen as? RepoLinkGenerator)?.page
        val playingEpisode = (currentMeta as? ResultEpisode)
            ?: drawerFullListByDub[drawerActiveDubStatus]?.getOrNull(viewModel.episodeIndex)

        val availableDubs = drawerEpisodesByIndexer.keys.map { it.dubStatus }.distinct()
        if (availableDubs.isNotEmpty() && drawerSelectedDub !in availableDubs) {
            drawerSelectedDub = availableDubs.first()
        }

        val seasonsForDub = drawerEpisodesByIndexer.keys
            .filter { it.dubStatus == drawerSelectedDub }
            .map { it.season }
            .distinct()
        if (seasonsForDub.isNotEmpty() && drawerSelectedSeason !in seasonsForDub) {
            drawerSelectedSeason = seasonsForDub.minByOrNull {
                kotlin.math.abs(it - drawerSelectedSeason)
            } ?: seasonsForDub.first()
        }

        val indexer = ResultViewModel2.EpisodeIndexer(drawerSelectedDub, drawerSelectedSeason)
        val indexerEpisodes = drawerEpisodesByIndexer[indexer].orEmpty()
        val ranges = computeDrawerRanges(indexerEpisodes)
        val selectedRange = drawerSelectedRange?.let { current ->
            if (ranges.contains(current)) {
                current
            } else {
                ranges.minByOrNull { kotlin.math.abs(it.startEpisode - current.startEpisode) }
            }
        } ?: ranges.firstOrNull()
        drawerSelectedRange = selectedRange

        val slicedEpisodes = if (selectedRange != null && indexerEpisodes.isNotEmpty()) {
            val start = selectedRange.startIndex.coerceIn(0, indexerEpisodes.size)
            val end = (start + selectedRange.length).coerceIn(start, indexerEpisodes.size)
            indexerEpisodes.subList(start, end)
        } else {
            indexerEpisodes
        }.map { ep ->
            val posDur = getViewPos(ep.id)
            val watchState = DataStoreHelper.getVideoWatchState(ep.id) ?: VideoWatchState.None
            ep.copy(
                position = posDur?.position ?: 0L,
                duration = posDur?.duration ?: 0L,
                videoWatchState = watchState,
            )
        }

        val sortOptions = buildDrawerSortOptions(slicedEpisodes)
        val preferredSort = DataStoreHelper.resultsSortingMode
        val effectiveSort = sortOptions.firstOrNull { it.second == preferredSort }?.second
            ?: EpisodeSortType.NUMBER_ASC
        val sortedEpisodes = if (slicedEpisodes.size <= 1) {
            slicedEpisodes
        } else {
            sortDrawerEpisodes(slicedEpisodes, effectiveSort)
        }
        val selectedSortLabel = if (slicedEpisodes.size <= 1) {
            null
        } else {
            sortLabelForType(effectiveSort).asStringNull(ctx)
        }

        val seasonNames = (response as? EpisodeResponse)?.seasonNames
        val seasonOptions = seasonsForDub.map { seasonNum ->
            val seasonData = seasonNames?.firstOrNull { it.season == seasonNum }
                ?: drawerEpisodesByIndexer[ResultViewModel2.EpisodeIndexer(drawerSelectedDub, seasonNum)]
                    ?.firstOrNull()?.seasonData
            val label = ResultViewModel2.seasonToTxt(seasonData, seasonNum)?.asStringNull(ctx)
                ?: ctx.getString(R.string.season_format, ctx.getString(R.string.season), seasonNum, "")
            SeasonOption(label = label, seasonNumber = seasonNum)
        }
        val selectedSeasonLabel = if (seasonOptions.size > 1) {
            seasonOptions.firstOrNull { it.seasonNumber == drawerSelectedSeason }?.label
        } else {
            null
        }

        val dubOptions = availableDubs.map { status ->
            DubOption(
                label = dubTxt(status)?.asStringNull(ctx) ?: status.name,
                status = status,
            )
        }
        val selectedDubLabel = if (dubOptions.size > 1) {
            dubOptions.firstOrNull { it.status == drawerSelectedDub }?.label
        } else {
            null
        }

        val rangeOptions = ranges.map { range ->
            RangeOption(
                label = txt(R.string.episodes_range, range.startEpisode, range.endEpisode).asString(ctx),
                range = range,
            )
        }
        val selectedRangeLabel = if (rangeOptions.size > 1 && selectedRange != null) {
            txt(R.string.episodes_range, selectedRange.startEpisode, selectedRange.endEpisode)
                .asStringNull(ctx)
        } else {
            null
        }

        val totalSeasonCount = indexerEpisodes.size
        val episodesCountText = if (totalSeasonCount > 0) {
            txt(
                R.string.episode_format,
                totalSeasonCount,
                txt(if (totalSeasonCount == 1) R.string.episode else R.string.episodes),
            ).asStringNull(ctx)
        } else {
            null
        }

        val fallbackBackdropUrl = response?.backgroundPosterUrl?.takeIf { it.isNotBlank() }
            ?: response?.posterUrl?.takeIf { it.isNotBlank() }
            ?: playingEpisode?.poster

        if (scrollToPlaying) {
            drawerScrollToken++
        }

        updateHudState {
            copy(
                episodesDrawer = PlayerEpisodesDrawerState(
                    seasonOptions = seasonOptions,
                    selectedSeasonLabel = selectedSeasonLabel,
                    dubOptions = dubOptions,
                    selectedDubLabel = selectedDubLabel,
                    rangeOptions = rangeOptions,
                    selectedRangeLabel = selectedRangeLabel,
                    selectedSortLabel = selectedSortLabel,
                    hasSortOptions = slicedEpisodes.size > 1,
                    episodesCountText = episodesCountText,
                    episodes = sortedEpisodes,
                    fallbackBackdropUrl = fallbackBackdropUrl,
                    playingEpisodeId = playingEpisode?.id,
                    scrollTriggerToken = drawerScrollToken,
                )
            )
        }
    }

    override fun onEpisodeDrawerSeasonSelected(season: Int) {
        if (drawerSelectedSeason == season) return
        drawerSelectedSeason = season
        val playingEpisode = currentMeta as? ResultEpisode
        val indexer = ResultViewModel2.EpisodeIndexer(drawerSelectedDub, season)
        val episodes = drawerEpisodesByIndexer[indexer].orEmpty()
        val ranges = computeDrawerRanges(episodes)
        drawerSelectedRange = if (playingEpisode != null && drawerSelectedDub == drawerActiveDubStatus && (playingEpisode.seasonIndex ?: 0) == season) {
            ranges.firstOrNull { range ->
                val start = range.startIndex.coerceAtMost(episodes.size)
                val end = (start + range.length).coerceAtMost(episodes.size)
                episodes.subList(start, end).any { it.id == playingEpisode.id }
            } ?: ranges.firstOrNull()
        } else {
            ranges.firstOrNull()
        }
        rebuildAndPushDrawerState(scrollToPlaying = false)
    }

    override fun onEpisodeDrawerDubSelected(status: DubStatus) {
        if (drawerSelectedDub == status) return
        drawerSelectedDub = status
        rebuildAndPushDrawerState(scrollToPlaying = false)
    }

    override fun onEpisodeDrawerRangeSelected(range: EpisodeRange) {
        if (drawerSelectedRange == range) return
        drawerSelectedRange = range
        rebuildAndPushDrawerState(scrollToPlaying = false)
    }

    override fun onEpisodeDrawerSortClick() {
        val ctx = context ?: return
        val currentEpisodes = hudStateFlow.value.episodesDrawer.episodes
        if (currentEpisodes.size <= 1) return

        val sortOptions = buildDrawerSortOptions(currentEpisodes)
        val names = sortOptions.mapNotNull { (text, type) ->
            type to (text.asStringNull(ctx) ?: return@mapNotNull null)
        }
        if (names.isEmpty()) return

        val currentSort = DataStoreHelper.resultsSortingMode
        val currentIndex = names.indexOfFirst { it.first == currentSort }.let {
            if (it >= 0) it else 0
        }

        if (useComposeHud) {
            showComposeSingleChoicePopup(
                title = ctx.getString(R.string.sort_by),
                items = names.map { it.second },
                selectedIndex = currentIndex,
                cancelable = true,
                onDismiss = { activity?.hideSystemUI() },
                onSelect = { itemId ->
                    DataStoreHelper.resultsSortingMode = names[itemId].first
                    rebuildAndPushDrawerState(scrollToPlaying = false)
                    activity?.hideSystemUI()
                },
            )
            return
        }

        activity?.showDialog(
            names.map { it.second },
            currentIndex,
            ctx.getString(R.string.sort_by),
            false,
            { activity?.hideSystemUI() },
        ) { itemId ->
            DataStoreHelper.resultsSortingMode = names[itemId].first
            rebuildAndPushDrawerState(scrollToPlaying = false)
            activity?.hideSystemUI()
        }
    }

    override fun onEpisodeDrawerEpisodeClick(episode: ResultEpisode) {
        if (drawerSelectedDub == drawerActiveDubStatus) {
            val targetIndex = drawerIndexByEpisodeId[episode.id]
                ?: viewModel.generator?.videos?.indexOfFirst { (it as? ResultEpisode)?.id == episode.id }
                    ?.takeIf { it >= 0 }
                ?: return
            isNextEpisode = false
            dismissEpisodesDrawerWithoutResume()
            releasePlayer()
            viewModel.loadThisEpisode(targetIndex)
        } else {
            val dubVideos = drawerFullListByDub[drawerSelectedDub] ?: return
            val targetIndex = dubVideos.indexOfFirst { it.id == episode.id }.takeIf { it >= 0 } ?: return
            val response = viewModel.state.generatorState?.response
                ?: (viewModel.generator as? RepoLinkGenerator)?.page
            val newGenerator = RepoLinkGenerator(dubVideos, page = response)
            isNextEpisode = false
            dismissEpisodesDrawerWithoutResume()
            releasePlayer()
            viewModel.attachGenerator(newGenerator, targetIndex)
            viewModel.loadLinks()
        }
    }

    override fun onEpisodeDrawerToggleWatched(episode: ResultEpisode) {
        val currentState = DataStoreHelper.getVideoWatchState(episode.id) ?: episode.videoWatchState
        val nextState = if (currentState == VideoWatchState.Watched) {
            VideoWatchState.None
        } else {
            VideoWatchState.Watched
        }
        DataStoreHelper.setVideoWatchState(episode.id, nextState)
        rebuildAndPushDrawerState(scrollToPlaying = false)
    }

    private fun computeDrawerRanges(episodes: List<ResultEpisode>): List<EpisodeRange> {
        if (episodes.isEmpty()) return emptyList()
        val rangeSize = 20
        val rangeOverload = rangeSize + 10
        if (episodes.size <= rangeOverload) {
            return listOf(
                EpisodeRange(
                    startIndex = 0,
                    length = episodes.size,
                    startEpisode = episodes.minOf { it.episode },
                    endEpisode = episodes.maxOf { it.episode },
                )
            )
        }

        val list = mutableListOf<EpisodeRange>()
        var currentIndex = 0
        val maxIndex = episodes.size
        var targetEpisode = 0
        var currentMin = episodes.first().episode
        var currentMax = episodes.first().episode

        while (currentIndex < maxIndex) {
            val startIndex = currentIndex
            targetEpisode += rangeSize
            while (currentIndex < maxIndex && episodes[currentIndex].episode <= targetEpisode) {
                val epNumber = episodes[currentIndex].episode
                if (epNumber < currentMin) currentMin = epNumber
                if (epNumber > currentMax) currentMax = epNumber
                ++currentIndex
            }
            val length = currentIndex - startIndex
            if (length <= 0) continue
            list.add(
                EpisodeRange(
                    startIndex = startIndex,
                    length = length,
                    startEpisode = currentMin,
                    endEpisode = currentMax,
                )
            )
            currentMin = Int.MAX_VALUE
            currentMax = Int.MIN_VALUE
        }
        return list
    }

    private fun buildDrawerSortOptions(episodes: List<ResultEpisode>): List<Pair<UiText, EpisodeSortType>> {
        return buildList {
            add(txt(R.string.sort_episodes_number_asc) to EpisodeSortType.NUMBER_ASC)
            add(txt(R.string.sort_episodes_number_desc) to EpisodeSortType.NUMBER_DESC)
            if (episodes.any { it.score != null }) {
                add(txt(R.string.sort_episodes_rating_high_low) to EpisodeSortType.RATING_HIGH_LOW)
                add(txt(R.string.sort_episodes_rating_low_high) to EpisodeSortType.RATING_LOW_HIGH)
            }
            if (episodes.any { it.airDate != null }) {
                add(txt(R.string.sort_episodes_date_newest) to EpisodeSortType.DATE_NEWEST)
                add(txt(R.string.sort_episodes_date_oldest) to EpisodeSortType.DATE_OLDEST)
            }
        }
    }

    private fun sortLabelForType(sortType: EpisodeSortType): UiText {
        return when (sortType) {
            EpisodeSortType.NUMBER_ASC -> txt(R.string.sort_button_episode, "↑")
            EpisodeSortType.NUMBER_DESC -> txt(R.string.sort_button_episode, "↓")
            EpisodeSortType.RATING_HIGH_LOW -> txt(R.string.sort_button_rating, "↓")
            EpisodeSortType.RATING_LOW_HIGH -> txt(R.string.sort_button_rating, "↑")
            EpisodeSortType.DATE_NEWEST -> txt(R.string.sort_button_date, "↓")
            EpisodeSortType.DATE_OLDEST -> txt(R.string.sort_button_date, "↑")
        }
    }

    private fun sortDrawerEpisodes(
        episodes: List<ResultEpisode>,
        sorting: EpisodeSortType,
    ): List<ResultEpisode> {
        return when (sorting) {
            EpisodeSortType.NUMBER_ASC -> episodes.sortedWith(
                compareBy<ResultEpisode> { it.episode }.thenBy { it.index }
            )
            EpisodeSortType.NUMBER_DESC -> episodes.sortedWith(
                compareByDescending<ResultEpisode> { it.episode }.thenByDescending { it.index }
            )
            EpisodeSortType.RATING_HIGH_LOW -> episodes.sortedByDescending {
                it.score?.toDouble() ?: 0.0
            }
            EpisodeSortType.RATING_LOW_HIGH -> episodes.sortedBy {
                it.score?.toDouble() ?: 0.0
            }
            EpisodeSortType.DATE_NEWEST -> episodes.sortedByDescending { it.airDate }
            EpisodeSortType.DATE_OLDEST -> episodes.sortedBy { it.airDate }
        }
    }

    private fun filterDrawerEpisodeName(name: String?): String? {
        if (name == null) return null
        Regex("^[eE]pisode [0-9]*(.*)").find(name)?.groupValues?.get(1)?.let {
            if (it.isEmpty()) return null
        }
        return name
    }

    @MainThread
    fun releasePlayer() {
        player.release()
        currentSelectedSubtitles = null
        currentSelectedLink = null
        isPlayerActive.set(false)
        loadingOverlayStateFlow.update { PlayerLoadingOverlayState() }
        if (!useComposeHud) {
            binding?.overlayLoadingSkipButton?.isVisible = false
        }
        binding?.playerLoadingOverlay?.isVisible = true
        viewModel.modifyState { setError(emptyList()) }
        uiReset()
    }

    private fun setupComposeLoadingOverlay(binding: FragmentPlayerBinding) {
        val composeView = binding.playerLoadingCompose
        binding.overlayLoadingSkipButton.isGone = true
        binding.mainLoad.isGone = true
        binding.videoGoBackHolderHolder.isGone = true
        composeView.isVisible = true

        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        composeView.setContent {
            LiquidGlassTheme {
                val state by loadingOverlayStateFlow.collectAsState()
                PlayerLoadingOverlay(
                    state = state,
                    onBackClick = { exitPlayer() },
                    onSkipClick = {
                        // Mark as "success" early
                        viewModel.modifyState {
                            copy(loading = Resource.Success(Unit))
                        }
                    },
                )
            }
        }
    }

    fun exitPlayer() {
        playerHostView?.exitFullscreen()
        player.release()
        activity?.popCurrentPage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("index", viewModel.episodeIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onBindingCreated(binding: FragmentPlayerBinding, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[PlayerGeneratorViewModel::class.java]
        sync = ViewModelProvider(this)[SyncViewModel::class.java]

        val uuid = savedInstanceState?.getString("uuid") ?: arguments?.getString("uuid")
        val index = savedInstanceState?.getInt("index") ?: arguments?.getInt("index")
        val generator = generators[uuid]

        unwrapBundle(savedInstanceState)
        unwrapBundle(arguments)

        super.onBindingCreated(binding, savedInstanceState)

        // Avoid showing no links found
        if (generator == null || index == null) {
            exitPlayer()
            return
        }
        viewModel.attachGenerator(generator, index)

        context?.let { ctx ->
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)
            showName = settingsManager.getBoolean(ctx.getString(R.string.show_name_key), true)
            showResolution =
                settingsManager.getBoolean(ctx.getString(R.string.show_resolution_key), true)
            showMediaInfo =
                settingsManager.getBoolean(ctx.getString(R.string.show_media_info_key), false)
            limitTitle = settingsManager.getInt(ctx.getString(R.string.prefer_title_limit_key), 0)
            updateForcedEncoding(ctx)
            viewModel.filterSubByLang =
                settingsManager.getBoolean(getString(R.string.filter_sub_lang_key), false)
            if (viewModel.filterSubByLang) {
                val langFromPrefMedia = settingsManager.getStringSet(
                    this.getString(R.string.provider_lang_key), mutableSetOf("en")
                )
                viewModel.langFilterList = langFromPrefMedia?.mapNotNull {
                    fromTagToEnglishLanguageName(it)?.lowercase() ?: return@mapNotNull null
                } ?: listOf()
            }

            // Set up TV clock visibility
            if (isLayout(TV)) {
                val showTvClock = settingsManager.getBoolean(ctx.getString(R.string.tv_layout_clock_key), false)
                playerBinding?.playerVideoClock?.isVisible = showTvClock
            } else {
                playerBinding?.playerVideoClock?.isVisible = false
            }
        }

        unwrapBundle(savedInstanceState)
        unwrapBundle(arguments)

        sync.updateUserData()

        preferredAutoSelectSubtitles = getAutoSelectLanguageTagIETF()

        val selectedLink = currentSelectedLink
        if (selectedLink == null) {
            viewModel.loadLinks()
        } else {
            // Recreated view, so we need to recreate the
            loadLink(selectedLink, true)
        }

        val useComposeLoadingOverlay = useComposeHud
        if (useComposeLoadingOverlay) {
            setupComposeLoadingOverlay(binding)
        } else {
            binding.overlayLoadingSkipButton.setOnClickListener {
                // Mark as "success" early
                viewModel.modifyState {
                    copy(loading = Resource.Success(Unit))
                }
            }

            binding.playerLoadingGoBack.setOnClickListener {
                exitPlayer()
            }
        }

        playerBinding?.downloadHeader?.setOnClickListener {
            it?.isVisible = false
        }

        playerBinding?.downloadHeaderToggle?.setOnClickListener {
            playerBinding?.downloadHeader?.let {
                it.isVisible = !it.isVisible
            }
        }

        observe(viewModel.currentStamps) { (stamps, instance) ->
            if (instance != viewModel.state.instance) return@observe // Outdated observe
            player.addTimeStamps(stamps)
        }

        observe(viewModel.currentSubtitles) { (subtitles, instance) ->
            if (instance != viewModel.state.instance) return@observe // Outdated observe
            player.setActiveSubtitles(subtitles)

            // If the file is downloaded then do not select auto select the subtitles
            // Downloaded subtitles cannot be selected immediately after loading since
            // player.getCurrentPreferredSubtitle() cannot fetch data from non-loaded subtitles
            // Resulting in unselecting the downloaded subtitle
            if (subtitles.lastOrNull()?.origin != SubtitleOrigin.DOWNLOADED_FILE) {
                autoSelectSubtitles()
            }
        }
        observe(viewModel.loadingLinks) { (loading, instance) ->
            if (instance != viewModel.state.instance) return@observe // Outdated observe

            when (loading) {
                is Resource.Loading -> {
                    releasePlayer()
                }

                is Resource.Success -> {
                    // provider returned false
                    //if (it.value != true) {
                    //    showToast(activity, R.string.unexpected_error, Toast.LENGTH_SHORT)
                    //}
                    startPlayer()
                }

                is Resource.Failure -> {
                    showToast(loading.errorString, Toast.LENGTH_LONG)
                    startPlayer()
                }
            }
        }

        observe(viewModel.currentLinks) { (_, instance) ->
            if (instance != viewModel.state.instance) return@observe // Outdated observe

            val sortedLinks = viewModel.state.sortLinks(currentQualityProfile)
            val usableLinks = sortedLinks.count { link -> link.shouldUseLink }

            val turnVisible = usableLinks > 0 && viewModel.generator?.canSkipLoading == true
            if (useComposeLoadingOverlay) {
                loadingOverlayStateFlow.update {
                    PlayerLoadingOverlayState(
                        canSkip = turnVisible,
                        usableLinks = usableLinks,
                    )
                }
            } else {
                val wasGone = binding.overlayLoadingSkipButton.isGone

                binding.overlayLoadingSkipButton.apply {
                    isVisible = turnVisible

                    if (usableLinks == 0) {
                        setText(R.string.skip_loading)
                    } else {
                        @SuppressLint("SetTextI18n")
                        text = "${context.getString(R.string.skip_loading)} (${usableLinks})"
                    }
                }

                if (turnVisible && wasGone) {
                    binding.overlayLoadingSkipButton.requestFocus()
                }
            }

            safe {
                if (!isPlayerActive.get() && viewModel.state.links.any { link ->
                        getLinkPriority(currentQualityProfile, link.first) >=
                                QualityDataHelper.AUTO_SKIP_PRIORITY
                    }
                ) {
                    startPlayer()
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.getSafeSerializable(key: String): T? =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) getSerializable(key) as? T else getSerializable(
        key,
        T::class.java
    )
