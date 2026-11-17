package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale

private val SegmentedPillShape = RoundedCornerShape(LiquidGlassTokens.Radius.full)

@Composable
fun PlayerCompressorDialog(
    visible: Boolean,
    state: PlayerCompressorDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onThresholdChanged: (Float) -> Unit,
    onMakeupGainChanged: (Float) -> Unit,
    onPresetSelected: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    CenteredDarkGlassOverlay(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
    ) { clickGuardModifier ->
        val wideCardMax = glassTokens.components.playerPanelMaxWidth * 1.18f
        val cardWidth = (maxWidth * 0.92f).coerceAtMost(wideCardMax)
        val useSideBySide = maxWidth >= LiquidGlassTokens.Breakpoint.largePhone

        val isLightActive = state.threshold == -18f && state.makeupGain == 4f
        val isDialogueBoostActive = state.threshold == -24f && state.makeupGain == 12f
        val isHeavyActive = state.threshold == -30f && state.makeupGain == 16f

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
                    .padding(
                        horizontal = LiquidGlassTokens.Space.s20,
                        vertical = LiquidGlassTokens.Space.s16,
                    ),
                verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s14),
            ) {
                // Row 1 — Mode Toggle, Live Status & Presets Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        CompressorEnableSegmentedPill(
                            enabled = state.enabled,
                            onEnabledChanged = onEnabledChanged,
                        )

                        Text(
                            text = state.statusText,
                            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = glassTokens.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(LiquidGlassTokens.Space.s10))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
                    ) {
                        FrostedGlassPresetPill(
                            label = stringResource(R.string.compressor_preset_light),
                            selected = isLightActive,
                            onClick = { onPresetSelected(-18f, 4f) },
                        )
                        FrostedGlassPresetPill(
                            label = stringResource(R.string.compressor_preset_dialog),
                            selected = isDialogueBoostActive,
                            onClick = { onPresetSelected(-24f, 12f) },
                        )
                        FrostedGlassPresetPill(
                            label = stringResource(R.string.compressor_preset_heavy),
                            selected = isHeavyActive,
                            onClick = { onPresetSelected(-30f, 16f) },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidGlassTokens.Border.thin)
                        .background(PlayerGlassWhiteDivider),
                )

                // Row 2 — Side-by-Side Sliders (Threshold | Makeup Gain)
                if (useSideBySide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompressorSliderColumn(
                            label = state.thresholdLabel,
                            hint = state.thresholdHint,
                            value = state.threshold,
                            valueRange = -30f..0f,
                            stepDescription = state.thresholdLabel,
                            onStep = { delta ->
                                onThresholdChanged((state.threshold + delta).coerceIn(-30f, 0f))
                            },
                            onValueChange = onThresholdChanged,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = LiquidGlassTokens.Space.s16),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                        )

                        CompressorSliderColumn(
                            label = state.makeupLabel,
                            hint = state.makeupHint,
                            value = state.makeupGain,
                            valueRange = 0f..24f,
                            stepDescription = state.makeupLabel,
                            onStep = { delta ->
                                onMakeupGainChanged((state.makeupGain + delta).coerceIn(0f, 24f))
                            },
                            onValueChange = onMakeupGainChanged,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = LiquidGlassTokens.Space.s16),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s12),
                    ) {
                        CompressorSliderColumn(
                            label = state.thresholdLabel,
                            hint = state.thresholdHint,
                            value = state.threshold,
                            valueRange = -30f..0f,
                            stepDescription = state.thresholdLabel,
                            onStep = { delta ->
                                onThresholdChanged((state.threshold + delta).coerceIn(-30f, 0f))
                            },
                            onValueChange = onThresholdChanged,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LiquidGlassTokens.Border.thin)
                                .background(PlayerGlassWhiteDivider),
                        )

                        CompressorSliderColumn(
                            label = state.makeupLabel,
                            hint = state.makeupHint,
                            value = state.makeupGain,
                            valueRange = 0f..24f,
                            stepDescription = state.makeupLabel,
                            onStep = { delta ->
                                onMakeupGainChanged((state.makeupGain + delta).coerceIn(0f, 24f))
                            },
                            onValueChange = onMakeupGainChanged,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidGlassTokens.Border.thin)
                        .background(PlayerGlassWhiteDivider),
                )

                // Row 3 — Bottom Action Pills (Reset on left; Cancel + Apply on right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FrostedDialogActionPill(
                        label = stringResource(R.string.reset_btn),
                        isPrimary = false,
                        onClick = onReset,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
                    ) {
                        FrostedDialogActionPill(
                            label = stringResource(R.string.sort_cancel),
                            isPrimary = false,
                            onClick = onDismiss,
                        )
                        FrostedDialogActionPill(
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

@Composable
private fun CompressorEnableSegmentedPill(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Row(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s28)
            .clip(SegmentedPillShape)
            .background(glassTokens.colors.textPrimary.copy(alpha = glassTokens.opacity.hover))
            .border(
                width = LiquidGlassTokens.Border.hairline,
                color = PlayerGlassWhiteBorder,
                shape = SegmentedPillShape,
            )
            .padding(LiquidGlassTokens.Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentedOptionChip(
            label = stringResource(R.string.compressor_on),
            selected = enabled,
            onClick = { onEnabledChanged(true) },
            selectedBg = glassTokens.colors.overlaySelected,
            textPrimary = glassTokens.colors.textPrimary,
            textMuted = glassTokens.colors.textMuted,
        )
        SegmentedOptionChip(
            label = stringResource(R.string.compressor_off),
            selected = !enabled,
            onClick = { onEnabledChanged(false) },
            selectedBg = glassTokens.colors.overlaySelected,
            textPrimary = glassTokens.colors.textPrimary,
            textMuted = glassTokens.colors.textMuted,
        )
    }
}

@Composable
private fun SegmentedOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedBg: Color,
    textPrimary: Color,
    textMuted: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(SegmentedPillShape)
            .background(if (selected) selectedBg else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(
                        width = LiquidGlassTokens.Border.hairline,
                        color = PlayerGlassWhiteActiveRowBorder,
                        shape = SegmentedPillShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = LiquidGlassTokens.Space.s10),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) textPrimary else textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompressorSliderColumn(
    label: String,
    hint: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepDescription: String,
    onStep: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
    ) {
        Text(
            text = label,
            style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = glassTokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = hint,
            style = MaterialTheme.liquidGlassTypeScale.labelSm,
            color = glassTokens.colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LiquidGlassTokens.Space.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s10),
        ) {
            FrostedCircleStepButton(
                isIncrement = false,
                contentDescription = stepDescription,
                onClick = { onStep(-1f) },
            )

            FrostedGlassSlider(
                value = value,
                valueRange = valueRange,
                stepSize = 1f,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
            )

            FrostedCircleStepButton(
                isIncrement = true,
                contentDescription = stepDescription,
                onClick = { onStep(1f) },
            )
        }
    }
}
