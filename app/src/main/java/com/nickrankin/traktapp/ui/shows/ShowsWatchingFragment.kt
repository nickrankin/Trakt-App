package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodeAndStats
import com.nickrankin.traktapp.databinding.FragmentWatchingBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.WatchedEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "WatchingFragment"
@AndroidEntryPoint
class WatchingFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow {
    
    private val viewModel by activityViewModels<WatchedEpisodesViewModel>()

    private lateinit var bindings: FragmentWatchingBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WatchedEpisodesPagingAdapter

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentWatchingBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeLayout = bindings.showwatchingfragmentSwipeRefreshLayout
        swipeLayout.setOnRefreshListener(this)

        setHasOptionsMenu(true)

        progressBar = bindings.showwatchingfragmentProgressbar

        updateTitle("Watched Episodes")

        initRecycler()
        collectEvents()

        getViewState()

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        collectEpisodes()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.layout_switcher_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

    }

    private fun collectEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest { latestData ->
                progressBar.visibility = View.GONE

                if(swipeLayout.isRefreshing) {
                    swipeLayout.isRefreshing = false
                }

                adapter.submitData(latestData)

            }
        }
    }

    private fun collectEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->

                when(event) {
                    is WatchedEpisodesViewModel.Event.RemoveWatchedHistoryEvent -> {
                        val syncResponseResource = event.syncResponse

                        when(syncResponseResource) {
                            is Resource.Success -> {
                                if(syncResponseResource.data?.deleted?.episodes ?: 0 > 0) {
                                    displayMessageToast("Succesfully removed play!", Toast.LENGTH_LONG)
                                } else {
                                    displayMessageToast("Error removing play", Toast.LENGTH_LONG)
                                }
                            }

                            is Resource.Error -> {
                                handleError(syncResponseResource.error, "Error removing watched Episode. ") }
                            else -> {}

                        }
                    } else -> {
                        //
                    }
                }

            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.showwatchingfragmentRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = WatchedEpisodesPagingAdapter(AdaptorActionControls(context?.getDrawable(R.drawable.ic_baseline_remove_red_eye_24),
            "Remove This Play", true,
            R.menu.watched_shows_popup_menu, entrySelectedCallback = { selectedEpisode ->
                navigateToEpisode(selectedEpisode.watchedEpisode)

            }, buttonClickedCallback = {selectedEpisode ->
                removeFromWatchedHistory(selectedEpisode.watchedEpisode)
            }, menuItemSelectedCallback = {selectedEpisode, menuSelected ->
                when(menuSelected) {
                    R.id.watchedshowspopup_nav_show -> {
                        navigateToShow(selectedEpisode.watchedEpisode.show_trakt_id ?: 0, selectedEpisode.watchedEpisode.show_tmdb_id, selectedEpisode.watchedEpisode.show_title)

                    }
                    R.id.watchedshowspopup_nav_episode -> {
                        navigateToEpisode(selectedEpisode.watchedEpisode)

                    }
                    R.id.watchedshowspopup_remove_show -> {
                        removeFromWatchedHistory(selectedEpisode.watchedEpisode)
                    }
                    else -> {
                        Log.e(TAG, "initRecycler: Invalid menu id $menuSelected", )
                    }
                }
            }),sharedPreferences, tmdbImageLoader)

        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = WatchedEpisodesLoadStateAdapter(adapter),
            footer = WatchedEpisodesLoadStateAdapter(adapter)
        )

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadStates ->
                bindings.showwatchingfragmentSwipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { bindings.showwatchingfragmentRecyclerview.scrollToPosition(0) }
        }

    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {
        val intent = Intent(requireActivity(), ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
        startActivity(intent)
    }

    private fun navigateToEpisode(selectedEpisode: WatchedEpisode?) {
        if(selectedEpisode == null) {
            Log.e(TAG, "navigateToEpisode: Selected Episode cannot be null", )
            return
        }
        val intent = Intent(context, EpisodeDetailsActivity::class.java)

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                selectedEpisode.show_trakt_id ?: 0,
                selectedEpisode.show_tmdb_id,
                selectedEpisode.episode_season ?: 0,
                selectedEpisode.episode_number ?: 0,
                selectedEpisode.language ?: "en"
            )
        )

        // We cannot guarantee Watched Episode data is up to date at this point so force refresh (user could have watched more of this show in meantime)
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, true)

        startActivity(intent)
    }

    private fun removeFromWatchedHistory(watcedEpisode: WatchedEpisode?) {
        val syncItems = SyncItems()
            .ids(watcedEpisode?.id ?: 0L)

        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Are you sure you want to remove ${watcedEpisode?.episode_title} (${watcedEpisode?.show_title}) play ${watcedEpisode?.watched_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))}?")
            .setPositiveButton("Ok", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeFromWatchedHistory(syncItems)
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            }).show()
    }

    override fun onResume() {
        super.onResume()

        if(isLoggedIn) {
            viewModel.onStart()
        }
    }

    override fun onRefresh() {
        super.onRefresh()

        if(isLoggedIn) {
            Log.e(TAG, "onRefresh: CALLED", )

            //https://developer.android.com/reference/kotlin/androidx/paging/PagingDataAdapter#refresh()
            viewModel.onRefresh()
            //adapter.refresh()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
        }
        return false
    }

    private fun getViewState() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewState ->
                adapter.switchView(viewState)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewState)

                recyclerView.scrollToPosition(0)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun displayMessageToast(message: String, duration: Int) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            WatchingFragment()
    }
}