package com.nickrankin.traktapp.ui.movies

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.ActivityMovieDetailsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "MovieDetailsActivity"
@AndroidEntryPoint
class MovieDetailsActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: MovieDetailsViewModel by viewModels()

    private lateinit var binding: ActivityMovieDetailsBinding
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        swipeRefreshLayout = binding.moviedetailsactivitySwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)
        progressBar = binding.moviedetailsactivityInner.moviedetailsactivityProgressbar

        setSupportActionBar(binding.moviedetailsactivityToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initActionButtons()

        getMovie()
        getUserRatings()

    }

    private fun initActionButtons() {
        supportFragmentManager.beginTransaction()
            .add(binding.moviedetailsactivityInner.moviedetailsactivityButtonsFragmentContainer.id, MovieDetailsActionButtonsFragment.newInstance())
            .commit()
    }

    private fun getMovie() {
        lifecycleScope.launchWhenStarted {
            viewModel.movie.collectLatest { movieResource ->

                when(movieResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        binding.moviedetailsactivityInner.moviedetailsactivityErrorGroup.visibility = View.GONE

                        Log.d(TAG, "getMovie: Loading ...")
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        binding.moviedetailsactivityInner.moviedetailsactivityMainGroup.visibility = View.VISIBLE
                        Log.d(TAG, "getMovie: ${movieResource.data}")

                        if(movieResource.data != null) {
                            displayMovie(movieResource.data!!)

                        }

                    }

                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        if(movieResource.data != null) {
                            displayMovie(movieResource.data!!)
                        } else {

                            binding.moviedetailsactivityInner.moviedetailsactivityErrorGroup.visibility = View.VISIBLE

                            binding.moviedetailsactivityInner.moviedetailsactivityErrorText.text = "An error occurred loading the movie. ${movieResource.error?.message}"

                            binding.moviedetailsactivityInner.moviedetailsactivityRetryButton.setOnClickListener {
                                onRefresh()
                            }
                        }


                        movieResource.error?.printStackTrace()
                    }
                }

            }
        }
    }

    private fun displayMovie(tmMovie: TmMovie) {
        val title = tmMovie.title
        val overview = tmMovie.overview
        val releaseDate = tmMovie.release_date
        val imdbId = tmMovie.imdb_id
        val runtime = tmMovie.runtime
        val trailerUrl = tmMovie.trailer

        val companies = tmMovie.production_companies
        val generes = tmMovie.genres


        val posterPath = tmMovie.poster_path
        val backdropPath = tmMovie.backdrop_path

        val status = tmMovie.status

        binding.apply {
            if(backdropPath != null && backdropPath.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + backdropPath)
                    .into(moviedetailsactivityBackdrop)
            }

            moviedetailsactivityCollapsingToolbarLayout.title = title

            moviedetailsactivityInner.apply {
                moviedetailsactivityTitle.text = title

                if(status != null) {
                    moviedetailsactivityStatus.text = status.value
                } else {
                    moviedetailsactivityStatus.text = "Unknown Status"
                }

                if(posterPath != null && posterPath.isNotBlank()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(moviedetailsactivityPoster)
                }

                if(releaseDate != null) {
                    moviedetailsactivityFirstAired.text = "Released: " + DateFormatUtils.format(releaseDate, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT))
                }
                if(runtime != null) {
                    moviedetailsactivityRuntime.text = "Runtime: ${calculateRuntime(runtime)}"
                }

                if(companies.isNotEmpty()) {
                    val companiesString = StringBuilder()

                    companies.map {
                        if(companies.last() != it) {
                            companiesString.append(it?.name)
                                .append(", ")
                        } else {
                            companiesString.append(it?.name)
                        }
                    }

                    moviedetailsactivityCompany.text = "Studio: $companiesString"
                }

                if(generes.isNotEmpty()) {
                    val genresString = StringBuilder()

                    generes.map {
                        if(generes.last() != it) {
                            genresString.append(it?.name)
                                .append(", ")
                        } else {
                            genresString.append(it?.name)
                        }
                    }

                    moviedetailsactivityGenres.text = "Genres: $genresString"
                }

                if(trailerUrl != null) {
                    handleTrailer(trailerUrl)
                }
        }

        }
        val imdbButton = binding.moviedetailsactivityInner.moviedetailsactivityImdbButton

        if(imdbId != null) {
            imdbButton.visibility = View.VISIBLE

            imdbButton.setOnClickListener {
                ImdbNavigationHelper.navigateToImdb(this, imdbId)
            }

        } else {
            imdbButton.visibility = View.GONE
        }
    }

    private fun getUserRatings() {
        binding.moviedetailsactivityInner.moviedetailsactivityTraktRating.visibility = View.GONE
        viewModel.userRatings.observe(this) { rating ->
            binding.moviedetailsactivityInner.moviedetailsactivityTraktRating.text = "Trakt Rating: ${"%.1f".format(rating)}"
            binding.moviedetailsactivityInner.moviedetailsactivityTraktRating.visibility = View.VISIBLE

        }
    }

    private fun handleTrailer(youtubeUrl: String?) {
        if(youtubeUrl != null) {
            val trailerButton = binding.moviedetailsactivityInner.moviedetailsactivityTrailer
            trailerButton.visibility = View.VISIBLE

            trailerButton.setOnClickListener {
                VideoTrailerHelper.navigateToVideoUrl(this, youtubeUrl)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}