package com.nickrankin.traktapp.ui.movies.moviedetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.dao.credits.model.TmCrewPerson
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.ActivityMovieDetailsLayoutBinding
import com.nickrankin.traktapp.databinding.FragmentMovieDetailsLayoutBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.PersonDataModel
import com.nickrankin.traktapp.model.movies.MovieDetailsViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.ui.OnSearchByGenre
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.collected.CollectedMoviesFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.uwetrottmann.tmdb2.entities.BaseCompany
import com.uwetrottmann.tmdb2.entities.Country
import com.uwetrottmann.trakt5.entities.CrewMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.StringUtils
import java.util.Locale
import javax.inject.Inject

private const val DISPLAY_SIMILAR = true
private const val DISPLAY_STREAMING_SERVICES = true
private const val ACTION_BUTTON_FRAGMENT = "action_button_fragment"
private const val TRAKT_DIRECTOR_KEY = "Director"
private const val TRAKT_WRITING_KEY = "Writer"
private const val TAG = "MovieDetailsActivity"

@AndroidEntryPoint
class MovieDetailsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: MovieDetailsViewModel by activityViewModels()
//    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var _bindings: FragmentMovieDetailsLayoutBinding? = null
    private val bindings get() = _bindings!!

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentMovieDetailsLayoutBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(arguments?.containsKey(MOVIE_DATA_KEY) == false || arguments?.getParcelable<MovieDataModel>(
                MOVIE_DATA_KEY) == null) {
            throw RuntimeException("MovieDataModel must be passed to this Activity.")
        }

        viewModel.switchMovie(arguments!!.getParcelable<MovieDataModel>(MOVIE_DATA_KEY)!!)

        (activity as OnNavigateToEntity).enableOverviewLayout(true)

        getMovie()

        displayStreamingService()

        setupCastCrewFragment()
        setupActionButtonFragment()

    }

    override fun onResume() {
        super.onResume()

        viewModel.resetRefreshState()
    }


    private fun getMovie() {
        lifecycleScope.launchWhenStarted {
            viewModel.movie.collectLatest { movieResource ->

                when (movieResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        val movie = movieResource.data
                        if (movie != null) {
                            displayMovie(movie)
                        }
                    }

                    is Resource.Error -> {
                        if (movieResource.data != null) {
                            val movie = movieResource.data
                            displayMovie(movie)
                        }

                        showErrorSnackbarRetryButton(movieResource.error, bindings.root) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun setupCastCrewFragment() {
        val fragmentTransaction = childFragmentManager.beginTransaction()
        val castCrewFragment = MovieDetailsCastCrewFragment.newInstance()

        fragmentTransaction.replace(bindings.moviedetailsactivityCastCrew.id, castCrewFragment)
            .commit()
    }

    private fun setupActionButtonFragment() {
        val fragmentTransaction = childFragmentManager.beginTransaction()
        val abFragment = MovieDetailsActionButtonsFragment.newInstance()

        fragmentTransaction.replace(bindings.moviedetailsactivityActionButtons.id, abFragment)
            .commit()
    }

    private fun displayMovie(tmMovie: TmMovie?) {

        if (tmMovie == null) {
            return
        }


        displayExternalLinks(tmMovie)

        val title = tmMovie.title
        val tagline = tmMovie.tagline
        val runtime = tmMovie.runtime
        val companies = tmMovie.production_companies
        val countries = tmMovie.production_countries
        val posterPath = tmMovie.poster_path
        val backdropPath = tmMovie.backdrop_path

        updateTitle(title)

        bindings.apply {
            if (backdropPath != null && backdropPath.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + backdropPath)
                    .into(moviedetailsactivityBackdrop)
            }
            moviedetailsactivityTitle.text = title
            moviedetailsactivityTagline.text = tagline
            moviedetailsactivityOverview.text = tmMovie.overview

            if(countries != null) {
                displayCountries(countries)
            }

            if(companies != null) {
                displayCompanies(companies)
            }

                if (posterPath != null && posterPath.isNotBlank()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(moviedetailsactivityPoster)
                }

            if(runtime != null) {
                moviedetailsactivityRuntime.text = "Runtime: ${ calculateRuntime(runtime) }"
            }

            if(tmMovie.original_language != null) {
                moviedetailsactivityLanguage.text = "Language: ${ Locale.Builder().setLanguage(tmMovie.original_language).build().displayLanguage }"
            }

        }

        if(tmMovie.release_date != null) {
            bindings.moviedetailsactivityReleaseDate.text = "Released: ${ getFormattedDate(tmMovie.release_date, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)) }"
        }

        displayGenres(tmMovie.genres)
        displayDirectors(tmMovie.directed_by.filter { it?.job == TRAKT_DIRECTOR_KEY })
        displayWriters(tmMovie.written_by.filter { it?.job == TRAKT_WRITING_KEY })

        if(DISPLAY_SIMILAR) {
            showSimilarMovies(tmMovie)
        }
    }

    private fun displayExternalLinks(movie: TmMovie) {
        if(movie.imdb_id != null) {
            bindings.moviedetailsactivityChipImdb.setOnClickListener {
                ImdbNavigationHelper.navigateToImdb(requireContext(), movie.imdb_id)
            }
        } else {
            bindings.moviedetailsactivityChipImdb.visibility = View.GONE
        }

        if(movie.homepage != null) {
            bindings.moviedetailsactivityChipOfficialWebsite.setOnClickListener {

                try {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(movie.homepage))
                    startActivity(i)
                } catch(e: Exception) {
                    Toast.makeText(requireContext(), "Error loading the Movie homepage. ", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "displayExternalLinks: Couldn't load homepage, ${e.message}")
                }
            }
        } else {
            bindings.moviedetailsactivityChipOfficialWebsite.visibility = View.GONE
        }

        if(movie.tmdb_id != null) {
            bindings.moviedetailsactivityChipTmdb.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TMDB_MOVIE_URL + movie.tmdb_id))
                    startActivity(i)
                } catch(e: Exception) {
                    Log.e(TAG, "displayExternalLinks: Error loading TMDB URL: ${e.message}")
                    Toast.makeText(requireContext(), "Error loading TMDB URL.", Toast.LENGTH_SHORT).show()
                }

            }
        } else {
            bindings.moviedetailsactivityChipTmdb.visibility = View.GONE
        }

    }

    private fun displayStreamingService() {
        val movieVideoServicesFragment = MovieVideoServicesFragment()

        childFragmentManager.beginTransaction()
            .replace(bindings.moviedetailsactivityWatch.id, movieVideoServicesFragment)
            .commit()
    }

    private fun displayWriters(writers: List<CrewMember?>) {
        if(writers.isEmpty()) {
            bindings.moviedetailsactivityWrittenByTitle.visibility = View.GONE
            bindings.moviedetailsactivityWrittenByChipgroup.visibility = View.GONE
            return
        } else {
            bindings.moviedetailsactivityWrittenByTitle.visibility = View.VISIBLE
            bindings.moviedetailsactivityWrittenByChipgroup.visibility = View.VISIBLE
        }
        val writersChipGroup = bindings.moviedetailsactivityWrittenByChipgroup
        writersChipGroup.removeAllViews()

        writersChipGroup.apply {
            writers.forEach { writer ->
                val chip = Chip(context)

                chip.text = writer?.person?.name

                chip.setOnClickListener {
                    navigateToPerson(writer?.person?.ids?.trakt)
                }

                this.addView(chip)
            }
        }
    }

    private fun displayDirectors(directors: List<CrewMember?>) {
        if(directors.isEmpty()) {
            bindings.moviedetailsactivityDirectedByTitle.visibility = View.GONE
            bindings.moviedetailsactivityDirectByChipgroup.visibility = View.GONE
            return
        } else {
            bindings.moviedetailsactivityDirectedByTitle.visibility = View.VISIBLE
            bindings.moviedetailsactivityDirectByChipgroup.visibility = View.VISIBLE
        }

        val directorsChipGroup = bindings.moviedetailsactivityDirectByChipgroup
        directorsChipGroup.removeAllViews()

        directorsChipGroup.apply {
            directors.forEach { director ->
                val chip = Chip(context)

                    chip.text = director?.person?.name

                    chip.setOnClickListener {
                        navigateToPerson(director?.person?.ids?.trakt)
                    }


                this.addView(chip)
            }
        }


    }

    private fun navigateToPerson(traktId: Int?) {
        if(traktId == null) {
            Log.e(TAG, "navigateToPerson: Director hasn't got Trakt ID associatted, returning")

            return
        }

        (activity as OnNavigateToEntity).navigateToPerson(
            PersonDataModel(traktId, null, null)

        )

    }

    private fun displayCompanies(
        companies: List<BaseCompany?>) {
        val layout = bindings.moviedetailsactivityCompanies

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

    private fun displayGenres(genres: List<String?>) {
        val genresGroup = bindings.moviedetailsactivityTagsChipgroup
        genresGroup.removeAllViews()

        genres.forEach { genre ->
            val tag = Chip(requireContext())
            tag.text = StringUtils.capitalize(genre)

            tag.setOnClickListener { tag ->
                childFragmentManager.popBackStack()
                val chip = tag as Chip

                (activity as OnSearchByGenre).doSearchWithGenre("", chip.text.toString())
            }

            genresGroup.addView(tag)
        }
    }

    private fun displayCountries(countries: List<Country?>) {
        val layout = bindings.moviedetailsactivityCountry

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

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
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

    private fun showSimilarMovies(tmMovie: TmMovie?) {
        if(tmMovie == null) {
            return
        }

        val fm = childFragmentManager
        val similarMoviesCardView = bindings.moviedetailsactivitySimilar

        fm.beginTransaction()
            .replace(similarMoviesCardView.id, SimilarMoviesFragment.newInstance(tmMovie.tmdb_id ?: 0, tmMovie.original_language))
            .commit()
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
            MovieDetailsFragment()

        const val MOVIE_DATA_KEY = "movie_data_key"
    }
}