package com.lagradost.cloudstream3.ui.player.hud

import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.ui.details.DubOption
import com.lagradost.cloudstream3.ui.details.RangeOption
import com.lagradost.cloudstream3.ui.details.SeasonOption
import com.lagradost.cloudstream3.ui.player.CSPlayerLoading
import com.lagradost.cloudstream3.ui.player.PlayerResize
import com.lagradost.cloudstream3.ui.player.SubtitleCue
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper
import com.lagradost.cloudstream3.ui.result.EpisodeRange
import com.lagradost.cloudstream3.ui.result.ResultEpisode

@Immutable
data class PlayerEpisodesDrawerState(
    val seasonOptions: List<SeasonOption> = emptyList(),
    val selectedSeasonLabel: String? = null,
    val dubOptions: List<DubOption> = emptyList(),
    val selectedDubLabel: String? = null,
    val rangeOptions: List<RangeOption> = emptyList(),
    val selectedRangeLabel: String? = null,
    val selectedSortLabel: String? = null,
    val hasSortOptions: Boolean = false,
    val episodesCountText: String? = null,
    val episodes: List<ResultEpisode> = emptyList(),
    val fallbackBackdropUrl: String? = null,
    val playingEpisodeId: Int? = null,
    val scrollTriggerToken: Long = 0L,
)

@Immutable
data class TorrentProgressState(
    val downloadedText: String = "",
    val speedText: String = "",
    val showSpeed: Boolean = true,
    val progressFraction: Float? = null,
)

@Immutable
data class DoubleTapSeekFeedback(
    val isForward: Boolean,
    val seconds: Int,
    val token: Long,
)

@Immutable
data class PlaybackProgressSnapshot(
    val positionMs: Long,
    val durationMs: Long,
    val bufferedPositionMs: Long,
    val isAtLiveEdge: Boolean,
)

@Immutable
data class PlayerHudState(
    val isShowing: Boolean = false,
    val isLocked: Boolean = false,
    val status: CSPlayerLoading = CSPlayerLoading.IsBuffering,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isAtLiveEdge: Boolean = false,
    val durationModeRemaining: Boolean = false,
    val title: String = "",
    val showOfflinePin: Boolean = false,
    val isFiller: Boolean = false,
    val sourceMetaText: String = "",
    val mediaInfoText: String = "",
    val showMediaInfoByDefault: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: PlayerResize = PlayerResize.Fit,
    val hideControlNames: Boolean = false,
    val isRotateEnabled: Boolean = false,
    val isResizeEnabled: Boolean = true,
    val isSpeedEnabled: Boolean = false,
    val isCompressorEnabled: Boolean = false,
    val isCompressorActive: Boolean = false,
    val canOffsetSubtitles: Boolean = false,
    val hasMultipleTracks: Boolean = false,
    val isSkipOpVisible: Boolean = false,
    val hasNextEpisode: Boolean = false,
    val hasEpisodes: Boolean = false,
    val isEpisodesDrawerVisible: Boolean = false,
    val episodesDrawer: PlayerEpisodesDrawerState = PlayerEpisodesDrawerState(),
    val isSourceSubsDialogVisible: Boolean = false,
    val sourceSubsDialog: PlayerSourceSubsDialogState = PlayerSourceSubsDialogState(),
    val isTracksDialogVisible: Boolean = false,
    val tracksDialog: PlayerTracksDialogState = PlayerTracksDialogState(),
    val isSpeedDialogVisible: Boolean = false,
    val speedDialog: PlayerSpeedDialogState = PlayerSpeedDialogState(),
    val isCompressorDialogVisible: Boolean = false,
    val compressorDialog: PlayerCompressorDialogState = PlayerCompressorDialogState(),
    val isSubtitleOffsetDialogVisible: Boolean = false,
    val subtitleOffsetDialog: PlayerSubtitleOffsetDialogState = PlayerSubtitleOffsetDialogState(),
    val selectionPopup: PlayerSelectionPopupState? = null,
    val isQualityProfileDialogVisible: Boolean = false,
    val qualityProfileDialog: PlayerQualityProfileDialogState = PlayerQualityProfileDialogState(),
    val isOnlineSubtitlesDialogVisible: Boolean = false,
    val onlineSubtitlesDialog: PlayerOnlineSubtitlesDialogState = PlayerOnlineSubtitlesDialogState(),
    val isSubtitleSettingsVisible: Boolean = false,
    val timestampText: String? = null,
    val isTorrent: Boolean = false,
    val torrentProgress: TorrentProgressState = TorrentProgressState(),
    val isSpeedUpActive: Boolean = false,
    val seekPreviewText: String? = null,
    val doubleTapFeedback: DoubleTapSeekFeedback? = null,
    val isVolumeVisible: Boolean = false,
    val volumeLevel: Float = 0f,
    val volumeBoostError: Boolean = false,
    val isBrightnessVisible: Boolean = false,
    val brightnessLevel: Float = 0.5f,
) {
    val isPlaying: Boolean
        get() = status == CSPlayerLoading.IsPlaying

    val hasAnySecondaryPopup: Boolean
        get() = selectionPopup != null ||
            isQualityProfileDialogVisible ||
            isOnlineSubtitlesDialogVisible ||
            isSubtitleSettingsVisible

    val hasAnyOverlayOpen: Boolean
        get() = isEpisodesDrawerVisible ||
            isSourceSubsDialogVisible ||
            isTracksDialogVisible ||
            isSpeedDialogVisible ||
            isCompressorDialogVisible ||
            isSubtitleOffsetDialogVisible ||
            hasAnySecondaryPopup
}

