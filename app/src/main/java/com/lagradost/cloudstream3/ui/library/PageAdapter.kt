package com.lagradost.cloudstream3.ui.library

import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.ui.AutofitRecyclerView
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.home.ComposeViewBinding
import com.lagradost.cloudstream3.ui.home.PosterCardFromSearchResponse
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_SHOW_METADATA
import com.lagradost.cloudstream3.ui.search.SearchClickCallback

class PageAdapter(
    private val resView: AutofitRecyclerView,
    val clickCallback: (SearchClickCallback) -> Unit
) : NoStateAdapter<SyncAPI.LibraryItem>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
    if (a.id != null || b.id != null) {
        a.id == b.id
    } else {
        a.name == b.name && a.url == b.url
    }
})) {

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }
        return ViewHolderState(ComposeViewBinding(composeView))
    }

    override fun onClearView(holder: ViewHolderState<Any>) {
        ((holder.view as? ComposeViewBinding)?.composeView ?: holder.itemView as? ComposeView)?.disposeComposition()
    }

    override fun onBindContent(
        holder: ViewHolderState<Any>,
        item: SyncAPI.LibraryItem,
        position: Int
    ) {
        val composeView = (holder.view as? ComposeViewBinding)?.composeView ?: holder.itemView as? ComposeView
        if (composeView != null) {
            composeView.setContent {
                LiquidGlassTheme {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val targetWidth = (maxWidth - 8.dp).coerceAtLeast(10.dp)
                        PosterCardFromSearchResponse(
                            item = item,
                            customWidth = targetWidth,
                            onClick = {
                                clickCallback(
                                    SearchClickCallback(
                                        action = SEARCH_ACTION_LOAD,
                                        view = holder.itemView,
                                        position = position,
                                        card = item
                                    )
                                )
                            },
                            onLongClick = {
                                clickCallback(
                                    SearchClickCallback(
                                        action = SEARCH_ACTION_SHOW_METADATA,
                                        view = holder.itemView,
                                        position = position,
                                        card = item
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
