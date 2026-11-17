package com.lagradost.cloudstream3.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.lagradost.cloudstream3.ui.settings.ApkUpdater
import com.lagradost.cloudstream3.utils.getSafeParcelableExtra

/** https://medium.com/@solrudev/painless-building-of-an-android-package-installer-app-d5a09b5df432 */
class PackageInstallerStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val userAction = intent.getSafeParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (userAction == null) {
                    ApkUpdater.onPackageInstallerStatus(
                        status = PackageInstaller.STATUS_FAILURE,
                        message = "Missing package installer confirmation intent",
                    )
                    return
                }
                userAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(userAction)
                } catch (t: Throwable) {
                    ApkUpdater.onPackageInstallerStatus(
                        status = PackageInstaller.STATUS_FAILURE,
                        message = t.message ?: "Unable to launch package installer confirmation",
                        throwable = t,
                    )
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                ApkUpdater.onPackageInstallerStatus(status = status, message = null)
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                ApkUpdater.onPackageInstallerStatus(status = status, message = message)
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS =
            "com.lagradost.cloudstream3.ACTION_PACKAGE_INSTALL_STATUS"
    }
}
