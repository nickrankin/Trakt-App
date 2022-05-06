package com.nickrankin.traktapp.ui.movies

import android.content.DialogInterface
import android.content.Intent
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.movies.ReccomendedMoviesAdaptor
import com.nickrankin.traktapp.databinding.FragmentRecommendedMoviesBinding
import com.nickrankin.traktapp.helper.ItemDecorator
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.RecommendedMoviesViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "RecommendedMoviesFragme"

@AndroidEntryPoint
class RecommendedMoviesFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: FragmentRecommendedMoviesBinding

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReccomendedMoviesAdaptor

    private val viewModel: RecommendedMoviesViewModel by activityViewModels()

    @Inject
    lateinit var tmdbPosterImageLoader: TmdbImageLoader

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentRecommendedMoviesBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeLayout = bindings.recommendedmoviesfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.recommendedmoviesfragmentProgressbar

        updateTitle("Suggested Movies")

        initRecyclerView()

        getRecommendedMovies()

        getEvents()
    }

    private fun getRecommendedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.recommendedMovies.collectLatest { recommendedMoviesResource ->
                when (recommendedMoviesResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE

                        bindings.recommendedmoviesfragmentErrorText.visibility = View.GONE
                        bindings.recommendedmoviesfragmentRetryButton.visibility = View.GONE

                        recyclerView.visibility = View.VISIBLE


                        Log.d(TAG, "getRecommendedMovies: Loading recommendations")
                    }

                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.d(TAG, "getRecommendedMovies: Got recommendations successfully")

                        adapter.submitList(recommendedMoviesResource.data)
                    }

                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        bindings.recommendedmoviesfragmentErrorText.visibility = View.VISIBLE
                        bindings.recommendedmoviesfragmentRetryButton.visibility = View.VISIBLE

                        recyclerView.visibility = View.GONE

                        bindings.recommendedmoviesfragmentErrorText.text =
                            "Error loading recommended Movies. ${recommendedMoviesResource.error?.message}"

                        bindings.recommendedmoviesfragmentRetryButton.setOnClickListener { onRefresh() }

                        Log.e(
                            TAG,
                            "getRecommendedMovies: Error loading recommendations. ${recommendedMoviesResource.error?.message}",
                        )
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is RecommendedMoviesViewModel.Event.RemoveRecommendationEvent -> {
                        val response = event.response

                        when (response) {
                            is Resource.Loading -> {

                            }
                            is Resource.Success -> {
                                displayToast(
                                    "Successfully removed recommended movie",
                                    Toast.LENGTH_SHORT
                                )
                            }
                            is Resource.Error -> {
                                val throwable = response.error

                                throwable?.printStackTrace()

                                if (throwable is HttpException) {
                                    displayToast(
                                        "HTTP Error occurred removing recommendation (HTTP Code (${throwable.code()})",
                                        Toast.LENGTH_LONG
                                    )
                                } else {
                                    displayToast(
                                        "An exception was raised trying to remove recommendation, ${throwable?.message}",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = bindings.recommendedmoviesfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        adapter = ReccomendedMoviesAdaptor(tmdbPosterImageLoader, callback = { pos, action, movie ->

            when (action) {
                ReccomendedMoviesAdaptor.ACTION_VIEW -> {
                    navigateToMovie(movie)
                }
                ReccomendedMoviesAdaptor.ACTION_REMOVE -> {
                    deleteSuggestion(pos, movie)
                }
                else -> {
                    navigateToMovie(movie)
                }
            }
        })

        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter

    }

    private fun navigateToMovie(movie: Movie?) {
        if (movie == null) {
            return
        }

        val intent = Intent(requireContext(), MovieDetailsActivity::class.java)
        intent.putExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, movie.ids?.trakt)

        startActivity(intent)
    }

    private fun deleteSuggestion(position: Int, movie: Movie?) {
        if (movie == null) {
            return
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete suggestion ${movie.title}")
            .setMessage("Would you like to remove suggestion of ${movie.title}?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeRecommendedMovie(movie.ids?.trakt ?: 0)

                // Should really be called after success remove event
                removeAdapterItemAtPosition(position)

                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        alertDialog.show()
    }

    private fun removeAdapterItemAtPosition(pos: Int) {
        val listCopy: MutableList<Movie> = mutableListOf()
        listCopy.addAll(adapter.currentList)
        listCopy.removeAt(pos)

        adapter.submitList(listCopy)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    private fun displayToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RecommendedMoviesFragment()
    }
}