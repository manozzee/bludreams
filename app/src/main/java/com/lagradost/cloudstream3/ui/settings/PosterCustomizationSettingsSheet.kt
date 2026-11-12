package com.lagradost.cloudstream3.ui.settings

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lagradost.cloudstream3.ui.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream4.compose.CloudStreamThemeAppSettings
import com.lagradost.cloudstream3.ui.poster.CardDepthPreset
import com.lagradost.cloudstream3.ui.poster.CardDepthRepository
import com.lagradost.cloudstream3.ui.poster.CardDepthSurface
import com.lagradost.cloudstream3.ui.poster.PosterCardDefaults
import com.lagradost.cloudstream3.ui.poster.PosterCardStyleRepository
import com.lagradost.cloudstream3.ui.poster.PosterRadiusPreset
import com.lagradost.cloudstream3.ui.poster.PosterWidthPreset
import com.lagradost.cloudstream3.ui.poster.cardDepth
import com.lagradost.cloudstream3.ui.poster.landscapePosterHeight
import com.lagradost.cloudstream3.ui.poster.landscapePosterWidth
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showMultiDialog
import com.lagradost.cloudstream4.rememberAppSettings
import com.mihon.presentation.settings.widget.BasePreferenceWidget
import com.mihon.presentation.settings.widget.CardBackgroundColor
import com.mihon.presentation.settings.widget.GroupPosition
import com.mihon.presentation.settings.widget.LocalGroupPosition
import com.mihon.presentation.settings.widget.PreferenceGroupHeader
import com.mihon.presentation.settings.widget.SettingsIconTile
import com.mihon.presentation.settings.widget.SwitchPreferenceWidget
import com.mihon.presentation.settings.widget.TextPreferenceWidget
import com.mihon.presentation.settings.widget.getGroupPosition
import com.mihon.presentation.settings.widget.settingsCard

class PosterCustomizationSettingsDialogFragment : DialogFragment() {
    override fun getTheme(): Int = R.style.DialogFullscreenPlayer

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setWindowAnimations(R.style.DialogFullscreenPlayer)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CloudStreamThemeAppSettings {
                    PosterCustomizationSettingsPage(
                        onBack = { dismissAllowingStateLoss() }
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "PosterCustomizationSettingsDialog"

        fun newInstance(): PosterCustomizationSettingsDialogFragment =
            PosterCustomizationSettingsDialogFragment()

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            PosterCustomizationSettingsDialogFragment().show(fragmentManager, TAG)
        }
    }
}

