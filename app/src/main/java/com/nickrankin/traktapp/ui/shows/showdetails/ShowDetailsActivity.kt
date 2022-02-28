package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityShowDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToEpisode
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import retrofit2.HttpException
import java.math.RoundingMode
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
class ShowDetailsActivity : AppCompatActivity(), OnNavigateToEpisode,
    SwipeRefreshLayout.OnRefreshListener, TabLayout.OnTabSelectedListener {
    private lateinit var bindings: ActivityShowDetailsBinding

    private lateinit var swipeRefeshLayout: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout

    private var isLoggedIn: Boolean = false

    private var showTraktId = 0
    private var showTmdbId: Int? = null
    private var showTitle: String? = ""

    private var shouldRefreshOverviewData = false
    private var shouldRefreshSeasonData = false
    private var shouldRefreshProgressData = false
    
    private val viewModel: ShowDetailsViewModel by viewModels()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowDetailsBinding.inflate(layoutInflater)

        // Add Overview and Button fragments on first load
        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                .add(
                    bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                    ShowDetailsOverviewFragment.newInstance(),
                    FRAGMENT_OVERVIEW_TAG
                )
                .add(
                    bindings.showdetailsactivityInner.showdetailsactivityButtonsFragmentContainer.id,
                    ShowDetailsActionButtonsFragment.newInstance(),
                    FRAGMENT_ACTION_BUTTONS
                )
                .commit()
        }

        setContentView(bindings.root)

        // Init SwipeRefreshLayout
        swipeRefeshLayout = bindings.showdetailsactivitySwipeRefreshLayout
        swipeRefeshLayout.setOnRefreshListener(this)

        // Init TablLayout
        tabLayout = bindings.showdetailsactivityInner.showdetailsactivityTablayout
        tabLayout.addOnTabSelectedListener(this)

        // If activity is destroyed, restore selected tab
        if (savedInstanceState?.containsKey(SELECT_TAB_POS) == true) {
            tabLayout.selectTab(tabLayout.getTabAt(savedInstanceState.getInt(SELECT_TAB_POS)))
        }

        // Action bar setup
        setSupportActionBar(bindings.showdetailsactivityToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Init variable
        showTraktId = intent.getIntExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, 0)
        showTmdbId = intent.getIntExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, 0)
        showTitle = intent.getStringExtra(ShowDetailsRepository.SHOW_TITLE_KEY)
        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        Log.d(TAG, "onCreate: Got show $showTitle with TraktId $showTraktId TmdbId $showTmdbId")

        // Get data
        getShow()
        getTraktRatings()
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
                        bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility = View.GONE
                        bindings.showdetailsactivityInner.showdetailsactivityErrorGroup.visibility = View.GONE

                        toggleProgressBar(true)
                        Log.d(TAG, "collectShow: Show loading")
                    }
                    is Resource.Success -> {
                        bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility = View.VISIBLE
                        bindings.showdetailsactivityInner.showdetailsactivityErrorGroup.visibility = View.GONE

                        toggleProgressBar(false)
                        displayShowInformation(show)

                    }
                    is Resource.Error -> {
                        toggleProgressBar(false)

                        // If show in cache, display it cache
                        if(show != null) {
                            bindings.showdetailsactivityInner.showdetailsactivityMainGroup.visibility = View.VISIBLE

                            displayShowInformation(show)
                        } else {
                            bindings.showdetailsactivityInner.showdetailsactivityErrorGroup.visibility = View.VISIBLE

                           handleError(showResource.error)

                        }

                        displayToastMessage(
                            "Error getting Show Details. ${showResource.error?.localizedMessage}",
                            Toast.LENGTH_LONG
                        )
                        Log.e(
                            TAG,
                            "collectShow: Couldn't get the show. ${showResource.error?.localizedMessage}",
                        )
                        showResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun toggleProgressBar(isRefreshing: Boolean) {
        val progressBar = bindings.showdetailsactivityInner.showdetailsactivityProgressbar

        if (swipeRefeshLayout.isRefreshing) {
            swipeRefeshLayout.isRefreshing = false
        }

        if(isRefreshing) progressBar.visibility = View.VISIBLE else progressBar.visibility = View.GONE

    }

    private fun handleError(exception: Throwable?) {
        val errorTextView = bindings.showdetailsactivityInner.showdetailsactivityErrorText
        val retryButton = bindings.showdetailsactivityInner.showdetailsactivityRetryButton

        if(exception is HttpException) {
            errorTextView.text = "A HTTP Error has occurred getting the show. (Error code: HTTP: ${exception.code()}). ${exception.localizedMessage}. Please check your internet connection"
        } else {
            errorTextView.text = "A Error has occurred getting the show. ${exception?.localizedMessage}. Please check your internet connection"
        }

        retryButton.setOnClickListener {
            onStart()

            // Refresh the current tab. Selecting current tab will reselect and trigger refresh
            refreshCurrentTab()
        }
    }

    private fun getTraktRatings() {
        viewModel.state.getLiveData<Double>("trakt_ratings").observe(this) { rating ->
            bindings.showdetailsactivityInner.showdetailsactivityTraktRating.visibility =
                View.VISIBLE
            bindings.showdetailsactivityInner.showdetailsactivityTraktRating.text =
                "Trakt user rating: ${rating.toBigDecimal().setScale(1, RoundingMode.UP)}/10"
        }
    }

    private fun displayShowInformation(tmShow: TmShow?) {
        bindings.showdetailsactivityCollapsingToolbarLayout.title = tmShow?.name

        if (tmShow?.backdrop_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + tmShow.backdrop_path)
                .into(bindings.showdetailsactivityBackdrop)
        }

        bindings.showdetailsactivityInner.apply {
            if (tmShow?.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + tmShow.poster_path)
                    .into(showdetailsactivityPoster)
            }

            showdetailsactivityTitle.text = "${tmShow?.name}"

            showdetailsactivityStatus.text = "${tmShow?.status ?: "Unknown"}"

            if (tmShow?.first_aired != null) {
                showdetailsactivityFirstAired.text = "Premiered: " + DateFormatUtils.format(
                    tmShow?.first_aired,
                    "dd MMM YYYY"
                )
            }

            if (tmShow?.networks?.isNotEmpty() == true) {
                if (tmShow.networks.size > 1) {
                    var networksString = "Networks: "

                    tmShow.networks.map { network ->
                        networksString += network?.name
                        if (network != tmShow.networks.last()) {
                            networksString += ", "
                        }
                    }

                    showdetailsactivityNetwork.text = networksString
                } else {
                    showdetailsactivityNetwork.text = "Network: ${tmShow.networks.first()?.name}"
                }
            }

            if (tmShow?.country?.isNotEmpty() == true) {
                showdetailsactivityCountry.text = "Countery: ${tmShow.country[0]}"
            }

            if (tmShow?.runtime != null) {
                showdetailsactivityRuntime.text = "Runtime: ${tmShow.runtime} Minutes"
            }

            if (tmShow?.genres?.isNotEmpty() == true) {
                showdetailsactivityGenres.visibility = View.VISIBLE

                var networksString = "Genres: "

                tmShow.genres.map { genre ->
                    networksString += genre?.name
                    if (genre != tmShow.genres.last()) {
                        networksString += ", "
                    }
                }

                showdetailsactivityGenres.text = networksString
            }

            if (tmShow?.created_by?.isNotEmpty() == true) {
                // Directed by names
                if (tmShow.created_by.isNotEmpty()) {
                    bindings.showdetailsactivityInner.showdetailsactivityDirected.visibility =
                        View.VISIBLE

                    // More than one creator/director
                    if (tmShow.created_by.size > 1) {
                        var directedByText = "Creators: "

                        tmShow.created_by.map { director ->
                            directedByText += director?.name
                            if (director != tmShow.created_by.last()) {
                                directedByText += ", "
                            }
                        }
                        bindings.showdetailsactivityInner.showdetailsactivityDirected.text =
                            directedByText
                    } else {
                        bindings.showdetailsactivityInner.showdetailsactivityDirected.text =
                            "Creator: ${tmShow.created_by.first()?.name}"
                    }

                }
            }

            showdetailsactivityTotalEpisodes.text = "Total episodes: ${tmShow?.num_episodes}"
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
        intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, language)

        // No need to force refresh of watched shows as this was done in this activity so assume the watched show data in cache is up to date
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, false)

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

        refreshActionButtons()

        val currentTab = tabLayout.selectedTabPosition

        // Refresh tabs on subsequent clicks
        shouldRefreshOverviewData = currentTab != 0
        shouldRefreshProgressData = currentTab != 2
        shouldRefreshSeasonData = currentTab != 1

        // Refresh the current tab. Selecting current tab will reselect and trigger refresh
        refreshCurrentTab()
        Log.d(TAG, "onRefresh: Selected tab: currentTab")
    }

    private fun refreshActionButtons() {
        Log.d(TAG, "refreshActionButtons: Refreshing Action buttons")
        try {

            val actionButtonSwipeLayout = supportFragmentManager.findFragmentByTag(
                FRAGMENT_ACTION_BUTTONS) as SwipeRefreshLayout.OnRefreshListener

            actionButtonSwipeLayout.onRefresh()

        } catch(e: ClassCastException) {
            Log.e(TAG, "refreshActionButtons: Couldn't Cast ${supportFragmentManager.findFragmentByTag(
                FRAGMENT_ACTION_BUTTONS)} to SwipeRefreshLayout.OnRefreshListener", )
            e.printStackTrace()
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.entitydetailsmenu_refresh -> {
                onRefresh()
            }
        }

        return false
    }

    private fun refreshCurrentTab() {
            // Refresh the current tab. Selecting current tab will reselect and trigger refresh
            val currentTab = tabLayout.selectedTabPosition
            tabLayout.selectTab(tabLayout.getTabAt(currentTab))
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
                val showDetailsOverviewFragment = ShowDetailsOverviewFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsOverviewFragment,
                        FRAGMENT_OVERVIEW_TAG
                    )
                    .commit()

                if (shouldRefreshOverviewData) {
                    Log.d(TAG, "onTabSelected: Forcing Refresh of Overview tab")

                    // without this, Dagger will fail. Make sure Fragment is fully attached before executing onRefresh()
                    supportFragmentManager.executePendingTransactions()


                    showDetailsOverviewFragment.onRefresh()

                    shouldRefreshOverviewData = false
                }

                Log.d(TAG, "onTabSelected: Tab Overview selected")
            }
            1 -> {
                val showDetailsSeasonsFragment = ShowDetailsSeasonsFragment.newInstance()

                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsSeasonsFragment,
                        FRAGMENT_SEASONS_TAG
                    )
                    .commit()

                if (shouldRefreshSeasonData) {
                    Log.d(TAG, "onTabSelected: Forcing Refresh of Season tab")

                    // without this, Dagger will fail. Make sure Fragment is fully attached before executing onRefresh()
                    supportFragmentManager.executePendingTransactions()

                    showDetailsSeasonsFragment.onRefresh()

                    shouldRefreshSeasonData = false

                }
                Log.d(TAG, "onTabSelected: Tab Seasons selected")
            }
            2 -> {
                val showDetailsProgressFragment = ShowDetailsProgressFragment.newInstance()

                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        showDetailsProgressFragment,
                        FRAGMENT_PROGRESS_TAG
                    )
                    .commit()

                if (shouldRefreshProgressData) {
                    Log.d(TAG, "onTabSelected: Forcing Refresh of Progress tab")

                    // without this, Dagger will fail. Make sure Fragment is fully attached before executing onRefresh()
                    supportFragmentManager.executePendingTransactions()

                    showDetailsProgressFragment.onRefresh()
                    shouldRefreshProgressData = false

                }
                Log.d(TAG, "onTabSelected: Tab Progress selected")
            }
            else -> {
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.showdetailsactivityInner.showdetailsactivityFragmentContainer.id,
                        ShowDetailsOverviewFragment.newInstance(
                        ),
                        FRAGMENT_OVERVIEW_TAG
                    )
                    .commit()

                Log.d(TAG, "onTabSelected: Tab Unknown (${tab?.position}) selected")
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {

                Log.d(TAG, "onTabSelected: Tab Overview unselected")
            }
            1 -> {
                Log.d(TAG, "onTabSelected: Tab Seasons unselected")
            }
            2 -> {
                Log.d(TAG, "onTabSelected: Tab Progress unselected")
            }
            else -> {
                Log.d(TAG, "onTabSelected: Tab Unknown (${tab?.position}) unselected")
            }
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
                refreshFragment(FRAGMENT_OVERVIEW_TAG)

                Log.d(TAG, "onTabSelected: Tab Overview reselected")
            }
            1 -> {
                refreshFragment(FRAGMENT_SEASONS_TAG)
                Log.d(TAG, "onTabSelected: Tab Seasons reselected")
            }
            2 -> {
                refreshFragment(FRAGMENT_PROGRESS_TAG)
                Log.d(TAG, "onTabSelected: Tab Progress reselected")
            }
            else -> {
                Log.d(TAG, "onTabSelected: Tab Unknown (${tab?.position}) reselected")
            }
        }
    }

    private fun refreshFragment(fragmentTag: String): Boolean {
        return try {
            Log.d(TAG, "refreshFragment: Refreshing Fragment $fragmentTag")
            val fragment =
                supportFragmentManager.findFragmentByTag(fragmentTag) as SwipeRefreshLayout.OnRefreshListener

            fragment.onRefresh()
            true
        } catch (cce: ClassCastException) {
            Log.e(TAG, "refreshFragment: Casting error $fragmentTag")
            cce.printStackTrace()
            false
        } catch (e: Exception) {
            Log.e(TAG, "refreshFragment: Exception occur refreshing Fragment")
            e.printStackTrace()
            false
        }
    }

    private fun displayToastMessage(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    companion object {

    }
}