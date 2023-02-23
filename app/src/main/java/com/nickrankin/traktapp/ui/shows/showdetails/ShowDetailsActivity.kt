package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityShowDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImdbNavigationHelper
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.movies.moviedetails.SimilarMoviesFragment
import com.nickrankin.traktapp.ui.person.PersonActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToEpisode
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.StringUtils
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

private const val TAG = "ShowDetailsActivity"

private const val TRAKT_DIRECTOR_KEY = "Director"
private const val TRAKT_WRITING_KEY = "Writer"
private const val FRAGMENT_OVERVIEW_TAG = "overview_fragment"
private const val FRAGMENT_SEASONS_TAG = "seasons_fragment"
private const val FRAGMENT_PROGRESS_TAG = "progress_fragment"
private const val FRAGMENT_ACTION_BUTTONS = "action_buttons_fragment"
private const val FRAGMENT_CREDITS = "action_buttons_fragment"

private const val DISPLAY_SIMILAR = true
private const val SHOW_STREAMING_SERVICES = true

private const val SELECT_TAB_POS = "selected_tab_pos"

@AndroidEntryPoint
class ShowDetailsActivity : BaseActivity(), OnNavigateToEpisode,
    SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: ActivityShowDetailsBinding

    private lateinit var swipeRefeshLayout: SwipeRefreshLayout

    private var showTraktId = 0
    private var showTmdbId: Int? = null
    private var showTitle: String? = ""

    private val viewModel: ShowDetailsViewModel by viewModels()

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowDetailsBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        // Init SwipeRefreshLayout
        swipeRefeshLayout = bindings.showdetailsactivitySwipeLayout
        swipeRefeshLayout.setOnRefreshListener(this)


        // Action bar setup
        setSupportActionBar(bindings.showdetailsactivityToolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Init variable
        showTraktId = intent.getIntExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, 0)

        Log.d(TAG, "onCreate: Got show $showTitle with TraktId $showTraktId TmdbId $showTmdbId")

        // We need to check if the parent activity has sent ShowDataModel
        if(intent.extras?.containsKey(SHOW_DATA_KEY) == false || intent!!.extras?.getParcelable<ShowDataModel>(
                SHOW_DATA_KEY) == null) {
            throw RuntimeException("Must pass ShowDataModel to ${this.javaClass.name}.")
        }

        initFragments()

        // Get data
        getShow()
    }

    override fun onResume() {
        super.onResume()

        viewModel.resetRefreshState()
    }

    private fun getShow() {
        val showLoadingProgressBar = bindings.showdetailsactivityProgressbar

        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when (showResource) {
                    is Resource.Loading -> {
                        // No need for 2 progressbars if swiperefresh invoked
                        if(!swipeRefeshLayout.isRefreshing) {
                            showLoadingProgressBar.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Success -> {


                        if(show != null) {
                            displayShowInformation(show)
                            displayExternalLinks(show)
                        }
                        if(swipeRefeshLayout.isRefreshing) {
                            swipeRefeshLayout.isRefreshing = false
                        }

                        showLoadingProgressBar.visibility = View.GONE
                    }
                    is Resource.Error -> {
                        // If show in cache, display it cache
                        if (show != null) {
                            displayShowInformation(show)
                        }

                        if(swipeRefeshLayout.isRefreshing) {
                            swipeRefeshLayout.isRefreshing = false
                        }
                        showLoadingProgressBar.visibility = View.GONE

                        showErrorSnackbarRetryButton(showResource.error, bindings.root) {
                            onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun initFragments() {
        val showActionButtonsFragment = ShowDetailsActionButtonsFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(
                bindings.showdetailsactivityActionButtons.id,
                showActionButtonsFragment,
                FRAGMENT_ACTION_BUTTONS
            )
            .replace(bindings.showdetailsactivitySeason.id, ShowSeasonsFragment.newInstance())
            .replace(bindings.showdetailsactivityCastCrew.id, ShowCreditsFragment.newInstance(), FRAGMENT_CREDITS)
            .commit()
    }

    private fun displayShowInformation(tmShow: TmShow?) {
        if(tmShow == null) {
            return
        }

        if(SHOW_STREAMING_SERVICES) {
            displayStreamingServices(tmShow)
        }

        showSimilarShows(tmShow)

        supportActionBar?.title = tmShow.name


        if (tmShow.backdrop_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + tmShow.backdrop_path)
                .into(bindings.showdetailsactivityBackdrop)
        }

        bindings.apply {

            showdetailsactivityNumberEpisodes.text = "Total episodes: ${tmShow.num_episodes ?:  "Unknown" }"

            if(!tmShow.language.isNullOrEmpty()) {
                showdetailsactivityLanguage.visibility = View.VISIBLE
                showdetailsactivityLanguage.text = "Language: ${ Locale.Builder().setLanguage(tmShow.language).build().displayLanguage }"
            } else {
                showdetailsactivityLanguage.visibility = View.GONE
            }

            if(!tmShow.created_by.isNullOrEmpty()) {
                showdetailsactivityDirectedByTitle.visibility = View.VISIBLE

                displayDirectors(tmShow.created_by)

            } else {
                showdetailsactivityDirectedByTitle.visibility = View.GONE
            }

            if(!tmShow.written_by.isNullOrEmpty()) {
                showdetailsactivityWrittenByTitle.visibility = View.VISIBLE

                displayWriters(tmShow.written_by)
            } else {
                showdetailsactivityWrittenByTitle.visibility = View.GONE
            }

            if (tmShow.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + tmShow.poster_path)
                    .into(showdetailsactivityPoster)
            }

            showdetailsactivityTitle.text = tmShow.name
            showdetailsactivityOverview.text = tmShow.overview

            if(tmShow.status != null) {
                showdetailsactivityStatus.visibility = View.VISIBLE

                showdetailsactivityStatus.text = "Status: ${ StringUtils.capitalize(tmShow.status.toString()) }"
            } else {
                showdetailsactivityStatus.visibility = View.GONE
            }

            if (tmShow.first_aired != null) {
                showdetailsactivityReleaseDate.visibility = View.VISIBLE

                showdetailsactivityReleaseDate.text = "Premiered: ${tmShow.first_aired.format(
                    DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT))
                ) }"
            } else {
                showdetailsactivityReleaseDate.visibility = View.GONE
            }

            if(tmShow.network != null) {

                displayNetworks(listOf(tmShow.network)) { networkClickCallback ->
                }
            }

            if(tmShow.country != null) {
                showdetailsactivityCountry.visibility = View.VISIBLE

                showdetailsactivityCountry.text = "Country ${ Locale("", tmShow.country).displayCountry }"

            } else {
                showdetailsactivityCountry.visibility = View.GONE
            }

            if (tmShow.runtime != null) {
                showdetailsactivityRuntime.visibility = View.VISIBLE

                showdetailsactivityRuntime.text = "Runtime: ${tmShow.runtime} Minutes"
            } else {
                showdetailsactivityRuntime.visibility = View.GONE
            }

            if(tmShow.genres?.isNotEmpty() == true) {

                displayGenres(tmShow.genres) { genresClickCallback ->

                }
            }
        }
    }

    private fun displayExternalLinks(show: TmShow) {
        if(show.imdb_id != null) {
            bindings.showdetailsactivityChipImdb.setOnClickListener {
                ImdbNavigationHelper.navigateToImdb(this, show.imdb_id)
            }
        } else {
            bindings.showdetailsactivityChipImdb.visibility = View.GONE
        }

        if(show.homepage != null) {
            bindings.showdetailsactivityChipOfficialWebsite.setOnClickListener {

                try {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(show.homepage))
                    startActivity(i)
                } catch(e: Exception) {
                    Toast.makeText(this, "Error loading the Movie homepage. ", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "displayExternalLinks: Couldn't load homepage, ${e.message}", )
                }
            }
        } else {
            bindings.showdetailsactivityChipOfficialWebsite.visibility = View.GONE
        }

        if(show.tmdb_id != null) {
            bindings.showdetailsactivityChipTmdb.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.TMDB_SHOW_URL + show.tmdb_id))
                    startActivity(i)
                } catch(e: Exception) {
                    Log.e(TAG, "displayExternalLinks: Error loading TMDB URL: ${e.message}", )
                    Toast.makeText(this, "Error loading TMDB URL.", Toast.LENGTH_SHORT).show()
                }

            }
        } else {
            bindings.showdetailsactivityChipTmdb.visibility = View.GONE
        }
    }

    private fun displayStreamingServices(show: TmShow) {
        lifecycleScope.launchWhenStarted {
            supportFragmentManager.beginTransaction()
                .replace(bindings.showdetailsactivityWatch.id, ShowVideServicesFragment())
                .commit()
        }

    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String?
    ) {
        val intent = Intent(this, EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
        EpisodeDataModel(
            showTraktId,
            showTmdbId,
            seasonNumber,
            episodeNumber,
            ""
        ))

        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModel.onRefresh()

    }


    override fun onDestroy() {
        super.onDestroy()
    }

    private fun displayDirectors(people: List<com.uwetrottmann.trakt5.entities.CrewMember?>) {

        Log.d(TAG, "displayDirectors: Got ${people.size} people!")

        if(people.isEmpty()) {
            bindings.showdetailsactivityDirectedByTitle.visibility = View.GONE
            bindings.showdetailsactivityDirectByChipgroup.visibility = View.GONE
            return
        } else {
            bindings.showdetailsactivityDirectedByTitle.visibility = View.VISIBLE
            bindings.showdetailsactivityDirectByChipgroup.visibility = View.VISIBLE
        }

        val directorsChipGroup = bindings.showdetailsactivityDirectByChipgroup
        directorsChipGroup.removeAllViews()

        directorsChipGroup.apply {
            people.forEach { director ->
                val chip = Chip(context)

                chip.text = director?.person?.name

                chip.setOnClickListener {
                    navigateToPerson(director?.person?.ids?.trakt)
                }


                this.addView(chip)
            }
        }
    }

    private fun displayWriters(
        people: List<com.uwetrottmann.trakt5.entities.CrewMember?>) {

        Log.d(TAG, "displayWriters: Got ${people.size} people!")

        if(people.isEmpty()) {
            bindings.showdetailsactivityWrittenByTitle.visibility = View.GONE
            bindings.showdetailsactivityWrittenByChipgroup.visibility = View.GONE
            return
        } else {
            bindings.showdetailsactivityWrittenByTitle.visibility = View.VISIBLE
            bindings.showdetailsactivityWrittenByChipgroup.visibility = View.VISIBLE
        }

        val directorsChipGroup = bindings.showdetailsactivityWrittenByChipgroup
        directorsChipGroup.removeAllViews()

        directorsChipGroup.apply {
            people.forEach { writer ->
                val chip = Chip(context)

                chip.text = writer?.person?.name

                chip.setOnClickListener {
                    navigateToPerson(writer?.person?.ids?.trakt)
                }


                this.addView(chip)
            }
        }
    }

    private fun displayNetworks(
        networks: List<String?>,
        callback: (company: String?) -> Unit
    ) {
        val layout = bindings.showdetailsactivityCompanies

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        if (networks.size > 1) {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Networks: ")))

        } else {
            layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Network: ")))
        }

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        networks.map { network ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (network != networks.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, network + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, network ?: ""))
            }

            // Director was clicked, we supply CrewMember object to callback
            textView.setOnClickListener {
                callback(network)
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

    private fun displayGenres(genres: List<String?>, callback: (genre: String?) -> Unit) {
        val genresGroup = bindings.showdetailsactivityTagsChipgroup
        genresGroup.removeAllViews()

        genres.forEach { genre ->
            val tag = Chip(this)
            tag.text = StringUtils.capitalize(genre)

            tag.setOnClickListener {
//                navigateToTag(genre)

            }

            genresGroup.addView(tag)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.entitydetailsmenu_refresh -> {
                onRefresh()
            }
        }

        return false
    }

    private fun navigateToPerson(traktId: Int?) {
        if(traktId == null) {
            Log.e(TAG, "navigateToPerson: Director hasn't got Trakt ID associatted, returning")

            return
        }

        val intent = Intent(this, PersonActivity::class.java)
        intent.putExtra(PersonActivity.PERSON_ID_KEY, traktId)

        startActivity(intent)
    }

    private fun showSimilarShows(show: TmShow?) {
        if(show == null || !DISPLAY_SIMILAR) {
            return
        }

        val fm = supportFragmentManager
        val similarShowsCardView = bindings.showdetailsactivitySimilar

        fm.beginTransaction()
            .replace(similarShowsCardView.id, SimilarShowsFragment.newInstance(show.tmdb_id ?: 0, show.language))
            .commit()
    }

    private fun displayToastMessage(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    companion object {
        const val SHOW_DATA_KEY = "show_data_key"

    }
}