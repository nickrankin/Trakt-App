package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.withTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.*
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.databinding.ShowsTrackingFragmentBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.shows.ShowsTrackingViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.settings.SettingsFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "ShowsTrackingFragment"

@AndroidEntryPoint
class ShowsTrackingFragment : BaseFragment(), OnNavigateToShow, OnNavigateToEpisode, SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: ShowsTrackingViewModel by activityViewModels()
    private lateinit var bindings: ShowsTrackingFragmentBinding
    private lateinit var fab: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var trackedShowsRecyclerView: RecyclerView
    private lateinit var trackedShowsAdapter: TrackedShowsAdapter

    private lateinit var addCollectDialog: AlertDialog
    private lateinit var addFromSearchDialog: AlertDialog
    private lateinit var upcomingEpisodesDialog: AlertDialog

    private lateinit var collectedShowsRecyclerView: RecyclerView
    private lateinit var collectedShowsPickerAdapter: CollectedShowsPickerAdapter
    private lateinit var collectedShowsProgressBar: ProgressBar

    private lateinit var searchShowsRecyclerView: RecyclerView
    private lateinit var searchShowsPickerAdapter: SearchResultShowsPickerAdapter
    private lateinit var searchShowsProgressBar: ProgressBar

    private lateinit var upcomingEpisodesRecyclerView: RecyclerView
    private lateinit var upcomingEpisodesAdapter: TrackedEpisodesAdapter

    private lateinit var sorting: Sorting

