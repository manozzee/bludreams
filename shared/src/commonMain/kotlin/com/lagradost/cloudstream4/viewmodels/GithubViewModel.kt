package com.lagradost.cloudstream4.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream4.AppSettings
import com.lagradost.cloudstream4.state.ActionHandler
import com.lagradost.cloudstream4.state.DefaultStateContainer
import com.lagradost.cloudstream4.state.SingleActiveQuery
import com.lagradost.cloudstream4.state.StateContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class GithubState(
    val dialog: GithubDialog? = null,
)

@Immutable
sealed class GithubUpdateDialogState {
    data class Error(val error: Throwable) : GithubUpdateDialogState()
    data class DownloadProgress(val progress: Long, val total: Long?) : GithubUpdateDialogState()
    data class ReadyToInstall(
        val file: GithubFile,
        val apkPath: String,
        val totalBytes: Long,
    ) : GithubUpdateDialogState()

    data class InstallError(
        val file: GithubFile,
        val apkPath: String,
        val totalBytes: Long,
        val error: Throwable,
    ) : GithubUpdateDialogState()

    object Loading : GithubUpdateDialogState()
    object NoUpdateFound : GithubUpdateDialogState()
    data class UpdateFound(
        val file: GithubFile,
    ) : GithubUpdateDialogState()
}

@Immutable
data class GithubDialog(
    val isFromUser: Boolean,
    val state: GithubUpdateDialogState,
)

/** GitHub file update package */
@Immutable
data class GithubFile(
    /** File digest, sha:xxx */
    val digest: String?,
    /** File url for download */
    val downloadUrl: String,
    /** Filename without the extension */
    val displayName: String,
    /** Changelog, aka the commit message */
    val changeLog: String,
    /** Name of the tag, aka unique release name like vX.X.X */
    val tagName: String,
    /** Unique node id */
    val nodeId: String,
)

@Immutable
sealed class GithubAction {
    object AutoSearchForUpdate : GithubAction()
    object SearchForUpdate : GithubAction()
    object Dismiss : GithubAction()
    data class SkipThisUpdate(val file: GithubFile) : GithubAction()
    data class Update(val file: GithubFile) : GithubAction()
    data class Install(
        val file: GithubFile,
        val apkPath: String,
        val totalBytes: Long,
    ) : GithubAction()

    data class SkipUpdate(val file: GithubFile) : GithubAction()
}

data class DownloadedUpdate(
    val apkPath: String,
    val totalBytes: Long,
)

sealed class ApkInstallStatus {
    data object Launched : ApkInstallStatus()
    data object PermissionRequired : ApkInstallStatus()
    data class Failed(val error: Throwable) : ApkInstallStatus()
}

sealed class ApkInstallEvent {
    data object Success : ApkInstallEvent()
    data object Cancelled : ApkInstallEvent()
    data class Failed(val error: Throwable) : ApkInstallEvent()
}

interface AppUpdater {
    val installEvents: Flow<ApkInstallEvent>

    @Throws
    suspend fun downloadUpdate(
        url: String,
        digest: DigestPair?,
        downloadProgress: (Long, Long?) -> Unit,
    ): DownloadedUpdate

    suspend fun installUpdate(
        settings: AppSettings,
        apkPath: String,
    ): ApkInstallStatus

    fun clearDownloadedUpdates()
}

interface GithubRepository {
    @Throws
    suspend fun getRelease(
        userName: String,
        repository: String,
        contentType: String,
    ): GithubFile
}

