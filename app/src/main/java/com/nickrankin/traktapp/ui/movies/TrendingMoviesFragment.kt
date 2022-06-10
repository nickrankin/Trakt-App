package com.nickrankin.traktapp.ui.movies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.adapter.movies.TrendingMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentTrendingMoviesBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.TrendingMoviesViewModel
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "TrendingMoviesFragment"
@AndroidEntryPoint
class TrendingMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentTrendingMoviesBinding
    private val viewModel: TrendingMoviesViewModel by activityViewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrendingMoviesAdaptor

    @Inject
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = FragmentTrendingMoviesBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeLayout = bindings.trendingmoviesfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.trendingmoviesfragmentProgressbar

        updateTitle("Trending Movies")

        initRecycler()
        getTrendingMovies()
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

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        adapter.submitList(trendingMoviesResource.data)
                    }

                    is Resource.Error -> {

                        progressBar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        recyclerView.visibility = View.GONE

                        (activity as IHandleError).showErrorSnackbarRetryButton(
                            trendingMoviesResource.error,
                            bindings.trendingmoviesfragmentSwipeLayout
                        ) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.trendingmoviesfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        adapter = TrendingMoviesAdaptor(tmdbPosterImageLoader, callback = { trendingMovie ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
            intent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY,
                MovieDataModel(
                    trendingMovie?.movie?.ids?.trakt ?: 0,
                    trendingMovie?.movie?.ids?.tmdb,
                    trendingMovie?.movie?.title,
                    trendingMovie?.movie?.year
                )
            )
            startActivity(intent)

        })

        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter
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