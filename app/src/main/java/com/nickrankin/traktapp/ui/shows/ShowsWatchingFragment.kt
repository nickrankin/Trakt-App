package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.FragmentWatchingBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.WatchedEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "WatchingFragment"
@AndroidEntryPoint
class WatchingFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToShow, OnNavigateToEpisode {
    
    private val viewModel by activityViewModels<WatchedEpisodesViewModel>()

    private lateinit var bindings: FragmentWatchingBinding
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
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

        progressBar = bindings.showwatchingfragmentProgressbar

        updateTitle("Watched Episodes")

        initRecycler()
        collectEvents()

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        collectEpisodes()
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
                                syncResponseResource.error?.printStackTrace()
                                displayMessageToast("Error removing watched Episode. ${syncResponseResource.error?.localizedMessage}", Toast.LENGTH_LONG)
                            }
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
        layoutManager = LinearLayoutManager(context)
        adapter = WatchedEpisodesPagingAdapter(sharedPreferences, tmdbImageLoader, callback = {selectedEpisodeAndStats, action ->

            val selectedEpisode = selectedEpisodeAndStats?.watchedEpisode

            when(action) {
                WatchedEpisodesPagingAdapter.ACTION_NAVIGATE_EPISODE -> {
                    navigateToEpisode(selectedEpisode?.show_trakt_id ?: 0,selectedEpisode?.show_tmdb_id, selectedEpisode?.episode_season ?: 0, selectedEpisode?.episode_number ?: 0, selectedEpisode?.language ?: "en")

                }

                WatchedEpisodesPagingAdapter.ACTION_NAVIGATE_SHOW -> {
                    navigateToShow(selectedEpisode?.show_trakt_id ?: 0, selectedEpisode?.show_tmdb_id, selectedEpisode?.show_title)
                }

                WatchedEpisodesPagingAdapter.ACTION_REMOVE_HISTORY -> {
                    removeFromWatchedHistory(selectedEpisode)
                }

                else -> {
                    navigateToEpisode(selectedEpisode?.show_trakt_id ?: 0,selectedEpisode?.show_tmdb_id, selectedEpisode?.episode_season ?: 0, selectedEpisode?.episode_number ?: 0, selectedEpisode?.language ?: "en")
                }
            }
        })
        recyclerView.layoutManager = layoutManager

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

    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int?, seasonNumber: Int, episodeNumber: Int, language: String?) {
        val intent = Intent(context, EpisodeDetailsActivity::class.java)

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                language
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
        if(isLoggedIn) {
            Log.e(TAG, "onRefresh: CALLED", )

            //https://developer.android.com/reference/kotlin/androidx/paging/PagingDataAdapter#refresh()
            viewModel.onRefresh()
            //adapter.refresh()
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