/** The digest pair to verify that a file is correctly downloaded */
data class DigestPair(
    val algorithm: String,
    val digest: ByteArray,
) {
    companion object {
        // "sha256:XXXX" or "sha256-XXXX" -> "sha256", bytearray(XXXX)
        fun parse(digestPair: String?): DigestPair? {
            if (digestPair == null) return null

            var split = digestPair.split(":", limit = 2)
            if (split.size != 2) {
                split = digestPair.split("-", limit = 2)
            }
            if (split.size != 2) {
                return null
            }

            val digestAlgorithm = split.getOrNull(0) ?: return null
            val digestByteArray = safe { split.getOrNull(1)?.hexToByteArray() } ?: return null

            return DigestPair(digestAlgorithm, digestByteArray)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val other = other as? DigestPair ?: return false

        if (algorithm != other.algorithm) return false
        if (!digest.contentEquals(other.digest)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + digest.contentHashCode()
        return result
    }
}

/**
 * Cross-platform downloader for updates served by GitHub.
 *
 * To allow this Cross-platform behavior work we split up the UI from the Viewmodel,
 * and the Viewmodel from the installer.
 * ```
 * UI:          Renders the current viewmodel state
 * Viewmodel:   Searches for updates on GitHub
 * AppUpdater:  Downloads and installs the update from the raw GitHub url
 *
 * UI -(onAction)-> Viewmodel -(invokes)-> AppUpdater
 *   <---(state)---/        <--(callback)--/
 * ```
 * */
class GithubViewModel(
    val remoteUserName: String,
    val remoteRepository: String,
    val remoteContentType: String,
    val versionName: String,
    @Suppress("unused")
    val isDebug: Boolean,
    val settings: AppSettings,
    val updater: AppUpdater,
    val repository: GithubRepository,
) : ViewModel(), StateContainer<GithubState> by DefaultStateContainer(GithubState()),
    ActionHandler<GithubAction> {
    private val updateDispatcher = SingleActiveQuery(Dispatchers.IO)
    private var hasAutoSearched: Boolean = false

    init {
        viewModelScope.launch {
            updater.installEvents.collect { event ->
                val currentDialog = state.value.dialog ?: return@collect
                val readyState = when (val currentState = currentDialog.state) {
                    is GithubUpdateDialogState.ReadyToInstall -> currentState
                    is GithubUpdateDialogState.InstallError -> GithubUpdateDialogState.ReadyToInstall(
                        file = currentState.file,
                        apkPath = currentState.apkPath,
                        totalBytes = currentState.totalBytes,
                    )
                    else -> return@collect
                }

                when (event) {
                    ApkInstallEvent.Success -> {
                        updater.clearDownloadedUpdates()
                        updateState { copy(dialog = null) }
                    }

                    ApkInstallEvent.Cancelled -> {
                        updateState {
                            copy(
                                dialog = currentDialog.copy(
                                    isFromUser = true,
                                    state = readyState,
                                ),
                            )
                        }
                    }

                    is ApkInstallEvent.Failed -> {
                        updateState {
                            copy(
                                dialog = currentDialog.copy(
                                    isFromUser = true,
                                    state = GithubUpdateDialogState.InstallError(
                                        file = readyState.file,
                                        apkPath = readyState.apkPath,
                                        totalBytes = readyState.totalBytes,
                                        error = event.error,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onAction(action: GithubAction) {
        when (action) {
            GithubAction.SearchForUpdate -> {
                ioSafe {
                    searchForUpdate(fromUser = true)
                }
            }

            GithubAction.Dismiss -> {
                viewModelScope.launch {
                    updateDispatcher.cancel()
                    updater.clearDownloadedUpdates()
                    updateState { copy(dialog = null) }
                }
            }

            is GithubAction.SkipThisUpdate -> {
                settings.updates.skipUpdate.set(action.file.nodeId)
            }

            GithubAction.AutoSearchForUpdate -> {
                if (!hasAutoSearched && settings.updates.showAppUpdates.get()) {
                    hasAutoSearched = true
                    ioSafe {
                        searchForUpdate(fromUser = false)
                    }
                }
            }

            is GithubAction.SkipUpdate -> {
                settings.updates.skipUpdate.set(action.file.nodeId)
            }

            is GithubAction.Update -> {
                ioSafe {
                    downloadAndInstallUpdate(action.file)
                }
            }

            is GithubAction.Install -> {
                ioSafe {
                    installDownloadedApk(
                        file = action.file,
                        apkPath = action.apkPath,
                        totalBytes = action.totalBytes,
                    )
                }
            }
        }
    }

    /** Cancel the old update, and catch possible errors from the block and show as a new state */
    private suspend fun dispatchUpdate(block: /* @Throws */ suspend () -> Unit) {
        updateDispatcher.launch {
            try {
                block()
            } catch (t: Throwable) {
                // If it was canceled ignore it as we probably launched another update check
                if (!isActive) {
                    return@launch
                }
                // Otherwise we display the error
                updateState {
                    copy(dialog = dialog?.copy(state = GithubUpdateDialogState.Error(t)))
                }
            }
        }
    }

    private suspend fun downloadAndInstallUpdate(file: GithubFile) = dispatchUpdate {
        updateState {
            copy(
                dialog = GithubDialog(
                    isFromUser = true,
                    state = GithubUpdateDialogState.DownloadProgress(
                        progress = 0L,
                        total = null,
                    ),
                ),
            )
        }

        val downloaded = updater.downloadUpdate(
            url = file.downloadUrl,
            digest = DigestPair.parse(file.digest),
        ) { progress, total ->
            updateState {
                copy(
                    dialog = GithubDialog(
                        isFromUser = true,
                        state = GithubUpdateDialogState.DownloadProgress(
                            progress = progress,
                            total = total,
                        ),
                    ),
                )
            }
        }

        val readyState = GithubUpdateDialogState.ReadyToInstall(
            file = file,
            apkPath = downloaded.apkPath,
            totalBytes = downloaded.totalBytes,
        )
        updateState {
            copy(
                dialog = GithubDialog(
                    isFromUser = true,
                    state = readyState,
                ),
            )
        }

        launchApkInstall(readyState)
    }

    private suspend fun installDownloadedApk(
        file: GithubFile,
        apkPath: String,
        totalBytes: Long,
    ) = dispatchUpdate {
        val readyState = GithubUpdateDialogState.ReadyToInstall(
            file = file,
            apkPath = apkPath,
            totalBytes = totalBytes,
        )
        updateState {
            copy(
                dialog = GithubDialog(
                    isFromUser = true,
                    state = readyState,
                ),
            )
        }
        launchApkInstall(readyState)
    }

    private suspend fun launchApkInstall(readyState: GithubUpdateDialogState.ReadyToInstall) {
        when (val result = updater.installUpdate(settings = settings, apkPath = readyState.apkPath)) {
            ApkInstallStatus.Launched,
            ApkInstallStatus.PermissionRequired -> {
                // Keep ReadyToInstall visible so user can tap "Install" after granting
                // unknown-sources permission or if they cancel the system installer prompt.
            }

            is ApkInstallStatus.Failed -> {
                updateState {
                    copy(
                        dialog = GithubDialog(
                            isFromUser = true,
                            state = GithubUpdateDialogState.InstallError(
                                file = readyState.file,
                                apkPath = readyState.apkPath,
                                totalBytes = readyState.totalBytes,
                                error = result.error,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun searchForUpdate(
        fromUser: Boolean,
    ) = dispatchUpdate {
        val baseDialog = GithubDialog(
            isFromUser = fromUser,
            state = GithubUpdateDialogState.Loading
        )

        updateState {
            copy(dialog = baseDialog)
        }

        val release = repository.getRelease(
            userName = remoteUserName,
            repository = remoteRepository,
            contentType = remoteContentType
        )

        // Only prompt if the remote release is strictly newer than the installed version
        if (!isNewerVersion(release, versionName)) {
            updateState {
                copy(dialog = baseDialog.copy(state = GithubUpdateDialogState.NoUpdateFound))
            }
            return@dispatchUpdate
        }

        // If this was automated, and we have pressed "skip this update" then check the node-id
        if (!fromUser && release.nodeId == settings.updates.skipUpdate.get()) {
            updateState {
                copy(dialog = baseDialog.copy(state = GithubUpdateDialogState.NoUpdateFound))
            }
            return@dispatchUpdate
        }

        updateState {
            copy(
                dialog = baseDialog.copy(
                    state = GithubUpdateDialogState.UpdateFound(
                        file = release,
                    )
                )
            )
        }
    }

    companion object {
        private val VERSION_REGEX = Regex("""\d+(?:\.\d+)+""")

        internal fun parseVersionParts(raw: String): List<Int>? {
            val match = VERSION_REGEX.find(raw) ?: return null
            return match.value.split('.').map { it.toIntOrNull() ?: return null }
        }

        internal fun compareVersions(remote: List<Int>, local: List<Int>): Int {
            val maxSize = maxOf(remote.size, local.size)
            for (i in 0 until maxSize) {
                val r = remote.getOrElse(i) { 0 }
                val l = local.getOrElse(i) { 0 }
                if (r != l) return r.compareTo(l)
            }
            return 0
        }

        internal fun isNewerVersion(release: GithubFile, currentVersionName: String): Boolean {
            val remoteParts =
                parseVersionParts(release.tagName) ?: parseVersionParts(release.displayName)
            val localParts = parseVersionParts(currentVersionName)
            if (remoteParts != null && localParts != null) {
                return compareVersions(remoteParts, localParts) > 0
            }

            val normalizedCurrent = currentVersionName.trim().removePrefix("v")
            val normalizedDisplay = release.displayName.trim().removePrefix("v")
            val normalizedTag = release.tagName.trim().removePrefix("v")
            return normalizedDisplay != normalizedCurrent && normalizedTag != normalizedCurrent
        }
    }
}