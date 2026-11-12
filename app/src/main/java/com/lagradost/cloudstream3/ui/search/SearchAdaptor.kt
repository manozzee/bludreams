package com.lagradost.cloudstream3.ui.search

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.databinding.SearchResultGridBinding
import com.lagradost.cloudstream3.databinding.SearchResultGridExpandedBinding
import com.lagradost.cloudstream3.ui.AutofitRecyclerView
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.home.ComposeViewBinding
import com.lagradost.cloudstream3.ui.home.PosterCardFromSearchResponse
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.liquidglass.PosterShape
import com.lagradost.cloudstream3.ui.newSharedPool

/** Click */
const val SEARCH_ACTION_LOAD = 0

/** Long press */
const val SEARCH_ACTION_SHOW_METADATA = 1
const val SEARCH_ACTION_PLAY_FILE = 2
const val SEARCH_ACTION_FOCUSED = 4

class SearchClickCallback(
    val action: Int,
    val view: View,
    val position: Int,
    val card: SearchResponse
)

class SearchAdapter(
    private val resView: AutofitRecyclerView,
    private val isHorizontal: Boolean = false,
    private val clickCallback: (SearchClickCallback) -> Unit,
) : NoStateAdapter<SearchResponse>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
    if (a.id != null || b.id != null) {
        a.id == b.id
    } else {
        a.name == b.name
    }
})) {
    companion object {
        val sharedPool =
            newSharedPool { setMaxRecycledViews(CONTENT, 15) }
    }

    var hasNext: Boolean = false

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
        clearImage(
            when (val binding = holder.view) {
                is SearchResultGridExpandedBinding -> binding.imageView
                is SearchResultGridBinding -> binding.imageView
                else -> null
            }
        )
    }

    override fun onBindContent(holder: ViewHolderState<Any>, item: SearchResponse, position: Int) {
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
                            forceShape = if (isHorizontal) PosterShape.Landscape else null,
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
        } else {
            SearchResultBuilder.bind(clickCallback, item, position, holder.view.root)
        }
    }
}
