package com.lagradost.cloudstream3.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Outline
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class AppIconOption(
    val id: String,
    val label: String,
    val backgroundRes: Int,
    val foregroundRes: Int,
    val manifestAlias: String,
)

object AppIconRegistry {
    private const val PREF_KEY_APP_ICON = "selected_app_icon_id"
    private const val MANIFEST_NAMESPACE = "com.lagradost.cloudstream3"

    /**
     * ID of the default app icon used on fresh installs or when no custom selection is saved.
     * To set a different icon as default in the future, change this ID to match an entry in [icons]
     * and set `android:enabled="true"` on its `<activity-alias>` (and `<application>` icon) in AndroidManifest.xml.
     */
    private const val DEFAULT_ICON_ID = "nami"

    /**
     * Single source of truth for predefined BluDreams app icons.
     * Add new entries here alongside their drawable pairs & manifest <activity-alias>.
     */
    val icons: List<AppIconOption> = listOf(
        AppIconOption(
            id = "nami",
            label = "Nami",
            backgroundRes = R.drawable.ic_bg_nami,
            foregroundRes = R.drawable.ic_fg_nami,
            manifestAlias = ".Icon_nami",
        ),
        AppIconOption(
            id = "reze",
            label = "Reze",
            backgroundRes = R.drawable.ic_bg_reze,
            foregroundRes = R.drawable.ic_fg_reze,
            manifestAlias = ".Icon_reze",
        ),
    )

    val defaultIcon: AppIconOption
        get() = icons.firstOrNull { it.id == DEFAULT_ICON_ID } ?: icons.first()

    private val _currentIcon = MutableStateFlow(defaultIcon)
    val currentIcon: StateFlow<AppIconOption> = _currentIcon.asStateFlow()

    @Volatile
    private var hasLoaded = false

    fun ensureLoaded(context: Context? = CloudStreamApp.context) {
        if (hasLoaded) return
        val ctx = context ?: CloudStreamApp.context ?: return
        hasLoaded = true

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val savedId = prefs.getString(PREF_KEY_APP_ICON, null)
        val savedMatch = icons.firstOrNull { it.id == savedId }
        val resolved = savedMatch
            ?: detectEnabledAlias(ctx)
            ?: defaultIcon
        _currentIcon.value = resolved

        // Recover cleanly if a previously persisted icon ID was removed (or if default alias was disabled)
        val needsAliasSync = (savedId != null && savedMatch == null) || isComponentDisabled(ctx, resolved)
        if (needsAliasSync) {
            prefs.edit { putString(PREF_KEY_APP_ICON, resolved.id) }
            applyLauncherAlias(ctx.applicationContext, resolved)
        }
    }

    fun selectIcon(context: Context, iconId: String) {
        ensureLoaded(context)
        val target = icons.firstOrNull { it.id == iconId } ?: return
        _currentIcon.value = target

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(PREF_KEY_APP_ICON, target.id)
        }

        applyLauncherAlias(context.applicationContext, target)
    }

    private fun componentFor(context: Context, option: AppIconOption): ComponentName {
        val fqcn = if (option.manifestAlias.startsWith(".")) {
            "$MANIFEST_NAMESPACE${option.manifestAlias}"
        } else {
            option.manifestAlias
        }
        return ComponentName(context.packageName, fqcn)
    }

    private fun isComponentDisabled(context: Context, option: AppIconOption): Boolean {
        return runCatching {
            context.packageManager.getComponentEnabledSetting(componentFor(context, option)) ==
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }.getOrDefault(false)
    }

    private fun applyLauncherAlias(context: Context, target: AppIconOption) {
        val pm = context.packageManager

        // Enable target alias before disabling others so the launcher always has an active entry
        try {
            val targetComponent = componentFor(context, target)
            if (pm.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        } catch (t: Throwable) {
            logError(t)
        }

        icons.forEach { option ->
            if (option.id == target.id) return@forEach
            try {
                val component = componentFor(context, option)
                if (pm.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            } catch (t: Throwable) {
                logError(t)
            }
        }
    }

    private fun detectEnabledAlias(context: Context): AppIconOption? {
        val pm = context.packageManager
        return icons.firstOrNull { option ->
            runCatching {
                pm.getComponentEnabledSetting(componentFor(context, option)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }.getOrDefault(false)
        }
    }

    fun applyToImageView(imageView: ImageView, icon: AppIconOption = currentIcon.value) {
        val ctx = imageView.context
        val bg = ContextCompat.getDrawable(ctx, icon.backgroundRes)
        val fg = ContextCompat.getDrawable(ctx, icon.foregroundRes)
        if (bg != null && fg != null) {
            val layerDrawable = LayerDrawable(arrayOf(bg, fg))
            val insetPx = (imageView.layoutParams?.width?.takeIf { it > 0 } ?: 120) / 14
            layerDrawable.setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
            imageView.setImageDrawable(layerDrawable)
        } else if (fg != null) {
            imageView.setImageDrawable(fg)
        }
        imageView.clipToOutline = true
        imageView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }
}

@Composable
fun rememberActiveAppIcon(): AppIconOption {
    val context = LocalContext.current
    val activeIcon by remember(context) {
        AppIconRegistry.ensureLoaded(context)
        AppIconRegistry.currentIcon
    }.collectAsState()
    return activeIcon
}

/**
 * Renders an [AppIconOption] by compositing its background and foreground drawables inside a circular frame.
 */
@Composable
fun MergedAppIconPreview(
    icon: AppIconOption,
    modifier: Modifier = Modifier,
    foregroundInset: Dp = 3.dp,
) {
    val context = LocalContext.current
    val bgRequest = remember(context, icon.backgroundRes) {
        ImageRequest.Builder(context)
            .data(icon.backgroundRes)
            .crossfade(false)
            .build()
    }
    val fgRequest = remember(context, icon.foregroundRes) {
        ImageRequest.Builder(context)
            .data(icon.foregroundRes)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier.clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = bgRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model = fgRequest,
            contentDescription = icon.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(foregroundInset),
        )
    }
}
