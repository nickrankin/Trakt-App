package com.nickrankin.traktapp.ui.movies.watched

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesLoadStateAdapter
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesPagingAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesLoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.databinding.FragmentWatchedMoviesBinding
import com.nickrankin.traktapp.databinding.FragmentWatchingBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.watched.WatchedMoviesViewModel
import com.nickrankin.traktapp.model.shows.WatchedEpisodesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.ui.movies.MovieDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class WatchedMoviesFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentWatchedMoviesBinding
    private val viewModel: WatchedMoviesViewModel by activityViewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: WatchedMoviesPagingAdapter

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var imageLoader: PosterImageLoader

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentWatchedMoviesBinding.inflate(inflater)


        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeLayout = bindings.moviewatchingfragmentSwipeRefreshLayout
        progressBar = bindings.moviewatchingfragmentProgressbar

        swipeLayout.setOnRefreshListener(this)

        initRecycler()

        getEvents()
        getWatchedMovies()

    }

    private fun getWatchedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedMovies.collectLatest { latestData ->


                progressBar.visibility = View.GONE

                if(swipeLayout.isRefreshing) {
                    swipeLayout.isRefreshing = false
                }

                adapter.submitData(latestData)

            }
        }

    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->

                when(event) {
                    is WatchedMoviesViewModel.Event.RemoveWatchedHistoryEvent -> {
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
                        }
                    } else -> {
                    //
                }
                }

            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.moviewatchingfragmentRecyclerview
        layoutManager = LinearLayoutManager(context)
        adapter = WatchedMoviesPagingAdapter(sharedPreferences, imageLoader, glide, callback = {selectedMovie, action ->

            val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
            intent.putExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, selectedMovie?.trakt_id)
            intent.putExtra(MovieDetailsRepository.MOVIE_TITLE_KEY, selectedMovie?.title)

            startActivity(intent)

//            when(action) {
//                WatchedEpisodesPagingAdapter.ACTION_NAVIGATE_EPISODE -> {
//                    navigateToEpisode(selectedEpisode?.show_trakt_id ?: 0,selectedEpisode?.show_tmdb_id, selectedEpisode?.episode_season ?: 0, selectedEpisode?.episode_number ?: 0, selectedEpisode?.language ?: "en")
//
//                }
//
//                WatchedEpisodesPagingAdapter.ACTION_NAVIGATE_SHOW -> {
//                    navigateToShow(selectedEpisode?.show_trakt_id ?: 0, selectedEpisode?.show_tmdb_id ?: 0, selectedEpisode?.show_title, selectedEpisode?.language)
//                }
//
//                WatchedEpisodesPagingAdapter.ACTION_REMOVE_HISTORY -> {
//                    removeFromWatchedHistory(selectedEpisode)
//                }
//
//                else -> {
//                    navigateToEpisode(selectedEpisode?.show_trakt_id ?: 0,selectedEpisode?.show_tmdb_id, selectedEpisode?.episode_season ?: 0, selectedEpisode?.episode_number ?: 0, selectedEpisode?.language ?: "en")
//                }
//            }
        })
        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = WatchedMoviesLoadStateAdapter(adapter),
            footer = WatchedMoviesLoadStateAdapter(adapter)
        )

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadStates ->
                bindings.moviewatchingfragmentSwipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { bindings.moviewatchingfragmentRecyclerview.scrollToPosition(0) }
        }

    }

    private fun displayMessageToast(message: String, duration: Int) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            WatchedMoviesFragment()
    }
}