@Immutable
data class SourceItemUi(
    val index: Int,
    val label: String,
)

@Immutable
data class SubtitleGroupUi(
    val index: Int,
    val label: String,
)

@Immutable
data class SubtitleOptionUi(
    val index: Int,
    val label: String,
)

@Immutable
data class PlayerSourceSubsDialogState(
    val hasSources: Boolean = true,
    val profileLabel: String = "",
    val sources: List<SourceItemUi> = emptyList(),
    val selectedSourceIndex: Int = -1,
    val hiddenSourcesText: String? = null,
    val subtitleVariants: List<SubtitleOptionUi> = emptyList(),
    val selectedSubtitleVariantIndex: Int = 0,
    val encodingLabel: String = "",
    val subtitleGroups: List<SubtitleGroupUi> = emptyList(),
    val selectedSubtitleGroupIndex: Int = 0,
    val loadFromFileLabel: String = "",
    val loadOnlineLabel: String? = null,
    val loadFirstOnlineLabel: String? = null,
    val scrollTriggerToken: Long = 0L,
)

@Immutable
data class TrackItemUi(
    val index: Int,
    val label: String,
)

@Immutable
data class PlayerTracksDialogState(
    val hasVideoTracks: Boolean = false,
    val videoTracks: List<TrackItemUi> = emptyList(),
    val selectedVideoTrackIndex: Int = -1,
    val hasAudioTracks: Boolean = false,
    val audioTracks: List<TrackItemUi> = emptyList(),
    val selectedAudioTrackIndex: Int = 0,
    val scrollTriggerToken: Long = 0L,
)

@Immutable
data class PlayerSpeedDialogState(
    val speed: Float = 1.0f,
    val speedText: String = "1.00x",
)

@Immutable
data class PlayerCompressorDialogState(
    val enabled: Boolean = false,
    val statusText: String = "",
    val threshold: Float = -24f,
    val thresholdLabel: String = "",
    val thresholdHint: String = "",
    val makeupGain: Float = 12f,
    val makeupLabel: String = "",
    val makeupHint: String = "",
)

@Immutable
data class PlayerSubtitleOffsetDialogState(
    val inputText: String = "0",
    val offsetMs: Long = 0L,
    val timelinePositionMs: Long = 0L,
    val statusText: String = "",
    val cues: List<SubtitleCue> = emptyList(),
    val activeCueIndex: Int = 0,
    val scrollTriggerToken: Long = 0L,
)

@Immutable
sealed interface PlayerSelectionPopupKind {
    @Immutable
    data class SingleChoice(
        val items: List<String>,
        val selectedIndex: Int,
    ) : PlayerSelectionPopupKind

    @Immutable
    data class MultiChoice(
        val items: List<String>,
        val selectedIndices: Set<Int>,
    ) : PlayerSelectionPopupKind

