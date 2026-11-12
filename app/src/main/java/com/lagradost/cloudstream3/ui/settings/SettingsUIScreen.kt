package com.lagradost.cloudstream3.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.lagradost.cloudstream3.ui.poster.PosterCardStyleRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.ui.clear
import com.lagradost.cloudstream3.ui.home.HomeChildItemAdapter
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.ui.APIRepository.Companion.noneApi
import com.lagradost.cloudstream3.ui.APIRepository.Companion.randomApi
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showBottomDialog
import com.lagradost.cloudstream3.ui.home.HomeFragment
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.settings.Globals.updateTv
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream4.compose.TV
import com.lagradost.cloudstream4.compose.isLayout
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.CloudStreamPrimaryColor
import com.lagradost.cloudstream4.theme.modeToTheme
import com.lagradost.cloudstream4.theme.perfToColor
import com.lagradost.cloudstream4.theme.perfToMode
import com.mihon.presentation.settings.Preference
import com.mihon.presentation.settings.SearchableSettings
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap

fun SearchQuality.toStringRes(): Int = when (this) {
    SearchQuality.BlueRay -> R.string.quality_blueray
    SearchQuality.Cam -> R.string.quality_cam
    SearchQuality.CamRip -> R.string.quality_cam_rip
    SearchQuality.DVD -> R.string.quality_dvd
    SearchQuality.HD -> R.string.quality_hd
    SearchQuality.HQ -> R.string.quality_hq
    SearchQuality.HdCam -> R.string.quality_cam_hd
    SearchQuality.Telecine -> R.string.quality_tc
    SearchQuality.Telesync -> R.string.quality_ts
    SearchQuality.WorkPrint -> R.string.quality_workprint
    SearchQuality.SD -> R.string.quality_sd
    SearchQuality.FourK -> R.string.quality_4k
    SearchQuality.UHD -> R.string.quality_uhd
    SearchQuality.SDR -> R.string.quality_sdr
    SearchQuality.HDR -> R.string.quality_hdr
    SearchQuality.WebRip -> R.string.quality_webrip
}

object SettingsUIScreen : SearchableSettings {
    @Composable
    override fun getTitleRes(): String = stringResource(R.string.category_ui)

