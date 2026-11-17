package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import com.lagradost.cloudstream3.ui.player.source_priority.QualityDataHelper
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesScreen
import com.lagradost.cloudstream4.compose.DeviceLayout
import com.lagradost.cloudstream4.compose.EMULATOR
import com.lagradost.cloudstream4.compose.PHONE
import com.lagradost.cloudstream4.compose.TV
import com.lagradost.cloudstream4.rememberAppSettings
import com.mihon.presentation.LocalBackPress
import com.mihon.presentation.settings.collectAsState

private val SelectionRowShape = RoundedCornerShape(LiquidGlassTokens.Radius.lg)
private val SecondaryPillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

// ============================================================================
// 1. Unified Selection / Confirmation Popup (Sort, Encoding, Year, Lang,
//    Set Default Multi-Choice, Quality Help, Torrent Acceptance)
// ============================================================================

@Composable
fun PlayerSelectionPopupDialog(
    state: PlayerSelectionPopupState?,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onSingleSelect: (Int) -> Unit,
    onMultiApply: (Set<Int>) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = state != null
    val activeState = remember(state) { state } ?: return

    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled

    CenteredDarkGlassOverlay(
        visible = visible,
        onDismiss = {
            if (activeState.cancelable) {
                onDismiss()
            }
        },
        modifier = modifier,
    ) { clickGuardModifier ->
        val cardWidth = (maxWidth * 0.88f).coerceAtMost(glassTokens.components.dialogMaxWidth)
        val maxContentHeight = maxHeight * 0.65f

        FloatingCardWrapper(
            backdrop = backdrop,
            modifier = Modifier
                .widthIn(max = cardWidth)
                .fillMaxWidth()
                .then(clickGuardModifier),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(glassTokens.spacing.dialogPadding),
                verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s12),
            ) {
                activeState.title?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.liquidGlassTypeScale.titleMd.copy(
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = glassTokens.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                when (val kind = activeState.kind) {
                    is PlayerSelectionPopupKind.SingleChoice -> {
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeState.token, kind.selectedIndex) {
                            if (kind.selectedIndex in kind.items.indices) {
                                listState.scrollToItem(kind.selectedIndex)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                        )

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxContentHeight)
                                .secondaryEdgeScrollFade(maskColor),
                            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
                        ) {
                            items(
                                count = kind.items.size,
                                key = { idx -> "sc_$idx" },
                            ) { idx ->
                                val selected = idx == kind.selectedIndex
                                SecondarySelectableRow(
                                    label = kind.items[idx],
                                    selected = selected,
                                    onClick = { onSingleSelect(idx) },
                                )
                            }
                        }

                        if (activeState.cancelable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(LiquidGlassTokens.Border.thin)
                                    .background(PlayerGlassWhiteDivider),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                FrostedDialogActionPill(
                                    label = stringResource(R.string.sort_cancel),
                                    isPrimary = false,
                                    onClick = onDismiss,
                                )
                            }
                        }
                    }

                    is PlayerSelectionPopupKind.MultiChoice -> {
                        var selectedIndices by remember(activeState.token) {
                            mutableStateOf(kind.selectedIndices)
                        }
                        val listState = rememberLazyListState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                        )

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxContentHeight)
                                .secondaryEdgeScrollFade(maskColor),
                            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
                        ) {
                            items(
                                count = kind.items.size,
                                key = { idx -> "mc_$idx" },
                            ) { idx ->
                                val checked = idx in selectedIndices
                                SecondarySelectableRow(
                                    label = kind.items[idx],
                                    selected = checked,
                                    onClick = {
                                        selectedIndices = if (checked) {
                                            selectedIndices - idx
                                        } else {
                                            selectedIndices + idx
                                        }
                                    },
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FrostedDialogActionPill(
                                label = stringResource(R.string.sort_cancel),
                                isPrimary = false,
                                onClick = onDismiss,
                            )
                            Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
                            FrostedDialogActionPill(
                                label = stringResource(R.string.sort_apply),
                                isPrimary = true,
                                onClick = { onMultiApply(selectedIndices) },
                            )
                        }
                    }

                    is PlayerSelectionPopupKind.MessageConfirm -> {
                        Text(
                            text = kind.message,
                            style = MaterialTheme.liquidGlassTypeScale.bodyMd,
                            color = glassTokens.colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxContentHeight)
                                .verticalScroll(rememberScrollState()),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            kind.dismissLabel?.takeIf { it.isNotBlank() }?.let { dismissText ->
                                FrostedDialogActionPill(
                                    label = dismissText,
                                    isPrimary = false,
                                    onClick = onDismiss,
                                )
                                Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
                            }
                            FrostedDialogActionPill(
                                label = kind.confirmLabel,
                                isPrimary = true,
                                onClick = onConfirm,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. Quality Profile, Source Priority Editor & Profile Settings
// ============================================================================

@Composable
fun PlayerQualityProfileDialog(
    visible: Boolean,
    state: PlayerQualityProfileDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onUseProfile: () -> Unit,
    onSelectProfile: (Int) -> Unit,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onEditNameChanged: (String) -> Unit,
    onSourcePriorityStep: (String, Int) -> Unit,
    onQualityPriorityStep: (String, Int) -> Unit,
    onEditSave: () -> Unit,
    onEditClose: () -> Unit,
    onHelpClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onHideNegativeChanged: (Boolean) -> Unit,
    onHideErrorChanged: (Boolean) -> Unit,
    onSettingsApply: () -> Unit,
    onSettingsCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeDismiss = when {
        state.isEditingProfileSettings -> onSettingsCancel
        state.editingProfileId != null -> onEditClose
        else -> onDismiss
    }

    SecondaryFullSheetOverlay(
        visible = visible,
        backdrop = backdrop,
        onDismiss = activeDismiss,
        modifier = modifier,
    ) {
        when {
            state.isEditingProfileSettings -> {
                ProfileSettingsContent(
                    hideNegativeSources = state.hideNegativeSources,
                    hideErrorSources = state.hideErrorSources,
                    onHideNegativeChanged = onHideNegativeChanged,
                    onHideErrorChanged = onHideErrorChanged,
                    onCancel = onSettingsCancel,
                    onApply = onSettingsApply,
                )
            }

            state.editingProfileId != null -> {
                SourcePriorityEditorContent(
                    state = state,
                    onNameChanged = onEditNameChanged,
                    onSourceStep = onSourcePriorityStep,
                    onQualityStep = onQualityPriorityStep,
                    onHelpClick = onHelpClick,
                    onOpenSettings = onOpenSettings,
                    onSave = onEditSave,
                    onClose = onEditClose,
                )
            }

            else -> {
                QualityProfileOverviewContent(
                    state = state,
                    onSelectProfile = onSelectProfile,
                    onEditClick = onEditClick,
                    onSetDefaultClick = onSetDefaultClick,
                    onCancel = onDismiss,
                    onUseOrApply = onUseProfile,
                )
            }
        }
    }
}

@Composable
private fun QualityProfileOverviewContent(
    state: PlayerQualityProfileDialogState,
    onSelectProfile: (Int) -> Unit,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onCancel: () -> Unit,
    onUseOrApply: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LiquidGlassTokens.Space.s14,
                    vertical = LiquidGlassTokens.Space.s10,
                )
                .heightIn(min = LiquidGlassTokens.Space.s32),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.source_priority),
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (state.usedProfileName.isNotBlank()) {
                SecondaryTagPill(text = state.usedProfileName)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = LiquidGlassTokens.Space.s16,
                    vertical = LiquidGlassTokens.Space.s16,
                ),
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s12),
            ) {
                items(
                    items = state.profiles,
                    key = { item -> "profile_${item.id}" },
                ) { item ->
                    val isSelected = item.id == state.selectedProfileId
                    ProfileCardItem(
                        item = item,
                        selected = isSelected,
                        onClick = { onSelectProfile(item.id) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LiquidGlassTokens.Space.s10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
            ) {
                if (state.selectedProfileId != null) {
                    FrostedDialogActionPill(
                        label = stringResource(R.string.edit),
                        isPrimary = false,
                        onClick = onEditClick,
                    )
                    FrostedDialogActionPill(
                        label = stringResource(R.string.set_default),
                        isPrimary = false,
                        onClick = onSetDefaultClick,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
            ) {
                if (state.useProfileSelection) {
                    FrostedDialogActionPill(
                        label = stringResource(R.string.sort_cancel),
                        isPrimary = false,
                        onClick = onCancel,
                    )
                    if (state.selectedProfileId != null) {
                        FrostedDialogActionPill(
                            label = stringResource(R.string.use),
                            isPrimary = true,
                            onClick = onUseOrApply,
                        )
                    }
                } else {
                    FrostedDialogActionPill(
                        label = stringResource(R.string.sort_apply),
                        isPrimary = true,
                        onClick = onUseOrApply,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCardItem(
    item: QualityProfileItemUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val cardShape = glassTokens.shapes.compactCard
    val bgColor = if (selected) {
        glassTokens.colors.overlaySelected
    } else {
        glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.subtle)
    }
    val borderColor = if (selected) {
        PlayerGlassWhiteBorderSelected
    } else {
        PlayerGlassWhiteBorderSubtle
    }

    Column(
        modifier = Modifier
            .width(LiquidGlassTokens.Space.s96 + LiquidGlassTokens.Space.s64)
            .heightIn(min = LiquidGlassTokens.Space.s96 + LiquidGlassTokens.Space.s40)
            .clip(cardShape)
            .background(bgColor)
            .border(
                width = if (selected) LiquidGlassTokens.Border.medium else LiquidGlassTokens.Border.hairline,
                color = borderColor,
                shape = cardShape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(LiquidGlassTokens.Space.s14),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.liquidGlassTypeScale.bodyLg.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = if (item.isUsed || selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s6))
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s4),
        ) {
            if (item.types.contains(QualityDataHelper.QualityProfileType.WiFi)) {
                SecondaryTagPill(text = stringResource(R.string.wifi))
            }
            if (item.types.contains(QualityDataHelper.QualityProfileType.Data)) {
                SecondaryTagPill(text = stringResource(R.string.mobile_data))
            }
            if (item.types.contains(QualityDataHelper.QualityProfileType.Download)) {
                SecondaryTagPill(text = stringResource(R.string.download))
            }
        }
    }
}

@Composable
private fun SourcePriorityEditorContent(
    state: PlayerQualityProfileDialogState,
    onNameChanged: (String) -> Unit,
    onSourceStep: (String, Int) -> Unit,
    onQualityStep: (String, Int) -> Unit,
    onHelpClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSplitLayout = maxWidth >= LiquidGlassTokens.Breakpoint.largePhone

        Column(modifier = Modifier.fillMaxSize()) {
            if (useSplitLayout) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    PriorityListPane(
                        title = stringResource(R.string.pick_source),
                        entries = state.sourcePriorities,
                        maskColor = maskColor,
                        onStep = onSourceStep,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = LiquidGlassTokens.Space.s4)
                            .width(LiquidGlassTokens.Border.thin)
                            .background(PlayerGlassWhiteDivider),
                    )

                    PriorityListPane(
                        title = stringResource(R.string.qualities),
                        entries = state.qualityPriorities,
                        maskColor = maskColor,
                        onStep = onQualityStep,
                        trailingAction = {
                            SecondaryIconCircleButton(
                                drawableRes = R.drawable.baseline_help_outline_24,
                                contentDescription = stringResource(R.string.help),
                                onClick = onHelpClick,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    PriorityListPane(
                        title = stringResource(R.string.pick_source),
                        entries = state.sourcePriorities,
                        maskColor = maskColor,
                        onStep = onSourceStep,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LiquidGlassTokens.Border.thin)
                            .background(PlayerGlassWhiteDivider),
                    )
                    PriorityListPane(
                        title = stringResource(R.string.qualities),
                        entries = state.qualityPriorities,
                        maskColor = maskColor,
                        onStep = onQualityStep,
                        trailingAction = {
                            SecondaryIconCircleButton(
                                drawableRes = R.drawable.baseline_help_outline_24,
                                contentDescription = stringResource(R.string.help),
                                onClick = onHelpClick,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LiquidGlassTokens.Border.thin)
                    .background(PlayerGlassWhiteDivider),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LiquidGlassTokens.Space.s10),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
            ) {
                SecondaryGlassTextField(
                    value = state.editingProfileNameInput,
                    placeholder = state.editingProfilePlaceholder,
                    onValueChange = onNameChanged,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = LiquidGlassTokens.Space.s96 * 2.4f),
                )

                FrostedDialogActionPill(
                    label = stringResource(R.string.title_settings),
                    isPrimary = false,
                    onClick = onOpenSettings,
                )

                FrostedDialogActionPill(
                    label = stringResource(R.string.sort_close),
                    isPrimary = false,
                    onClick = onClose,
                )

                FrostedDialogActionPill(
                    label = stringResource(R.string.sort_save),
                    isPrimary = true,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun PriorityListPane(
    title: String,
    entries: List<PriorityEntryUi>,
    maskColor: Color,
    onStep: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LiquidGlassTokens.Space.s14,
                    vertical = LiquidGlassTokens.Space.s10,
                )
                .heightIn(min = LiquidGlassTokens.Space.s32),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingAction?.invoke()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .secondaryEdgeScrollFade(maskColor),
            contentPadding = PaddingValues(
                horizontal = LiquidGlassTokens.Space.s10,
                vertical = LiquidGlassTokens.Space.s12,
            ),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
        ) {
            items(
                items = entries,
                key = { entry -> "prio_${entry.key}" },
            ) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SelectionRowShape)
                        .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.subtle))
                        .padding(
                            horizontal = LiquidGlassTokens.Space.s12,
                            vertical = LiquidGlassTokens.Space.s8,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.liquidGlassTypeScale.bodyMd,
                        color = glassTokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    FrostedCircleStepButton(
                        isIncrement = false,
                        contentDescription = entry.label,
                        onClick = { onStep(entry.key, -1) },
                    )

                    Text(
                        text = entry.priority.toString(),
                        style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = glassTokens.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = LiquidGlassTokens.Space.s28),
                    )

                    FrostedCircleStepButton(
                        isIncrement = true,
                        contentDescription = entry.label,
                        onClick = { onStep(entry.key, 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsContent(
    hideNegativeSources: Boolean,
    hideErrorSources: Boolean,
    onHideNegativeChanged: (Boolean) -> Unit,
    onHideErrorChanged: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LiquidGlassTokens.Space.s14,
                    vertical = LiquidGlassTokens.Space.s10,
                )
                .heightIn(min = LiquidGlassTokens.Space.s32),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_settings),
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = LiquidGlassTokens.Space.s12,
                    vertical = LiquidGlassTokens.Space.s14,
                ),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
        ) {
            ProfileSettingToggleRow(
                label = stringResource(R.string.profile_hide_negative_sources),
                checked = hideNegativeSources,
                onCheckedChange = onHideNegativeChanged,
            )
            ProfileSettingToggleRow(
                label = stringResource(R.string.profile_hide_error_sources),
                checked = hideErrorSources,
                onCheckedChange = onHideErrorChanged,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LiquidGlassTokens.Space.s10),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrostedDialogActionPill(
                label = stringResource(R.string.sort_cancel),
                isPrimary = false,
                onClick = onCancel,
            )
            Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
            FrostedDialogActionPill(
                label = stringResource(R.string.sort_apply),
                isPrimary = true,
                onClick = onApply,
            )
        }
    }
}

@Composable
private fun ProfileSettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SelectionRowShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.subtle))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(
                horizontal = LiquidGlassTokens.Space.s14,
                vertical = LiquidGlassTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd,
            color = glassTokens.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(end = LiquidGlassTokens.Space.s12),
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = glassTokens.colors.onAccent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = glassTokens.colors.textSecondary,
                uncheckedTrackColor = glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover),
                uncheckedBorderColor = PlayerGlassWhiteBorder,
            ),
        )
    }
}

