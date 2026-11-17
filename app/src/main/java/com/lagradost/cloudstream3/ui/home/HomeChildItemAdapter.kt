package com.lagradost.cloudstream3.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.preference.PreferenceManager
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.databinding.HomeRemoveGridBinding
import com.lagradost.cloudstream3.databinding.HomeRemoveGridExpandedBinding
import com.lagradost.cloudstream3.databinding.HomeResultGridBinding
import com.lagradost.cloudstream3.databinding.HomeResultGridExpandedBinding
import com.lagradost.cloudstream3.ui.BaseAdapter
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.newSharedPool
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.ui.home.PosterCardFromSearchResponse
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.liquidglass.PosterShape
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchResultBuilder
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.UIHelper.isBottomLayout
import com.lagradost.cloudstream3.utils.UIHelper.toPx

class ComposeViewBinding(val composeView: ComposeView) : ViewBinding {
    override fun getRoot(): View = composeView
}

class HomeScrollViewHolderState(view: ViewBinding) : ViewHolderState<Boolean>(view) {
    // very shitty that we cant store the state when the view clears,
    // but this is because the focus clears before the view is removed
    // so we have to manually store it
    var wasFocused: Boolean = false
    override fun save(): Boolean = wasFocused
    override fun restore(state: Boolean) {
        if (state) {
            wasFocused = false
            // only refocus if tv
            if (isLayout(TV)) {
                itemView.requestFocus()
            }
        }
    }
}

class ResumeItemAdapter(
    nextFocusUp: Int? = null,
    nextFocusDown: Int? = null,
    clickCallback: (SearchClickCallback) -> Unit,
    private val removeCallback: (View) -> Unit,
) : HomeChildItemAdapter(
    id = "resumeAdapter".hashCode(),
    nextFocusUp = nextFocusUp,
    nextFocusDown = nextFocusDown,
    clickCallback = clickCallback
) {
    // As there is no popup on TV we instead use the footer to clear
    override val footers = if (isLayout(TV or EMULATOR)) 1 else 0

    override fun onCreateFooter(parent: ViewGroup): ViewHolderState<Boolean> {
        val expanded = parent.context.isBottomLayout()
        val inflater = LayoutInflater.from(parent.context)
        val binding = if (expanded) HomeRemoveGridExpandedBinding.inflate(
            inflater,
            parent,
            false
        ) else HomeRemoveGridBinding.inflate(inflater, parent, false)
        return HomeScrollViewHolderState(binding)
    }

    override fun onClearView(holder: ViewHolderState<Boolean>) {
        // Clear the image, idk if this saves ram or not, but I guess?
        clearImage(holder.view.root.findViewById(R.id.imageView))
    }

    override fun onBindFooter(holder: ViewHolderState<Boolean>) {
        this.applyBinding(holder, false)
        when (val binding = holder.view) {
            is HomeRemoveGridBinding -> {
                updateLayoutParms(binding.backgroundCard, setWidth, setHeight)
            }

            is HomeRemoveGridExpandedBinding -> {
                updateLayoutParms(binding.backgroundCard, setWidth, setHeight)
            }
        }
        holder.itemView.apply {
            if (isLayout(TV)) {
                isFocusableInTouchMode = true
                isFocusable = true
            }
            nextFocusUp?.let {
                nextFocusUpId = it
            }
            nextFocusDown?.let {
                nextFocusDownId = it
            }

            setOnClickListener { v ->
                removeCallback.invoke(v ?: return@setOnClickListener)
            }
        }
    }
}

/** Remember to set `updatePosterSize` to cache the poster size,
 * otherwise the width and height is unset */
