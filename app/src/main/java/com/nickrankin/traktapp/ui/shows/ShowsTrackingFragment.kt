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
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.withTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.search.ShowSearchLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.*
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.databinding.ShowsTrackingFragmentBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.ShowsTrackingViewModel
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.settings.SettingsFragment
import com.uwetrottmann.trakt5.entities.SearchResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "ShowsTrackingFragment"

@AndroidEntryPoint
class ShowsTrackingFragment : BaseFragment(), OnNavigateToShow, OnNavigateToEpisode, SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: ShowsTrackingViewModel by activityViewModels()

    private var _bindings: ShowsTrackingFragmentBinding? = null
    private val bindings get() = _bindings!!

    private lateinit var fab: FloatingActionButton

    private lateinit var trackedShowsRecyclerView: RecyclerView
    private lateinit var trackedShowsAdapter: TrackedShowsAdapter

    private lateinit var addCollectDialog: AlertDialog
    private lateinit var addFromSearchDialog: AlertDialog

    private lateinit var collectedShowsRecyclerView: RecyclerView
    private lateinit var collectedShowsPickerAdapter: CollectedShowsPickerAdapter
    private lateinit var collectedShowsProgressBar: ProgressBar

    private lateinit var searchShowsRecyclerView: RecyclerView
    private lateinit var searchShowsPickerAdapter: SearchResultShowsPickerAdapter
    private lateinit var searchShowsProgressBar: ProgressBar

//    // for testing
    @Inject
    lateinit var tracktAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var showsDatabase: ShowsDatabase

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = ShowsTrackingFragmentBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        updateTitle("Tracked Shows")

        if (isLoggedIn) {
            fab = bindings.showstrackingfragmentFab
            fab.visibility = View.VISIBLE

            fab.setOnClickListener {
                showFabPopUpMenu()
            }

            initRecycler()

            createAddFromCollectionDialog()
            createAddFromSearchDialog()

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
            viewModel.trackedShows.collectLatest { trackedShowsResource ->

                when(trackedShowsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getTrackedShows: Loading Tracked Shows")
                    }
                    is Resource.Success -> {
                        val trackedShows = trackedShowsResource.data
                        Log.d(TAG, "getTrackedShows: Loaded ${trackedShows?.size} tracked shows")

                        bindings.showstrackingfragmentErrorText.visibility = View.GONE
                        trackedShowsRecyclerView.visibility = View.VISIBLE


                        if (trackedShows?.isNotEmpty() == true) {
                            trackedShowsAdapter.submitList(trackedShows) {
                                trackedShowsRecyclerView.scrollToPosition(0)
                            }
                        } else {
                            bindings.showstrackingfragmentErrorText.visibility = View.VISIBLE
                            bindings.showstrackingfragmentErrorText.text =
                                "You have not tracked any shows yet! Why not add some?"
                        }
                    }

                    is Resource.Error -> {
                        handleError(trackedShowsResource.error, null)
                    }
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
                        val show = event.episodesResource.data

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
                    .add(R.id.splitviewactivity_first_container, SettingsFragment())
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
        trackedShowsRecyclerView.layoutManager = LinearLayoutManager(context)


        trackedShowsAdapter = TrackedShowsAdapter(
            callback = { operation, selectedItem ->

                when(operation) {
                    TrackedShowsAdapter.OPT_VIEW -> {
                        navigateToShow(selectedItem.trackedShow.trakt_id, selectedItem.trackedShow.tmdb_id, selectedItem.trackedShow.title)
                    }
                    TrackedShowsAdapter.OPT_LIST_EPISODES -> {
                            createUpcomingEpisodesDialog(selectedItem.episodes).show()
                    }
                    TrackedShowsAdapter.OPT_STOP_TRACKING -> {
                        viewModel.cancelTracking(selectedItem.trackedShow)
                    }
                }

            },
            tmdbImageLoader = tmdbImageLoader,
            sharedPreferences = sharedPreferences
        )

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

    private fun createUpcomingEpisodesDialog(trackedEpisodes: List<TrackedEpisode?>): AlertDialog {
        val upcomingEpisodesRecyclerView = RecyclerView(requireContext())
        val upcomingEpisodesAdapter = TrackedEpisodesAdapter(sharedPreferences, tmdbImageLoader, callback = { trackedEpisode ->
            navigateToEpisode(trackedEpisode.show_trakt_id, trackedEpisode.show_tmdb_id, trackedEpisode.season, trackedEpisode.episode, null)
        })

        upcomingEpisodesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        upcomingEpisodesRecyclerView.adapter = upcomingEpisodesAdapter

        upcomingEpisodesAdapter.submitList(trackedEpisodes)

        return AlertDialog.Builder(requireContext())
            .setTitle("Upcoming episodes")
            .setView(upcomingEpisodesRecyclerView)
            .setNegativeButton("Exit", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()
    }

    private fun initCollectedRecyclerView() {
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

                        (activity as IHandleError).showErrorSnackbarRetryButton(collectedShowsResource.error, bindings.showstrackingfragmentSwipeRefreshLayout) {
                            viewModel.onRefresh()
                        }
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
                collectedShow.status,
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
                searchResult?.show?.status,
                OffsetDateTime.now()
            )
        )

        displayMessageToast("You are now tracking ${searchResult?.show?.title}", Toast.LENGTH_SHORT)

        getUpcomingEpisodes(searchResult?.show?.ids?.trakt ?: -1)
    }

    private fun getUpcomingEpisodes(showTraktId: Int) {
        viewModel.getUpcomingEpisodesPerShow(showTraktId)
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {
        (activity as OnNavigateToEntity).navigateToShow(
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        title: String?
    ) {
        (activity as OnNavigateToEntity).navigateToEpisode(
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                title
            )
        )

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
                    viewModel.applySorting(ShowsTrackingViewModel.SORT_BY_NEXT_AIRING)
            }
            R.id.trackedfiltermenu_title -> {
                    viewModel.applySorting(ISortable.SORT_BY_TITLE)
            }
            R.id.trackedfiltermenu_year -> {
                viewModel.applySorting(ISortable.SORT_BY_YEAR)
            }
            R.id.trackedfiltermenu_tracked_at -> {
                viewModel.applySorting(ShowsTrackingViewModel.SORT_BY_TRACKED_AT)
            }
            R.id.trackedfiltermenu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
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
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        fun newInstance() = ShowsTrackingFragment()
    }

}