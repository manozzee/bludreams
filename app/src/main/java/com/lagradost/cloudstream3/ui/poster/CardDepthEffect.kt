package com.lagradost.cloudstream3.ui.poster

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CardDepthSurface {
    PosterCards,
    ContinueWatching,
    EpisodeCards,
    CastCards,
    StreamCards;

    companion object {
        val POSTER = PosterCards
        val CARD = EpisodeCards
    }
}

enum class CardDepthPreset(
    val key: String,
    val label: String,
    val elevation: Dp,
    val ambientAlpha: Float,
    val spotAlpha: Float,
    val topHighlightAlpha: Float,
    val sideRimAlpha: Float,
    val bottomEdgeAlpha: Float,
) {
    Subtle(
        key = "subtle",
        label = "Subtle",
        elevation = 4.dp,
        ambientAlpha = 0.22f,
        spotAlpha = 0.34f,
        topHighlightAlpha = 0.20f,
        sideRimAlpha = 0.08f,
        bottomEdgeAlpha = 0.24f,
    ),
    Balanced(
        key = "balanced",
        label = "Balanced",
        elevation = 8.dp,
        ambientAlpha = 0.32f,
        spotAlpha = 0.48f,
        topHighlightAlpha = 0.26f,
        sideRimAlpha = 0.11f,
        bottomEdgeAlpha = 0.32f,
    ),
    Deep(
        key = "deep",
        label = "Deep",
        elevation = 13.dp,
        ambientAlpha = 0.44f,
        spotAlpha = 0.64f,
        topHighlightAlpha = 0.34f,
        sideRimAlpha = 0.14f,
        bottomEdgeAlpha = 0.42f,
    ),
    ;

    companion object {
        fun fromKey(key: String?): CardDepthPreset =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Balanced
    }
}

@Immutable
data class CardDepthStyleState(
    val enabled: Boolean = true,
    val preset: CardDepthPreset = CardDepthPreset.Balanced,
    val posterCardsEnabled: Boolean = true,
    val continueWatchingEnabled: Boolean = true,
    val episodeCardsEnabled: Boolean = true,
    val castCardsEnabled: Boolean = true,
    val streamCardsEnabled: Boolean = true,
) {
    fun isEnabledFor(surface: CardDepthSurface): Boolean {
        if (!enabled) return false
        return when (surface) {
            CardDepthSurface.PosterCards -> posterCardsEnabled
            CardDepthSurface.ContinueWatching -> continueWatchingEnabled
            CardDepthSurface.EpisodeCards -> episodeCardsEnabled
            CardDepthSurface.CastCards -> castCardsEnabled
            CardDepthSurface.StreamCards -> streamCardsEnabled
        }
    }
}

object CardDepthRepository {
    private const val KEY_ENABLED = "card_depth_enabled"
    private const val KEY_PRESET = "card_depth_preset"

    private val _state = MutableStateFlow(CardDepthStyleState())
    val state: StateFlow<CardDepthStyleState> = _state.asStateFlow()

    @Volatile
    private var hasLoaded = false

    fun ensureLoaded(context: Context? = CloudStreamApp.context) {
        if (hasLoaded) return
        val ctx = context ?: CloudStreamApp.context ?: return
        hasLoaded = true
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val preset = CardDepthPreset.fromKey(prefs.getString(KEY_PRESET, CardDepthPreset.Balanced.key))
        _state.value = CardDepthStyleState(
            enabled = enabled,
            preset = preset,
        )
    }

    fun setEnabled(enabled: Boolean, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        if (_state.value.enabled == enabled) return
        _state.value = _state.value.copy(enabled = enabled)
        val ctx = context ?: CloudStreamApp.context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx).edit {
            putBoolean(KEY_ENABLED, enabled)
        }
    }

    fun setPreset(preset: CardDepthPreset, context: Context? = CloudStreamApp.context) {
        ensureLoaded(context)
        if (_state.value.preset == preset) return
        _state.value = _state.value.copy(preset = preset)
        val ctx = context ?: CloudStreamApp.context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx).edit {
            putString(KEY_PRESET, preset.key)
        }
    }
}

val LocalCardDepthStyle = staticCompositionLocalOf<CardDepthStyleState?> { null }

@Composable
fun rememberCardDepthStyleState(): CardDepthStyleState {
    LocalCardDepthStyle.current?.let { return it }
    val context = LocalContext.current
    val flow = remember(context) {
        CardDepthRepository.ensureLoaded(context)
        CardDepthRepository.state
    }
    val state by flow.collectAsState()
    return state
}

fun Modifier.cardDepth(
    shape: Shape,
    surface: CardDepthSurface = CardDepthSurface.PosterCards,
    fallbackBorderAlpha: Float = 0f,
    fallbackBorderWidth: Dp = 1.dp,
): Modifier = composed {
    val style = rememberCardDepthStyleState()
    if (!style.isEnabledFor(surface)) {
        if (fallbackBorderAlpha > 0f) {
            this.border(
                width = fallbackBorderWidth,
                color = Color.White.copy(alpha = fallbackBorderAlpha),
                shape = shape,
            )
        } else {
            this
        }
    } else {
        val preset = style.preset
        val rimBrush = remember(preset) {
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = preset.topHighlightAlpha),
                0.32f to Color.White.copy(alpha = preset.sideRimAlpha),
                0.78f to Color.White.copy(alpha = preset.sideRimAlpha * 0.45f),
                1f to Color.Black.copy(alpha = preset.bottomEdgeAlpha),
            )
        }
        this
            .shadow(
                elevation = preset.elevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = preset.ambientAlpha),
                spotColor = Color.Black.copy(alpha = preset.spotAlpha),
            )
            .drawWithContent {
                drawContent()
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    brush = rimBrush,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
    }
}

