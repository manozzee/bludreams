package com.lagradost.cloudstream3.ui.poster

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.ui.clear
import com.lagradost.cloudstream3.ui.home.HomeChildItemAdapter
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.search.SearchResultBuilder
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PosterCardStyleRepository {
    private const val KEY_WIDTH_DP = "poster_width_dp"
    private const val KEY_CORNER_RADIUS_DP = "poster_corner_radius_dp"
    private const val KEY_LANDSCAPE_MODE = "poster_landscape_mode"
    private const val KEY_HIDE_LABELS = "poster_hide_labels"
    private const val KEY_ALWAYS_CLEARLOGO = "poster_always_clearlogo"

    // Information visibility keys matching existing SharedPreferences / DevicePreferenceStore
    private const val KEY_SHOW_RATING = "show_rating_key"
    private const val KEY_SHOW_HD = "show_hd_key"
    private const val KEY_SHOW_DUB = "show_dub_key"
    private const val KEY_SHOW_SUB = "show_sub_key"
    private const val KEY_SHOW_EPISODE = "show_episode_text_key"

    private val _state = MutableStateFlow(PosterCardDefaults.DefaultState)
    val state: StateFlow<PosterCardStyleUiState> = _state.asStateFlow()

    @Volatile
    private var hasLoaded = false

    fun ensureLoaded(context: Context? = CloudStreamApp.context) {
        if (hasLoaded) return
        val ctx = context ?: CloudStreamApp.context ?: return
        hasLoaded = true
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val width = prefs.getInt(KEY_WIDTH_DP, PosterCardDefaults.widthDp)
            .coerceIn(PosterCardDefaults.widthRange)
        val radius = prefs.getInt(KEY_CORNER_RADIUS_DP, PosterCardDefaults.cornerRadiusDp)
            .coerceIn(PosterCardDefaults.cornerRadiusRange)
        val landscape = prefs.getBoolean(KEY_LANDSCAPE_MODE, PosterCardDefaults.catalogLandscapeModeEnabled)
        val hideLabels = prefs.getBoolean(KEY_HIDE_LABELS, PosterCardDefaults.hideLabelsEnabled)
        val clearlogo = prefs.getBoolean(KEY_ALWAYS_CLEARLOGO, PosterCardDefaults.alwaysShowLandscapeClearlogo)
        val showRating = prefs.getBoolean(KEY_SHOW_RATING, PosterCardDefaults.showRating)
        val showQuality = prefs.getBoolean(KEY_SHOW_HD, PosterCardDefaults.showQuality)
        val showDub = prefs.getBoolean(KEY_SHOW_DUB, PosterCardDefaults.showDub)
        val showSub = prefs.getBoolean(KEY_SHOW_SUB, PosterCardDefaults.showSub)
        val showEpisode = prefs.getBoolean(KEY_SHOW_EPISODE, PosterCardDefaults.showEpisode)

        _state.value = PosterCardStyleUiState(
            widthDp = width,
            heightDp = PosterCardDefaults.calculateHeightDp(width),
            cornerRadiusDp = radius,
            catalogLandscapeModeEnabled = landscape,
            hideLabelsEnabled = hideLabels,
            alwaysShowLandscapeClearlogo = clearlogo,
            showRating = showRating,
            showQuality = showQuality,
            showDub = showDub,
            showSub = showSub,
            showEpisode = showEpisode,
        )
        syncLegacyAdapterSizes(ctx)
    }

    fun setWidthDp(widthDp: Int, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val clamped = widthDp.coerceIn(PosterCardDefaults.widthRange)
        val updated = _state.value.copy(
            widthDp = clamped,
            heightDp = PosterCardDefaults.calculateHeightDp(clamped),
        )
        if (updated == _state.value) return
        _state.value = updated
        persist(context)
        syncLegacyAdapterSizes(context)
    }

    fun setCornerRadiusDp(cornerRadiusDp: Int, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val clamped = cornerRadiusDp.coerceIn(PosterCardDefaults.cornerRadiusRange)
        val updated = _state.value.copy(cornerRadiusDp = clamped)
        if (updated == _state.value) return
        _state.value = updated
        persist(context)
    }

    fun setCatalogLandscapeModeEnabled(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(catalogLandscapeModeEnabled = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persist(context)
        syncLegacyAdapterSizes(context)
    }

    fun setHideLabelsEnabled(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(hideLabelsEnabled = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persist(context)
    }

    fun setAlwaysShowLandscapeClearlogo(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(alwaysShowLandscapeClearlogo = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persist(context)
    }

    fun setShowRating(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(showRating = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persistInfo(context, KEY_SHOW_RATING, enabled)
    }

    fun setShowQuality(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(showQuality = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persistInfo(context, KEY_SHOW_HD, enabled)
    }

    fun setShowDub(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(showDub = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persistInfo(context, KEY_SHOW_DUB, enabled)
    }

    fun setShowSub(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(showSub = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persistInfo(context, KEY_SHOW_SUB, enabled)
    }

    fun setShowEpisode(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        val updated = _state.value.copy(showEpisode = enabled)
        if (updated == _state.value) return
        _state.value = updated
        persistInfo(context, KEY_SHOW_EPISODE, enabled)
    }

    fun resetToDefault(context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        _state.value = PosterCardDefaults.DefaultState
        persist(context)
        val ctx = context ?: CloudStreamApp.context
        if (ctx != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit {
                putBoolean(KEY_SHOW_RATING, PosterCardDefaults.showRating)
                putBoolean(KEY_SHOW_HD, PosterCardDefaults.showQuality)
                putBoolean(KEY_SHOW_DUB, PosterCardDefaults.showDub)
                putBoolean(KEY_SHOW_SUB, PosterCardDefaults.showSub)
                putBoolean(KEY_SHOW_EPISODE, PosterCardDefaults.showEpisode)
            }
            SearchResultBuilder.updateCache(ctx)
        }
        syncLegacyAdapterSizes(context)
    }

    private fun persistInfo(context: Context?, key: String, value: Boolean) {
        val ctx = context ?: CloudStreamApp.context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx).edit {
            putBoolean(key, value)
        }
        SearchResultBuilder.updateCache(ctx)
    }

    private fun persist(context: Context?) {
        val ctx = context ?: CloudStreamApp.context ?: return
        val current = _state.value
        PreferenceManager.getDefaultSharedPreferences(ctx).edit {
            putInt(KEY_WIDTH_DP, current.widthDp)
            putInt(KEY_CORNER_RADIUS_DP, current.cornerRadiusDp)
            putBoolean(KEY_LANDSCAPE_MODE, current.catalogLandscapeModeEnabled)
            putBoolean(KEY_HIDE_LABELS, current.hideLabelsEnabled)
            putBoolean(KEY_ALWAYS_CLEARLOGO, current.alwaysShowLandscapeClearlogo)
            putBoolean(KEY_SHOW_RATING, current.showRating)
            putBoolean(KEY_SHOW_HD, current.showQuality)
            putBoolean(KEY_SHOW_DUB, current.showDub)
            putBoolean(KEY_SHOW_SUB, current.showSub)
            putBoolean(KEY_SHOW_EPISODE, current.showEpisode)
        }
        SearchResultBuilder.updateCache(ctx)
    }

    private fun syncLegacyAdapterSizes(context: Context?) {
        if (context == null && CloudStreamApp.context == null) return
        val current = _state.value
        HomeChildItemAdapter.minPosterSize = current.widthDp.toPx
        HomeChildItemAdapter.maxPosterSize = current.heightDp.toPx
        HomeChildItemAdapter.sharedPool.clear()
        ParentItemAdapter.sharedPool.clear()
        SearchAdapter.sharedPool.clear()
    }
}


