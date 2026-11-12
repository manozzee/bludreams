package com.lagradost.cloudstream3.ui.poster

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalPosterCardStyle = staticCompositionLocalOf<PosterCardStyleUiState?> { null }

@Composable
fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    LocalPosterCardStyle.current?.let { return it }
    val context = LocalContext.current
    val stateFlow = remember(context) {
        PosterCardStyleRepository.ensureLoaded(context)
        PosterCardStyleRepository.state
    }
    val state by stateFlow.collectAsState()
    return state
}
