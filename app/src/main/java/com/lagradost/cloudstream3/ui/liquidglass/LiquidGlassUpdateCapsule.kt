package com.lagradost.cloudstream3.ui.liquidglass

import android.os.SystemClock
import android.text.format.Formatter.formatFileSize
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.getStackTracePretty
import com.lagradost.cloudstream3.ui.AppIcons
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight
import com.lagradost.cloudstream3.utils.parseMarkdown
import com.lagradost.cloudstream4.viewmodels.GithubAction
import com.lagradost.cloudstream4.viewmodels.GithubDialog
import com.lagradost.cloudstream4.viewmodels.GithubFile
import com.lagradost.cloudstream4.viewmodels.GithubState
import com.lagradost.cloudstream4.viewmodels.GithubUpdateDialogState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val NO_UPDATE_AUTO_DISMISS_MS = 3500L
private const val BACKDROP_PREDRAW_MIN_INTERVAL_MS = 32L
private const val WATER_SHIMMER_CYCLE_MILLIS = 2100

private sealed interface UpdateCapsuleVisualState {
    data class UpdateAvailable(val file: GithubFile) : UpdateCapsuleVisualState
    data class Downloading(
        val progress: Long,
        val total: Long?,
        val file: GithubFile?,
    ) : UpdateCapsuleVisualState

    data class ReadyToInstall(
        val totalBytes: Long,
        val file: GithubFile,
        val apkPath: String,
    ) : UpdateCapsuleVisualState

    data class InstallFailed(
        val file: GithubFile,
        val apkPath: String,
        val totalBytes: Long,
        val error: Throwable,
    ) : UpdateCapsuleVisualState

    data object Checking : UpdateCapsuleVisualState
    data object NoUpdate : UpdateCapsuleVisualState
    data class Failed(val error: Throwable) : UpdateCapsuleVisualState
}

private fun GithubDialog?.shouldShowCapsule(): Boolean = when (this?.state) {
    is GithubUpdateDialogState.UpdateFound,
    is GithubUpdateDialogState.DownloadProgress,
    is GithubUpdateDialogState.ReadyToInstall,
    is GithubUpdateDialogState.InstallError -> true
    GithubUpdateDialogState.Loading,
    GithubUpdateDialogState.NoUpdateFound,
    is GithubUpdateDialogState.Error -> isFromUser
    null -> false
}

private fun GithubDialog.toVisualState(lastReleaseFile: GithubFile?): UpdateCapsuleVisualState =
    when (val s = state) {
        is GithubUpdateDialogState.UpdateFound -> UpdateCapsuleVisualState.UpdateAvailable(s.file)
        is GithubUpdateDialogState.DownloadProgress -> UpdateCapsuleVisualState.Downloading(
            progress = s.progress,
            total = s.total,
            file = lastReleaseFile,
        )
        is GithubUpdateDialogState.ReadyToInstall -> UpdateCapsuleVisualState.ReadyToInstall(
            totalBytes = s.totalBytes,
            file = s.file,
            apkPath = s.apkPath,
        )
        is GithubUpdateDialogState.InstallError -> UpdateCapsuleVisualState.InstallFailed(
            file = s.file,
            apkPath = s.apkPath,
            totalBytes = s.totalBytes,
            error = s.error,
        )
        GithubUpdateDialogState.Loading -> UpdateCapsuleVisualState.Checking
        GithubUpdateDialogState.NoUpdateFound -> UpdateCapsuleVisualState.NoUpdate
        is GithubUpdateDialogState.Error -> UpdateCapsuleVisualState.Failed(s.error)
    }

private fun formatVersionTitle(appName: String, file: GithubFile): String {
    val raw = file.tagName.trim().ifBlank { file.displayName.trim() }
    if (raw.isBlank()) return appName
    val formatted = if (raw.startsWith("v", ignoreCase = true) || raw.firstOrNull()?.isDigit() != true) {
        raw
    } else {
        "v$raw"
    }
    return "$appName $formatted"
}

