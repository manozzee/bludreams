package com.lagradost.cloudstream3.ui.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.loadThemes
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.BiometricAuthenticator
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.BiometricCallback
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.biometricPrompt
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.deviceHasPasswordPinLock
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.promptInfo
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.startBiometricAuthentication
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.Account
import com.lagradost.cloudstream3.utils.UIHelper.enableEdgeToEdgeCompat
import com.lagradost.cloudstream3.utils.UIHelper.openActivity
import com.lagradost.cloudstream4.AppSettings
import com.lagradost.cloudstream4.compose.CloudStreamThemeAppSettings
import com.lagradost.cloudstream4.state.SearchableData
import com.lagradost.cloudstream4.state.sortByInt
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList

class AccountSelectActivity2 : FragmentActivity(), BiometricCallback {
    companion object {
        var hasLoggedIn: Boolean = false
        const val IS_FROM_MAIN_ACTIVITY = "isFromMainActivity"
        const val IS_EDITING_FROM_MAIN_ACTIVITY = "isEditingFromMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Are we editing and coming from MainActivity?
        val isEditingFromMainActivity = intent.getBooleanExtra(IS_EDITING_FROM_MAIN_ACTIVITY, false)

        // Are we invoked from the main activity to edit/switch?
        val isFromMainActivity = intent.getBooleanExtra(IS_FROM_MAIN_ACTIVITY, false)

        /**
         * Sometimes we start this activity when we have already logged in
         * For example when using cloudstreamsearch://
         *
         * In those cases we want to just go to the main activity instantly
         */
        if (hasLoggedIn && !isEditingFromMainActivity && !isFromMainActivity) {
            navigateToMainActivity()
            return
        }

        /**
         * CommonActivity.init : loads the correct locale for default_account
         * loadThemes : loads the correct color on the toast when logging in
         */
        CommonActivity.init(this)
        loadThemes(this)
        enableEdgeToEdgeCompat()

        val availableAvatars = AvatarCatalog.getAvatars(this)
        val defaultAvatarUri = availableAvatars.firstOrNull()

        val persistedAccounts = DataStoreHelper.accounts
        val migratedAccounts = persistedAccounts
            .map { AvatarCatalog.migrateAccount(this, it) }
            .toTypedArray()
        if (!migratedAccounts.contentEquals(persistedAccounts)) {
            DataStoreHelper.accounts = migratedAccounts
        }

        val defaultName = getString(R.string.default_account)
        val defaultAccount = Account(
            keyIndex = 0,
            name = defaultName,
            defaultImageIndex = 0,
            avatar = defaultAvatarUri,
        )
        val initialState = AccountState(
            canEditAccount = isEditingFromMainActivity,
            canNavigateBack = isFromMainActivity || isEditingFromMainActivity,
            availableAvatars = availableAvatars.toImmutableList(),
            lastLoginKeyIndex = DataStoreHelper.selectedKeyIndex,
            accounts = SearchableData.from(
                /** We must always have a default account even if DataStoreHelper.accounts.size = 0 */
                data = persistentMapOf(defaultAccount.keyIndex to defaultAccount)
                    .puttingAll(migratedAccounts.associateBy { it.keyIndex }),
                sortedBy = sortByInt { it.keyIndex },
            ),
        )

        val settings = AppSettings(this)
        val skipAccountSelection = settings.security.skipAccountSelection.get()

        /** Don't show account selection if there is only one account that exists */
        if (
            !isFromMainActivity &&
            !isEditingFromMainActivity &&
            (skipAccountSelection || initialState.accounts.data.size == 1)
        ) {
            /** We must have an account without a pin */
            val currentAccount = initialState.accounts.data[initialState.lastLoginKeyIndex]
            if (currentAccount != null && currentAccount.lockPin == null) {
                DataStoreHelper.setAccount(
                    currentAccount,
                    showToast = initialState.accounts.data.size > 1,
                )
                navigateToMainActivity()
                return
            }
        }

        /**
         * If this app is locked with biometrics,
         * then require that on a startup that is not skipped!
         */
        if (
            !isFromMainActivity &&
            !isEditingFromMainActivity &&
            isLayout(PHONE) &&
            settings.security.biometrics.get() &&
            deviceHasPasswordPinLock(this)
        ) {
            startBiometricAuthentication(
                this,
                R.string.biometric_authentication_title,
                false,
            )
            promptInfo?.let { prompt ->
                biometricPrompt?.authenticate(prompt)
            }
        }

        setContent {
            val viewModel = viewModel<AccountViewModel2> {
                AccountViewModel2(
                    defaultAccount = defaultAccount,
                    initialState = initialState,
                )
            }
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.loggedInWith) {
                if (state.loggedInWith != null) {
                    if ((isFromMainActivity || isEditingFromMainActivity) && !isTaskRoot) {
                        finish()
                    } else {
                        navigateToMainActivity()
                    }
                }
            }

            CloudStreamThemeAppSettings {
                AccountScreen.Content(
                    state = state,
                    onAction = viewModel::onAction,
                    onBack = { finish() },
                )
            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun navigateToMainActivity() {
        hasLoggedIn = true
        openActivity(MainActivity::class.java, baseIntent = intent)
        finish()
    }

    override fun onAuthenticationSuccess() {
        Log.i(BiometricAuthenticator.TAG, "Authentication successful in AccountSelectActivity")
    }

    override fun onAuthenticationError() {
        finish()
    }
}
