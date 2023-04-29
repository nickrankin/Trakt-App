package com.nickrankin.traktapp.ui.movies.watched

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
import androidx.paging.map
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBasePagingAdapter
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesLoadStateAdapter
import com.nickrankin.traktapp.adapter.movies.WatchedMoviesPagingAdapter
//import com.nickrankin.traktapp.adapter.movies.WatchedMoviesPagingAdapter
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.databinding.FragmentWatchedMoviesBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.WatchedMoviesViewModel
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "WatchedMoviesFragment"
@AndroidEntryPoint
class WatchedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentSplitviewLayoutBinding
    private val viewModel: WatchedMoviesViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaEntryBasePagingAdapter<WatchedMovieAndStats>

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentSplitviewLayoutBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        progressBar = bindings.splitviewlayoutProgressbar


        updateTitle("Watched Movies")

        initRecycler()

        (activity as OnNavigateToEntity).enableOverviewLayout(false)


        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }
        getEvents()
        getWatchedMovies()
        getViewTypeState()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.layout_switcher_menu, menu)

    }

    private fun getWatchedMovies() {
        lifecycleScope.launchWhenStarted {

            viewModel.watchedMovies.collectLatest { latestData ->
                Log.e(TAG, "getWatchedMovies: Triggered ${System.currentTimeMillis()}", )
                
                progressBar.visibility = View.GONE

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

                                val syncResponse = getSyncResponse(syncResponseResource.data, Type.MOVIE)

                                when(syncResponse) {
                                    Response.DELETED_OK -> {
                                        displayMessageToast("Succesfully removed play!", Toast.LENGTH_SHORT)
                                    }
                                    Response.ERROR -> {
                                        displayMessageToast("Error removing play", Toast.LENGTH_SHORT)
                                    }
                                    Response.NOT_FOUND -> {
                                        displayMessageToast("Movie ID not found on Trakt", Toast.LENGTH_LONG)
                                    }
                                    else -> {}

                                }
                            }
                            is Resource.Error -> {
                                (activity as IHandleError).handleError(event.syncResponse.error, "Error removing play")
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
        recyclerView = bindings.splitviewlayoutRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBasePagingAdapter.VIEW_TYPE_POSTER)

        adapter = WatchedMoviesPagingAdapter(AdaptorActionControls(context?.getDrawable(R.drawable.ic_baseline_remove_red_eye_24),
            "Remove This Play", false, 
            R.menu.watched_popup_menu, entrySelectedCallback = { selectedMovie ->
                navigateToMovie(selectedMovie.watchedMovie)

            }, buttonClickedCallback = {selectedMovie ->
                handleWatchedHistoryDeletion(selectedMovie.watchedMovie)
            }, menuItemSelectedCallback = {selectedMovie, menuSelected -> 
                when(menuSelected) {
                    R.id.watchedpopupmenu_remove -> {
                        handleWatchedHistoryDeletion(selectedMovie.watchedMovie)
                    } 
                    else -> {
                        Log.e(TAG, "initRecycler: Invalid menu id $menuSelected", )
                    }
                }
            }), sharedPreferences, glide, tmdbPosterImageLoader)

        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = WatchedMoviesLoadStateAdapter(adapter),
            footer = WatchedMoviesLoadStateAdapter(adapter)
        )

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { loadStates ->
//                bindings.splitviewlayoutProgressbar.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { bindings.splitviewlayoutRecyclerview.scrollToPosition(0) }
        }
    }

    private fun navigateToMovie(watchedMovie: WatchedMovie?) {
        if(watchedMovie == null) {
            return
        }

        (activity as OnNavigateToEntity).navigateToMovie(
            MovieDataModel(
                watchedMovie.trakt_id,
                watchedMovie.tmdb_id,
                watchedMovie.title,
                0
            )
        )
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
                DateTimeFormatter.ofPattern(sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)))}?")
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
        super.onRefresh()

        viewModel.onRefresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
        }

        return false
    }

    private fun getViewTypeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewType ->

                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)

                recyclerView.scrollToPosition(0)

                adapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            WatchedMoviesFragment()
    }
}