    @Immutable
    data class MessageConfirm(
        val message: String,
        val confirmLabel: String,
        val dismissLabel: String? = null,
    ) : PlayerSelectionPopupKind
}

@Immutable
data class PlayerSelectionPopupState(
    val token: Long = 0L,
    val title: String? = null,
    val kind: PlayerSelectionPopupKind,
    val cancelable: Boolean = true,
)

@Immutable
data class QualityProfileItemUi(
    val id: Int,
    val name: String,
    val isUsed: Boolean,
    val types: Set<QualityDataHelper.QualityProfileType>,
)

@Immutable
data class PriorityEntryUi(
    val key: String,
    val label: String,
    val priority: Int,
)

@Immutable
data class PlayerQualityProfileDialogState(
    val usedProfileId: Int = 1,
    val usedProfileName: String = "",
    val selectedProfileId: Int? = null,
    val profiles: List<QualityProfileItemUi> = emptyList(),
    val useProfileSelection: Boolean = true,
    val editingProfileId: Int? = null,
    val editingProfileNameInput: String = "",
    val editingProfilePlaceholder: String = "",
    val sourcePriorities: List<PriorityEntryUi> = emptyList(),
    val qualityPriorities: List<PriorityEntryUi> = emptyList(),
    val isEditingProfileSettings: Boolean = false,
    val hideErrorSources: Boolean = false,
    val hideNegativeSources: Boolean = false,
)

@Immutable
data class OnlineSubtitleItemUi(
    val index: Int,
    val title: String,
    val subtitle: String,
    val isHearingImpaired: Boolean,
)

@Immutable
data class PlayerOnlineSubtitlesDialogState(
    val query: String = "",
    val yearLabel: String = "",
    val languageLabel: String = "",
    val isSearching: Boolean = false,
    val isApplying: Boolean = false,
    val results: List<OnlineSubtitleItemUi> = emptyList(),
    val selectedIndex: Int = -1,
)

