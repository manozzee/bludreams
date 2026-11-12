package com.lagradost.cloudstream3.ui.library

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.view.allViews
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.openBrowser
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.databinding.FragmentLibraryBinding
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.debugAssert
import com.lagradost.cloudstream3.mvvm.observe
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.ui.AutofitRecyclerView
import com.lagradost.cloudstream3.ui.BaseFragment
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassCategoryChips
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassLibraryHeader
import com.lagradost.cloudstream3.ui.liquidglass.LiquidGlassTheme
import com.lagradost.cloudstream3.ui.poster.PosterCardStyleRepository
import com.lagradost.cloudstream3.ui.quicksearch.QuickSearchFragment
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_SHOW_METADATA
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.AppContextUtils.loadResult
import com.lagradost.cloudstream3.utils.AppContextUtils.loadSearchResult
import com.lagradost.cloudstream3.utils.AppContextUtils.reduceDragSensitivity
import com.lagradost.cloudstream3.utils.DataStoreHelper.currentAccount
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showBottomDialog
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.getSpanCount
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.txt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

const val LIBRARY_FOLDER = "library_folder"

enum class LibraryOpenerType(@StringRes val stringRes: Int) {
    Default(R.string.action_default),
    Provider(R.string.none),
    Browser(R.string.browser),
    Search(R.string.search),
    None(R.string.none),
}

/** Used to store how the user wants to open said poster */
@Serializable
data class LibraryOpener(
    @JsonProperty("openType") @SerialName("openType") val openType: LibraryOpenerType,
    @JsonProperty("providerData") @SerialName("providerData") val providerData: ProviderLibraryData?,
)

@Serializable
data class ProviderLibraryData(
    @JsonProperty("apiName") @SerialName("apiName") val apiName: String,
)

