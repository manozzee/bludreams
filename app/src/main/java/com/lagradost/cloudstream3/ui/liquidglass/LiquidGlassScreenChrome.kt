package com.lagradost.cloudstream3.ui.liquidglass

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream4.compose.focusOutline
import kotlin.math.roundToInt

@Composable
internal fun rememberLiquidGlassReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

@Composable
internal fun Modifier.liquidGlassPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberLiquidGlassReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "liquidGlassPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
internal fun LiquidGlassCapsule(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 999.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius)),
        propagateMinConstraints = false,
    ) {
        GlassBarSurface(
            hazeState = null,
            modifier = Modifier.matchParentSize(),
            fixedCornerRadius = cornerRadius,
        )
        content()
    }
}

@Composable
internal fun LiquidGlassActionButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    if (label == null) {
        Box(
            modifier = modifier
                .size(40.dp)
                .liquidGlassPressFeedback(interactionSource)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        Box(
            modifier = modifier
                .height(40.dp)
                .liquidGlassPressFeedback(interactionSource)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White),
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CircularActionButton(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    @DrawableRes drawableRes: Int? = null,
    isActive: Boolean = false,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val glyphTint = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    Box(
        modifier = modifier
            .size(size)
            .liquidGlassPressFeedback(interactionSource)
            .clip(CircleShape)
            .background(bgColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = glyphTint,
                modifier = Modifier.size(iconSize),
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = glyphTint,
                modifier = Modifier.size(iconSize),
            )
        } else if (drawableRes != null) {
            Icon(
                painter = painterResource(drawableRes),
                contentDescription = contentDescription,
                tint = glyphTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun CircularActionButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = CircularActionButton(
    contentDescription = contentDescription,
    modifier = modifier,
    icon = icon,
    painter = null,
    drawableRes = null,
    isActive = isActive,
    size = size,
    iconSize = iconSize,
    onLongClick = onLongClick,
    onClick = onClick,
)

@Composable
fun CircularActionButton(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = CircularActionButton(
    contentDescription = contentDescription,
    modifier = modifier,
    icon = null,
    painter = painter,
    drawableRes = null,
    isActive = isActive,
    size = size,
    iconSize = iconSize,
    onLongClick = onLongClick,
    onClick = onClick,
)

@Composable
fun CircularActionButton(
    @DrawableRes drawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = CircularActionButton(
    contentDescription = contentDescription,
    modifier = modifier,
    icon = null,
    painter = null,
    drawableRes = drawableRes,
    isActive = isActive,
    size = size,
    iconSize = iconSize,
    onLongClick = onLongClick,
    onClick = onClick,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    autoFocus: Boolean = true,
    horizontalPadding: Dp? = null,
    isLoading: Boolean = false,
    onSearch: ((String) -> Unit)? = null,
) {
    var hasFocus by remember { mutableStateOf(false) }
    var isWritable by remember { mutableStateOf(false) }
    val focusProgress by animateFloatAsState(targetValue = if (hasFocus) 1.0f else 0.0f)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val isTv = isLayout(TV)

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
            showKeyboardOnFocus = if (isTv) isWritable else true,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke(query)
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (horizontalPadding != null) {
                    Modifier.padding(horizontal = horizontalPadding)
                } else {
                    Modifier.padding(horizontal = 24.dp - 8.dp * focusProgress)
                }
            )
            .focusOutline(enabled = isTv, CircleShape)
            .onFocusChanged { newFocus ->
                hasFocus = newFocus.hasFocus
                if (!newFocus.hasFocus) {
                    isWritable = false
                }
            }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (isWritable || !isTv || !hasFocus || keyEvent.type != KeyEventType.KeyUp) {
                    return@onPreviewKeyEvent false
                }

                return@onPreviewKeyEvent when (keyEvent.key) {
                    Key.Enter, Key.DirectionCenter, Key.NumPadEnter, Key.Spacebar -> {
                        isWritable = true
                        true
                    }
                    else -> false
                }
            },
        placeholder = {
            Text(
                text = stringResource(R.string.search_hint),
                color = Color.White.copy(alpha = 0.5f),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        },
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
        ),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            focusedLeadingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
            focusedTrailingIconColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color.White,
        ),
        leadingIcon = {
            Crossfade(
                targetState = hasFocus,
                label = "leftsearch",
            ) { value ->
                if (value) {
                    IconButton(onClick = {
                        onClear()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_arrow_left_24px),
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                } else {
                    IconButton(onClick = {
                        focusRequester.requestFocus()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        },
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Crossfade(
                    targetState = hasFocus || query.isNotEmpty(),
                    label = "rightsearch",
                ) { value ->
                    if (value) {
                        IconButton(onClick = onClear) {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        },
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }
}

@Composable
fun SearchHeaderBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onVoiceClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSearchBox(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f),
            onClear = onClear,
            autoFocus = false,
            horizontalPadding = 0.dp,
            isLoading = isLoading,
            onSearch = onSearch,
        )

        CircularActionButton(
            drawableRes = R.drawable.ic_mic,
            contentDescription = "Voice search",
            size = 48.dp,
            iconSize = 22.dp,
            onClick = onVoiceClick,
        )

        CircularActionButton(
            drawableRes = R.drawable.ic_baseline_tune_24,
            contentDescription = stringResource(R.string.change_providers_img_des),
            size = 48.dp,
            iconSize = 22.dp,
            onClick = onFilterClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSearchBox(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    var hasFocus by remember { mutableStateOf(false) }
    var isWritable by remember { mutableStateOf(false) }
    val focusProgress by animateFloatAsState(targetValue = if (hasFocus) 1.0f else 0.0f)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val isTv = isLayout(TV)

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    TextField(
        state = textFieldState,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
            showKeyboardOnFocus = if (isTv) isWritable else true,
        ),
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp - 8.dp * focusProgress)
            .focusOutline(enabled = isTv, CircleShape)
            .onFocusChanged { newFocus ->
                hasFocus = newFocus.hasFocus
                if (!newFocus.hasFocus) {
                    isWritable = false
                }
            }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (isWritable || !isTv || !hasFocus || keyEvent.type != KeyEventType.KeyUp) {
                    return@onPreviewKeyEvent false
                }

                return@onPreviewKeyEvent when (keyEvent.key) {
                    Key.Enter, Key.DirectionCenter, Key.NumPadEnter, Key.Spacebar -> {
                        isWritable = true
                        true
                    }
                    else -> false
                }
            },
        placeholder = {
            Text(
                text = stringResource(R.string.search_hint),
                color = Color.White.copy(alpha = 0.5f),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        },
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
        ),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            focusedLeadingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
            focusedTrailingIconColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color.White,
        ),
        leadingIcon = {
            Crossfade(
                targetState = hasFocus,
                label = "leftsearch",
            ) { value ->
                if (value) {
                    IconButton(onClick = {
                        textFieldState.edit { replace(0, length, "") }
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_arrow_left_24px),
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                } else {
                    IconButton(onClick = {
                        focusRequester.requestFocus()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        },
        trailingIcon = {
            Crossfade(
                targetState = hasFocus,
                label = "rightsearch",
            ) { value ->
                if (value) {
                    IconButton(onClick = {
                        textFieldState.edit { replace(0, length, "") }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        },
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }
}

@Composable
internal fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
) {
    SettingsSearchBox(
        query = query,
        onQueryChange = onQueryChange,
        modifier = modifier,
        onClear = onClear,
        autoFocus = true,
    )
}

@Composable
internal fun LiquidGlassLibraryHeader(
    title: String,
    modifier: Modifier = Modifier,
    sourceName: String? = null,
    isSearchOpen: Boolean = false,
    searchQuery: String = "",
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSelectProvider: () -> Unit = {},
    onSelectSource: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    sortOptions: List<String> = emptyList(),
    selectedSortIndex: Int = -1,
    onSelectSort: (Int) -> Unit = {},
) {
    var sortExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!sourceName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.65f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularActionButton(
                    icon = LiquidGlassIcons.Extension,
                    contentDescription = "Open with",
                    onClick = onSelectProvider,
                )

                CircularActionButton(
                    icon = LiquidGlassIcons.FilterList,
                    contentDescription = "Select library source",
                    onClick = onSelectSource,
                )

                CircularActionButton(
                    icon = LiquidGlassIcons.Download,
                    contentDescription = "Downloads",
                    onClick = onOpenDownloads,
                )

                Box {
                    CircularActionButton(
                        icon = LiquidGlassIcons.Sort,
                        contentDescription = "Sort",
                        isActive = sortExpanded,
                        onClick = { sortExpanded = !sortExpanded },
                    )

                    if (sortExpanded && sortOptions.isNotEmpty()) {
                        LiquidGlassSortMenu(
                            options = sortOptions,
                            selectedIndex = selectedSortIndex,
                            onSelect = { index ->
                                sortExpanded = false
                                onSelectSort(index)
                            },
                            onDismiss = { sortExpanded = false },
                        )
                    }
                }

                CircularActionButton(
                    painter = painterResource(if (isSearchOpen) R.drawable.close_24px else R.drawable.search_icon),
                    contentDescription = "Search",
                    isActive = isSearchOpen,
                    onClick = onToggleSearch,
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                LibrarySearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClear = { onSearchQueryChange("") },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
internal fun LiquidGlassCategoryChips(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (titles.isEmpty()) return
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val shape = RoundedCornerShape(16.dp)
            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = shape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.White),
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xCCFFFFFF),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}



@Composable
internal fun LiquidGlassSortMenu(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val reducedMotion = rememberLiquidGlassReducedMotion()

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, 48),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = if (reducedMotion) {
                fadeIn(snap())
            } else {
                fadeIn(tween(180)) + scaleIn(
                    tween(180, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                    initialScale = 0.85f,
                    transformOrigin = TransformOrigin(1f, 0f),
                )
            },
            exit = if (reducedMotion) {
                fadeOut(snap())
            } else {
                fadeOut(tween(140)) + scaleOut(
                    tween(140),
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(1f, 0f),
                )
            },
        ) {
            LiquidGlassCapsule(
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 280.dp)
                    .wrapContentHeight()
                    .border(
                        0.75.dp,
                        Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(18.dp),
                    ),
                cornerRadius = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == selectedIndex
                        val itemInteraction = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = ripple(color = Color.White),
                                    onClick = { onSelect(index) },
                                )
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = LiquidGlassIcons.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
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
internal fun LiquidGlassDownloadsToolbar(
    title: String,
    modifier: Modifier = Modifier,
    queueLabel: String? = null,
    showPlay: Boolean = true,
    onPlay: () -> Unit = {},
    onStream: () -> Unit = {},
    onQueue: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // Single row layout for tablets / landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showPlay) {
                        LiquidGlassActionButton(
                            icon = LiquidGlassIcons.Play,
                            contentDescription = "Open local video",
                            onClick = onPlay,
                        )
                    }

                    LiquidGlassActionButton(
                        icon = LiquidGlassIcons.NetworkStream,
                        contentDescription = "Network stream",
                        label = "Stream",
                        onClick = onStream,
                    )

                    LiquidGlassActionButton(
                        icon = LiquidGlassIcons.Download,
                        contentDescription = "Download queue",
                        label = queueLabel ?: "Queue",
                        trailingIcon = LiquidGlassIcons.ChevronUp,
                        onClick = onQueue,
                    )
                }
            }
        } else {
            // Mobile layout: Title on top row, action buttons beneath
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showPlay) {
                        LiquidGlassActionButton(
                            icon = LiquidGlassIcons.Play,
                            contentDescription = "Open local video",
                            onClick = onPlay,
                        )
                    }

                    LiquidGlassActionButton(
                        icon = LiquidGlassIcons.NetworkStream,
                        contentDescription = "Network stream",
                        label = "Stream",
                        onClick = onStream,
                    )

                    LiquidGlassActionButton(
                        icon = LiquidGlassIcons.Download,
                        contentDescription = "Download queue",
                        label = queueLabel ?: "Queue",
                        trailingIcon = LiquidGlassIcons.ChevronUp,
                        onClick = onQueue,
                    )
                }
            }
        }
    }
}
