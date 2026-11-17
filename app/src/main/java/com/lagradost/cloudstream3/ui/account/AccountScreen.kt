package com.lagradost.cloudstream3.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.settings.MergedAppIconPreview
import com.lagradost.cloudstream3.ui.settings.rememberActiveAppIcon
import com.lagradost.cloudstream3.utils.DataStoreHelper.Account
import com.lagradost.cloudstream4.state.SearchableData
import com.lagradost.cloudstream4.state.sortByInt
import com.lagradost.cloudstream4.theme.CloudStreamPreviewTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.delay

@PreviewLightDark
@Composable
private fun WhosWatchingPreview() {
    CloudStreamPreviewTheme {
        AccountScreen.Content(
            state = AccountState(
                canEditAccount = false,
                canNavigateBack = true,
                availableAvatars = persistentListOf(
                    "file:///android_asset/avatars/01_luffy.webp",
                    "file:///android_asset/avatars/02_nami.webp",
                    "file:///android_asset/avatars/03_niko_robin.webp",
                    "file:///android_asset/avatars/04_yamato.webp",
                ),
                accounts = SearchableData.from(
                    data = mapOf(
                        0 to Account(0, "Luffy", defaultImageIndex = 0, avatar = "file:///android_asset/avatars/01_luffy.webp"),
                        1 to Account(1, "Nami", defaultImageIndex = 1, avatar = "file:///android_asset/avatars/02_nami.webp"),
                        2 to Account(2, "Robin", defaultImageIndex = 2, avatar = "file:///android_asset/avatars/03_niko_robin.webp", lockPin = "1234"),
                    ).toPersistentMap(),
                    sortedBy = sortByInt { it.keyIndex },
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

private sealed interface AccountScreenDestination {
    data object Select : AccountScreenDestination
    data class Edit(val account: Account, val isExisting: Boolean) : AccountScreenDestination
    data class PinPrompt(val account: Account) : AccountScreenDestination
}

object AccountScreen {

    @Composable
    fun Content(
        state: AccountState,
        onAction: (AccountAction) -> Unit,
        onBack: () -> Unit = {},
    ) {
        val destination: AccountScreenDestination = when {
            state.editAccount != null -> AccountScreenDestination.Edit(
                account = state.editAccount,
                isExisting = state.accounts.data.containsKey(state.editAccount.keyIndex),
            )
            state.pendingPinAccount != null -> AccountScreenDestination.PinPrompt(
                account = state.pendingPinAccount,
            )
            else -> AccountScreenDestination.Select
        }

        BackHandler(enabled = destination !is AccountScreenDestination.Select || state.canNavigateBack) {
            when (destination) {
                is AccountScreenDestination.Edit -> onAction(AccountAction.EditAccount(null))
                is AccountScreenDestination.PinPrompt -> onAction(AccountAction.RequestPinForLogin(null))
                AccountScreenDestination.Select -> if (state.canNavigateBack) onBack()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = destination,
                contentKey = { dest ->
                    when (dest) {
                        AccountScreenDestination.Select -> "select"
                        is AccountScreenDestination.Edit -> "edit_${dest.account.keyIndex}"
                        is AccountScreenDestination.PinPrompt -> "pin_${dest.account.keyIndex}"
                    }
                },
                transitionSpec = {
                    val enteringSubScreen =
                        initialState is AccountScreenDestination.Select &&
                            targetState !is AccountScreenDestination.Select
                    val returningToSelect =
                        initialState !is AccountScreenDestination.Select &&
                            targetState is AccountScreenDestination.Select

                    when {
                        enteringSubScreen -> {
                            (slideInHorizontally(animationSpec = tween(280)) { it / 3 } +
                                fadeIn(animationSpec = tween(280))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(280)) { -it / 4 } +
                                    fadeOut(animationSpec = tween(220)))
                        }
                        returningToSelect -> {
                            (slideInHorizontally(animationSpec = tween(280)) { -it / 3 } +
                                fadeIn(animationSpec = tween(280))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(280)) { it / 3 } +
                                    fadeOut(animationSpec = tween(220)))
                        }
                        else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    }
                },
                label = "account_screen_transition",
            ) { currentDest ->
                when (currentDest) {
                    AccountScreenDestination.Select -> {
                        WhosWatchingScreen(
                            state = state,
                            onAction = onAction,
                            onBack = onBack,
                        )
                    }
                    is AccountScreenDestination.Edit -> {
                        EditProfileScreen(
                            account = currentDest.account,
                            isExisting = currentDest.isExisting,
                            availableAvatars = state.availableAvatars,
                            onAction = onAction,
                        )
                    }
                    is AccountScreenDestination.PinPrompt -> {
                        InlinePinPromptScreen(
                            account = currentDest.account,
                            onCancel = { onAction(AccountAction.RequestPinForLogin(null)) },
                            onVerified = { onAction(AccountAction.LoginWithAccount(currentDest.account.keyIndex)) },
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun WhosWatchingScreen(
        state: AccountState,
        onAction: (AccountAction) -> Unit,
        onBack: () -> Unit,
    ) {
        val context = LocalContext.current
        val isPreview = LocalInspectionMode.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar reserving clean vertical rhythm; back button shown when opened from settings/menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.canNavigateBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Centered subtle app mark + "Who's Watching?"
                if (isPreview) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                } else {
                    val activeIcon = rememberActiveAppIcon()
                    MergedAppIconPreview(
                        icon = activeIcon,
                        modifier = Modifier
                            .size(36.dp)
                            .alpha(0.92f),
                        foregroundInset = 2.dp,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Who's Watching?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Centered 2-column grid of circular avatars
                FlowRow(
                    modifier = Modifier.widthIn(max = 320.dp),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    state.accounts.sorted.forEach { keyIndex ->
                        val account = state.accounts.data[keyIndex] ?: return@forEach
                        val resolvedAvatar = remember(account, state.availableAvatars) {
                            AvatarCatalog.resolveAvatar(context, account)
                                ?: state.availableAvatars.firstOrNull()
                        }

                        ProfileGridItem(
                            name = account.name,
                            avatarModel = resolvedAvatar,
                            isEditMode = state.canEditAccount,
                            isLocked = account.lockPin != null,
                            onClick = {
                                if (state.canEditAccount) {
                                    onAction(AccountAction.EditAccount(account))
                                } else if (account.lockPin != null) {
                                    onAction(AccountAction.RequestPinForLogin(account))
                                } else {
                                    onAction(AccountAction.LoginWithAccount(account.keyIndex))
                                }
                            },
                        )
                    }

                    AnimatedVisibility(
                        visible = state.canEditAccount || state.accounts.data.isEmpty(),
                        enter = scaleIn(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        AddProfileGridItem(
                            onClick = { onAction(AccountAction.NewAccount) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom pill-shaped Manage Profiles / Done toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { onAction(AccountAction.SetEditMode(!state.canEditAccount)) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    AnimatedContent(
                        targetState = state.canEditAccount,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                        },
                        label = "manage_toggle_label",
                    ) { isEditing ->
                        Text(
                            text = if (isEditing) "Done" else "Manage Profiles",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ProfileGridItem(
        name: String,
        avatarModel: String?,
        isEditMode: Boolean,
        isLocked: Boolean,
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "profile_press_scale",
        )
        val manageScale by animateFloatAsState(
            targetValue = if (isEditMode) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "profile_manage_scale",
        )

        Column(
            modifier = Modifier
                .width(116.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        val combined = pressScale * manageScale
                        scaleX = combined
                        scaleY = combined
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = avatarModel,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                if (isLocked && !isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.video_locked),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = isEditMode,
                    enter = scaleIn(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    @Composable
    private fun AddProfileGridItem(
        onClick: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "add_profile_press_scale",
        )

        Column(
            modifier = Modifier
                .width(116.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = "Add Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Add Profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    @Composable
    private fun EditProfileScreen(
        account: Account,
        isExisting: Boolean,
        availableAvatars: List<String>,
        onAction: (AccountAction) -> Unit,
    ) {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val focusManager = LocalFocusManager.current

        val defaultAvatar = availableAvatars.firstOrNull()
        val initialResolved = remember(account.keyIndex) {
            AvatarCatalog.resolveAvatar(context, account) ?: defaultAvatar
        }
        val initialPreset = remember(account.keyIndex) {
            if (initialResolved != null && initialResolved in availableAvatars) {
                initialResolved
            } else {
                defaultAvatar
            }
        }
        val initialCustomUrl = remember(account.keyIndex) {
            if (initialResolved != null && !AvatarCatalog.isBundledAssetUri(initialResolved)) {
                initialResolved
            } else {
                ""
            }
        }

        var nameInput by remember(account.keyIndex) { mutableStateOf(account.name) }
        var customLinkInput by remember(account.keyIndex) { mutableStateOf(initialCustomUrl) }
        var selectedPresetUri by remember(account.keyIndex) { mutableStateOf(initialPreset) }
        var showDeleteConfirm by remember(account.keyIndex) { mutableStateOf(false) }
        var failedCustomUrl by remember(account.keyIndex) { mutableStateOf<String?>(null) }

        val trimmedCustomLink = customLinkInput.trim()
        val isCustomLinkActive = trimmedCustomLink.isNotEmpty()
        val isCustomImageError = isCustomLinkActive && failedCustomUrl == trimmedCustomLink
        val fallbackAvatar = selectedPresetUri ?: defaultAvatar
        val previewModel = if (isCustomLinkActive && !isCustomImageError) {
            trimmedCustomLink
        } else {
            fallbackAvatar
        }

        val isDeletable = isExisting && account.keyIndex != 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAction(AccountAction.EditAccount(null)) }) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = if (isExisting) "Edit Profile" else "Create Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isDeletable) {
                    IconButton(onClick = { showDeleteConfirm = !showDeleteConfirm }) {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Inline delete confirmation prompt (zero popups)
            AnimatedVisibility(
                visible = isDeletable && showDeleteConfirm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Delete \"${nameInput.trim().ifEmpty { account.name }}\"?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            onAction(AccountAction.DeleteAccount(account.keyIndex))
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Scrollable form content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Live Hero Avatar Preview (112.dp, 2.5.dp primary ring, 250ms Crossfade)
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(
                        targetState = previewModel,
                        animationSpec = tween(durationMillis = 250),
                        label = "hero_avatar_crossfade",
                    ) { model ->
                        AsyncImage(
                            model = model,
                            contentDescription = nameInput.ifBlank { "Profile avatar" },
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                if (model == trimmedCustomLink && isCustomLinkActive) {
                                    failedCustomUrl = null
                                }
                            },
                            onError = {
                                if (model == trimmedCustomLink && isCustomLinkActive) {
                                    failedCustomUrl = trimmedCustomLink
                                }
                            },
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCustomImageError,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.error_24px),
                                contentDescription = "Invalid image URL",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 1. Profile Name flat pill field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Profile Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                    )
                    FlatPillInputField(
                        value = nameInput,
                        onValueChange = { newText ->
                            nameInput = if (newText.length <= 100) newText else newText.substring(0, 100)
                        },
                        placeholder = "Enter profile name",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Custom Image Link flat pill field with Paste / Clear trailing button
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Custom Image Link",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                    )
                    FlatPillInputField(
                        value = customLinkInput,
                        onValueChange = { newText ->
                            customLinkInput = newText
                            if (newText.trim() != failedCustomUrl) {
                                failedCustomUrl = null
                            }
                        },
                        placeholder = "https://...",
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Uri,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                        trailingContent = {
                            if (customLinkInput.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val clipText = clipboardManager.getText()?.text?.trim()
                                            if (!clipText.isNullOrEmpty()) {
                                                customLinkInput = clipText
                                                failedCustomUrl = null
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.copy_all_24px),
                                        contentDescription = "Paste",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Paste",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        customLinkInput = ""
                                        failedCustomUrl = null
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Close,
                                        contentDescription = "Clear link",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                    )

                    AnimatedVisibility(visible = isCustomImageError) {
                        Text(
                            text = "Couldn't load image from this link. Showing fallback avatar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Built-In Avatar 4-column Grid (Directly Visible)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Built-in Avatars",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
                    )

                    val rows = remember(availableAvatars) { availableAvatars.chunked(4) }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        rows.forEach { rowAvatars ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                rowAvatars.forEach { avatarUri ->
                                    val isSelected = !isCustomLinkActive && selectedPresetUri == avatarUri
                                    PresetAvatarTile(
                                        avatarUri = avatarUri,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedPresetUri = avatarUri
                                            customLinkInput = ""
                                            failedCustomUrl = null
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(4 - rowAvatars.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Pinned Save pill button at bottom with navigationBarsPadding() and imePadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                val canSave = nameInput.isNotBlank()
                Button(
                    onClick = {
                        val finalAvatar = if (isCustomLinkActive && !isCustomImageError) {
                            trimmedCustomLink
                        } else {
                            selectedPresetUri ?: defaultAvatar
                        }
                        val isBundled = AvatarCatalog.isBundledAssetUri(finalAvatar)
                        val bundledIndex = if (finalAvatar != null) {
                            availableAvatars.indexOf(finalAvatar).coerceAtLeast(0)
                        } else {
                            0
                        }

                        onAction(
                            AccountAction.SaveAccount(
                                account.copy(
                                    name = nameInput.trim(),
                                    avatar = finalAvatar,
                                    customImage = if (isBundled) null else finalAvatar,
                                    defaultImageIndex = if (isBundled) bundledIndex else account.defaultImageIndex,
                                )
                            )
                        )
                    },
                    enabled = canSave,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    @Composable
    private fun PresetAvatarTile(
        avatarUri: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        var isBouncing by remember { mutableStateOf(false) }

        LaunchedEffect(isBouncing) {
            if (isBouncing) {
                delay(80)
                isBouncing = false
            }
        }

        val scale by animateFloatAsState(
            targetValue = if (isPressed || isBouncing) 0.90f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "preset_avatar_bounce",
        )

        Box(
            modifier = modifier
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isSelected) {
                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        isBouncing = true
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = avatarUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
    }

    @Composable
    private fun FlatPillInputField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
            androidx.compose.ui.text.input.VisualTransformation.None,
        trailingContent: (@Composable () -> Unit)? = null,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 20.dp, end = if (trailingContent != null) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }

    @Composable
    private fun InlinePinPromptScreen(
        account: Account,
        onCancel: () -> Unit,
        onVerified: () -> Unit,
    ) {
        val context = LocalContext.current
        val expectedPin = account.lockPin.orEmpty()
        var pinInput by remember(account.keyIndex) { mutableStateOf("") }
        var hasError by remember(account.keyIndex) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AsyncImage(
                    model = AvatarCatalog.resolveAvatar(context, account),
                    contentDescription = account.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.enter_pin_with_name, account.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlatPillInputField(
                    value = pinInput,
                    onValueChange = { newText ->
                        hasError = false
                        if (newText.length >= expectedPin.length && expectedPin.isNotEmpty()) {
                            if (newText == expectedPin) {
                                onVerified()
                            } else {
                                pinInput = ""
                                hasError = true
                            }
                        } else {
                            pinInput = newText
                        }
                    },
                    placeholder = stringResource(R.string.pin),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.NumberPassword,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                )

                AnimatedVisibility(visible = hasError) {
                    Text(
                        text = stringResource(R.string.pin_error_incorrect),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}