class PlayerHudCallbacks(
    val onBackClick: () -> Unit,
    val onPlayPauseClick: () -> Unit,
    val onLockToggle: () -> Unit,
    val onRotateClick: () -> Unit,
    val onResizeClick: () -> Unit,
    val onSpeedClick: () -> Unit,
    val onCompressorClick: () -> Unit,
    val onSubtitleOffsetClick: () -> Unit,
    val onTracksClick: () -> Unit,
    val onSourcesClick: () -> Unit,
    val onSkipOpClick: () -> Unit,
    val onNextEpisodeClick: () -> Unit,
    val onSkipChapterClick: () -> Unit,
    val onEpisodesClick: () -> Unit,
    val onDismissEpisodesDrawer: () -> Unit = {},
    val onEpisodeDrawerSeasonSelected: (Int) -> Unit = {},
    val onEpisodeDrawerDubSelected: (DubStatus) -> Unit = {},
    val onEpisodeDrawerRangeSelected: (EpisodeRange) -> Unit = {},
    val onEpisodeDrawerSortClick: () -> Unit = {},
    val onEpisodeDrawerEpisodeClick: (ResultEpisode) -> Unit = {},
    val onEpisodeDrawerToggleWatched: (ResultEpisode) -> Unit = {},
    val onSourceSubsDismiss: () -> Unit = {},
    val onSourceSubsApply: () -> Unit = {},
    val onSourceSelected: (Int) -> Unit = {},
    val onSourceLongClick: (Int) -> Unit = {},
    val onSourceProfileSettingsClick: () -> Unit = {},
    val onSubtitleGroupSelected: (Int) -> Unit = {},
    val onSubtitleAudioOptionSelected: (Int) -> Unit = {},
    val onSubtitleEncodingClick: () -> Unit = {},
    val onSubtitleSettingsClick: () -> Unit = {},
    val onLoadSubtitlesFromFile: () -> Unit = {},
    val onLoadSubtitlesOnline: () -> Unit = {},
    val onLoadFirstSubtitleOnline: () -> Unit = {},
    val onTracksDismiss: () -> Unit = {},
    val onTracksApply: () -> Unit = {},
    val onVideoTrackSelected: (Int) -> Unit = {},
    val onAudioTrackSelected: (Int) -> Unit = {},
    val onSpeedDismiss: () -> Unit = {},
    val onSpeedChanged: (Float) -> Unit = {},
    val onSpeedStep: (Float) -> Unit = {},
    val onSpeedPresetSelected: (Float) -> Unit = {},
    val onCompressorDismiss: () -> Unit = {},
    val onCompressorApply: () -> Unit = {},
    val onCompressorReset: () -> Unit = {},
    val onCompressorEnabledChanged: (Boolean) -> Unit = {},
    val onCompressorThresholdChanged: (Float) -> Unit = {},
    val onCompressorMakeupGainChanged: (Float) -> Unit = {},
    val onCompressorPresetSelected: (Float, Float) -> Unit = { _, _ -> },
    val onSubtitleOffsetDismiss: () -> Unit = {},
    val onSubtitleOffsetApply: () -> Unit = {},
    val onSubtitleOffsetReset: () -> Unit = {},
    val onSubtitleOffsetInputChanged: (String) -> Unit = {},
    val onSubtitleOffsetStep: (Long) -> Unit = {},
    val onSubtitleOffsetCueClick: (SubtitleCue) -> Unit = {},
    val onSelectionPopupDismiss: () -> Unit = {},
    val onSelectionPopupSingleSelect: (Int) -> Unit = {},
    val onSelectionPopupMultiApply: (Set<Int>) -> Unit = {},
    val onSelectionPopupConfirm: () -> Unit = {},
    val onQualityProfileDismiss: () -> Unit = {},
    val onQualityProfileUse: () -> Unit = {},
    val onQualityProfileSelected: (Int) -> Unit = {},
    val onQualityProfileEditClick: () -> Unit = {},
    val onQualityProfileSetDefaultClick: () -> Unit = {},
    val onQualityProfileEditNameChanged: (String) -> Unit = {},
    val onQualityProfileSourcePriorityStep: (String, Int) -> Unit = { _, _ -> },
    val onQualityProfileQualityPriorityStep: (String, Int) -> Unit = { _, _ -> },
    val onQualityProfileEditSave: () -> Unit = {},
    val onQualityProfileEditClose: () -> Unit = {},
    val onQualityProfileHelpClick: () -> Unit = {},
    val onQualityProfileSettingsOpen: () -> Unit = {},
    val onQualityProfileHideNegativeChanged: (Boolean) -> Unit = {},
    val onQualityProfileHideErrorChanged: (Boolean) -> Unit = {},
    val onQualityProfileSettingsApply: () -> Unit = {},
    val onQualityProfileSettingsCancel: () -> Unit = {},
    val onOnlineSubtitlesDismiss: () -> Unit = {},
    val onOnlineSubtitlesQueryChanged: (String) -> Unit = {},
    val onOnlineSubtitlesSearchSubmit: () -> Unit = {},
    val onOnlineSubtitlesYearClick: () -> Unit = {},
    val onOnlineSubtitlesLanguageClick: () -> Unit = {},
    val onOnlineSubtitlesResultSelected: (Int) -> Unit = {},
    val onOnlineSubtitlesApply: () -> Unit = {},
    val onSubtitleSettingsDismiss: () -> Unit = {},
    val onToggleDurationMode: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onScrubStateChanged: (Boolean) -> Unit,
    val hasPreview: () -> Boolean,
    val getPreviewBitmap: (Float) -> Bitmap?,
    val pollProgress: () -> PlaybackProgressSnapshot,
    val onInteraction: () -> Unit,
    val onBottomDockHeightChanged: (Int) -> Unit,
)

internal fun formatPlayerTime(ms: Long): String {
    val safeSeconds = (ms.coerceAtLeast(0L) + 500L) / 1000L
    return DateUtils.formatElapsedTime(safeSeconds)
}

internal fun formatSpeedLabel(speed: Float): String {
    val roundedTwoDecimals = ((speed * 100f).toInt())
    return when {
        roundedTwoDecimals % 100 == 0 -> "${roundedTwoDecimals / 100}x"
        roundedTwoDecimals % 10 == 0 -> String.format(java.util.Locale.US, "%.1fx", speed)
        else -> String.format(java.util.Locale.US, "%.2fx", speed)
    }
}
