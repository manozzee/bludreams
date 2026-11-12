package com.lagradost.cloudstream3.ui.liquidglass

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LiquidGlassSettingsRepository {
    const val KEY_LIQUID_GLASS_GLOW_ENABLED = "liquid_glass_glow_enabled_key"

    private val _liquidGlassEnabled = MutableStateFlow(true)
    val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    private val _glowEnabled = MutableStateFlow(true)
    val glowEnabled: StateFlow<Boolean> = _glowEnabled.asStateFlow()

    @Volatile
    private var hasLoaded = false

    fun ensureLoaded(context: Context? = CloudStreamApp.context) {
        if (hasLoaded) return
        val ctx = context ?: CloudStreamApp.context ?: return
        hasLoaded = true
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        _glowEnabled.value = prefs.getBoolean(KEY_LIQUID_GLASS_GLOW_ENABLED, true)
    }

    fun setGlowEnabled(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        if (_glowEnabled.value == enabled) return
        _glowEnabled.value = enabled
        val ctx = context ?: CloudStreamApp.context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx).edit {
            putBoolean(KEY_LIQUID_GLASS_GLOW_ENABLED, enabled)
        }
    }
}