// ============================================================================
// 3. Online Subtitles Search Picker
// ============================================================================

@Composable
fun PlayerOnlineSubtitlesDialog(
    visible: Boolean,
    state: PlayerOnlineSubtitlesDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onYearClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onResultSelected: (Int) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled
    val focusManager = LocalFocusManager.current

    SecondaryFullSheetOverlay(
        visible = visible,
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search + Year + Language Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LiquidGlassTokens.Space.s12,
                        vertical = LiquidGlassTokens.Space.s10,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(LiquidGlassTokens.Space.s36)
                        .clip(SecondaryPillShape)
                        .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
                        .border(
                            width = LiquidGlassTokens.Border.hairline,
                            color = PlayerGlassWhiteBorder,
                            shape = SecondaryPillShape,
                        )
                        .padding(horizontal = LiquidGlassTokens.Space.s12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search_icon),
                        contentDescription = stringResource(R.string.search_hint),
                        tint = glassTokens.colors.textSecondary,
                        modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.liquidGlassTypeScale.bodyMd,
                                color = glassTokens.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                color = glassTokens.colors.textPrimary,
                            ),
                            cursorBrush = SolidColor(glassTokens.colors.textPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSearchSubmit()
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                SecondaryHeaderPill(
                    label = state.yearLabel.ifBlank { stringResource(R.string.none) },
                    onClick = onYearClick,
                )

                SecondaryIconCircleButton(
                    drawableRes = R.drawable.ic_baseline_tune_24,
                    contentDescription = stringResource(R.string.subs_subtitle_languages),
                    onClick = onLanguageClick,
                )
            }

            if (state.isSearching || state.isApplying) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = glassTokens.colors.playerTimelineTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidGlassTokens.Border.medium),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidGlassTokens.Border.thin)
                        .background(PlayerGlassWhiteDivider),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .secondaryEdgeScrollFade(maskColor),
                contentPadding = PaddingValues(
                    horizontal = LiquidGlassTokens.Space.s8,
                    vertical = LiquidGlassTokens.Space.s12,
                ),
                verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
            ) {
                if (!state.isSearching && state.results.isEmpty()) {
                    item(key = "online_subs_empty") {
                        Text(
                            text = stringResource(R.string.no_subtitles),
                            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                fontStyle = FontStyle.Italic,
                            ),
                            color = glassTokens.colors.textMuted,
                            modifier = Modifier.padding(
                                horizontal = LiquidGlassTokens.Space.s14,
                                vertical = LiquidGlassTokens.Space.s16,
                            ),
                        )
                    }
                } else {
                    items(
                        items = state.results,
                        key = { item -> "online_sub_${item.index}" },
                    ) { item ->
                        val selected = item.index == state.selectedIndex
                        OnlineSubtitleRow(
                            item = item,
                            selected = selected,
                            onClick = { onResultSelected(item.index) },
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LiquidGlassTokens.Border.thin)
                    .background(PlayerGlassWhiteDivider),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LiquidGlassTokens.Space.s10),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FrostedDialogActionPill(
                    label = stringResource(R.string.sort_cancel),
                    isPrimary = false,
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
                FrostedDialogActionPill(
                    label = stringResource(R.string.sort_apply),
                    isPrimary = true,
                    onClick = {
                        if (!state.isApplying) {
                            onApply()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun OnlineSubtitleRow(
    item: OnlineSubtitleItemUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SelectionRowShape)
            .then(
                if (selected) {
                    Modifier
                        .background(glassTokens.colors.overlaySelected)
                        .border(
                            width = LiquidGlassTokens.Border.hairline,
                            color = PlayerGlassWhiteActiveRowBorder,
                            shape = SelectionRowShape,
                        )
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(
                horizontal = LiquidGlassTokens.Space.s12,
                vertical = LiquidGlassTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected) glassTokens.colors.textPrimary else glassTokens.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.liquidGlassTypeScale.labelSm,
                    color = glassTokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (item.isHearingImpaired) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_hearing_24),
                contentDescription = null,
                tint = glassTokens.colors.textSecondary,
                modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
            )
        }

        if (selected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = glassTokens.colors.textPrimary,
                modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
            )
        }
    }
}

// ============================================================================
// 4. Subtitle Settings Sheet (Hosts SubtitlesScreen inside Kyant Liquid Glass)
// ============================================================================

@Composable
fun PlayerSubtitleSettingsDialog(
    visible: Boolean,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryFullSheetOverlay(
        visible = visible,
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        val context = LocalContext.current
        val settings = rememberAppSettings()
        val layoutPref by settings.ui.layout.collectAsState()
        val layoutFlag = remember(context, layoutPref) {
            when (layoutPref) {
                -1 -> if (DeviceLayout.isAutoTv(context)) TV else PHONE
                0 -> PHONE
                1 -> TV
                2 -> EMULATOR
                else -> PHONE
            }
        }

        CompositionLocalProvider(
            LocalBackPress provides onDismiss,
            DeviceLayout.LocalLayout provides layoutFlag,
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = Color.Transparent,
                    outline = PlayerGlassWhiteBorder,
                    outlineVariant = PlayerGlassWhiteDivider,
                ),
            ) {
                SubtitlesScreen.Content()
            }
        }
    }
}

