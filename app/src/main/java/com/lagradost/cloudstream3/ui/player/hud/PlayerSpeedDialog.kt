package com.lagradost.cloudstream3.ui.player.hud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTokens
import com.lagradost.cloudstream3.ui.liquidglass.PlatformLayerBackdrop
import com.lagradost.cloudstream3.ui.liquidglass.PlusJakartaSansFontFamily
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlass
import com.lagradost.cloudstream3.ui.liquidglass.liquidGlassTypeScale
import kotlin.math.abs

private data class SpeedPresetOption(
    val speed: Float,
    val label: String,
)

private val SpeedPresets = listOf(
    SpeedPresetOption(0.25f, "0.25x"),
    SpeedPresetOption(1.0f, "1.0x"),
    SpeedPresetOption(1.25f, "1.25x"),
    SpeedPresetOption(1.5f, "1.5x"),
    SpeedPresetOption(2.0f, "2.0x"),
)

@Composable
fun PlayerSpeedDialog(
    visible: Boolean,
    state: PlayerSpeedDialogState,
    backdrop: PlatformLayerBackdrop?,
    onDismiss: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSpeedStep: (Float) -> Unit,
    onSpeedPresetSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassTokens = MaterialTheme.liquidGlass
    CenteredDarkGlassOverlay(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
    ) { clickGuardModifier ->
        val cardWidth = (maxWidth * 0.88f).coerceAtMost(glassTokens.components.dialogMaxWidth)

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
                        vertical = LiquidGlassTokens.Space.s18,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s16),
            ) {
                // Top: Centered speed readout
                Text(
                    text = state.speedText,
                    style = MaterialTheme.liquidGlassTypeScale.titleMd.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = glassTokens.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                // Middle: Circular frosted - / + flanking horizontal speed slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s12),
                ) {
                    FrostedCircleStepButton(
                        isIncrement = false,
                        contentDescription = stringResource(R.string.player_speed_text_format, state.speed),
                        onClick = { onSpeedStep(-0.1f) },
                    )

                    FrostedGlassSlider(
                        value = state.speed,
                        valueRange = 0.1f..2.0f,
                        stepSize = 0.05f,
                        onValueChange = onSpeedChanged,
                        modifier = Modifier.weight(1f),
                    )

                    FrostedCircleStepButton(
                        isIncrement = true,
                        contentDescription = stringResource(R.string.player_speed_text_format, state.speed),
                        onClick = { onSpeedStep(0.1f) },
                    )
                }

                // Bottom: Horizontal speed preset pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = LiquidGlassTokens.Space.s8,
                        alignment = Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeedPresets.forEach { preset ->
                        val isSelected = abs(state.speed - preset.speed) < 0.001f
                        FrostedGlassPresetPill(
                            label = preset.label,
                            selected = isSelected,
                            onClick = { onSpeedPresetSelected(preset.speed) },
                        )
                    }
                }
            }
        }
    }
}