    @Composable
    fun RoundColor(color: Color) {
        Box(
            modifier = Modifier
                .padding(start = 15.dp)
                .size(20.dp)
                .border(width = 1.5.dp, shape = CircleShape, color = MaterialTheme.colorScheme.onBackground)
                .background(color, CircleShape)
        )
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val settings = rememberAppSettings()
        val context = LocalContext.current
        val posterStyle by remember(context) {
            PosterCardStyleRepository.ensureLoaded(context)
            PosterCardStyleRepository.state
        }.collectAsState()

        return persistentListOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_looks),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = settings.ui.primaryColor,
                        icon = painterResource(R.drawable.colors_24px),
                        title = stringResource(R.string.primary_color_settings),
                        entries = stringArrayResource(R.array.themes_overlay_names_values).zip(
                            stringArrayResource(R.array.themes_overlay_names)
                        ).toMap().toPersistentMap().mutate { map ->
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { // remove monet on android 11 and less
                                map.remove("Monet")
                                map.remove("Monet2")
                            }
                        },
                        iconProvider = { k, _ ->
                            val color = perfToColor(k)
                            RoundColor(color.color)
                        },
                        onValueChanged = { newValue ->
                            settings.ui.primaryColor.set(newValue) // We need to set before we recreate
                            safe {
                                activity?.recreate()
                            }
                            return@ListPreference false
                        }),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.pureBlack,
                        icon = painterResource(R.drawable.palette_24px),
                        title = stringResource(R.string.pure_black_settings),
                        subtitle = stringResource(R.string.pure_black_settings_des),
                        onValueChanged = { newValue ->
                            settings.ui.pureBlack.set(newValue)
                            activity?.let { act ->
                                CommonActivity.loadThemes(act)
                                act.recreate()
                            }
                            true
                        }),
                    Preference.PreferenceItem.ListPreference(
                        preference = settings.ui.layout,
                        icon = painterResource(R.drawable.responsive_layout_24px),
                        title = stringResource(R.string.app_layout),
                        entries = integerArrayResource(R.array.app_layout_values).zip(
                            stringArrayResource(R.array.app_layout)
                        ).toMap().toPersistentMap(),
                        onValueChanged = { newValue ->
                            settings.ui.layout.set(newValue) // We need to set before we recreate
                            safe {
                                activity?.updateTv()
                                activity?.recreate()
                            }
                            return@ListPreference false
                        }),
                    Preference.PreferenceItem.TextPreference(
                        title = "Poster Card Style",
                        subtitle = buildString {
                            append("${posterStyle.widthDp.toInt()}dp · ${posterStyle.cornerRadiusDp.toInt()}dp radius")
                            if (posterStyle.landscapePostersEnabled) append(" · Landscape")
                            if (posterStyle.hideLabels) append(" · No labels")
                        },
                        icon = painterResource(R.drawable.baseline_grid_view_24),
                        onClick = {
                            val fragActivity = activity as? FragmentActivity
                            if (fragActivity != null) {
                                PosterCustomizationSettingsDialogFragment.newInstance()
                                    .show(fragActivity.supportFragmentManager, "poster_customization_settings")
                            }
                        }
                    ),
                )
            ),

            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_ui_features),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SliderPreference(
                        preference = settings.ui.overscanDp,
                        title = stringResource(R.string.overscan_settings),
                        subtitle = stringResource(R.string.overscan_settings_des),
                        valueRange = 0..100,
                        icon = painterResource(R.drawable.arrows_input_24px),
                        enabled = isLayout(TV),
                        onValueChanged = { newValue ->
                            settings.ui.overscanDp.set(newValue)
                            val padding = newValue.toPx
                            (activity as? MainActivity)?.binding?.homeRoot?.setPadding(
                                padding, padding, padding, padding
                            )
                            return@SliderPreference false
                        }),

                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.advancedSearch,
                        title = stringResource(R.string.advanced_search),
                        subtitle = stringResource(R.string.advanced_search_des),
                        icon = painterResource(R.drawable.search_icon)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.searchSuggestions,
                        title = stringResource(R.string.search_suggestions),
                        subtitle = stringResource(R.string.search_suggestions_des),
                        icon = painterResource(R.drawable.tooltip_24px)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.trailersEnabled,
                        title = stringResource(R.string.show_trailers_settings),
                        icon = painterResource(R.drawable.baseline_theaters_24)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.kitsuPostersEnabled,
                        title = stringResource(R.string.kitsu_settings),
                        icon = painterResource(R.drawable.kitsu_icon)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.castEnabled,
                        title = stringResource(R.string.show_cast_in_details),
                        icon = painterResource(R.drawable.face_24px)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.fillersEnabled,
                        title = stringResource(R.string.show_fillers_settings),
                        icon = painterResource(R.drawable.skip_next_24px)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.showMetadataOverlay,
                        title = stringResource(R.string.show_player_metadata_overlay),
                        icon = painterResource(R.drawable.metadata_overlay_icon)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = settings.ui.showClock,
                        title = stringResource(R.string.tv_layout_clock_settings),
                        subtitle = stringResource(R.string.tv_layout_clock_settings_des),
                        icon = painterResource(R.drawable.ic_baseline_clock_24),
                        enabled = isLayout(TV),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        icon = painterResource(R.drawable.filter_alt_24px),
                        title = stringResource(R.string.home_page_source_title),
                        subtitle = DataStoreHelper.currentHomePage ?: stringResource(R.string.none),
                        onClick = {
                            activity?.let { ctx ->
                                val apis = APIHolder.apis.map { it.name }.toMutableList()
                                apis.add(0, randomApi.name)
                                apis.add(0, noneApi.name)
                                val current = DataStoreHelper.currentHomePage
                                ctx.showBottomDialog(
                                    apis,
                                    apis.indexOf(current),
                                    ctx.getString(R.string.home_page_source_title),
                                    true,
                                    {},
                                ) { index ->
                                    val selected = apis.getOrNull(index) ?: return@showBottomDialog
                                    DataStoreHelper.currentHomePage = selected
                                    ctx.recreate()
                                }
                            }
                        }
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = settings.ui.confirmExit,
                        title = stringResource(R.string.confirm_before_exiting_title),
                        subtitle = stringResource(R.string.confirm_before_exiting_desc),
                        icon = painterResource(R.drawable.ic_baseline_exit_24),
                        entries = integerArrayResource(R.array.confirm_exit_values).zip(
                            stringArrayResource(R.array.confirm_exit)
                        ).toMap()
                    ),
                )
            )
        )
    }
}