@Composable
fun PosterCustomizationSettingsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = rememberAppSettings()
    val styleState by remember(context) {
        PosterCardStyleRepository.ensureLoaded(context)
        PosterCardStyleRepository.state
    }.collectAsState()

    val cardDepthState by remember(context) {
        CardDepthRepository.ensureLoaded(context)
        CardDepthRepository.state
    }.collectAsState()

    val isDefault = styleState == PosterCardDefaults.DefaultState &&
        cardDepthState.enabled &&
        cardDepthState.preset == CardDepthPreset.Balanced

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarTop),
        ) {
            // Standard CloudStream Settings Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.keyboard_arrow_left_24px),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Text(
                    text = "Poster Card Style",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                )

                // Circular Reset Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (!isDefault) 0.10f else 0.04f))
                        .clickable(enabled = !isDefault) {
                            PosterCardStyleRepository.resetToDefault(context)
                            CardDepthRepository.setEnabled(true, context)
                            CardDepthRepository.setPreset(CardDepthPreset.Balanced, context)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "Reset to default",
                        tint = if (!isDefault) Color.White else Color.White.copy(alpha = 0.38f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = navBarBottom + 32.dp,
                ),
            ) {
                // Live Poster Preview
                item(key = "preview") {
                    PosterPreviewCard(
                        widthDp = styleState.widthDp,
                        heightDp = styleState.heightDp,
                        cornerRadiusDp = styleState.cornerRadiusDp,
                        landscapeMode = styleState.catalogLandscapeModeEnabled,
                        hideLabels = styleState.hideLabelsEnabled,
                        alwaysShowClearlogo = styleState.alwaysShowLandscapeClearlogo,
                        showRating = styleState.showRating,
                        showQuality = styleState.showQuality,
                        showDub = styleState.showDub,
                        showSub = styleState.showSub,
                        showEpisode = styleState.showEpisode,
                    )
                }

                // DIMENSIONS & SHAPE CATEGORY
                item(key = "dimensions_header") {
                    PreferenceGroupHeader(title = "Dimensions & Shape")
                }

                // Poster Width preset card
                item(key = "width_presets") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.First) {
                        PosterPresetSelectionCard(
                            icon = Icons.Default.ViewColumn,
                            title = "Poster Width",
                            subtitle = "${styleState.widthDp} dp × ${styleState.heightDp} dp",
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PosterWidthPreset.entries.forEach { preset ->
                                    val selected = styleState.widthDp == preset.widthDp
                                    SettingsPresetChip(
                                        label = preset.label,
                                        subLabel = "${preset.widthDp}dp",
                                        selected = selected,
                                        onClick = {
                                            PosterCardStyleRepository.setWidthDp(preset.widthDp, context)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Corner Radius preset card
                item(key = "corner_radius_presets") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Last) {
                        PosterPresetSelectionCard(
                            icon = Icons.Default.RoundedCorner,
                            title = "Corner Radius",
                            subtitle = "${styleState.cornerRadiusDp} dp",
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PosterRadiusPreset.entries.forEach { preset ->
                                    val selected = styleState.cornerRadiusDp == preset.radiusDp
                                    SettingsPresetChip(
                                        label = preset.label,
                                        subLabel = "${preset.radiusDp}dp",
                                        selected = selected,
                                        onClick = {
                                            PosterCardStyleRepository.setCornerRadiusDp(preset.radiusDp, context)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // POSTER FEATURES CATEGORY
                item(key = "features_header") {
                    PreferenceGroupHeader(title = "Poster Features")
                }

                // 1. Landscape Posters
                item(key = "landscape_posters") {
                    val pos = if (styleState.catalogLandscapeModeEnabled) GroupPosition.First else GroupPosition.First
                    CompositionLocalProvider(LocalGroupPosition provides pos) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.CropLandscape,
                            title = "Landscape Posters",
                            subtitle = "Use 16:9 widescreen artwork cards across Home catalog rows",
                            checked = styleState.catalogLandscapeModeEnabled,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setCatalogLandscapeModeEnabled(it, context)
                            },
                        )
                    }
                }

                // 2. Always Show Clearlogo on Landscape (conditionally visible)
                if (styleState.catalogLandscapeModeEnabled) {
                    item(key = "always_show_clearlogo") {
                        CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Middle) {
                            SettingsStandardSwitchRow(
                                icon = Icons.Default.Image,
                                title = "Always Show Clearlogo on Landscape",
                                subtitle = "Display title logo/text overlay inside landscape poster cards",
                                checked = styleState.alwaysShowLandscapeClearlogo,
                                onCheckedChanged = {
                                    PosterCardStyleRepository.setAlwaysShowLandscapeClearlogo(it, context)
                                },
                            )
                        }
                    }
                }

                // 3. Hide Labels
                item(key = "hide_labels") {
                    val pos = if (cardDepthState.enabled) GroupPosition.Middle else GroupPosition.Middle
                    CompositionLocalProvider(LocalGroupPosition provides pos) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.Title,
                            title = "Hide Labels",
                            subtitle = "Hide content titles below poster cards for a cleaner shelf",
                            checked = styleState.hideLabelsEnabled,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setHideLabelsEnabled(it, context)
                            },
                        )
                    }
                }

                // 4. Card Depth Effect
                item(key = "card_depth_effect") {
                    val pos = if (cardDepthState.enabled) GroupPosition.Middle else GroupPosition.Last
                    CompositionLocalProvider(LocalGroupPosition provides pos) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.Layers,
                            title = "Card Depth Effect",
                            subtitle = "Add realistic 3D rim lighting and elevation shadows to cards",
                            checked = cardDepthState.enabled,
                            onCheckedChanged = {
                                CardDepthRepository.setEnabled(it, context)
                            },
                        )
                    }
                }

                // 5. Card Depth Presets (when enabled)
                if (cardDepthState.enabled) {
                    item(key = "card_depth_presets") {
                        CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Last) {
                            PosterPresetSelectionCard(
                                icon = Icons.Default.Tune,
                                title = "Depth Intensity",
                                subtitle = "${cardDepthState.preset.label} (${cardDepthState.preset.elevation.value.toInt()}dp)",
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CardDepthPreset.entries.forEach { preset ->
                                        SettingsPresetChip(
                                            label = preset.label,
                                            subLabel = "${preset.elevation.value.toInt()}dp",
                                            selected = cardDepthState.preset == preset,
                                            onClick = {
                                                CardDepthRepository.setPreset(preset, context)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // POSTER INFORMATION CATEGORY
                item(key = "info_header") {
                    PreferenceGroupHeader(title = "Poster Information")
                }

                item(key = "info_rating") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.First) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.Star,
                            title = "Rating Label",
                            subtitle = "Show rating score badge on movie and TV show cards",
                            checked = styleState.showRating,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setShowRating(it, context)
                            },
                        )
                    }
                }

                item(key = "info_quality") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Middle) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.Movie,
                            title = "Quality Label",
                            subtitle = "Show video resolution badge (4K, HD, etc.) on cards",
                            checked = styleState.showQuality,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setShowQuality(it, context)
                            },
                        )
                    }
                }

                item(key = "info_dub") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Middle) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Dub Label",
                            subtitle = "Show dubbed audio badge on anime and international titles",
                            checked = styleState.showDub,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setShowDub(it, context)
                            },
                        )
                    }
                }

                item(key = "info_sub") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Middle) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.Title,
                            title = "Sub Label",
                            subtitle = "Show subtitle badge on anime and international titles",
                            checked = styleState.showSub,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setShowSub(it, context)
                            },
                        )
                    }
                }

                item(key = "info_episode") {
                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Last) {
                        SettingsStandardSwitchRow(
                            icon = Icons.Default.ViewList,
                            title = "Episode Text",
                            subtitle = "Show episode counts and indicators on poster cards",
                            checked = styleState.showEpisode,
                            onCheckedChanged = {
                                PosterCardStyleRepository.setShowEpisode(it, context)
                            },
                        )
                    }
                }

                // FILTERING & DISCOVERY CATEGORY
                item(key = "filtering_header") {
                    PreferenceGroupHeader(title = "Filtering & Discovery")
                }

                item(key = "quality_filter") {
                    val allQualities = remember { SearchQuality.entries }
                    val qualityNamesMap = allQualities.associateWith { stringResource(it.toStringRes()) }
                    val currentFilterSet by settings.ui.filterQuality.changes().collectAsState(initial = settings.ui.filterQuality.get())
                    val noneString = stringResource(R.string.none)
                    val filterSummary = remember(currentFilterSet, noneString, qualityNamesMap) {
                        if (currentFilterSet.isEmpty()) {
                            noneString
                        } else {
                            currentFilterSet.mapNotNull { qualityNamesMap[it] }.joinToString(", ")
                        }
                    }
                    val filterTitle = stringResource(R.string.pref_filter_search_quality)

                    CompositionLocalProvider(LocalGroupPosition provides GroupPosition.Single) {
                        SettingsStandardClickableRow(
                            icon = Icons.Default.Tune,
                            title = filterTitle,
                            subtitle = filterSummary,
                            onClick = {
                                val activity = (context as? Activity) ?: context.getActivity() ?: CommonActivity.activity
                                if (activity != null) {
                                    val qualityNames = allQualities.map { qualityNamesMap[it].orEmpty() }
                                    val selectedIndices = allQualities.mapIndexedNotNull { index, quality ->
                                        if (currentFilterSet.contains(quality)) index else null
                                    }

                                    activity.showMultiDialog(
                                        items = qualityNames,
                                        selectedIndex = selectedIndices,
                                        name = filterTitle,
                                        dismissCallback = {},
                                    ) { newSelectedIndices ->
                                        val newSet = newSelectedIndices.map { allQualities[it] }.toSet()
                                        settings.ui.filterQuality.set(newSet)
                                        PreferenceManager.getDefaultSharedPreferences(context).edit {
                                            putStringSet(
                                                "pref_filter_search_quality_key",
                                                newSelectedIndices.map { it.toString() }.toSet()
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterPreviewCard(
    widthDp: Int,
    heightDp: Int,
    cornerRadiusDp: Int,
    landscapeMode: Boolean,
    hideLabels: Boolean,
    alwaysShowClearlogo: Boolean,
    showRating: Boolean,
    showQuality: Boolean,
    showDub: Boolean,
    showSub: Boolean,
    showEpisode: Boolean,
) {
    val targetWidth = if (landscapeMode) landscapePosterWidth(widthDp) else widthDp.dp
    val targetHeight = if (landscapeMode) landscapePosterHeight(widthDp) else heightDp.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "previewWidth",
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "previewHeight",
    )
    val animatedRadius by animateDpAsState(
        targetValue = cornerRadiusDp.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "previewRadius",
    )
    val cardShape = RoundedCornerShape(animatedRadius)

    Column(
        modifier = Modifier
            .settingsCard(GroupPosition.Single)
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "LIVE PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color(0xFFDEC3BE),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.width(animatedWidth),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .cardDepth(
                        shape = cardShape,
                        surface = CardDepthSurface.PosterCards,
                    )
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2B44),
                                Color(0xFF2E1E3B),
                                Color(0xFF111520),
                            ),
                        ),
                    ),
            ) {
                // Subtle cinematic highlight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.08f),
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.82f),
                            ),
                        ),
                )

                // Top-left Badges (Quality, Dub, Sub, Episode counts)
                val previewBadges = buildList {
                    if (showQuality) add("4K")
                    if (showDub) {
                        if (showEpisode) add("DUB 12") else add("DUB")
                    }
                    if (showSub) {
                        if (showEpisode) add("SUB 12") else add("SUB")
                    }
                }
                if (previewBadges.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = previewBadges.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                    }
                }

                // Top-right rating badge
                if (showRating) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "★ 8.7",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFFF5C518),
                        )
                    }
                }

                if (landscapeMode && alwaysShowClearlogo) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "BLUDREAMS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Sci-Fi • 2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                } else if (!landscapeMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "BLUDREAMS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            if (!hideLabels) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sample Movie Title",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = buildString {
                append(if (landscapeMode) "Landscape (16:9)" else "Portrait (2:3)")
                append(" • ")
                append("${animatedWidth.value.toInt()}×${animatedHeight.value.toInt()} dp")
                append(" • ")
                append("Radius ${cornerRadiusDp}dp")
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Settings card with an icon tile, title, subtitle, and an embedded custom control (e.g. preset chip row).
 * Follows the exact same asymmetric corner shapes and gaps from [settingsCard].
 */
@Composable
private fun PosterPresetSelectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tileColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val groupPosition = LocalGroupPosition.current
    val bg = tileColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    Column(
        modifier = Modifier
            .settingsCard(groupPosition)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        content()
    }
}

/**
 * Selection preset chip matching the dark grouped settings palette.
 * Unselected: dark elevated container with subtle border.
 * Selected: primary accent container with white checkmark.
 */
@Composable
private fun SettingsPresetChip(
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF2A292E)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                fontSize = 13.sp,
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor.copy(alpha = 0.78f),
            )
        }
    }
}

/**
 * Standard CloudStream Settings Switch Row using [BasePreferenceWidget] and [Switch].
 */
@Composable
private fun SettingsStandardSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    tileColor: Color? = null,
) {
    val bg = tileColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    BasePreferenceWidget(
        title = title,
        subcomponent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        onClick = { onCheckedChanged(!checked) },
        widget = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

/**
 * Standard CloudStream Settings Clickable Row using [BasePreferenceWidget].
 */
@Composable
private fun SettingsStandardClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tileColor: Color? = null,
) {
    val bg = tileColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    BasePreferenceWidget(
        title = title,
        subcomponent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        onClick = onClick,
    )
}