// ============================================================================
// Shared Kyant Liquid Glass Sheet & Row Primitives for Secondary Popups
// ============================================================================

@Composable
private fun SecondaryFullSheetOverlay(
    visible: Boolean,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val glassTokens = MaterialTheme.liquidGlass
    val sheetShape = glassTokens.shapes.sheet

    val startInset = safePadding.calculateStartPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val endInset = safePadding.calculateEndPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val topInset = safePadding.calculateTopPadding() + LiquidGlassTokens.Space.s12
    val bottomInset = safePadding.calculateBottomPadding() + LiquidGlassTokens.Space.s12

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LiquidGlassTokens.Motion.normalMillis)),
            exit = fadeOut(tween(LiquidGlassTokens.Motion.fastMillis)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassTokens.colors.overlayScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(LiquidGlassTokens.Motion.normalMillis)) +
                scaleIn(tween(LiquidGlassTokens.Motion.normalMillis), initialScale = 0.96f),
            exit = fadeOut(tween(LiquidGlassTokens.Motion.fastMillis)) +
                scaleOut(tween(LiquidGlassTokens.Motion.fastMillis), targetScale = 0.96f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = startInset,
                        top = topInset,
                        end = endInset,
                        bottom = bottomInset,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(sheetShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    KyantPlayerGlassChromeLayer(
                        shape = sheetShape,
                        backdrop = backdrop,
                        fallbackCornerRadius = LiquidGlassTokens.Radius.sheet,
                        modifier = Modifier.matchParentSize(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                glassTokens.colors.surfaceSheet.copy(
                                    alpha = glassTokens.opacity.overlayLight,
                                ),
                            )
                            .padding(LiquidGlassTokens.Space.s12),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondarySelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SelectionRowShape)
            .then(
                if (selected) {
                    Modifier
                        .background(glassTokens.colors.overlaySelected)
                        .border(
                            width = LiquidGlassTokens.Border.hairline,
                            color = PlayerGlassWhiteActiveRowBorder,
                            shape = SelectionRowShape,
                        )
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(
                horizontal = LiquidGlassTokens.Space.s12,
                vertical = LiquidGlassTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) glassTokens.colors.textPrimary else glassTokens.colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (selected) {
            Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s8))
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = glassTokens.colors.textPrimary,
                modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
            )
        }
    }
}

