package com.lagradost.cloudstream3.ui.settings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.receivers.PackageInstallerStatusReceiver
import com.lagradost.cloudstream4.AppSettings
import com.lagradost.cloudstream4.viewmodels.ApkInstallEvent
import com.lagradost.cloudstream4.viewmodels.ApkInstallStatus
import com.lagradost.cloudstream4.viewmodels.AppUpdater
import com.lagradost.cloudstream4.viewmodels.DigestPair
import com.lagradost.cloudstream4.viewmodels.DownloadedUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestException
import java.security.MessageDigest

object ApkUpdater : AppUpdater {
    private const val APP_UPDATE_NAME = "BluDreams"
    private const val APP_UPDATE_SUFFIX = "apk"

    private val _installEvents = MutableSharedFlow<ApkInstallEvent>(extraBufferCapacity = 8)
    override val installEvents: Flow<ApkInstallEvent> = _installEvents.asSharedFlow()

    private fun resolveContext(): Context {
        return CommonActivity.activity
            ?: CloudStreamApp.context
            ?: throw ErrorLoadingException("No activity or application context found")
    }

    @Throws
    override suspend fun downloadUpdate(
        url: String,
        digest: DigestPair?,
        downloadProgress: (Long, Long?) -> Unit,
    ): DownloadedUpdate = withContext(Dispatchers.IO) {
        val context = resolveContext()
        clearDownloadedUpdates()

        val cacheDir = context.cacheDir
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val downloadedFile = File.createTempFile(
            "$APP_UPDATE_NAME-",
            ".$APP_UPDATE_SUFFIX",
            cacheDir,
        )

        try {
            val request = app.get(url)
            val requestSize = request.size
            request.body.use { body ->
                val length = requestSize ?: body.contentLength().takeIf { it > 0L }
                val readStream = body.byteStream()
                downloadedFile.outputStream().use { writeStream ->
                    transfer(
                        writeStream = writeStream,
                        readStream = readStream,
                        length = length,
                        downloadProgress = downloadProgress,
                        digest = digest,
                    )
                }
            }
            val finalSize = downloadedFile.length()
            if (finalSize <= 0L) {
                throw IllegalStateException("Downloaded APK file is empty")
            }
            DownloadedUpdate(
                apkPath = downloadedFile.absolutePath,
                totalBytes = finalSize,
            )
        } catch (t: Throwable) {
            safe { downloadedFile.delete() }
            throw t
        }
    }

    override suspend fun installUpdate(
        settings: AppSettings,
        apkPath: String,
    ): ApkInstallStatus {
        val context = CommonActivity.activity ?: resolveContext()
        val apkFile = File(apkPath)
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return ApkInstallStatus.Failed(
                FileNotFoundException("Downloaded APK file could not be found"),
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return withContext(Dispatchers.Main) {
                try {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri(),
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    ApkInstallStatus.PermissionRequired
                } catch (t: Throwable) {
                    ApkInstallStatus.Failed(t)
                }
            }
        }

        return when (settings.updates.apkInstaller.get()) {
            0 -> installWithPackageInstaller(context, apkFile)
            else -> installWithLegacyIntent(context, apkFile)
        }
    }

    override fun clearDownloadedUpdates() {
        val context = CommonActivity.activity ?: CloudStreamApp.context ?: return
        context.cacheDir.listFiles()?.filter {
            it.name.startsWith(APP_UPDATE_NAME) && it.extension == APP_UPDATE_SUFFIX
        }?.forEach { file ->
            safe { file.delete() }
        }
    }

