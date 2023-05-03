package com.nickrankin.traktapp.ui.movies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.TrendingMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentSplitviewLayoutBinding
import com.nickrankin.traktapp.databinding.FragmentTrendingMoviesBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.TrendingMoviesViewModel
import com.uwetrottmann.trakt5.entities.TrendingMovie
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "TrendingMoviesFragment"
@AndroidEntryPoint
class TrendingMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentSplitviewLayoutBinding
    private val viewModel: TrendingMoviesViewModel by activityViewModels()

    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrendingMoviesAdaptor

    @Inject
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentSplitviewLayoutBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        progressBar = bindings.splitviewlayoutProgressbar

        updateTitle("Trending Movies")

        initRecycler()
        getViewType()

        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        getTrendingMovies()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.trending_filter_menu, menu)
        inflater.inflate(R.menu.layout_switcher_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

    }

    private fun getTrendingMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.trendingMovies.collectLatest { trendingMoviesResource ->
                when(trendingMoviesResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getTrendingMovies: Loading Trending Movies")
                        progressBar.visibility = View.VISIBLE

                        recyclerView.visibility = View.VISIBLE
                    }

                    is Resource.Success -> {
                        Log.d(TAG, "getTrendingMovies: Got ${trendingMoviesResource.data?.size} movies")

                        progressBar.visibility = View.GONE

                        adapter.submitList(trendingMoviesResource.data) {
                            recyclerView.scrollToPosition(0)
                        }

                    }

                    is Resource.Error -> {

                        progressBar.visibility = View.GONE

                        recyclerView.visibility = View.GONE

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            trendingMoviesResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.splitviewlayoutRecyclerview

        switchRecyclerViewLayoutManager(requireContext(), recyclerView, MediaEntryBaseAdapter.VIEW_TYPE_POSTER)

        adapter = TrendingMoviesAdaptor(tmdbPosterImageLoader, callback = { trendingMovie ->
            navigateToMovie(trendingMovie)
        })

        recyclerView.adapter = adapter
    }

    private fun navigateToMovie(trendingMovie: TrendingMovie?) {
        if(trendingMovie == null) {
            Log.e(TAG, "navigateToMovie: Trending movies object cannot be null")

            return
        }

        (activity as MoviesMainActivity).navigateToMovie(
            MovieDataModel(
                trendingMovie.movie?.ids?.trakt ?: 0,
                trendingMovie.movie?.ids?.tmdb,
                trendingMovie.movie?.title,
                trendingMovie.movie?.year
            )
        )
    }

    private fun getViewType() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewType.collectLatest { viewType ->
                adapter.switchView(viewType)

                switchRecyclerViewLayoutManager(requireContext(), recyclerView, viewType)


                recyclerView.scrollToPosition(0)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.menu_switch_layout -> {
                lifecycleScope.launchWhenStarted {
                    viewModel.switchViewType()
                }
            }
            R.id.trendingfiltermenu_title -> {
                viewModel.applySorting(ISortable.SORT_BY_TITLE)
            }
            R.id.trendingfiltermenu_year -> {
                viewModel.applySorting(ISortable.SORT_BY_YEAR)
            }
            R.id.trendingfiltermenu_watchers -> {
                viewModel.applySorting(TrendingMoviesViewModel.TOTAL_WATCHING_SORT_BY)
            }
            else -> {
                Log.e(TAG, "onOptionsItemSelected: Invalid menu item ${item.itemId}")
            }
        }

        return false
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
            TrendingMoviesFragment()
    }
}