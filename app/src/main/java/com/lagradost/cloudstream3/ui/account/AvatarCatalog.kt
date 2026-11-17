package com.lagradost.cloudstream3.ui.account

import android.content.Context
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.DataStoreHelper

object AvatarCatalog {
    const val AVATARS_ASSET_DIR = "avatars"
    const val ASSET_URI_PREFIX = "file:///android_asset/$AVATARS_ASSET_DIR/"

    private val supportedExtensions = setOf("webp", "png", "jpg", "jpeg")

    @Volatile
    private var cachedAvatars: List<String>? = null

    fun getAvatars(context: Context?): List<String> {
        cachedAvatars?.let { return it }
        val ctx = context?.applicationContext ?: context ?: return emptyList()

        val loaded = try {
            ctx.assets.list(AVATARS_ASSET_DIR)
                ?.asSequence()
                ?.filter { fileName ->
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    ext in supportedExtensions
                }
                ?.sorted()
                ?.map { fileName -> "$ASSET_URI_PREFIX$fileName" }
                ?.toList()
                .orEmpty()
        } catch (t: Throwable) {
            logError(t)
            emptyList()
        }

        if (loaded.isNotEmpty()) {
            cachedAvatars = loaded
        }
        return loaded
    }

    fun defaultAvatar(context: Context?): String? {
        return getAvatars(context).firstOrNull()
    }

    fun isBundledAssetUri(uri: String?): Boolean {
        return !uri.isNullOrBlank() && uri.startsWith(ASSET_URI_PREFIX)
    }

    fun isWebUrl(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        val trimmed = uri.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
    }

    /**
     * Resolves a profile's unified avatar URI (bundled asset URI or custom URL/URI).
     * Any profile whose legacy drawable or old asset no longer exists falls back to index 0
     * of the dynamically scanned `assets/avatars/` catalog.
     */
    fun resolveAvatar(context: Context?, account: DataStoreHelper.Account): String? {
        val avatars = getAvatars(context)
        val fallback = avatars.firstOrNull()

        val candidate = account.avatar?.trim()?.takeIf { it.isNotEmpty() }
            ?: account.customImage?.trim()?.takeIf { it.isNotEmpty() }

        if (candidate != null) {
            return if (isBundledAssetUri(candidate)) {
                if (avatars.isEmpty() || candidate in avatars) candidate else fallback
            } else {
                candidate
            }
        }

        return fallback
    }

    fun migrateAccount(context: Context?, account: DataStoreHelper.Account): DataStoreHelper.Account {
        val avatars = getAvatars(context)
        val resolved = resolveAvatar(context, account) ?: return account
        val bundledIndex = avatars.indexOf(resolved)

        return if (isBundledAssetUri(resolved)) {
            account.copy(
                avatar = resolved,
                customImage = null,
                defaultImageIndex = if (bundledIndex >= 0) bundledIndex else 0,
            )
        } else {
            account.copy(
                avatar = resolved,
                customImage = resolved,
            )
        }
    }
}
