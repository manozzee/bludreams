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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale

private val SelectedRowShape = RoundedCornerShape(LiquidGlassTokens.Radius.lg)
private val PillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

@Composable
fun PlayerSourceSubsDialog(
    visible: Boolean,
    state: PlayerSourceSubsDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onSourceSelected: (Int) -> Unit,
    onSourceLongClick: (Int) -> Unit,
    onProfileSettingsClick: () -> Unit,
    onSubtitleGroupSelected: (Int) -> Unit,
    onSubtitleVariantSelected: (Int) -> Unit,
    onEncodingClick: () -> Unit,
    onSubtitleSettingsClick: () -> Unit,
    onLoadFromFile: () -> Unit,
    onLoadOnline: () -> Unit,
    onLoadFirstOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val glassTokens = MaterialTheme.liquidGlass
    val sheetShape = glassTokens.shapes.sheet

    val startInset = safePadding.calculateStartPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val endInset = safePadding.calculateEndPadding(layoutDirection) + glassTokens.spacing.screenHorizontal
    val topInset = safePadding.calculateTopPadding() + LiquidGlassTokens.Space.s12
    val bottomInset = safePadding.calculateBottomPadding() + LiquidGlassTokens.Space.s12

    val sourcesListState = rememberLazyListState()
    val subtitlesListState = rememberLazyListState()
    val subtitleVariantsListState = rememberLazyListState()

    LaunchedEffect(visible, state.scrollTriggerToken) {
        if (!visible) return@LaunchedEffect
        if (state.selectedSourceIndex >= 0 && state.selectedSourceIndex < state.sources.size) {
            sourcesListState.scrollToItem(state.selectedSourceIndex)
        }
        if (state.selectedSubtitleGroupIndex >= 0 && state.selectedSubtitleGroupIndex < state.subtitleGroups.size) {
            subtitlesListState.scrollToItem(state.selectedSubtitleGroupIndex)
        }
        if (state.selectedSubtitleVariantIndex >= 0 &&
            state.selectedSubtitleVariantIndex < state.subtitleVariants.size
        ) {
            subtitleVariantsListState.scrollToItem(state.selectedSubtitleVariantIndex)
        }
    }

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

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                glassTokens.colors.surfaceSheet.copy(
                                    alpha = glassTokens.opacity.overlayLight,
                                ),
                            )
                            .padding(LiquidGlassTokens.Space.s12),
                    ) {
                        val useSplitLayout = maxWidth >= LiquidGlassTokens.Breakpoint.largePhone

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (useSplitLayout) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    if (state.hasSources) {
                                        SourcesPane(
                                            state = state,
                                            listState = sourcesListState,
                                            onSourceSelected = onSourceSelected,
                                            onSourceLongClick = onSourceLongClick,
                                            onProfileSettingsClick = onProfileSettingsClick,
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
                                    }

                                    SubtitlesPane(
                                        state = state,
                                        subtitleVariantsListState = subtitleVariantsListState,
                                        subtitlesListState = subtitlesListState,
                                        onSubtitleVariantSelected = onSubtitleVariantSelected,
                                        onEncodingClick = onEncodingClick,
                                        onSubtitleSettingsClick = onSubtitleSettingsClick,
                                        onSubtitleGroupSelected = onSubtitleGroupSelected,
                                        onLoadFromFile = onLoadFromFile,
                                        onLoadOnline = onLoadOnline,
                                        onLoadFirstOnline = onLoadFirstOnline,
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
                                    if (state.hasSources) {
                                        SourcesPane(
                                            state = state,
                                            listState = sourcesListState,
                                            onSourceSelected = onSourceSelected,
                                            onSourceLongClick = onSourceLongClick,
                                            onProfileSettingsClick = onProfileSettingsClick,
                                            modifier = Modifier
                                                .weight(0.9f)
                                                .fillMaxWidth(),
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(LiquidGlassTokens.Border.thin)
                                            .background(PlayerGlassWhiteDivider),
                                        )
                                    }

                                    SubtitlesPane(
                                        state = state,
                                        subtitleVariantsListState = subtitleVariantsListState,
                                        subtitlesListState = subtitlesListState,
                                        onSubtitleVariantSelected = onSubtitleVariantSelected,
                                        onEncodingClick = onEncodingClick,
                                        onSubtitleSettingsClick = onSubtitleSettingsClick,
                                        onSubtitleGroupSelected = onSubtitleGroupSelected,
                                        onLoadFromFile = onLoadFromFile,
                                        onLoadOnline = onLoadOnline,
                                        onLoadFirstOnline = onLoadFirstOnline,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .fillMaxWidth(),
                                    )
                                }
                            }

                            // Bottom action bar
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
                                FrostedActionPill(
                                    label = stringResource(R.string.sort_cancel),
                                    isPrimary = false,
                                    onClick = onDismiss,
                                )
                                Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))
                                FrostedActionPill(
                                    label = stringResource(R.string.sort_apply),
                                    isPrimary = true,
                                    onClick = onApply,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesPane(
    state: PlayerSourceSubsDialogState,
    listState: LazyListState,
    onSourceSelected: (Int) -> Unit,
    onSourceLongClick: (Int) -> Unit,
    onProfileSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled
    Column(modifier = modifier) {
        // Header Row: "Sources" + profile pill + gear button (aligned with Subtitles header)
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
                text = stringResource(R.string.pick_source),
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
            ) {
                if (state.profileLabel.isNotBlank()) {
                    FrostedHeaderPill(
                        label = state.profileLabel,
                        onClick = onProfileSettingsClick,
                        modifier = Modifier.widthIn(max = LiquidGlassTokens.Space.s80 * 1.65f),
                    )
                }
                FrostedIconCircleButton(
                    drawableRes = R.drawable.ic_outline_settings_24,
                    contentDescription = stringResource(R.string.video_source),
                    onClick = onProfileSettingsClick,
                )
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
                .weight(1f)
                .fillMaxWidth()
                .edgeScrollFade(maskColor),
            contentPadding = PaddingValues(
                horizontal = LiquidGlassTokens.Space.s8,
                vertical = LiquidGlassTokens.Space.s12,
            ),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
        ) {
            items(
                items = state.sources,
                key = { item -> "src_${item.index}" },
            ) { item ->
                val isSelected = item.index == state.selectedSourceIndex
                SelectableGlassRow(
                    label = item.label,
                    selected = isSelected,
                    onClick = { onSourceSelected(item.index) },
                    onLongClick = { onSourceLongClick(item.index) },
                )
            }

            if (!state.hiddenSourcesText.isNullOrBlank()) {
                item(key = "hidden_sources_footer") {
                    Text(
                        text = state.hiddenSourcesText,
                        style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                            fontStyle = FontStyle.Italic,
                        ),
                        color = glassTokens.colors.textMuted,
                        modifier = Modifier.padding(
                            horizontal = LiquidGlassTokens.Space.s12,
                            vertical = LiquidGlassTokens.Space.s10,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitlesPane(
    state: PlayerSourceSubsDialogState,
    subtitleVariantsListState: LazyListState,
    subtitlesListState: LazyListState,
    onSubtitleVariantSelected: (Int) -> Unit,
    onEncodingClick: () -> Unit,
    onSubtitleSettingsClick: () -> Unit,
    onSubtitleGroupSelected: (Int) -> Unit,
    onLoadFromFile: () -> Unit,
    onLoadOnline: () -> Unit,
    onLoadFirstOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val maskColor = glassTokens.colors.backgroundAmoled
    Column(modifier = modifier) {
        // Header Row: "Subtitles" + Auto/Encoding pill + gear button (aligned with Sources header)
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
                text = stringResource(R.string.pick_subtitle),
                style = MaterialTheme.liquidGlassTypeScale.titleSm.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = glassTokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
            ) {
                if (state.encodingLabel.isNotBlank()) {
                    FrostedHeaderPill(
                        label = state.encodingLabel,
                        onClick = onEncodingClick,
                        modifier = Modifier.widthIn(max = LiquidGlassTokens.Space.s80 * 1.55f),
                    )
                }
                FrostedIconCircleButton(
                    drawableRes = R.drawable.ic_outline_settings_24,
                    contentDescription = stringResource(R.string.subtitles_settings),
                    onClick = onSubtitleSettingsClick,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassTokens.Border.thin)
                .background(PlayerGlassWhiteDivider),
        )

        // Unlabeled horizontal subtitle variant pills directly beneath Subtitles header
        if (state.subtitleVariants.size >= 2) {
            LazyRow(
                state = subtitleVariantsListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = LiquidGlassTokens.Space.s10,
                        bottom = LiquidGlassTokens.Space.s4,
                    ),
                contentPadding = PaddingValues(horizontal = LiquidGlassTokens.Space.s14),
                horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s8),
            ) {
                items(
                    items = state.subtitleVariants,
                    key = { variant -> "sub_variant_${variant.index}" },
                ) { variant ->
                    val isSelected = variant.index == state.selectedSubtitleVariantIndex
                    SubtitleVariantGlassPill(
                        label = variant.label,
                        selected = isSelected,
                        onClick = { onSubtitleVariantSelected(variant.index) },
                    )
                }
            }
        }

        // Borderless Subtitles List + Footer Load Actions
        LazyColumn(
            state = subtitlesListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .edgeScrollFade(maskColor),
            contentPadding = PaddingValues(
                horizontal = LiquidGlassTokens.Space.s8,
                vertical = LiquidGlassTokens.Space.s12,
            ),
            verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s2),
        ) {
            items(
                items = state.subtitleGroups,
                key = { group -> "sub_grp_${group.index}" },
            ) { group ->
                val isSelected = group.index == state.selectedSubtitleGroupIndex
                SelectableGlassRow(
                    label = group.label,
                    selected = isSelected,
                    onClick = { onSubtitleGroupSelected(group.index) },
                )
            }

            if (state.loadFromFileLabel.isNotBlank() ||
                !state.loadOnlineLabel.isNullOrBlank() ||
                !state.loadFirstOnlineLabel.isNullOrBlank()
            ) {
                item(key = "subs_footer_divider") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = LiquidGlassTokens.Space.s6,
                                vertical = LiquidGlassTokens.Space.s6,
                            )
                            .height(LiquidGlassTokens.Border.thin)
                            .background(PlayerGlassWhiteDivider),
                    )
                }
            }

            if (state.loadFromFileLabel.isNotBlank()) {
                item(key = "subs_load_file") {
                    FooterActionRow(
                        label = state.loadFromFileLabel,
                        onClick = onLoadFromFile,
                    )
                }
            }

            state.loadOnlineLabel?.takeIf { it.isNotBlank() }?.let { onlineLabel ->
                item(key = "subs_load_online") {
                    FooterActionRow(
                        label = onlineLabel,
                        onClick = onLoadOnline,
                    )
                }
            }

            state.loadFirstOnlineLabel?.takeIf { it.isNotBlank() }?.let { firstOnlineLabel ->
                item(key = "subs_load_first_online") {
                    FooterActionRow(
                        label = firstOnlineLabel,
                        onClick = onLoadFirstOnline,
                    )
                }
            }
        }
    }
}