//    // for testing
    @Inject
    lateinit var tracktAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var showsDatabase: ShowsDatabase

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private var isloggedIn = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ShowsTrackingFragmentBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        updateTitle("Tracked Shows")

        isloggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        swipeRefreshLayout = bindings.showstrackingfragmentSwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        if (isloggedIn) {
            fab = bindings.showstrackingfragmentFab
            fab.visibility = View.VISIBLE

            fab.setOnClickListener {
                showFabPopUpMenu()
            }

            initRecycler()

            createAddFromCollectionDialog()
            createAddFromSearchDialog()
            createUpcomingEpisodesDialog()

            getTrackedShows()
            getCollectedShows()
            getSearchResults()
            getEvents()
        } else {
            // TODO Show error
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.tracked_filter_menu, menu)
    }

    override fun onResume() {
        super.onResume()
        checkNotificationsEnabled()

    }

    private fun getTrackedShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.trackedShows.collectLatest { trackedShows ->
                if(swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }

                if (trackedShows.isNotEmpty()) {
                    bindings.showstrackingfragmentErrorText.visibility = View.GONE
                    trackedShowsRecyclerView.visibility = View.VISIBLE

                    trackedShowsAdapter.submitList(trackedShows) {
                        trackedShowsRecyclerView.scrollToPosition(0)
                    }


                } else {
                    bindings.showstrackingfragmentErrorText.visibility = View.VISIBLE
                    bindings.showstrackingfragmentErrorText.text =
                        "You have not tracked any shows yet! Why not add some?"
                }
            }
        }
    }

    private fun getSearchResults() {
        lifecycleScope.launchWhenStarted {
            viewModel.searchResults.collectLatest { searchResults ->
                searchShowsPickerAdapter.submitData(searchResults)
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->

                when(event) {
                    is ShowsTrackingViewModel.Event.UpdateTrackedEpisodeDataEvent -> {
                        val show = event.episodesResource.data?.keys?.first()

                        if(event.episodesResource is Resource.Success) {
                            Log.d(TAG, "getEvents: Tracking was successful")
                        } else if(event.episodesResource is Resource.Error) {
                            val traktId = show?.trakt_id
                            val exception = event.episodesResource.error

                            Log.d(TAG, "getEvents: An error occurred ${exception?.message}")
                            exception?.printStackTrace()

                            // Notify the user
                            displayMessageToast("An error occurred refreshing tracked show ${show?.name ?: "Unknown"} Episodes. ${exception?.localizedMessage} Please try again later.", Toast.LENGTH_LONG)

                        }
                    }
                    is ShowsTrackingViewModel.Event.RefreshTrackedEpisodeDataEvent -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        if(event.trackedEpisodes is Resource.Success) {
                            Log.d(TAG, "getEvents: Successfully refreshed Tracked Episodes")
                        } else if(event.trackedEpisodes is Resource.Error) {
                            val exception = event.trackedEpisodes.error
                            displayMessageToast("Error refreshing tracked eposides ${exception?.localizedMessage}", Toast.LENGTH_LONG)

                            Log.e(TAG, "getEvents: Error refreshing tracked eposides ${exception?.localizedMessage} ", )

                            exception?.printStackTrace()
                        }
                    }
                    is ShowsTrackingViewModel.Event.RefreshAllTrackedShowsDataEvent -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        if(event.successResource is Resource.Success) {
                            Log.d(TAG, "getEvents: Successfully refreshed Tracked Episodes")
                        } else if(event.successResource is Resource.Error) {
                            val exception = event.successResource.error
                            displayMessageToast("Error refreshing tracked eposides ${exception?.localizedMessage}", Toast.LENGTH_LONG)

                            Log.e(TAG, "getEvents: Error refreshing tracked eposides ${exception?.localizedMessage} ", )

                            exception?.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationsEnabled() {
        val isNotificationsEnabled =
            sharedPreferences.getBoolean(SettingsFragment.EPISODE_TRACKING_ENABLED, false)

        if (!isNotificationsEnabled) {
            getSnackbar(
                trackedShowsRecyclerView,
                "You have not enabled Show Tracking in preferences! You will not receive upcoming Episode notifications!",
                "Fix",
                Snackbar.LENGTH_INDEFINITE
            ) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .add(R.id.showsmainactivity_container, SettingsFragment())
                    .commit()
            }.show()
        }
    }

    private fun showFabPopUpMenu() {
        val popupMenu = PopupMenu(requireContext(), fab)
        popupMenu.menuInflater.inflate(R.menu.show_tracking_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            Log.d(TAG, "showFabPopUpMenu: You clicked ${menuItem.title}")
            when (menuItem.itemId) {
                R.id.showtrackingmenu_add_from_collection -> {
                    viewModel.filterCollectedShows("")
                    addCollectDialog.show()
                }
                R.id.showtrackingmenu_add_from_search -> {
                    addFromSearchDialog.show()
                }
            }
            true
        }

        popupMenu.setOnDismissListener {
            it.dismiss()
        }

        popupMenu.show()
    }

    private fun initRecycler() {
        trackedShowsRecyclerView = bindings.showstrackingfragmentRecyclerview
        val layoutManager = LinearLayoutManager(requireContext())
        trackedShowsAdapter = TrackedShowsAdapter(tmdbImageLoader, callback = { trackedShowWithEpisodes ->
            val trackedShow = trackedShowWithEpisodes.trackedShow
            navigateToShow(trackedShow.trakt_id, trackedShow.tmdb_id ?: -1, trackedShow.title, null)
        }) { showTitle, upcomingEpisodes ->
            upcomingEpisodesDialog.setTitle("Upcoming episodes for $showTitle")
            upcomingEpisodesAdapter.submitList(emptyList())
            upcomingEpisodesAdapter.submitList(upcomingEpisodes.filter {
                // Only show episodes which are airing after current time
                it?.airs_date?.isAfter(OffsetDateTime.now()) ?: false
            })
            upcomingEpisodesDialog.show()
        }

        trackedShowsRecyclerView.layoutManager = layoutManager
        trackedShowsRecyclerView.adapter = trackedShowsAdapter
    }

    private fun createAddFromCollectionDialog() {
        val view = layoutInflater.inflate(R.layout.collected_shows_picker_dialog, null, false)
        collectedShowsRecyclerView = view.findViewById(R.id.collectedshowspicker_recyclerview)

        collectedShowsProgressBar = view.findViewById(R.id.collectedshowspicker_progressbar)

        val filterBox = view.findViewById<EditText>(R.id.collectedshowspicker_filter)

        initCollectedRecyclerView()

        addCollectDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Collected Show")
            .setView(view)
            .setNegativeButton("Exit") { dialogInterface, i ->
                filterBox.text = null
                dialogInterface.dismiss()
            }.create()

        filterBox.doOnTextChanged { text, start, before, count ->
            viewModel.filterCollectedShows(text.toString())
        }
    }

    private fun createAddFromSearchDialog() {
        val view = layoutInflater.inflate(R.layout.search_shows_picker_dialog, null, false)
        searchShowsRecyclerView = view.findViewById(R.id.searchshowspicker_recyclerview)

        searchShowsProgressBar = view.findViewById(R.id.searchshowspicker_progressbar)

        val searchBox = view.findViewById<SearchView>(R.id.searchshowspicker_filter)
        searchBox.setIconifiedByDefault(false)

        initSearchResultsRecyclerView()

        addFromSearchDialog = AlertDialog.Builder(requireContext())
            .setTitle("Find Show")
            .setView(view)
            .setNegativeButton("Exit") { dialogInterface, i ->
                dialogInterface.dismiss()
            }.create()

        searchBox.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "onQueryTextSubmit: Searched $query")
                viewModel.newSearch(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun createUpcomingEpisodesDialog() {
        val layoutManager = LinearLayoutManager(requireContext())
        upcomingEpisodesRecyclerView = RecyclerView(requireContext())
        upcomingEpisodesAdapter = TrackedEpisodesAdapter(sharedPreferences, tmdbImageLoader, callback = { trackedEpisode ->
            navigateToEpisode(trackedEpisode.show_trakt_id, trackedEpisode.show_tmdb_id, trackedEpisode.season, trackedEpisode.episode, null)
        })

        upcomingEpisodesRecyclerView.layoutManager = layoutManager
        upcomingEpisodesRecyclerView.adapter = upcomingEpisodesAdapter

        upcomingEpisodesDialog = AlertDialog.Builder(requireContext())
            .setTitle("Upcoming episodes")
            .setView(upcomingEpisodesRecyclerView)
            .setNegativeButton("Exit", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

    }

    private fun initCollectedRecyclerView() {
        setupViewSwipeBehaviour()

        val layoutManager = LinearLayoutManager(requireContext())

        collectedShowsPickerAdapter = CollectedShowsPickerAdapter(sharedPreferences) { collectedShow ->
            addShowFromCollection(collectedShow)
        }

        collectedShowsRecyclerView.layoutManager = layoutManager
        collectedShowsRecyclerView.adapter = collectedShowsPickerAdapter

    }

    private fun initSearchResultsRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())

        searchShowsPickerAdapter = SearchResultShowsPickerAdapter(sharedPreferences) { searchResult ->
            addShowFromSearchResult(searchResult)
        }

        searchShowsRecyclerView.layoutManager = layoutManager

        searchShowsRecyclerView.adapter = searchShowsPickerAdapter.withLoadStateHeaderAndFooter(
            header = ShowSearchLoadStateAdapter(searchShowsPickerAdapter),
            footer = ShowSearchLoadStateAdapter(searchShowsPickerAdapter)
        )

        lifecycleScope.launchWhenCreated {
            searchShowsPickerAdapter.loadStateFlow.collectLatest { loadStates ->
                searchShowsProgressBar.visibility = if(loadStates.refresh is LoadState.Loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenCreated {
            searchShowsPickerAdapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { searchShowsRecyclerView.scrollToPosition(0) }
        }
    }

    private fun getCollectedShows() {
        lifecycleScope.launchWhenStarted {
            viewModel.collectedShows.collectLatest { collectedShowsResource ->
                when (collectedShowsResource) {
                    is Resource.Loading -> {
                        collectedShowsProgressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        collectedShowsProgressBar.visibility = View.GONE
                        Log.d(
                            TAG,
                            "getCollectedShows: Got ${collectedShowsResource.data?.size} shows from collection"
                        )
                        collectedShowsPickerAdapter.submitList(collectedShowsResource.data?.sortedBy { it.collected_at }
                            ?.reversed())
                    }
                    is Resource.Error -> {
                        collectedShowsProgressBar.visibility = View.GONE

                        Log.e(TAG, "getCollectedShows: Error occurred")
                        collectedShowsResource.error?.printStackTrace()
                    }
                }

            }
        }
    }

    private fun addShowFromCollection(collectedShow: CollectedShow) {
        viewModel.addTrackedShow(
            TrackedShow(
                collectedShow.show_trakt_id,
                collectedShow.show_tmdb_id,
                collectedShow.show_title,
                collectedShow.show_overview,
                collectedShow.language,
                collectedShow.airedDate,
                collectedShow.runtime,
                OffsetDateTime.now()
            )
        )

        getUpcomingEpisodes(collectedShow.show_trakt_id)
    }

    private fun addShowFromSearchResult(searchResult: SearchResult?) {
        viewModel.addTrackedShow(
            TrackedShow(
                searchResult?.show?.ids?.trakt ?: -1,
                searchResult?.show?.ids?.tmdb,
                searchResult?.show?.title,
                searchResult?.show?.overview,
                searchResult?.show?.language,
                searchResult?.show?.first_aired,
                searchResult?.show?.runtime,
                OffsetDateTime.now()
            )
        )

        displayMessageToast("You are now tracking ${searchResult?.show?.title}", Toast.LENGTH_SHORT)

        getUpcomingEpisodes(searchResult?.show?.ids?.trakt ?: -1)
    }

    private fun getUpcomingEpisodes(showTraktId: Int) {
        val response = viewModel.getUpcomingEpisodes(showTraktId)
    }

    private fun setupViewSwipeBehaviour() {

        var itemTouchHelper: ItemTouchHelper? = null

        itemTouchHelper = ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    viewHolder.itemView.background = null

                    return true
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val colorAlert = ContextCompat.getColor(requireContext(), R.color.red)
                    val teal200 = ContextCompat.getColor(requireContext(), R.color.teal_200)
                    val defaultWhiteColor = ContextCompat.getColor(requireContext(), R.color.white)

                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                        iconHorizontalMargin = 23f,
                        backgroundColorFromStartToEnd = teal200,
                        backgroundColorFromEndToStart = colorAlert,
                        textFromStartToEnd = "",
                        textFromEndToStart = "Stop Tracking",
                        textColorFromStartToEnd = defaultWhiteColor,
                        textColorFromEndToStart = defaultWhiteColor,
                        iconTintColorFromStartToEnd = defaultWhiteColor,
                        iconTintColorFromEndToStart = defaultWhiteColor,
                        textSizeFromStartToEnd = 16f,
                        textSizeFromEndToStart = 16f,
                        typeFaceFromStartToEnd = Typeface.DEFAULT_BOLD,
                        typeFaceFromEndToStart = Typeface.SANS_SERIF,
                        iconResIdFromStartToEnd = R.drawable.ic_baseline_delete_forever_24,
                        iconResIdFromEndToStart = R.drawable.ic_trakt_svgrepo_com
                    )

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )

                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val showsList: MutableList<TrackedShowWithEpisodes> = mutableListOf()
                    showsList.addAll(trackedShowsAdapter.currentList)

                    val showPosition = viewHolder.layoutPosition
                    val show = showsList[showPosition]

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val updatedList: MutableList<TrackedShowWithEpisodes> = mutableListOf()
                            updatedList.addAll(showsList)
                            updatedList.remove(show)

                            trackedShowsAdapter.submitList(updatedList)

                            val timer = getTimer() {
                                Log.e(
                                    TAG,
                                    "onFinish: Timer ended for remove tracked show ${show.trackedShow.title}!"
                                )
                                viewModel.stopTracking(show.trackedShow)

                            }.start()

                            getSnackbar(
                                trackedShowsRecyclerView,
                                "You have stopped tracking: ${show.trackedShow.title}",
                                "Cancel",
                                Snackbar.LENGTH_LONG
                            ) {
                                timer.cancel()
                                trackedShowsAdapter.submitList(showsList) {
                                    // For first and last element, always scroll to the position to bring the element to focus
                                    if (showPosition == 0) {
                                        trackedShowsRecyclerView.scrollToPosition(0)
                                    } else if (showPosition == showsList.size - 1) {
                                        trackedShowsRecyclerView.scrollToPosition(showsList.size - 1)
                                    }
                                }
                            }.show()
                        }

                        ItemTouchHelper.RIGHT -> {

                        }

                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(trackedShowsRecyclerView)
    }

    private fun getTimer(doAction: () -> Unit): CountDownTimer {
        return object : CountDownTimer(5000, 1000) {
            override fun onTick(p0: Long) {
            }

            override fun onFinish() {
                doAction()
            }
        }
    }



    override fun navigateToShow(traktId: Int, tmdbId: Int, title: String?, language: String?) {
        val intent = Intent(requireActivity(), ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)

        startActivity(intent)
    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String?
    ) {
        val intent = Intent(requireActivity(), EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, language)

        startActivity(intent)
    }

    private fun getSnackbar(
        v: View,
        message: String,
        buttonText: String,
        length: Int,
        listener: View.OnClickListener
    ): Snackbar {
        return Snackbar.make(
            v,
            message,
            length
        )
            .setAction(buttonText, listener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.trackedfiltermenu_upcoming -> {
                sorting = if(sorting.sortBy == Sorting.SORT_BY_NEXT_AIRING) {
                    if(sorting.sortHow == Sorting.SORT_ORDER_DESC) {
                        Sorting(Sorting.SORT_BY_NEXT_AIRING, Sorting.SORT_ORDER_ASC)
                    } else {
                        Sorting(Sorting.SORT_BY_NEXT_AIRING, Sorting.SORT_ORDER_DESC)
                    }
                } else {
                    Sorting(Sorting.SORT_BY_NEXT_AIRING, Sorting.SORT_ORDER_DESC)
                }

                viewModel.applySorting(sorting)
            }
            R.id.trackedfiltermenu_title -> {
                sorting = if(sorting.sortBy == Sorting.SORT_BY_TITLE) {
                    if(sorting.sortHow == Sorting.SORT_ORDER_DESC) {
                        Sorting(Sorting.SORT_BY_TITLE, Sorting.SORT_ORDER_ASC)
                    } else {
                        Sorting(Sorting.SORT_BY_TITLE, Sorting.SORT_ORDER_DESC)
                    }
                } else {
                    Sorting(Sorting.SORT_BY_TITLE, Sorting.SORT_ORDER_DESC)
                }

                viewModel.applySorting(sorting)
            }
            R.id.trackedfiltermenu_year -> {
                sorting = if(sorting.sortBy == Sorting.SORT_BY_YEAR) {
                    if(sorting.sortHow == Sorting.SORT_ORDER_DESC) {
                        Sorting(Sorting.SORT_BY_YEAR, Sorting.SORT_ORDER_ASC)
                    } else {
                        Sorting(Sorting.SORT_BY_YEAR, Sorting.SORT_ORDER_DESC)
                    }
                } else {
                    Sorting(Sorting.SORT_BY_YEAR, Sorting.SORT_ORDER_DESC)
                }

                viewModel.applySorting(sorting)
            }
            R.id.trackedfiltermenu_tracked_at -> {
                sorting = if(sorting.sortBy == Sorting.SORT_BY_TRACKED_AT) {
                    if(sorting.sortHow == Sorting.SORT_ORDER_DESC) {
                        Sorting(Sorting.SORT_BY_TRACKED_AT, Sorting.SORT_ORDER_ASC)
                    } else {
                        Sorting(Sorting.SORT_BY_TRACKED_AT, Sorting.SORT_ORDER_DESC)
                    }
                } else {
                    Sorting(Sorting.SORT_BY_TRACKED_AT, Sorting.SORT_ORDER_DESC)
                }

                viewModel.applySorting(sorting)
            }
        }
        return false
    }

    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()

        //Initialize default sorting
        sorting = Sorting(Sorting.SORT_BY_NEXT_AIRING, Sorting.SORT_ORDER_DESC)
        viewModel.applySorting(sorting)
    }

    override fun onRefresh() {
        viewModel.onRefresh()

        //generateTestNotifications()
    }

    private fun generateTestNotifications() {
        val testTrackingEpisodes = listOf(
            TrackedEpisode(
                5907165,3501517,42221,42445, OffsetDateTime.now().plusSeconds(6L),
                emptyList(),"Episode 6","Borgen",4,6,OffsetDateTime.now(),0,false
            ),
            TrackedEpisode(
                5907167,3501519,42221,42445,OffsetDateTime.now().plusSeconds(12L),emptyList(),"Episode 7","Borgen",4,7,OffsetDateTime.now(),0,false
            ),
            TrackedEpisode(
                5907169,3501520,42221,42445,OffsetDateTime.now().plusSeconds(24L),emptyList(),"Episode 8","Borgen",4,8,OffsetDateTime.now(),0,false
            )
        )

        lifecycleScope.launchWhenStarted {
            showsDatabase.withTransaction {
                showsDatabase.trackedEpisodeDao().insert(testTrackingEpisodes)
            }
        }

        testTrackingEpisodes.map { ep ->
            tracktAlarmScheduler.scheduleTrackedEpisodeAlarm(ep)
        }
    }

    companion object {
        fun newInstance() = ShowsTrackingFragment()
    }

}