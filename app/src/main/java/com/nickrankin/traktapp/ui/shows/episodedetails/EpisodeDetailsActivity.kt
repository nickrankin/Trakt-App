package com.nickrankin.traktapp.ui.shows.episodedetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.EpisodeCreditsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActivity"

private const val FRAGMENT_ACTION_BUTTONS ="action_buttons_fragment"
@AndroidEntryPoint
class EpisodeDetailsActivity : BaseActivity(), OnNavigateToShow, SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: ActivityEpisodeDetailsBinding
    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var watchedEpisodesRecyclerView: RecyclerView
    private lateinit var watchedEpisodesAdapter: EpisodeWatchedHistoryItemAdapter
    
    @Inject
    lateinit var trackedEpisodesAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityEpisodeDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.episodedetailsactivityToolbar.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        swipeRefreshLayout = bindings.episodedetailsactivitySwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        if(intent.hasExtra(TrackedEpisodeNotificationsBuilder.FROM_NOTIFICATION_TAP) && intent.extras?.getBoolean(TrackedEpisodeNotificationsBuilder.FROM_NOTIFICATION_TAP, false) == true) {
            dismissEipsodeNotifications()
        }

        // We need to check if the parent activity has sent ShowDataModel
        if(intent.extras?.containsKey(EPISODE_DATA_KEY) == false || intent!!.extras?.getParcelable<EpisodeDataModel>(
                EPISODE_DATA_KEY
            ) == null) {
            throw RuntimeException("Must pass EpisodeDataModel to ${this.javaClass.name}.")
        }

        initFragments()

        getShow()
        getEpisode()


        getSeasonEpisodes()
    }

    private fun dismissEipsodeNotifications() {
        val episodeTraktId = intent.extras?.getInt(TrackedEpisodeNotificationsBuilder.EPISODE_TRAKT_ID)
        Log.d(TAG, "dismissEipsodeNotifications: Dismissing notifications for episode ${episodeTraktId}", )

        lifecycleScope.launchWhenStarted {
            trackedEpisodesAlarmScheduler.dismissNotification(episodeTraktId ?: 0, true)
        }
    }

    private fun initFragments() {

        if(isLoggedIn) {
            supportFragmentManager.beginTransaction()
                .replace(bindings.episodedetailsactivityActionButtons.id, EpisodeDetailsActionButtonsFragment.newInstance(), FRAGMENT_ACTION_BUTTONS)
                .replace(bindings.episodedetailsactivityCastCrew.id, EpisodeCreditsFragment.newInstance())
                .commit()
        }

    }


    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data

                when (showResource) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        displayShow(show)

                        if(isLoggedIn) {
                            getWatchedEpisodes(show)
                        }
                    }
                    is Resource.Error -> {

                        // Try display cached show if available
                        if(show != null) {
                            displayShow(show)
                        }

                        showErrorSnackbarRetryButton(showResource.error, bindings.episodedetailsactivitySwipeLayout) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getEpisode() {
        val progressBar = bindings.episodedetailsactivityProgressbar
        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episodeResource ->

                val episode = episodeResource.data

                when (episodeResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        if(!swipeRefreshLayout.isRefreshing) {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        supportActionBar?.title = episode?.name ?: "Unknown"

                        displayEpisode(episode)
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                        // Try to display cached episode if available
                        if(episodeResource.data != null) {
                            displayEpisode(episode)
                        }

                        showErrorSnackbarRetryButton(episodeResource.error, bindings.episodedetailsactivitySwipeLayout) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun getSeasonEpisodes() {
        val nextEpisodeButton = bindings.episodedetailsactivityNextEpisode
        val prevEpisodeButton = bindings.episodedetailsactivityPreviousEpisode



        lifecycleScope.launchWhenStarted {
            viewModel.seasonEpisodes.collectLatest { episodeNumberAndseasonEpisodesResource ->
                val currentEpisodeNumber = episodeNumberAndseasonEpisodesResource.first
                val seasonEpisodesResource = episodeNumberAndseasonEpisodesResource.second
                Log.d(TAG, "getSeasonEpisodes: currentEpisodeNumber $currentEpisodeNumber")

                if(currentEpisodeNumber == -1) {
                    Log.e(TAG, "getSeasonEpisodes: Current episode is not correct", )
        
                    prevEpisodeButton.visibility = View.GONE
                    nextEpisodeButton.visibility = View.GONE
                }
                
                when(seasonEpisodesResource) {
                    is Resource.Loading -> {
                        prevEpisodeButton.visibility = View.GONE
                        nextEpisodeButton.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        val seasonEpisodeData = seasonEpisodesResource.data
                        if(currentEpisodeNumber > 1) {
                            prevEpisodeButton.visibility = View.VISIBLE

                            val previousEpisode = seasonEpisodeData?.find { it.episode.episode_number == currentEpisodeNumber -1 }
                            prevEpisodeButton.text = "Episode ${previousEpisode?.episode?.episode_number?.toString() }"

                            prevEpisodeButton.setOnClickListener {
                                viewModel.switchEpisode(previousEpisode?.episode?.episode_number)
                            }
                        } else {
                            prevEpisodeButton.visibility = View.GONE
                        }

                        // We have more Episodes after this Episode
                        if(currentEpisodeNumber < (seasonEpisodeData?.size ?: 0)) {
                            nextEpisodeButton.visibility = View.VISIBLE

                            val nextEpisode = seasonEpisodeData?.find { it.episode.episode_number == currentEpisodeNumber+1 }

                            nextEpisodeButton.text = "Episode ${nextEpisode?.episode?.episode_number}"

                            nextEpisodeButton.setOnClickListener {
                                viewModel.switchEpisode(nextEpisode?.episode?.episode_number)
                            }
                        } else {
                            nextEpisodeButton.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        handleError(seasonEpisodesResource.error, null)
                    }

                }
            }
        }
    }

    private fun getWatchedEpisodes(show: TmShow?) {
        if(show == null) {
            return
        }

        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodeStats.collectLatest { watchedEpisodes ->
                val foundShow = watchedEpisodes.find { it.show_trakt_id == show.trakt_id }
            }
        }
    }

    private fun displayShow(show: TmShow?) {

        if(show == null) {
            return
        }

        bindings.apply {

            if(show.poster_path != null && show.poster_path != "") {
                glide
                    .load(AppConstants.TMDB_POSTER_URL+show.poster_path)
                    .into(episodedetailsactivityPoster)
            }

            episodedetailsactivityShowTitle.text = show.name

            episodedetailsactivityShowTitle.setOnClickListener {
                navigateToShow(show.trakt_id ?: 0, show.tmdb_id, show.name)
            }

            episodedetailsactivityPosterCardview.setOnClickListener {
                navigateToShow(show.trakt_id ?: 0, show.tmdb_id, show.name,)
            }
        }
    }

    private fun displayEpisode(episode: TmEpisode?) {
        if(episode == null) {
            return
        }

        if (episode.still_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                .into(bindings.episodedetailsactivityBackdrop)
        } else {
            bindings.episodedetailsactivityBackdrop.visibility = View.GONE
        }

        bindings.apply {
            episodedetailsactivityEpisodeTitle.text = episode.name
            episodedetailsactivitySeasonNumber.text = "Season: ${episode.season_number}"
            episodedetailsactivityEpisodeNumber.text = "Episode: ${episode.episode_number}"

            episodedetailsactivityOverview.text = episode.overview

            if (episode.air_date != null) {
                episodedetailsactivityReleaseDate.text = "Aired: ${
                    episode.air_date.format(
                        DateTimeFormatter.ofPattern(
                            sharedPreferences.getString(
                                "date_format",
                                AppConstants.DEFAULT_DATE_TIME_FORMAT
                            )
                        )
                    )
                }"
            }

            if(episode.runtime != null) {
                episodedetailsactivityRuntime.text = "Runtime ${ calculateRuntime(episode.runtime) }"
            }

        }
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {
        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsActivity.SHOW_DATA_KEY,
            ShowDataModel(
                traktId, tmdbId, title
            )
        )
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

    companion object {
        const val EPISODE_DATA_KEY = "episode_data_key"
    }
}