class LibraryFragment : BaseFragment<FragmentLibraryBinding>(
    BaseFragment.BindingCreator.Bind(FragmentLibraryBinding::bind)
) {
    companion object {
        fun newInstance() = LibraryFragment()

        /**
         * Store which page was last seen when exiting the fragment and returning
         **/
        const val VIEWPAGER_ITEM_KEY = "viewpager_item"
    }

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    private var toggleRandomButton = false

    private val tabTitlesState = mutableStateOf<List<String>>(emptyList())
    private val selectedTabState = mutableIntStateOf(0)
    private val sortOptionsState = mutableStateOf<List<String>>(emptyList())
    private val selectedSortState = mutableIntStateOf(-1)
    private val sourceNameState = mutableStateOf<String?>(null)
    private val isSearchOpenState = mutableStateOf(false)
    private val searchQueryState = mutableStateOf("")

    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable {
        libraryViewModel.sort(ListSorting.Query, searchQueryState.value)
    }

    private fun hideViewpager(distance: Int) {
        if (distance < 3) return
        val viewpager = binding?.viewpager ?: return
        val hideAnimation = AlphaAnimation(1f, 0f).apply {
            duration = distance * 50L
            fillAfter = true
        }
        val showAnimation = AlphaAnimation(0f, 1f).apply {
            duration = distance * 50L
            startOffset = distance * 100L
            fillAfter = true
        }
        viewpager.startAnimation(hideAnimation)
        viewpager.startAnimation(showAnimation)
    }

    private fun showSourceSelectionDialog(context: Context) {
        val items = libraryViewModel.availableApiNames
        val currentItem = libraryViewModel.currentApiName.value

        activity?.showBottomDialog(
            items,
            items.indexOf(currentItem),
            txt(R.string.select_library).asString(context),
            false,
            {}
        ) { index ->
            val selectedItem = items.getOrNull(index) ?: return@showBottomDialog
            libraryViewModel.switchList(selectedItem)
        }
    }

    private fun showPluginSelectionDialog(
        activity: Activity,
        key: String,
        syncId: SyncIdName,
        apiName: String? = null,
    ) {
        val availableProviders = allProviders.filter {
            it.supportedSyncNames.contains(syncId)
        }.map { it.name } +
            (APIHolder.getApiFromNameNull(apiName)?.let { listOf(it.name) }
                ?: emptyList())

        val baseOptions = listOf(
            LibraryOpenerType.Default,
            LibraryOpenerType.None,
            LibraryOpenerType.Browser,
            LibraryOpenerType.Search
        )

        val items = baseOptions.map { txt(it.stringRes).asString(activity) } + availableProviders

        val savedSelection = getKey<LibraryOpener>("$currentAccount/$LIBRARY_FOLDER", key)
        val selectedIndex =
            when {
                savedSelection == null -> 0
                savedSelection.openType == LibraryOpenerType.Provider
                        && savedSelection.providerData?.apiName != null -> {
                    availableProviders.indexOf(savedSelection.providerData.apiName)
                        .takeIf { it != -1 }
                        ?.plus(baseOptions.size) ?: 0
                }
                else -> baseOptions.indexOf(savedSelection.openType)
            }

        activity.showBottomDialog(
            items,
            selectedIndex,
            txt(R.string.open_with).asString(activity),
            false,
            {},
        ) {
            val savedData = if (it < baseOptions.size) {
                LibraryOpener(
                    baseOptions[it],
                    null
                )
            } else {
                LibraryOpener(
                    LibraryOpenerType.Provider,
                    ProviderLibraryData(items[it])
                )
            }

            setKey(
                "$currentAccount/$LIBRARY_FOLDER",
                key,
                savedData,
            )
        }
    }

    override fun pickLayout(): Int? =
        if (isLayout(PHONE)) R.layout.fragment_library else R.layout.fragment_library_tv

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.viewpager?.currentItem?.let { currentItem ->
            outState.putInt(VIEWPAGER_ITEM_KEY, currentItem)
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateRandomVisibility(binding: FragmentLibraryBinding) {
        if (!toggleRandomButton) {
            binding.libraryRandom.isGone = true
            binding.libraryRandomButtonTv.isGone = true
            return
        }
        val position = libraryViewModel.currentPage.value ?: 0
        val pages = (libraryViewModel.pages.value as? Resource.Success)?.value ?: return
        val hasItems = pages[position].items.isNotEmpty()
        val isPhone = isLayout(PHONE)

        binding.libraryRandom.isVisible = isPhone && hasItems
        binding.libraryRandomButtonTv.isVisible = !isPhone && hasItems
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = !isLayout(PHONE)
        )
    }

    @SuppressLint("ResourceType", "CutPasteId")
    override fun onBindingCreated(
        binding: FragmentLibraryBinding,
        savedInstanceState: Bundle?
    ) {
        binding.sortFab.setOnClickListener(sortChangeClickListener)
        binding.librarySort.setOnClickListener(sortChangeClickListener)
        binding.libraryDownloadsBtt.setOnClickListener {
            activity?.navigate(R.id.navigation_downloads)
        }

        if (isLayout(PHONE)) {
            binding.libraryHeaderComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    LiquidGlassTheme {
                        val sourceName by sourceNameState
                        val isSearchOpen by isSearchOpenState
                        val searchQuery by searchQueryState
                        val sortOptions by sortOptionsState
                        val selectedSort by selectedSortState

                        LiquidGlassLibraryHeader(
                            title = stringResource(R.string.library),
                            sourceName = sourceName,
                            isSearchOpen = isSearchOpen,
                            searchQuery = searchQuery,
                            onToggleSearch = {
                                val willOpen = !isSearchOpenState.value
                                isSearchOpenState.value = willOpen
                                if (willOpen) {
                                    binding.searchBar.setExpanded(true)
                                } else {
                                    searchQueryState.value = ""
                                    searchHandler.removeCallbacks(searchRunnable)
                                    libraryViewModel.sort(ListSorting.Query, "")
                                }
                            },
                            onSearchQueryChange = { newQuery ->
                                searchQueryState.value = newQuery
                                searchHandler.removeCallbacks(searchRunnable)
                                searchHandler.postDelayed(searchRunnable, 300)
                            },
                            onSelectProvider = {
                                val syncName = libraryViewModel.currentSyncApi?.syncIdName ?: return@LiquidGlassLibraryHeader
                                val act = activity ?: return@LiquidGlassLibraryHeader
                                showPluginSelectionDialog(act, syncName.name, syncName)
                            },
                            onSelectSource = {
                                context?.let { showSourceSelectionDialog(it) }
                            },
                            onOpenDownloads = {
                                activity?.navigate(R.id.navigation_downloads)
                            },
                            sortOptions = sortOptions,
                            selectedSortIndex = selectedSort,
                            onSelectSort = { index ->
                                selectedSortState.intValue = index
                                val method = libraryViewModel.sortingMethods.getOrNull(index) ?: return@LiquidGlassLibraryHeader
                                libraryViewModel.sort(method)
                            },
                        )
                    }
                }
            }

            binding.libraryTabsComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    LiquidGlassTheme {
                        val titles by tabTitlesState
                        val selectedTab by selectedTabState

                        LiquidGlassCategoryChips(
                            titles = titles,
                            selectedIndex = selectedTab,
                            onSelect = { index ->
                                if (index != selectedTabState.intValue) {
                                    val current = binding.viewpager.currentItem
                                    hideViewpager(abs(index - current))
                                    selectedTabState.intValue = index
                                    binding.viewpager.setCurrentItem(index, false)
                                    libraryViewModel.switchPage(index)
                                }
                            },
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                PosterCardStyleRepository.state.collect {
                    (binding.viewpager.adapter as? ViewpagerAdapter)?.notifyDataSetChanged()
                }
            }

            binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    selectedTabState.intValue = position
                    libraryViewModel.switchPage(position)
                }
            })
        }

        libraryViewModel.reloadPages(false)

        binding.listSelector.setOnClickListener {
            showSourceSelectionDialog(it.context)
        }

        //Load value for toggling Random button. Hide at startup
        context?.let {
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(it)
            toggleRandomButton =
                settingsManager.getBoolean(
                    getString(R.string.random_button_key),
                    false
                )
            binding.libraryRandom.visibility = View.GONE
            binding.libraryRandomButtonTv.visibility = View.GONE
        }

        binding.providerSelector.setOnClickListener {
            val syncName = libraryViewModel.currentSyncApi?.syncIdName ?: return@setOnClickListener
            val act = activity ?: return@setOnClickListener
            showPluginSelectionDialog(act, syncName.name, syncName)
        }

        binding.viewpager.setPageTransformer(LibraryScrollTransformer())

        binding.viewpager.adapter = ViewpagerAdapter(
            { isScrollingDown: Boolean ->
                if (isScrollingDown) {
                    binding.sortFab.shrink()
                    binding.libraryRandom.shrink()
                } else {
                    binding.sortFab.extend()
                    binding.libraryRandom.extend()
                }
            }) callback@{ searchClickCallback ->
            // To prevent future accidents
            debugAssert({
                searchClickCallback.card !is SyncAPI.LibraryItem
            }, {
                "searchClickCallback ${searchClickCallback.card} is not a LibraryItem"
            })

            val syncId = (searchClickCallback.card as SyncAPI.LibraryItem).syncId
            val syncName =
                libraryViewModel.currentSyncApi?.syncIdName ?: return@callback

            when (searchClickCallback.action) {
                SEARCH_ACTION_SHOW_METADATA -> {
                    (activity as? MainActivity)?.loadPopup(
                        searchClickCallback.card,
                        load = false
                    )
                    /*activity?.showPluginSelectionDialog(
                            syncId,
                            syncName,
                            searchClickCallback.card.apiName
                        )*/
                }

                SEARCH_ACTION_LOAD -> {
                    loadLibraryItem(syncName, syncId, searchClickCallback.card)
                }
            }
        }

        binding.apply {
            viewpager.offscreenPageLimit = 2
            viewpager.reduceDragSensitivity()
            searchBar.setExpanded(true)
        }

        val startLoading = Runnable {
            binding.apply {
                gridview.numColumns = root.context.getSpanCount()
                gridview.adapter =
                    context?.let { LoadingPosterAdapter(it, 6 * 3) }
                libraryLoadingOverlay.isVisible = true
                libraryLoadingShimmer.startShimmer()
                emptyListTextview.isVisible = false
            }
        }

        val stopLoading = Runnable {
            binding.apply {
                gridview.adapter = null
                libraryLoadingOverlay.isVisible = false
                libraryLoadingShimmer.stopShimmer()
            }
        }

        val handler = Handler(Looper.getMainLooper())

        observe(libraryViewModel.pages) { resource ->
            when (resource) {
                is Resource.Success -> {
                    handler.removeCallbacks(startLoading)
                    val pages = resource.value
                    val showNotice = pages.all { it.items.isEmpty() }

                    binding.apply {
                        emptyListTextview.isVisible = showNotice
                        if (showNotice) {
                            if (libraryViewModel.availableApiNames.size > 1) {
                                emptyListTextview.setText(R.string.empty_library_logged_in_message)
                            } else {
                                emptyListTextview.setText(R.string.empty_library_no_accounts_message)
                            }
                        }

                        (viewpager.adapter as? ViewpagerAdapter)?.submitList(pages.map {
                            it.copy(
                                items = CopyOnWriteArrayList(it.items)
                            )
                        })
                        //fix focus on the viewpager itself
                        (viewpager.getChildAt(0) as RecyclerView).apply {
                            tag = "tv_no_focus_tag"
                            //isFocusable = false
                        }

                        // Using notifyItemRangeChanged keeps the animations when sorting
                        /*viewpager.adapter?.notifyItemRangeChanged(
                            0,
                            viewpager.adapter?.itemCount ?: 0
                        )*/

                        libraryViewModel.currentPage.value?.let { page ->
                            binding.viewpager.setCurrentItem(page, false)
                            binding.searchBar.setExpanded(true)
                        }

                        // Set up random button click listener
                        if (toggleRandomButton) {
                            val randomClickListener = View.OnClickListener {
                                val position = libraryViewModel.currentPage.value ?: 0
                                val syncIdName = libraryViewModel.currentSyncApi?.syncIdName ?: return@OnClickListener
                                pages[position].items.randomOrNull()?.let { item ->
                                    loadLibraryItem(syncIdName, item.syncId, item)
                                }
                            }
                            libraryRandom.setOnClickListener(randomClickListener)
                            libraryRandomButtonTv.setOnClickListener(randomClickListener)
                        }
                        updateRandomVisibility(binding)

                        // Only stop loading after 300ms to hide the fade effect the viewpager produces when updating
                        // Without this there would be a flashing effect:
                        // loading -> show old viewpager -> black screen -> show new viewpager
                        handler.postDelayed(stopLoading, 300)

                        savedInstanceState?.getInt(VIEWPAGER_ITEM_KEY)?.let { currentPos ->
                            if (currentPos < 0) return@let
                            viewpager.setCurrentItem(currentPos, false)
                            // Using remove() sets the key to 0 instead of removing it
                            savedInstanceState.putInt(VIEWPAGER_ITEM_KEY, -1)
                        }

                        tabTitlesState.value = pages.mapNotNull { it.title.asStringNull(context) }
                        val sortingMethods = libraryViewModel.sortingMethods
                        sortOptionsState.value = sortingMethods.map { txt(it.stringRes).asString(context ?: return@map "") }
                        selectedSortState.intValue = sortingMethods.indexOf(libraryViewModel.currentSortingMethod)

                        if (!isLayout(PHONE)) {
                            TabLayoutMediator(
                                libraryTabLayout,
                                viewpager,
                            ) { tab, position ->
                                tab.text = pages.getOrNull(position)?.title?.asStringNull(context)
                                tab.view.tag = "tv_no_focus_tag"
                                tab.view.nextFocusDownId = R.id.search_result_root

                                tab.view.setOnClickListener {
                                    val currentItem = binding.viewpager.currentItem
                                    val distance = abs(position - currentItem)
                                    hideViewpager(distance)
                                }
                                //Expand the appBar on tab focus
                                tab.view.setOnFocusChangeListener { _, _ ->
                                    binding.searchBar.setExpanded(true)
                                }
                            }.attach()

                            binding.libraryTabLayout.addOnTabSelectedListener(object :
                                TabLayout.OnTabSelectedListener {
                                override fun onTabSelected(tab: TabLayout.Tab?) {
                                    binding.libraryTabLayout.selectedTabPosition.let { page ->
                                        libraryViewModel.switchPage(page)
                                    }
                                }

                                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
                            })
                        }
                    }
                }

                is Resource.Loading -> {
                    // Only start loading after 200ms to prevent loading cached lists
                    handler.postDelayed(startLoading, 200)
                }

                is Resource.Failure -> {
                    stopLoading.run()
                    // No user indication it failed :(
                    // TODO
                }
            }
        }

        observe(libraryViewModel.currentPage) { position ->
            selectedTabState.intValue = position
            updateRandomVisibility(binding)
            val all = binding.viewpager.allViews.toList()
                .filterIsInstance<AutofitRecyclerView>()

            all.forEach { view ->
                view.isVisible = view.tag == position
                view.isFocusable = view.tag == position

                if (view.tag == position)
                    view.descendantFocusability = FOCUS_AFTER_DESCENDANTS
                else
                    view.descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            }
        }

        observe(libraryViewModel.currentApiName) { name ->
            sourceNameState.value = name
        }
    }

    private fun loadLibraryItem(
        syncName: SyncIdName,
        syncId: String,
        card: SearchResponse
    ) {
        // This basically first selects the individual opener and if that is default then
        // selects the whole list opener
        val savedListSelection =
            getKey<LibraryOpener>("$currentAccount/$LIBRARY_FOLDER", syncName.name)

        val savedSelection = getKey<LibraryOpener>(
            "$currentAccount/$LIBRARY_FOLDER",
            syncId
        ).takeIf {
            it?.openType != LibraryOpenerType.Default
        } ?: savedListSelection

        when (savedSelection?.openType) {
            null, LibraryOpenerType.Default -> {
                // Prevents opening MAL/AniList as a provider
                if (APIHolder.getApiFromNameNull(card.apiName) != null) {
                    activity?.loadSearchResult(
                        card
                    )
                } else {
                    // Search when no provider can open
                    QuickSearchFragment.pushSearch(
                        activity,
                        card.name
                    )
                }
            }

            LibraryOpenerType.None -> {}
            LibraryOpenerType.Provider ->
                savedSelection.providerData?.apiName?.let { apiName ->
                    activity?.loadResult(
                        card.url,
                        apiName,
                        card.name
                    )
                }

            LibraryOpenerType.Browser ->
                openBrowser(card.url)

            LibraryOpenerType.Search -> {
                QuickSearchFragment.pushSearch(
                    activity,
                    card.name
                )
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val adapter = binding?.viewpager?.adapter ?: return
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    private val sortChangeClickListener = View.OnClickListener { view ->
        val methods = libraryViewModel.sortingMethods.map {
            txt(it.stringRes).asString(view.context)
        }

        activity?.showBottomDialog(
            methods,
            libraryViewModel.sortingMethods.indexOf(libraryViewModel.currentSortingMethod),
            txt(R.string.sort_by).asString(view.context),
            false,
            {},
            {
                selectedSortState.intValue = it
                val method = libraryViewModel.sortingMethods[it]
                libraryViewModel.sort(method)
            })
    }
}

class MenuSearchView(context: Context) : SearchView(context)