private fun Modifier.edgeScrollFade(maskColor: Color): Modifier =
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

@Composable
private fun SelectableGlassRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val rowModifier = if (selected) {
        Modifier
            .fillMaxWidth()
            .clip(SelectedRowShape)
            .background(glassTokens.colors.overlaySelected)
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteActiveRowBorder,
                shape = SelectedRowShape,
            )
    } else {
        Modifier
            .fillMaxWidth()
            .clip(SelectedRowShape)
    }

    Row(
        modifier = rowModifier
            .combinedClickable(
                role = Role.RadioButton,
                onClick = onClick,
                onLongClick = onLongClick,
            )
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
                tint = glassTokens.colors.textPrimary.copy(alpha = 0.90f),
                modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
            )
        }
    }
}

@Composable
private fun FooterActionRow(
    label: String,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SelectedRowShape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                horizontal = LiquidGlassTokens.Space.s12,
                vertical = LiquidGlassTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodySm.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = glassTokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s6))
        Icon(
            imageVector = AppIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = glassTokens.colors.textMuted,
            modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
        )
    }
}

@Composable
private fun SubtitleVariantGlassPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val bgColor = if (selected) {
        glassTokens.colors.overlaySelected
    } else {
        glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.subtle)
    }
    val borderBrush = if (selected) {
        PlayerGlassWhiteSelectedBorderBrush
    } else {
        PlayerGlassWhiteBorderBrush
    }

    Row(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s28)
            .clip(PillShape)
            .background(bgColor)
            .border(
                width = if (selected) LiquidGlassTokens.Border.thin else LiquidGlassTokens.Border.hairline,
                brush = borderBrush,
                shape = PillShape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s5),
    ) {
        if (selected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = glassTokens.colors.textPrimary,
                modifier = Modifier.size(LiquidGlassTokens.Icon.xs),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) glassTokens.colors.textPrimary else glassTokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FrostedHeaderPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = modifier
            .height(LiquidGlassTokens.Space.s28)
            .clip(PillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = PillShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s10),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = glassTokens.colors.textPrimary.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FrostedIconCircleButton(
    drawableRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Box(
        modifier = Modifier
            .size(LiquidGlassTokens.Space.s28)
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
            tint = glassTokens.colors.textPrimary.copy(alpha = 0.90f),
            modifier = Modifier.size(LiquidGlassTokens.Icon.sm),
        )
    }
}

@Composable
private fun FrostedActionPill(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    val bgModifier = if (isPrimary) {
        Modifier
            .clip(PillShape)
            .background(glassTokens.colors.textPrimary)
    } else {
        Modifier
            .clip(PillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = PillShape,
            )
    }

    Box(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s36)
            .then(bgModifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s20),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isPrimary) {
                glassTokens.colors.textInverse
            } else {
                glassTokens.colors.textPrimary
            },
            maxLines = 1,
        )
    }
}
