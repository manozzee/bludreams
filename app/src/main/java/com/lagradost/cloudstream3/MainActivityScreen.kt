package com.lagradost.cloudstream3

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassUpdateOverlay
import com.lagradost.cloudstream3.ui.settings.ApkUpdater
import com.lagradost.cloudstream3.ui.settings.GithubReleases
import com.lagradost.cloudstream3.ui.settings.GithubReleases.APK_CONTENT_TYPE
import com.lagradost.cloudstream3.ui.settings.GithubReleases.APK_REPOSITORY
import com.lagradost.cloudstream3.ui.settings.GithubReleases.APK_USERNAME
import com.lagradost.cloudstream4.compose.Screen
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.viewmodels.GithubAction
import com.lagradost.cloudstream4.viewmodels.GithubAction.AutoSearchForUpdate
import com.lagradost.cloudstream4.viewmodels.GithubState
import com.lagradost.cloudstream4.viewmodels.GithubViewModel

object MainActivityScreen : Screen {
    @Composable
    fun githubViewModel(): GithubViewModel? {
        // By having the ComponentActivity as the owner we can have a singleton viewmodel
        val activity = LocalActivity.current as? ComponentActivity ?: return null
        val settings = rememberAppSettings()
        return viewModel(viewModelStoreOwner = activity) {
            GithubViewModel(
                remoteUserName = APK_USERNAME,
                remoteRepository = APK_REPOSITORY,
                remoteContentType = APK_CONTENT_TYPE,
                versionName = BuildConfig.VERSION_NAME,
                isDebug = BuildConfig.DEBUG,
                settings = settings,
                updater = ApkUpdater,
                repository = GithubReleases,
            ).apply {
                /** Only search once per Activity session when not in safe mode */
                if (!PluginManager.isSafeMode()) {
                    onAction(AutoSearchForUpdate)
                }
            }
        }
    }

    @Composable
    override fun Content() {
        GithubView()
    }

    @Composable
    fun GithubView() {
        val updateViewModel = githubViewModel() ?: return
        val state by updateViewModel.state.collectAsState()
        GithubState(state, updateViewModel::onAction)
    }

    @Composable
    fun GithubState(updaterState: GithubState, onAction: (GithubAction) -> Unit) {
        LiquidGlassTheme {
            LiquidGlassUpdateOverlay(
                updaterState = updaterState,
                onAction = onAction,
            )
        }
    }
}