open class HomeChildItemAdapter(
    id: Int,
    var nextFocusUp: Int? = null,
    var nextFocusDown: Int? = null,
    var clickCallback: (SearchClickCallback) -> Unit,
) :
    BaseAdapter<SearchResponse, Boolean>(
        id, diffCallback = BaseDiffCallback(
            itemSame = { a, b ->
                a.url == b.url && a.name == b.name
            },
            contentSame = { a, b ->
                a == b
            })
    ) {
    var hasNext: Boolean = false
    var isHorizontal: Boolean = false
        set(value) {
            field = value
            updateCachedPosterSize()
        }

    private fun updateCachedPosterSize() {
        setWidth = if (!isHorizontal) {
            minPosterSize
        } else {
            maxPosterSize
        }
        setHeight = if (!isHorizontal) {
            maxPosterSize
        } else {
            minPosterSize
        }
    }

    init {
        updateCachedPosterSize()
    }

    protected var setWidth = 0
    protected var setHeight = 0

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Boolean> {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }
        return HomeScrollViewHolderState(ComposeViewBinding(composeView))
    }

    override fun onClearView(holder: ViewHolderState<Boolean>) {
        ((holder.view as? ComposeViewBinding)?.composeView ?: holder.itemView as? ComposeView)?.disposeComposition()
        val imageView = holder.view.root.findViewById<ImageView>(R.id.imageView)
        if (imageView != null) {
            clearImage(imageView)
        }
    }

    companion object {
        // The vast majority of the lag comes from creating the view
        // This simply shares the views between all HomeChildItemAdapter
        val sharedPool =
            newSharedPool { setMaxRecycledViews(CONTENT, 20) }

        var minPosterSize: Int = 0
        var maxPosterSize: Int = 0

        fun updatePosterSize(context: Context, value: Int? = null) {
            val scale = value ?: PreferenceManager.getDefaultSharedPreferences(context)
                ?.getInt(context.getString(R.string.poster_size_key), 0) ?: 0
            // Scale by +10% per step
            val mul = 1.0f + scale * 0.1f
            minPosterSize = (114.toPx.toFloat() * mul).toInt()
            maxPosterSize = (180.toPx.toFloat() * mul).toInt()
        }

        fun updateLayoutParms(layout: FrameLayout, width: Int, height: Int) {
            val params = layout.layoutParams
            if (params.height == height && params.width == width) return

            params.width = width
            params.height = height

            layout.layoutParams = params
        }
    }

    protected fun applyBinding(holder: ViewHolderState<Boolean>, isFirstItem: Boolean) {
        when (val binding = holder.view) {
            is HomeResultGridBinding -> {
                updateLayoutParms(binding.backgroundCard, setWidth, setHeight)
            }

            is HomeResultGridExpandedBinding -> {
                updateLayoutParms(binding.backgroundCard, setWidth, setHeight)

                if (isFirstItem) { // to fix tv
                    binding.backgroundCard.nextFocusLeftId = R.id.nav_rail_view
                }
            }
        }
    }

    override fun onBindContent(
        holder: ViewHolderState<Boolean>,
        item: SearchResponse,
        position: Int
    ) {
        val composeView = (holder.view as? ComposeViewBinding)?.composeView ?: holder.itemView as? ComposeView
        if (composeView != null) {
            composeView.setContent {
                LiquidGlassTheme {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        PosterCardFromSearchResponse(
                            item = item,
                            forceShape = if (isHorizontal) PosterShape.Landscape else null,
                            onClick = {
                                (holder as? HomeScrollViewHolderState)?.wasFocused = true
                                clickCallback(
                                    SearchClickCallback(
                                        action = SEARCH_ACTION_LOAD,
                                        view = holder.itemView,
                                        position = position,
                                        card = item
                                    )
                                )
                            },
                        )
                    }
                }
            }
        } else {
            applyBinding(holder, position == 0)

            SearchResultBuilder.bind(
                clickCallback = { click ->
                    when (click.action) {
                        SEARCH_ACTION_LOAD -> (holder as? HomeScrollViewHolderState)?.wasFocused = true
                    }
                    clickCallback(click)
                },
                item,
                position,
                holder.itemView,
                nextFocusUp,
                nextFocusDown
            )
        }

        holder.itemView.tag = position
    }
}