    internal fun onPackageInstallerStatus(
        status: Int,
        message: String?,
        throwable: Throwable? = null,
    ) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                _installEvents.tryEmit(ApkInstallEvent.Success)
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _installEvents.tryEmit(ApkInstallEvent.Cancelled)
            }

            else -> {
                val detail = buildString {
                    append("PackageInstaller failed (status=")
                    append(status)
                    append(')')
                    if (!message.isNullOrBlank()) {
                        append(": ")
                        append(message)
                    }
                }
                _installEvents.tryEmit(
                    ApkInstallEvent.Failed(throwable ?: IllegalStateException(detail)),
                )
            }
        }
    }

    /** https://medium.com/@solrudev/painless-building-of-an-android-package-installer-app-d5a09b5df432 */
    @SuppressLint("RequestInstallPackagesPolicy")
    private suspend fun installWithPackageInstaller(
        context: Context,
        apkFile: File,
    ): ApkInstallStatus = withContext(Dispatchers.IO) {
        var sessionId: Int? = null
        val packageInstaller = context.packageManager.packageInstaller
        try {
            val apkSize = apkFile.length()
            val installParams =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setRequireUserAction(
                                PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED,
                            )
                        }
                        if (apkSize > 0L) {
                            setSize(apkSize)
                        }
                    }

            sessionId = packageInstaller.createSession(installParams)
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite(
                    "${context.packageName}.apk",
                    0,
                    if (apkSize > 0L) apkSize else -1L,
                ).use { writeStream ->
                    apkFile.inputStream().use { inputStream ->
                        inputStream.copyTo(writeStream, DEFAULT_BUFFER_SIZE)
                    }
                    session.fsync(writeStream)
                }

                val receiverIntent = Intent(
                    context,
                    PackageInstallerStatusReceiver::class.java,
                ).apply {
                    action = PackageInstallerStatusReceiver.ACTION_INSTALL_STATUS
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val receiverPendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    receiverIntent,
                    flags,
                )

                session.commit(receiverPendingIntent.intentSender)
            }
            ApkInstallStatus.Launched
        } catch (t: Throwable) {
            sessionId?.let { id ->
                safe { packageInstaller.abandonSession(id) }
            }
            ApkInstallStatus.Failed(t)
        }
    }

    private suspend fun installWithLegacyIntent(
        context: Context,
        apkFile: File,
    ): ApkInstallStatus = withContext(Dispatchers.Main) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            context.startActivity(installIntent)
            ApkInstallStatus.Launched
        } catch (t: Throwable) {
            ApkInstallStatus.Failed(t)
        }
    }

    /**
     * Write the "readStream" to the "writeStream", while notifying the "downloadProgress" and
     * calculating the digest
     *
     * ------------------------------------------------------------------------------------------
     *
     * throws a DigestException if the Digest can be calculated (non-null + correct algorithm),
     * and is mismatched
     *
     * throws a CancellationException if canceled, but may not be "instant" if the read is blocking
     *
     * throws IOException and similar for reading
     *
     * ------------------------------------------------------------------------------------------
     *
     * In case of crashes from the digest, we ignore it as we do not want to "block" someone from
     * updating if they got a broken OS without the desired algorithm or implementation.
     *
     * This is because a malicious CDN should not be able to "crash" the algorithm, but a broken
     * OS will. And recovering from a broken OS by ignoring it is better than refusing it.
     * */
    @Throws
    suspend fun transfer(
        writeStream: OutputStream,
        readStream: InputStream,
        length: Long?,
        downloadProgress: (Long, Long?) -> Unit,
        digest: DigestPair?,
    ) = withContext(Dispatchers.IO) {
        val md = digest?.algorithm?.let { digestAlgorithm ->
            safe {
                MessageDigest.getInstance(digestAlgorithm)
            }
        }

        val context = currentCoroutineContext()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        var transferred: Long = 0
        while ((readStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                .also { read = it }) >= 0
        ) {
            writeStream.write(buffer, 0, read)

            safe {
                md?.update(buffer, 0, read)
            }

            transferred += read.toLong()
            downloadProgress(transferred, length)
            context.ensureActive()
        }
        writeStream.flush()

        val check = safe { md?.digest() }
        if (check != null && !check.contentEquals(digest?.digest)) {
            throw DigestException("Mismatched digest on transfer ${digest?.algorithm} : ${check.toHexString()} / ${digest?.digest?.toHexString()}")
        }
    }

    /**
     * Returns true if we have a non-null digest that matches a digest on the specified existing file
     * */
    suspend fun isFileDigest(file: File, digest: DigestPair?): Boolean {
        if (digest == null) {
            return false
        }
        try {
            val fileDigest = calculateDigest(
                digestAlgorithm = digest.algorithm,
                readStream = file.inputStream()
            )
            return fileDigest != null && digest.digest.contentEquals(fileDigest)
        } catch (_: Throwable) {
            // File not found/security/read issues, not very useful errors
        }
        return false
    }

    /**
     * Calculates the digest of a stream with the specified algorithm, use transfer instead if
     * you already download something.
     *
     * Will throw read errors
     * Will return null if the algorithm is null or does not exist/has some error
     * */
    @Throws
    suspend fun calculateDigest(digestAlgorithm: String?, readStream: InputStream) =
        withContext(Dispatchers.IO) {
            if (digestAlgorithm == null) return@withContext null

            val md =
                safe {
                    MessageDigest.getInstance(digestAlgorithm)
                } ?: return@withContext null

            val context = currentCoroutineContext()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while ((readStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)
                    .also { read = it }) >= 0
            ) {
                try {
                    md.update(buffer, 0, read)
                } catch (_: Throwable) {
                    return@withContext null
                }
                context.ensureActive()
            }
            return@withContext safe { md.digest() }
        }
}
