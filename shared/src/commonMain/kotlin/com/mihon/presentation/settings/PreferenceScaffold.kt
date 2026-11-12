package com.mihon.presentation.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.mihon.material.Scaffold

@Composable
fun PreferenceScaffold(
    @Suppress("UNUSED_PARAMETER") title: String,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
                onBackPressed = onBackPressed,
                actions = actions,
            )
        },
    )
}
