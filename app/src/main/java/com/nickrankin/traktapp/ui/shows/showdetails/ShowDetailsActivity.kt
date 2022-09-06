package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityShowDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImdbNavigationHelper
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.VideoTrailerHelper
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToEpisode
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.uwetrottmann.tmdb2.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject
import kotlin.ClassCastException
import kotlin.Exception

private const val TAG = "ShowDetailsActivity"

private const val FRAGMENT_OVERVIEW_TAG = "overview_fragment"
private const val FRAGMENT_SEASONS_TAG = "seasons_fragment"
private const val FRAGMENT_PROGRESS_TAG = "progress_fragment"
private const val FRAGMENT_ACTION_BUTTONS = "action_buttons_fragment"
private const val SELECT_TAB_POS = "selected_tab_pos"

@AndroidEntryPoint
class ShowDetailsActivity : BaseActivity(), OnNavigateToEpisode,
    SwipeRefreshLayout.OnRefreshListener, TabLayout.OnTabSelectedListener {
    private lateinit var bindings: ActivityShowDetailsBinding

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var swipeRefeshLayout: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout

    private var showTraktId = 0
    private var showTmdbId: Int? = null
    private var showTitle: String? = ""

    private var shouldRefreshOverviewData = false
    private var shouldRefreshSeasonData = false
    private var shouldRefreshProgressData = false

    private lateinit var showActionButtonsFragment: ShowDetailsActionButtonsFragment
    private lateinit var showDetailsOverviewFragment: ShowDetailsOverviewFragment
    private lateinit var showDetailsSeasonsFragment: ShowDetailsSeasonsFragment
    private lateinit var showDetailsProgressFragment: ShowDetailsProgressFragment

    private val viewModel: ShowDetailsViewModel by viewModels()

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var gson: Gson

    private var selectedTab: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowDetailsBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        appBarLayout = bindings.showdetailsactivityAppbarlayout
        collapsingToolbarLayout = bindings.showdetailsactivityCollapsingToolbarLayout

        // Init SwipeRefreshLayout
        swipeRefeshLayout = bindings.showdetailsactivitySwipeRefreshLayout
        swipeRefeshLayout.setOnRefreshListener(this)

        // Init TablLayout
        tabLayout = bindings.showdetailsactivityInner.showdetailsactivityTablayout
        tabLayout.addOnTabSelectedListener(this)

        // If activity is destroyed, restore selected tab.
        if (savedInstanceState?.containsKey(SELECT_TAB_POS) == true) {
            selectedTab = savedInstanceState.getInt(SELECT_TAB_POS)
        }

        // Action bar setup
        setSupportActionBar(bindings.showdetailsactivityToolbar)
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

    override fun onSaveInstanceState(outState: Bundle) {
        // Persist tab position in case activity gets destroy e.g device rotation
        outState.putInt(SELECT_TAB_POS, tabLayout.selectedTabPosition)
        super.onSaveInstanceState(outState)
    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when (showResource) {
                    is Resource.Loading -> {
                        bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility =
                            View.GONE

                        toggleProgressBar(true)
                        Log.d(TAG, "collectShow: Show loading")
                    }
                    is Resource.Success -> {
                        bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility =
                            View.VISIBLE

                        if(show != null) {
                            toggleProgressBar(false)
                            displayShowInformation(show)

                            handleExternalLinks(show.external_ids)
                            handleTrailer(show.videos)
                        }
                    }
                    is Resource.Error -> {
                        toggleProgressBar(false)

                        // If show in cache, display it cache
                        if (show != null) {
                            bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility =
                                View.VISIBLE

                            displayShowInformation(show)
                            handleExternalLinks(show.external_ids!!)
                            handleTrailer(show.videos)
                        }

                        showErrorSnackbarRetryButton(showResource.error, bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun initFragments() {
        showActionButtonsFragment = ShowDetailsActionButtonsFragment.newInstance()
        showDetailsOverviewFragment = ShowDetailsOverviewFragment.newInstance()
        showDetailsSeasonsFragment = ShowDetailsSeasonsFragment.newInstance()
        showDetailsProgressFragment = ShowDetailsProgressFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(
                bindings.showdetailsactivityInner.showdetailsactivityButtonsFragmentContainer.id,
                showActionButtonsFragment,
                FRAGMENT_ACTION_BUTTONS
            )
            .commit()

        // If device is rotated, user will see the tab that was selected, otherwise show Overview tab.
        if (selectedTab != null) {
            tabLayout.selectTab(tabLayout.getTabAt(selectedTab!!))
        } else if(supportFragmentManager.findFragmentByTag(FRAGMENT_OVERVIEW_TAG) == null) {
            // Fragment not added yet, so add it
            supportFragmentManager.beginTransaction()
                .replace(
                    bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                    showDetailsOverviewFragment,
                    FRAGMENT_OVERVIEW_TAG
                )
                .commit()
        } else {
            // Replace Fragment with Overview Fragment
            supportFragmentManager.beginTransaction()
                .replace(
                    bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                    showDetailsOverviewFragment,
                    FRAGMENT_OVERVIEW_TAG
                )
                .commit()
        }

    }

    private fun toggleProgressBar(isRefreshing: Boolean) {
        val progressBar = bindings.showdetailsactivityInner.showdetailsactivityProgressbar
        val actionButtonsProgressBar = showActionButtonsFragment.view?.findViewById<ProgressBar>(R.id.actionbutton_loading_progress)
        val overviewFragmentProgressBar = showDetailsOverviewFragment.view?.findViewById<ProgressBar>(R.id.showdetailsoverview_progressbar)

        if (swipeRefeshLayout.isRefreshing) {
            swipeRefeshLayout.isRefreshing = false
        }

        if (isRefreshing) {
            progressBar.visibility = View.VISIBLE
            actionButtonsProgressBar?.visibility = View.VISIBLE
            overviewFragmentProgressBar?.visibility = View.VISIBLE
        }
        else {
            progressBar.visibility = View.GONE
            actionButtonsProgressBar?.visibility = View.GONE
            overviewFragmentProgressBar?.visibility = View.GONE
        }

    }

    private fun displayShowInformation(tmShow: TmShow?) {
        if(tmShow == null) {
            return
        }

        displayToolbarTitle(tmShow.name)

        if (tmShow.backdrop_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + tmShow.backdrop_path)
                .into(bindings.showdetailsactivityBackdrop)
        }

        bindings.showdetailsactivityInner.apply {
            if (tmShow.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + tmShow.poster_path)
                    .into(showdetailsactivityPoster)
            }

            showdetailsactivityTitle.text = tmShow.name

            if(tmShow.status != null) {
                showdetailsactivityStatus.visibility = View.VISIBLE

                showdetailsactivityStatus.text = "Status: ${tmShow.status}"
            } else {
                showdetailsactivityStatus.visibility = View.GONE

            }

            if (tmShow.first_aired != null) {
                showdetailsactivityFirstAired.visibility = View.VISIBLE

                showdetailsactivityFirstAired.text = "Premiered: " + DateFormatUtils.format(
                    tmShow.first_aired,
                    "dd MMM YYYY"
                )
            } else {
                showdetailsactivityFirstAired.visibility = View.GONE

            }

            if(tmShow.networks.isNotEmpty()) {
                bindings.showdetailsactivityInner.showdetailsactivityCompany.visibility = View.VISIBLE

                displayNetworks(tmShow.networks) { networkClickCallback ->
                }
            } else {
                bindings.showdetailsactivityInner.showdetailsactivityCompany.visibility = View.GONE
            }

            if(tmShow.country.isNotEmpty()) {
                showdetailsactivityCountry.visibility = View.VISIBLE

                displayCountries(tmShow.country) { countryClickCallback ->

                }
            } else {
                showdetailsactivityCountry.visibility = View.GONE
            }


            if (tmShow.runtime != null) {
                showdetailsactivityRuntime.visibility = View.VISIBLE

                showdetailsactivityRuntime.text = "Runtime: ${tmShow.runtime} Minutes"
            } else {
                showdetailsactivityRuntime.visibility = View.GONE
            }

            if(tmShow.genres.isNotEmpty()) {
                showdetailsactivityGenres.visibility = View.VISIBLE

                displayGenres(tmShow.genres) { genresClickCallback ->

                }
            } else {
                showdetailsactivityGenres.visibility = View.GONE
            }

            if(tmShow.created_by.isNotEmpty()) {
                showdetailsactivityDirected.visibility = View.VISIBLE

                displayDirectors(tmShow.created_by) { directorClickedCallback ->

                }

            } else {
                showdetailsactivityDirected.visibility = View.GONE

            }

            if(tmShow.trakt_rating != 0.0) {
                showdetailsactivityTraktRating.visibility = View.VISIBLE

                showdetailsactivityTraktRating.text = "Trakt user rating ${String.format("%.1f", tmShow.trakt_rating)}"
            } else {
                showdetailsactivityTraktRating.visibility = View.GONE

            }

            showdetailsactivityNumEpisodes.text = "Total episodes: ${tmShow.num_episodes}"
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
        viewModel.onRefresh()
    }

    private fun handleTrailer(videos: Videos?) {
        if (videos != null && videos.results?.isNotEmpty() == true) {
            val trailerButton = bindings.showdetailsactivityInner.showdetailsactivityTrailer
            trailerButton.visibility = View.VISIBLE

            trailerButton.setOnClickListener {
                VideoTrailerHelper.watchVideoTrailer(this, videos)
            }
        }
    }

    private fun handleExternalLinks(externalIds: TvExternalIds?) {
        val imdbExternalId = externalIds?.imdb_id

        if (imdbExternalId != null) {
            val imdbButton = bindings.showdetailsactivityInner.showdetailsactivityImdbButton
            imdbButton.visibility = View.VISIBLE

            imdbButton.setOnClickListener {
                ImdbNavigationHelper.navigateToImdb(this, imdbExternalId)
            }

        } else {
            bindings.showdetailsactivityInner.showdetailsactivityImdbButton.visibility = View.GONE
        }
    }

    private fun displayDirectors(
        people: List<Person?>,
        callback: (person: Person?) -> Unit
    ) {
        val layout = bindings.showdetailsactivityInner.showdetailsactivityDirected

        // If user refreshes or rotates screen, ensure to clear existing views
        layout.removeAllViews()

        Log.d(TAG, "getDirectorsViewList: Got ${people.size} people!")

        layout.addView(setTextViewLayoutConfig(getTextView(layout.context, "Directed by: ")))

        // List to store a TextView for each director
        val textViews: MutableList<TextView> = mutableListOf()

        people.map { director ->

            // Check if director is last entry, in this case no trailing comma ','
            val textView: TextView = if (director != people.last()) {
                setTextViewLayoutConfig(getTextView(layout.context, director?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, director?.name ?: ""))
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

    private fun displayNetworks(
        networks: List<Network?>,
        callback: (company: Network?) -> Unit
    ) {
        val layout = bindings.showdetailsactivityInner.showdetailsactivityCompany

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
                setTextViewLayoutConfig(getTextView(layout.context, network?.name + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, network?.name ?: ""))
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

    private fun displayGenres(genres: List<Genre?>, callback: (genre: Genre?) -> Unit) {
        val layout = bindings.showdetailsactivityInner.showdetailsactivityGenres

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

    private fun displayCountries(countries: List<String?>, callback: (country: String?) -> Unit) {
        val layout = bindings.showdetailsactivityInner.showdetailsactivityCountry

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
                setTextViewLayoutConfig(getTextView(layout.context, country + ", "))
            } else {
                setTextViewLayoutConfig(getTextView(layout.context, country ?: ""))
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


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.entitydetailsmenu_refresh -> {
                onRefresh()
            }
        }

        return false
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsOverviewFragment,
                        FRAGMENT_OVERVIEW_TAG
                    )
                    .commit()

                Log.d(TAG, "onTabSelected: Tab Overview selected")
            }
            1 -> {
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsSeasonsFragment,
                        FRAGMENT_SEASONS_TAG
                    )
                    .commit()

                Log.d(TAG, "onTabSelected: Tab Seasons selected")
            }
            2 -> {
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsProgressFragment,
                        FRAGMENT_PROGRESS_TAG
                    )
                    .commit()

                Log.d(TAG, "onTabSelected: Tab Progress selected")
            }
            else -> {

                Log.d(TAG, "onTabSelected: Tab Unknown (${tab?.position}) selected")
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {

    }

    override fun onTabReselected(tab: TabLayout.Tab?) {

    }



    private fun displayToastMessage(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    companion object {
        const val SHOW_DATA_KEY = "show_data_key"

    }
}