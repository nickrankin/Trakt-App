package com.nickrankin.traktapp.ui.movies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.movies.ReccomendedMoviesAdaptor
import com.nickrankin.traktapp.adapter.movies.TrendingMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentTrendingMoviesBinding
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.TrendingMoviesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "TrendingMoviesFragment"
@AndroidEntryPoint
class TrendingMoviesFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentTrendingMoviesBinding
    private val viewModel: TrendingMoviesViewModel by activityViewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrendingMoviesAdaptor

    @Inject
    lateinit var posterImageLoader: PosterImageLoader

    @Inject
    lateinit var glide: RequestManager

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
                        bindings.trendingmoviesfragmentErrorText.visibility = View.GONE
                        bindings.trendingmoviesfragmentRetryButton.visibility = View.GONE

                    }

                    is Resource.Success -> {
                        Log.d(TAG, "getTrendingMovies: Got ${trendingMoviesResource.data?.size} movies")

                        progressBar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        adapter.submitList(trendingMoviesResource.data)
//
//                        trendingMoviesResource.data?.map { trending ->
//
//                            Log.d(TAG, "getTrendingMovies: Got Trending ${trending.movie.title} . Watching now: ${trending.watchers}")
//
//                        }
                    }

                    is Resource.Error -> {

                        progressBar.visibility = View.GONE

                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        bindings.trendingmoviesfragmentErrorText.visibility = View.VISIBLE
                        bindings.trendingmoviesfragmentRetryButton.visibility = View.VISIBLE

                        recyclerView.visibility = View.GONE

                        bindings.trendingmoviesfragmentErrorText.text = "Error loading recommended Movies. ${trendingMoviesResource.error?.message}"

                        bindings.trendingmoviesfragmentRetryButton.setOnClickListener { onRefresh() }

                        Log.e(TAG, "getTrendingMovies: Error getting movies ${trendingMoviesResource.error?.message}", )
                        trendingMoviesResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.trendingmoviesfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        adapter = TrendingMoviesAdaptor(glide, posterImageLoader, callback = {trendingMovie ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
            intent.putExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, trendingMovie?.movie?.ids?.trakt ?: -1)
            intent.putExtra(MovieDetailsRepository.MOVIE_TITLE_KEY, trendingMovie?.movie?.title)

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