@Composable
private fun SecondaryHeaderPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .height(LiquidGlassTokens.Space.s32)
            .clip(SecondaryPillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = SecondaryPillShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = glassTokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SecondaryTagPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .clip(SecondaryPillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorderSubtle,
                shape = SecondaryPillShape,
            )
            .padding(
                horizontal = LiquidGlassTokens.Space.s10,
                vertical = LiquidGlassTokens.Space.s4,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.liquidGlassTypeScale.labelXs.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = glassTokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SecondaryIconCircleButton(
    drawableRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .size(LiquidGlassTokens.Space.s32)
            .clip(CircleShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            tint = glassTokens.colors.textPrimary,
            modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
        )
    }
}

@Composable
private fun SecondaryGlassTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .height(LiquidGlassTokens.Space.s36)
            .clip(SecondaryPillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = SecondaryPillShape,
            )
            .padding(horizontal = LiquidGlassTokens.Space.s14),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.liquidGlassTypeScale.bodyMd,
                color = glassTokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                color = glassTokens.colors.textPrimary,
            ),
            cursorBrush = SolidColor(glassTokens.colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Modifier.secondaryEdgeScrollFade(maskColor: Color): Modifier =
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            if (size.height > 0f) {
                val topFadeFraction =
                    (LiquidGlassTokens.Space.s18.toPx() / size.height).coerceIn(0f, 0.2f)
                val bottomFadeFraction =
                    (LiquidGlassTokens.Space.s24.toPx() / size.height).coerceIn(0f, 0.25f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        topFadeFraction to maskColor,
                        (1f - bottomFadeFraction) to maskColor,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