@Composable
fun LiquidGlassUpdateOverlay(
    updaterState: GithubState,
    onAction: (GithubAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.liquidGlass
    val composeView = LocalView.current
    val navHostView = remember(composeView) {
        composeView.rootView?.findViewById<View>(R.id.nav_host_fragment)
    }

    val currentDialog = updaterState.dialog
    val isVisible = currentDialog.shouldShowCapsule()

    var retainedDialog by remember { mutableStateOf<GithubDialog?>(null) }
    var retainedReleaseFile by remember { mutableStateOf<GithubFile?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentDialog) {
        if (currentDialog == null) {
            retainedReleaseFile = null
            showInfoSheet = false
        } else {
            val dialogState = currentDialog.state
            if (dialogState is GithubUpdateDialogState.UpdateFound) {
                retainedReleaseFile = dialogState.file
            }
            if (dialogState !is GithubUpdateDialogState.UpdateFound &&
                dialogState !is GithubUpdateDialogState.Error &&
                dialogState !is GithubUpdateDialogState.InstallError
            ) {
                showInfoSheet = false
            }
            if (currentDialog.shouldShowCapsule()) {
                retainedDialog = currentDialog
            }
        }
    }

    // Auto-dismiss user-initiated "No Update Found" toast-capsule after a brief interval
    LaunchedEffect(currentDialog?.state) {
        if (currentDialog?.state is GithubUpdateDialogState.NoUpdateFound && currentDialog.isFromUser) {
            delay(NO_UPDATE_AUTO_DISMISS_MS.milliseconds)
            onAction(GithubAction.Dismiss)
        }
    }

    val activeDialog = if (isVisible) currentDialog else retainedDialog
    val visualState = activeDialog?.toVisualState(retainedReleaseFile)

    val capsuleVisibility = remember { MutableTransitionState(false) }
    capsuleVisibility.targetState = isVisible && visualState != null
    val backdropActive = capsuleVisibility.currentState || capsuleVisibility.targetState

    val drawTickState = remember { mutableIntStateOf(0) }
    val useLiquidGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val hazeState = rememberHazeState()
    val windowBgColor = MaterialTheme.colorScheme.background
    val backdrop = rememberNavHostLayerBackdrop(
        navHostView = navHostView,
        composeView = composeView,
        drawTickState = drawTickState,
        windowBgColor = windowBgColor,
    )

    if (backdropActive && navHostView != null) {
        ObserveBackdropViewTree(
            navHostView = navHostView,
            composeView = composeView,
            drawTickState = drawTickState,
        )
    }

    val glowEnabled by LiquidGlassSettingsRepository.glowEnabled.collectAsState()
    val showGlow = !floatingNavigationGlowSupported || glowEnabled
    val glowStrength by animateFloatAsState(
        targetValue = if (showGlow) tokens.opacity.visible else tokens.opacity.invisible,
        animationSpec = tween(tokens.motion.slowMillis, easing = tokens.motion.standard),
        label = "update_capsule_glow_strength",
    )

    val context = LocalContext.current
    val density = LocalDensity.current
    val composeStatusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val composeCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val composeTopInset = maxOf(composeStatusBarsTop, composeCutoutTop)
    val fallbackStatusBarTop = remember(composeView, density, context) {
        val rootTopPx = ViewCompat.getRootWindowInsets(composeView)
            ?.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            ?.top ?: 0
        val dimenTopPx = context.getStatusBarHeight()
        with(density) { maxOf(rootTopPx, dimenTopPx).toDp() }
    }
    val extraTopInset = if (composeTopInset > LiquidGlassTokens.Space.none) {
        LiquidGlassTokens.Space.none
    } else {
        fallbackStatusBarTop
    }
    val horizontalSafeInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()

    CompositionLocalProvider(LocalLiquidGlassDrawTick provides drawTickState) {
        Box(modifier = modifier.fillMaxSize()) {
            // Shared full-screen background capture identical to MainActivity's bottom nav bar
            if (backdropActive && navHostView != null) {
                if (useLiquidGlass) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .liquidGlassSource(backdrop),
                    )
                } else {
                    val navLoc = remember { IntArray(2) }
                    val composeLoc = remember { IntArray(2) }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeSource(state = hazeState)
                            .drawBehind {
                                @Suppress("UNUSED_VARIABLE")
                                val tick = drawTickState.intValue
                                drawRect(windowBgColor)
                                if (navHostView.width > 0 && navHostView.height > 0 && navHostView.isAttachedToWindow) {
                                    navHostView.getLocationInWindow(navLoc)
                                    composeView.getLocationInWindow(composeLoc)
                                    val offsetX = (navLoc[0] - composeLoc[0]).toFloat()
                                    val offsetY = (navLoc[1] - composeLoc[1]).toFloat()
                                    translate(left = offsetX, top = offsetY) {
                                        drawIntoCanvas { canvas ->
                                            try {
                                                navHostView.draw(canvas.nativeCanvas)
                                            } catch (_: Throwable) {
                                            }
                                        }
                                    }
                                }
                            },
                    )
                }
            }

            // Non-blocking top-anchored Kyant Glass capsule
            AnimatedVisibility(
                visibleState = capsuleVisibility,
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = tokens.motion.sheetEnterMillis,
                        easing = tokens.motion.emphasized,
                    ),
                    initialOffsetY = { -it },
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = tokens.motion.normalMillis,
                        easing = tokens.motion.decelerate,
                    ),
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = tokens.motion.sheetExitMillis,
                        easing = tokens.motion.accelerate,
                    ),
                    targetOffsetY = { -it },
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = tokens.motion.fastMillis,
                        easing = tokens.motion.accelerate,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontalSafeInsets)
                    .padding(
                        top = extraTopInset + LiquidGlassTokens.Space.s56,
                        start = tokens.spacing.screenHorizontal,
                        end = tokens.spacing.screenHorizontal,
                    ),
            ) {
                if (visualState != null) {
                    FloatingUpdateCapsule(
                        visualState = visualState,
                        backdrop = backdrop,
                        hazeState = hazeState,
                        useLiquidGlass = useLiquidGlass,
                        glowStrength = glowStrength,
                        onOpenInfo = { showInfoSheet = true },
                        onAction = onAction,
                    )
                }
            }

            // Kyant Glass Changelog / Error details sheet opened from the `i` button
            val sheetState = if (showInfoSheet && isVisible) visualState else null
            val canShowSheet = sheetState is UpdateCapsuleVisualState.UpdateAvailable ||
                sheetState is UpdateCapsuleVisualState.Failed ||
                sheetState is UpdateCapsuleVisualState.InstallFailed

            BackHandler(enabled = canShowSheet) {
                showInfoSheet = false
            }

            AnimatedVisibility(
                visible = canShowSheet,
                enter = fadeIn(tween(tokens.motion.normalMillis, easing = tokens.motion.decelerate)),
                exit = fadeOut(tween(tokens.motion.fastMillis, easing = tokens.motion.accelerate)),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (sheetState != null) {
                    UpdateDetailsGlassSheet(
                        visualState = sheetState,
                        backdrop = backdrop,
                        hazeState = hazeState,
                        useLiquidGlass = useLiquidGlass,
                        glowStrength = glowStrength,
                        onDismissSheet = { showInfoSheet = false },
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun ObserveBackdropViewTree(
    navHostView: View,
    composeView: View,
    drawTickState: MutableIntState,
) {
    DisposableEffect(navHostView, composeView) {
        var lastPreDrawMs = 0L
        val scrollListener = ViewTreeObserver.OnScrollChangedListener {
            drawTickState.intValue++
        }
        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            drawTickState.intValue++
        }
        val preDrawListener = ViewTreeObserver.OnPreDrawListener {
            val now = SystemClock.uptimeMillis()
            if (now - lastPreDrawMs >= BACKDROP_PREDRAW_MIN_INTERVAL_MS) {
                lastPreDrawMs = now
                drawTickState.intValue++
            }
            true
        }
        val vto = composeView.viewTreeObserver
        vto.addOnScrollChangedListener(scrollListener)
        vto.addOnGlobalLayoutListener(globalLayoutListener)
        vto.addOnPreDrawListener(preDrawListener)
        onDispose {
            val disposeVto = composeView.viewTreeObserver
            if (disposeVto.isAlive) {
                disposeVto.removeOnScrollChangedListener(scrollListener)
                disposeVto.removeOnGlobalLayoutListener(globalLayoutListener)
                disposeVto.removeOnPreDrawListener(preDrawListener)
            }
        }
    }
}

@Composable
private fun rememberNavHostLayerBackdrop(
    navHostView: View?,
    composeView: View,
    drawTickState: MutableIntState,
    windowBgColor: Color,
): PlatformLayerBackdrop {
    val navLoc = remember { IntArray(2) }
    val composeLoc = remember { IntArray(2) }
    return rememberLayerBackdrop {
        @Suppress("UNUSED_VARIABLE")
        val tick = drawTickState.intValue
        drawRect(windowBgColor)
        if (navHostView != null && navHostView.width > 0 && navHostView.height > 0 && navHostView.isAttachedToWindow) {
            navHostView.getLocationInWindow(navLoc)
            composeView.getLocationInWindow(composeLoc)
            val offsetX = (navLoc[0] - composeLoc[0]).toFloat()
            val offsetY = (navLoc[1] - composeLoc[1]).toFloat()
            translate(left = offsetX, top = offsetY) {
                drawIntoCanvas { canvas ->
                    try {
                        navHostView.draw(canvas.nativeCanvas)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingUpdateCapsule(
    visualState: UpdateCapsuleVisualState,
    backdrop: PlatformLayerBackdrop,
    hazeState: HazeState,
    useLiquidGlass: Boolean,
    glowStrength: Float,
    onOpenInfo: () -> Unit,
    onAction: (GithubAction) -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val capsuleShape = CircleShape

    val downloadTargetProgress = when (visualState) {
        is UpdateCapsuleVisualState.Downloading -> {
            val total = visualState.total
            if (total != null && total > 0L) {
                (visualState.progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
        }
        is UpdateCapsuleVisualState.ReadyToInstall -> 1f
        else -> 0f
    }

    val isIndeterminateFlow =
        (visualState is UpdateCapsuleVisualState.Downloading && downloadTargetProgress == null) ||
            visualState is UpdateCapsuleVisualState.Checking

    val animatedProgress by animateFloatAsState(
        targetValue = downloadTargetProgress ?: 0f,
        animationSpec = tween(
            durationMillis = tokens.motion.normalMillis,
            easing = tokens.motion.standard,
        ),
        label = "update_water_flow_progress",
    )

    val showWaterFill = visualState is UpdateCapsuleVisualState.Downloading ||
        visualState is UpdateCapsuleVisualState.ReadyToInstall ||
        visualState is UpdateCapsuleVisualState.Checking

    val capsuleInteraction = remember { MutableInteractionSource() }
    val canTapBodyForInfo = visualState is UpdateCapsuleVisualState.UpdateAvailable ||
        visualState is UpdateCapsuleVisualState.Failed ||
        visualState is UpdateCapsuleVisualState.InstallFailed

    Box(
        modifier = Modifier
            .widthIn(max = tokens.components.sheetMaxWidth)
            .fillMaxWidth()
            .heightIn(min = LiquidGlassTokens.Space.s56)
            .clip(capsuleShape)
            .then(
                if (canTapBodyForInfo) {
                    Modifier.clickable(
                        interactionSource = capsuleInteraction,
                        indication = ripple(color = tokens.colors.textPrimary),
                        onClick = onOpenInfo,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 1. Exact Kyant Glass surface (matches FloatingNavigationBar)
        if (useLiquidGlass) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .liquidGlassChrome(
                        shape = capsuleShape,
                        enabled = true,
                        preset = LiquidGlassPreset.BottomNavigation,
                        backdrop = backdrop,
                    ),
            )
        } else {
            GlassBarSurface(
                hazeState = hazeState,
                modifier = Modifier.matchParentSize(),
                glowStrength = glowStrength,
            )
        }

        // 2. Smooth left-to-right straight-edged horizontal progress fill inside the glass capsule
        if (showWaterFill) {
            WaterFlowProgressOverlay(
                determinateProgress = if (isIndeterminateFlow) null else animatedProgress,
                modifier = Modifier.matchParentSize(),
            )
        }

        // 3. Foreground capsule content
        AnimatedContent(
            targetState = visualState,
            transitionSpec = {
                fadeIn(tween(tokens.motion.normalMillis, easing = tokens.motion.decelerate))
                    .togetherWith(
                        fadeOut(tween(tokens.motion.fastMillis, easing = tokens.motion.accelerate)),
                    )
            },
            contentKey = { state ->
                when (state) {
                    is UpdateCapsuleVisualState.UpdateAvailable -> "available_${state.file.nodeId}"
                    is UpdateCapsuleVisualState.Downloading -> "downloading"
                    is UpdateCapsuleVisualState.ReadyToInstall -> "ready"
                    is UpdateCapsuleVisualState.InstallFailed -> "install_failed"
                    UpdateCapsuleVisualState.Checking -> "checking"
                    UpdateCapsuleVisualState.NoUpdate -> "none"
                    is UpdateCapsuleVisualState.Failed -> "failed"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LiquidGlassTokens.Space.s10,
                    vertical = LiquidGlassTokens.Space.s8,
                ),
            label = "update_capsule_state_content",
        ) { state ->
            when (state) {
                is UpdateCapsuleVisualState.UpdateAvailable -> {
                    UpdateAvailableRow(
                        file = state.file,
                        onOpenInfo = onOpenInfo,
                        onUpdate = { onAction(GithubAction.Update(state.file)) },
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }

                is UpdateCapsuleVisualState.Downloading -> {
                    DownloadingRow(
                        progress = state.progress,
                        total = state.total,
                        onCancel = { onAction(GithubAction.Dismiss) },
                    )
                }

                is UpdateCapsuleVisualState.ReadyToInstall -> {
                    ReadyToInstallRow(
                        totalBytes = state.totalBytes,
                        onInstall = {
                            onAction(
                                GithubAction.Install(
                                    file = state.file,
                                    apkPath = state.apkPath,
                                    totalBytes = state.totalBytes,
                                ),
                            )
                        },
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }

                is UpdateCapsuleVisualState.InstallFailed -> {
                    FailedInstallRow(
                        file = state.file,
                        onOpenInfo = onOpenInfo,
                        onInstall = {
                            onAction(
                                GithubAction.Install(
                                    file = state.file,
                                    apkPath = state.apkPath,
                                    totalBytes = state.totalBytes,
                                ),
                            )
                        },
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }

                UpdateCapsuleVisualState.Checking -> {
                    CheckingUpdatesRow(
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }

                UpdateCapsuleVisualState.NoUpdate -> {
                    NoUpdateRow(
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }

                is UpdateCapsuleVisualState.Failed -> {
                    FailedUpdateRow(
                        onOpenInfo = onOpenInfo,
                        onRetry = { onAction(GithubAction.SearchForUpdate) },
                        onDismiss = { onAction(GithubAction.Dismiss) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterFlowProgressOverlay(
    determinateProgress: Float?,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.liquidGlass
    val reducedMotion = rememberLiquidGlassReducedMotion()
    val accent = tokens.colors.accent
    val focusRing = tokens.colors.focusRing
    val crestHighlight = tokens.colors.textPrimary

    val shouldAnimateShimmer = !reducedMotion &&
        (determinateProgress == null || (determinateProgress > 0.002f && determinateProgress < 0.998f))

    val shimmerPhase: Float
    if (shouldAnimateShimmer) {
        val transition = rememberInfiniteTransition(label = "water_flow_transition")
        val animatedShimmer by transition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = WATER_SHIMMER_CYCLE_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "water_shimmer_phase",
        )
        shimmerPhase = animatedShimmer
    } else {
        shimmerPhase = 0.5f
    }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        if (determinateProgress == null) {
            // Indeterminate smooth translucent glass ribbon gliding left-to-right
            val bandWidth = size.width * 0.42f
            val centerX = shimmerPhase * size.width
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.28f),
                        crestHighlight.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.24f),
                        Color.Transparent,
                    ),
                    startX = centerX - bandWidth,
                    endX = centerX + bandWidth,
                ),
            )
            return@Canvas
        }

        val fraction = determinateProgress.coerceIn(0f, 1f)
        if (fraction <= 0.001f) return@Canvas

        val fillWidth = (size.width * fraction).coerceIn(0f, size.width)
        val liquidBrush = Brush.horizontalGradient(
            colors = listOf(
                accent.copy(alpha = 0.28f),
                accent.copy(alpha = 0.44f),
                focusRing.copy(alpha = 0.50f),
            ),
            startX = 0f,
            endX = fillWidth.coerceAtLeast(size.width * 0.15f),
        )

        drawRect(
            brush = liquidBrush,
            size = Size(fillWidth, size.height),
        )

        // Subtle translucent luminance shimmer across the straight-edged progress fill
        if (!reducedMotion && fraction < 0.998f) {
            val shimmerCenter = shimmerPhase * fillWidth
            val shimmerSpread = (size.width * 0.24f).coerceAtLeast(size.height)
            clipRect(right = fillWidth) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            crestHighlight.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        startX = shimmerCenter - shimmerSpread,
                        endX = shimmerCenter + shimmerSpread,
                    ),
                    size = Size(fillWidth, size.height),
                )
            }
        }

        // Crisp straight vertical glass edge highlight while filling
        if (fraction > 0.01f && fraction < 0.998f) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        crestHighlight.copy(alpha = 0.14f),
                        crestHighlight.copy(alpha = 0.44f),
                        crestHighlight.copy(alpha = 0.14f),
                    ),
                ),
                start = Offset(fillWidth, 0f),
                end = Offset(fillWidth, size.height),
                strokeWidth = tokens.borders.thin.toPx(),
            )
        }
    }
}

@Composable
private fun UpdateAvailableRow(
    file: GithubFile,
    onOpenInfo: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val title = formatVersionTitle(stringResource(R.string.app_name), file)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(icon = AppIcons.Download)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.update_available_subtitle),
                style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LiquidGlassTokens.Space.s6),
        ) {
            CapsuleCircleIconButton(
                icon = AppIcons.Info,
                contentDescription = stringResource(R.string.update_changelog_action_desc),
                onClick = onOpenInfo,
            )

            CapsulePrimaryPillButton(
                text = stringResource(R.string.update),
                onClick = onUpdate,
            )

            CapsuleCircleIconButton(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.update_dismiss_desc),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun DownloadingRow(
    progress: Long,
    total: Long?,
    onCancel: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val context = LocalContext.current
    val progressSubtitle = if (total != null && total > 0L) {
        val fraction = (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        val percent = (fraction * 100f).toInt().coerceIn(0, 99)
        "$percent% • ${formatFileSize(context, progress)} / ${formatFileSize(context, total)}"
    } else {
        formatFileSize(context, progress)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(icon = AppIcons.Download)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.update_notification_downloading),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progressSubtitle,
                style = MaterialTheme.liquidGlassTypeScale.timer.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.cancel),
            onClick = onCancel,
        )
    }
}

@Composable
private fun ReadyToInstallRow(
    totalBytes: Long,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val context = LocalContext.current
    val subtitle = "100% • ${formatFileSize(context, totalBytes)}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(
            icon = AppIcons.Check,
            tint = tokens.colors.success,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.update_ready_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.liquidGlassTypeScale.timer.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsulePrimaryPillButton(
            text = stringResource(R.string.update_install_action),
            onClick = onInstall,
        )

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.update_dismiss_desc),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun CheckingUpdatesRow(
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(icon = AppIcons.Refresh)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.update_checking_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.cancel),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun NoUpdateRow(
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(
            icon = AppIcons.Check,
            tint = tokens.colors.success,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.no_update_found),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.update_dismiss_desc),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun FailedUpdateRow(
    onOpenInfo: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(
            icon = AppIcons.Info,
            tint = tokens.colors.danger,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.download_failed),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsuleCircleIconButton(
            icon = AppIcons.Info,
            contentDescription = stringResource(R.string.update_changelog_action_desc),
            onClick = onOpenInfo,
        )

        CapsulePrimaryPillButton(
            text = stringResource(R.string.update_retry_action),
            onClick = onRetry,
        )

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.update_dismiss_desc),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun FailedInstallRow(
    file: GithubFile,
    onOpenInfo: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
    ) {
        CapsuleLeadingBadge(
            icon = AppIcons.Info,
            tint = tokens.colors.danger,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.update_notification_failed),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatVersionTitle(stringResource(R.string.app_name), file),
                style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                    color = tokens.colors.textSecondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CapsuleCircleIconButton(
            icon = AppIcons.Info,
            contentDescription = stringResource(R.string.update_changelog_action_desc),
            onClick = onOpenInfo,
        )

        CapsulePrimaryPillButton(
            text = stringResource(R.string.update_install_action),
            onClick = onInstall,
        )

        CapsuleCircleIconButton(
            icon = AppIcons.Close,
            contentDescription = stringResource(R.string.update_dismiss_desc),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun CapsuleLeadingBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.liquidGlass.colors.textPrimary,
) {
    val tokens = MaterialTheme.liquidGlass
    Box(
        modifier = Modifier
            .size(LiquidGlassTokens.Space.s32)
            .clip(CircleShape)
            .background(tokens.colors.textPrimary.copy(alpha = tokens.opacity.hover))
            .border(tokens.borders.hairline, tokens.colors.borderSubtle, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(tokens.icons.sm),
        )
    }
}

@Composable
private fun CapsuleCircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(LiquidGlassTokens.Space.s32)
            .liquidGlassPressFeedback(interactionSource)
            .clip(CircleShape)
            .background(tokens.colors.textPrimary.copy(alpha = tokens.opacity.hover))
            .border(tokens.borders.hairline, tokens.colors.borderSubtle, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tokens.colors.textPrimary),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tokens.colors.textPrimary,
            modifier = Modifier.size(tokens.icons.sm),
        )
    }
}

@Composable
private fun CapsulePrimaryPillButton(
    text: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s32)
            .liquidGlassPressFeedback(interactionSource)
            .clip(CircleShape)
            .background(tokens.colors.textPrimary)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tokens.colors.textInverse),
                onClick = onClick,
            )
            .padding(horizontal = LiquidGlassTokens.Space.s12),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = tokens.colors.textInverse,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CapsuleSecondaryPillButton(
    text: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(LiquidGlassTokens.Space.s36)
            .liquidGlassPressFeedback(interactionSource)
            .clip(CircleShape)
            .background(tokens.colors.textPrimary.copy(alpha = tokens.opacity.hover))
            .border(tokens.borders.hairline, tokens.colors.borderDefault, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tokens.colors.textPrimary),
                onClick = onClick,
            )
            .padding(horizontal = LiquidGlassTokens.Space.s16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Medium,
                color = tokens.colors.textPrimary,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UpdateDetailsGlassSheet(
    visualState: UpdateCapsuleVisualState,
    backdrop: PlatformLayerBackdrop,
    hazeState: HazeState,
    useLiquidGlass: Boolean,
    glowStrength: Float,
    onDismissSheet: () -> Unit,
    onAction: (GithubAction) -> Unit,
) {
    val tokens = MaterialTheme.liquidGlass
    val sheetShape = tokens.shapes.sheet
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.overlayScrim)
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismissSheet,
            )
            .padding(safeInsets)
            .padding(
                horizontal = tokens.spacing.screenHorizontal,
                vertical = tokens.spacing.sectionGap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = tokens.components.sheetMaxWidth)
                .fillMaxWidth()
                .clip(sheetShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            if (useLiquidGlass) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .liquidGlassChrome(
                            shape = sheetShape,
                            enabled = true,
                            preset = LiquidGlassPreset.BottomNavigation,
                            backdrop = backdrop,
                        ),
                )
            } else {
                GlassBarSurface(
                    hazeState = hazeState,
                    modifier = Modifier.matchParentSize(),
                    glowStrength = glowStrength,
                    fixedCornerRadius = LiquidGlassTokens.Radius.sheet,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.colors.surfaceSheet.copy(alpha = tokens.opacity.overlayLight))
                    .padding(tokens.spacing.sheetPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
            ) {
                when (visualState) {
                    is UpdateCapsuleVisualState.UpdateAvailable -> {
                        val file = visualState.file
                        val title = formatVersionTitle(stringResource(R.string.app_name), file)
                        val currentVersionText = stringResource(
                            R.string.update_current_version_format,
                            BuildConfig.VERSION_NAME,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.liquidGlassTypeScale.titleMd.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        color = tokens.colors.textPrimary,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = currentVersionText,
                                    style = MaterialTheme.liquidGlassTypeScale.labelSm.copy(
                                        color = tokens.colors.textSecondary,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            CapsuleCircleIconButton(
                                icon = AppIcons.Close,
                                contentDescription = stringResource(R.string.update_dismiss_desc),
                                onClick = onDismissSheet,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = tokens.components.dialogMaxWidth)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            val notesText = file.changeLog.trim()
                            if (notesText.isNotEmpty()) {
                                Text(
                                    text = parseMarkdown(notesText),
                                    style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                        color = tokens.colors.textSecondary,
                                    ),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.update_no_changelog),
                                    style = MaterialTheme.liquidGlassTypeScale.bodyMd.copy(
                                        color = tokens.colors.textMuted,
                                    ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                tokens.spacing.controlGap,
                                Alignment.End,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CapsuleSecondaryPillButton(
                                text = stringResource(R.string.skip_update),
                                onClick = {
                                    onDismissSheet()
                                    onAction(GithubAction.SkipUpdate(file))
                                    onAction(GithubAction.Dismiss)
                                },
                            )
                            CapsulePrimaryPillButton(
                                text = stringResource(R.string.update),
                                onClick = {
                                    onDismissSheet()
                                    onAction(GithubAction.Update(file))
                                },
                            )
                        }
                    }

                    is UpdateCapsuleVisualState.Failed,
                    is UpdateCapsuleVisualState.InstallFailed -> {
                        val errorThrowable = when (visualState) {
                            is UpdateCapsuleVisualState.Failed -> visualState.error
                            is UpdateCapsuleVisualState.InstallFailed -> visualState.error
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                        ) {
                            Text(
                                text = stringResource(R.string.update_error_details_title),
                                style = MaterialTheme.liquidGlassTypeScale.titleMd.copy(
                                    fontFamily = PlusJakartaSansFontFamily,
                                    color = tokens.colors.textPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            )

                            CapsuleCircleIconButton(
                                icon = AppIcons.Close,
                                contentDescription = stringResource(R.string.update_dismiss_desc),
                                onClick = onDismissSheet,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = tokens.components.dialogMaxWidth)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = errorThrowable.getStackTracePretty(),
                                style = MaterialTheme.liquidGlassTypeScale.bodySm.copy(
                                    fontFamily = InterFontFamily,
                                    color = tokens.colors.textSecondary,
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                tokens.spacing.controlGap,
                                Alignment.End,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CapsuleSecondaryPillButton(
                                text = stringResource(R.string.ok),
                                onClick = {
                                    onDismissSheet()
                                    onAction(GithubAction.Dismiss)
                                },
                            )
                            CapsulePrimaryPillButton(
                                text = stringResource(
                                    if (visualState is UpdateCapsuleVisualState.InstallFailed) {
                                        R.string.update_install_action
                                    } else {
                                        R.string.update_retry_action
                                    },
                                ),
                                onClick = {
                                    onDismissSheet()
                                    if (visualState is UpdateCapsuleVisualState.InstallFailed) {
                                        onAction(
                                            GithubAction.Install(
                                                file = visualState.file,
                                                apkPath = visualState.apkPath,
                                                totalBytes = visualState.totalBytes,
                                            ),
                                        )
                                    } else {
                                        onAction(GithubAction.SearchForUpdate)
                                    }
                                },
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}
