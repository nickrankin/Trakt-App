package com.nickrankin.traktapp.ui.movies.watched

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesLoadStateAdapter
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesPagingAdapter
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.databinding.FragmentWatchedMoviesBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.watched.WatchedMoviesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "WatchedMoviesFragment"
@AndroidEntryPoint
class WatchedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

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
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

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

        updateTitle("Watched Movies")

        initRecycler()

        getEvents()
    }

    private fun getWatchedMovies() {
        lifecycleScope.launchWhenStarted {

            viewModel.watchedMovies.collectLatest { latestData ->
                
                latestData.map {
                    Log.e(TAG, "getWatchedMovies: ${it.watchedMovie.title}", )
                }

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
                                if(syncResponseResource.data?.deleted?.movies ?: 0 > 0) {
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
        adapter = WatchedMoviesPagingAdapter(sharedPreferences, tmdbPosterImageLoader, callback = {selectedMovie, action ->

            when(action) {
                WatchedMoviesPagingAdapter.ACTION_NAVIGATE_MOVIE -> {
                    navigateToMovie(selectedMovie)
                }
                WatchedMoviesPagingAdapter.ACTION_REMOVE_HISTORY -> {
                    handleWatchedHistoryDeletion(selectedMovie)
                }
                else -> {
                    navigateToMovie(selectedMovie)
                }
            }
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

        getWatchedMovies()


    }

    private fun navigateToMovie(watchedMovie: WatchedMovie?) {
        if(watchedMovie == null) {
            return
        }
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
        intent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY,
            MovieDataModel(
                watchedMovie.trakt_id,
                watchedMovie.tmdb_id,
                watchedMovie.title
            )
        )
        startActivity(intent)
    }

    private fun handleWatchedHistoryDeletion(watchedMovie: WatchedMovie?) {

        if(watchedMovie == null) {
            return
        }

        val syncItems = SyncItems().ids(watchedMovie.id)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Remove play for ${watchedMovie.title}")
            .setMessage("Do you want to remove play for ${watchedMovie.title} at ${watchedMovie.watched_at?.atZoneSameInstant(
                org.threeten.bp.ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)))}?")
            .setPositiveButton("Remove", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeFromWatchedHistory(syncItems)
                dialogInterface.dismiss()
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            }).create()

        alertDialog.show()
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