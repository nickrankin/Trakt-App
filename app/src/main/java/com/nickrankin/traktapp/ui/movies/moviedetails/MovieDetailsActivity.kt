package com.nickrankin.traktapp.ui.movies.moviedetails

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.ActivityMovieDetailsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.uwetrottmann.tmdb2.entities.BaseCompany
import com.uwetrottmann.tmdb2.entities.Country
import com.uwetrottmann.tmdb2.entities.Genre
import com.uwetrottmann.trakt5.entities.CrewMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val ACTION_BUTTON_FRAGMENT = "action_button_fragment"
private const val TAG = "MovieDetailsActivity"

@AndroidEntryPoint
class MovieDetailsActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: MovieDetailsViewModel by viewModels()

    private lateinit var binding: ActivityMovieDetailsBinding
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var appBarLayout: AppBarLayout

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var movieDetailsOverviewFragment: MovieDetailsOverviewFragment
    private lateinit var movieDetailsActionButtonsFragment: MovieDetailsActionButtonsFragment

    private var movieTraktId: Int = 0


    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appBarLayout = binding.moviedetailsactivityAppbarlayout
        collapsingToolbarLayout = binding.moviedetailsactivityCollapsingToolbarLayout

        swipeRefreshLayout = binding.moviedetailsactivitySwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)
        progressBar = binding.moviedetailsactivityInner.moviedetailsactivityProgressbar

        setSupportActionBar(binding.moviedetailsactivityToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        movieTraktId = intent.getIntExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, -1)


        initFragments()

        getMovie()
        getPlayCount()

        if(intent.extras?.containsKey(MOVIE_DATA_KEY) == false || intent.extras?.getParcelable<MovieDataModel>(
                MOVIE_DATA_KEY) == null) {
            throw RuntimeException("MovieDataModel must be passed to this Activity.")
        }


    }

    private fun initFragments() {
        movieDetailsActionButtonsFragment = MovieDetailsActionButtonsFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(
                binding.moviedetailsactivityInner.moviedetailsactivityButtonsFragmentContainer.id,
                movieDetailsActionButtonsFragment,
                ACTION_BUTTON_FRAGMENT
            )
            .commit()

        if(isLoggedIn) {
            movieDetailsOverviewFragment = MovieDetailsOverviewFragment.newInstance()


            supportFragmentManager.beginTransaction()
                .replace(
                    binding.moviedetailsactivityInner.moviedetailsactivityFragmentContainer.id,
                    movieDetailsOverviewFragment, "overview_fragment"
                )
                .commit()
        }
    }

    private fun getMovie() {
        lifecycleScope.launchWhenStarted {
            viewModel.movie.collectLatest { movieResource ->

                when (movieResource) {
                    is Resource.Loading -> {

                        toggleFragmentProgressBar(true)

                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "getMovie: Loading ...")
                    }
                    is Resource.Success -> {
                        val movie = movieResource.data
                        progressBar.visibility = View.GONE

                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        binding.moviedetailsactivityInner.moviedetailsactivityMainGroup.visibility =
                            View.VISIBLE

                        if (movie != null) {
                            displayMovie(movie)
                        }

                        toggleFragmentProgressBar(false)
                    }

                    is Resource.Error -> {
                        toggleFragmentProgressBar(false)

                        progressBar.visibility = View.GONE
                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        if (movieResource.data != null) {
                            val movie = movieResource.data
                            displayMovie(movie)

                        }

                        showErrorSnackbarRetryButton(movieResource.error, binding.moviedetailsactivityInner.moviedetailsactivityFragmentContainer) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun toggleFragmentProgressBar(shouldDisplay: Boolean) {
        if(shouldDisplay) {
            movieDetailsActionButtonsFragment.view?.findViewById<ProgressBar>(R.id.actionbutton_loading_progress)?.visibility = View.VISIBLE
            movieDetailsOverviewFragment.view?.findViewById<ProgressBar>(R.id.moviedetailsoverview_progressbar)?.visibility = View.VISIBLE
        } else {
            movieDetailsActionButtonsFragment.view?.findViewById<ProgressBar>(R.id.actionbutton_loading_progress)?.visibility = View.GONE
            movieDetailsOverviewFragment.view?.findViewById<ProgressBar>(R.id.moviedetailsoverview_progressbar)?.visibility = View.GONE
        }
    }

    private fun displayMovie(tmMovie: TmMovie?) {

        if (tmMovie == null) {
            return
        }

        val title = tmMovie.title
        val tagline = tmMovie.tagline
        val releaseDate = tmMovie.release_date
        val imdbId = tmMovie.imdb_id
        val runtime = tmMovie.runtime
        val directors = tmMovie.directed_by
        val trailerUrl = tmMovie.trailer
        val companies = tmMovie.production_companies
        val genres = tmMovie.genres
        val countries = tmMovie.production_countries
        val posterPath = tmMovie.poster_path
        val backdropPath = tmMovie.backdrop_path
        val traktUserRating = tmMovie.trakt_rating

        binding.apply {
            if (backdropPath != null && backdropPath.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + backdropPath)
                    .into(moviedetailsactivityBackdrop)
            }

            displayToolbarTitle(title)

            moviedetailsactivityInner.apply {
                moviedetailsactivityTitle.text = title
                moviedetailsactivityTagline.text = tagline
                moviedetailsactivityRuntime.text = "Runtime: ${calculateRuntime(runtime ?: 0)}"

                if (directors.isNotEmpty()) {
                    moviedetailsactivityDirected.visibility = View.VISIBLE

                    displayDirectors(directors) { crewMember ->
                        Log.e(TAG, "displayMovie: Clicked ${crewMember?.person?.name}")

                    }
                } else {
                    moviedetailsactivityDirected.visibility = View.GONE
                }


                if (posterPath != null && posterPath.isNotBlank()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(moviedetailsactivityPoster)
                }

                if (releaseDate != null) {
                    moviedetailsactivityFirstAired.visibility = View.VISIBLE

                    moviedetailsactivityFirstAired.text = "Released: ${
                        getFormattedDate(
                            releaseDate,
                            sharedPreferences.getString(
                                "date_format",
                                AppConstants.DEFAULT_DATE_FORMAT
                            )!!, ""
                        )
                    }"
                } else {
                    moviedetailsactivityFirstAired.visibility = View.GONE
                }

                if (companies.isNotEmpty()) {
                    moviedetailsactivityCompany.visibility = View.VISIBLE

                    displayCompanies(companies) { baseCompany -> }
                } else {
                    moviedetailsactivityCompany.visibility = View.GONE
                }

                if (countries.isNotEmpty()) {
                    moviedetailsactivityCountry.visibility = View.VISIBLE

                    displayCountries(countries) { country -> }
                } else {
                    moviedetailsactivityCountry.visibility = View.GONE
                }

                if (genres.isNotEmpty()) {
                    moviedetailsactivityGenres.visibility = View.VISIBLE

                    displayGenres(genres) { genre -> }
                } else {
                    moviedetailsactivityGenres.visibility = View.GONE
                }

                if(traktUserRating != 0.0) {
                    moviedetailsactivityTraktRating.text = "Trakt Rating: ${ "%.1f".format(traktUserRating) }"
                }

                if (trailerUrl != null) {
                    handleTrailer(trailerUrl)
                }
            }

        }
        val imdbButton = binding.moviedetailsactivityInner.moviedetailsactivityImdbButton

        if (imdbId != null) {
            imdbButton.visibility = View.VISIBLE

            imdbButton.setOnClickListener {
                ImdbNavigationHelper.navigateToImdb(this, imdbId)
            }

        } else {
            imdbButton.visibility = View.GONE
        }
    }

    private fun getPlayCount() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedMovieStats.collectLatest { watchedMovieStats ->
                val totalPlaysTextView =
                    binding.moviedetailsactivityInner.moviedetailsactivityTotalPlays

                val totalPlays = watchedMovieStats?.plays ?: 0

                if (totalPlays == 0) {
                    totalPlaysTextView.text = "Plays: Unwatched"
                } else {
                    totalPlaysTextView.text = "$totalPlays Plays"
                }
            }
        }
    }

    private fun displayToolbarTitle(title: String?) {
        // Only show the title once the toolbar is totally collapsed
        // https://stackoverflow.com/questions/31662416/show-collapsingtoolbarlayout-title-only-when-collapsed
        var isShow = true
        var scrollRange = -1
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0) {
                collapsingToolbarLayout.title = title
                isShow = true
            } else if (isShow) {
                collapsingToolbarLayout.title =
                    " " //careful there should a space between double quote otherwise it wont work
                isShow = false
            }
        })
    }

    private fun handleTrailer(youtubeUrl: String?) {
        if (youtubeUrl != null) {
            val trailerButton = binding.moviedetailsactivityInner.moviedetailsactivityTrailer
            trailerButton.visibility = View.VISIBLE

            trailerButton.setOnClickListener {
                VideoTrailerHelper.navigateToVideoUrl(this, youtubeUrl)
            }
        }
    }

    private fun displayDirectors(
        crewMembers: List<CrewMember?>,
        callback: (crewMember: CrewMember?) -> Unit
    ) {
        val layout = binding.moviedetailsactivityInner.moviedetailsactivityDirected

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        Log.d(TAG, "getDirectorsViewList: Got ${crewMembers.size} crewmembers!")

        layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Directed by: ")))

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        crewMembers.map { director ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (director != crewMembers.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, director?.person?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, director?.person?.name ?: ""))
            }

            // Director was clicked, we supply CrewMember object to callback
            textView.setOnClickListener {
                callback(director)
            }

            textViews.add(textView)
        }

        if (textViews.size > 2) {
            val initialTextViews = truncateTextViewList(layout, textViews)

            initialTextViews.map { initialTextView ->
                layout.addView(initialTextView)
            }


        } else {
            textViews.map {
                layout.addView(it)
            }
        }
    }

    private fun displayCompanies(
        companies: List<BaseCompany?>,
        callback: (company: BaseCompany?) -> Unit
    ) {
        val layout = binding.moviedetailsactivityInner.moviedetailsactivityCompany

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        if (companies.size > 1) {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Companies: ")))

        } else {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Company: ")))
        }

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        companies.map { company ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (company != companies.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, company?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, company?.name ?: ""))
            }

            // Director was clicked, we supply CrewMember object to callback
            textView.setOnClickListener {
                callback(company)
            }

            textViews.add(textView)
        }

        if (textViews.size > 2) {
            val initialTextViews = truncateTextViewList(layout, textViews)

            initialTextViews.map { initialTextView ->
                layout.addView(initialTextView)
            }


        } else {
            textViews.map {
                layout.addView(it)
            }
        }
    }

    private fun displayGenres(genres: List<Genre?>, callback: (genre: Genre?) -> Unit) {
        val layout = binding.moviedetailsactivityInner.moviedetailsactivityGenres

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        if (genres.size > 1) {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Genres: ")))

        } else {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Genre: ")))
        }

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        genres.map { genre ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (genre != genres.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, genre?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, genre?.name ?: ""))
            }

            // Director was clicked, we supply CrewMember object to callback
            textView.setOnClickListener {
                callback(genre)
            }

            textViews.add(textView)
        }

        if (textViews.size > 4) {
            val initialTextViews = truncateTextViewList(layout, textViews)

            initialTextViews.map { initialTextView ->
                layout.addView(initialTextView)
            }


        } else {
            textViews.map {
                layout.addView(it)
            }
        }
    }

    private fun displayCountries(countries: List<Country?>, callback: (country: Country?) -> Unit) {
        val layout = binding.moviedetailsactivityInner.moviedetailsactivityCountry

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        if (countries.size > 1) {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Countries: ")))

        } else {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Country: ")))
        }

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        countries.map { country ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (country != countries.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, country?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, country?.name ?: ""))
            }

            // Director was clicked, we supply CrewMember object to callback
            textView.setOnClickListener {
                callback(country)
            }

            textViews.add(textView)
        }

        if (textViews.size > 2) {
            val initialTextViews = truncateTextViewList(layout, textViews)

            initialTextViews.map { initialTextView ->
                layout.addView(initialTextView)
            }


        } else {
            textViews.map {
                layout.addView(it)
            }
        }
    }

    private fun getTextView(context: Context, title: String): TextView {
        val textView = TextView(context)

        textView.text = title

        return textView
    }

    private fun setTextViewLayoutConfig(textView: TextView): TextView {
        val lp = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 8, 6, 8)
        textView.layoutParams = lp

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        textView.setTypeface(textView.typeface, Typeface.BOLD)

        return textView
    }

    private fun truncateTextViewList(
        layout: FlexboxLayout,
        textViews: List<TextView>
    ): List<TextView> {
        // Trakt will truncate views when too many entries are appearing e.g, for a situation where there are 10 production companies, Trakt will show the first two with a trailing "plus 8 more..."
        // This is a simple implementation of the same feature

        // TextViews to initially show. We show one TextViews (Note TextView 0 is assumed to be the label string hence ending at Array pos 1)
        val initialTextViews = textViews.subList(0, 1).toMutableList()
        // Remaining TextViews to be appended by user action
        val remainingTextViews = textViews.subList(1, textViews.size).toMutableList()

        // A simple message to user to click to extend
        val extenderTextView = setTextViewLayoutConfig(
            getTextView(
                layout.context,
                " + ${remainingTextViews.size} more..."
            )
        )

        initialTextViews.add(extenderTextView)

        extenderTextView.setOnClickListener { extenderTextView ->
            // Remove the Extender text view
            layout.removeView(extenderTextView)

            // Display remaining views
            remainingTextViews.map { remainingTextViews ->
                layout.addView(remainingTextViews)
            }
        }

        return initialTextViews
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

    companion object {
        const val MOVIE_DATA_KEY = "movie_data_key"
    }
}