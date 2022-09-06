package com.nickrankin.traktapp.ui.shows.episodedetails

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActivity"

private const val FRAGMENT_ACTION_BUTTONS ="action_buttons_fragment"
@AndroidEntryPoint
class EpisodeDetailsActivity : BaseActivity(), OnNavigateToShow, SwipeRefreshLayout.OnRefreshListener {
    private lateinit var bindings: ActivityEpisodeDetailsBinding
    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var watchedEpisodesRecyclerView: RecyclerView
    private lateinit var watchedEpisodesAdapter: EpisodeWatchedHistoryItemAdapter

    private lateinit var episodeDetailsActionButtonsFragment: EpisodeDetailsActionButtonsFragment
    private lateinit var episodeDetailsOverviewFragment: EpisodeDetailsOverviewFragment

    @Inject
    lateinit var trackedEpisodesAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityEpisodeDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.episodedetailsactivityToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressBar = bindings.episodedetailsactivityInner.episodedetailsactivityProgressbar
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
//        getEvents()


    }

    private fun dismissEipsodeNotifications() {
        val episodeTraktId = intent.extras?.getInt(TrackedEpisodeNotificationsBuilder.EPISODE_TRAKT_ID)
        Log.d(TAG, "dismissEipsodeNotifications: Dismissing notifications for episode ${episodeTraktId}", )

        lifecycleScope.launchWhenStarted {
            trackedEpisodesAlarmScheduler.dismissNotification(episodeTraktId ?: 0, true)
        }
    }

    private fun initFragments() {
        episodeDetailsOverviewFragment = EpisodeDetailsOverviewFragment.newInstance()
        episodeDetailsActionButtonsFragment = EpisodeDetailsActionButtonsFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(bindings.episodedetailsactivityInner.episodedetailsactivityMainFragmentContainer.id, episodeDetailsOverviewFragment)
            .commit()

        if(isLoggedIn) {
            supportFragmentManager.beginTransaction()
                .add(bindings.episodedetailsactivityInner.episodedetailsactivityButtonsFragmentContainer.id, episodeDetailsActionButtonsFragment, FRAGMENT_ACTION_BUTTONS)
                .commit()
        }


    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data

                when (showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "collectShow: Loading show..")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "collectShow: Got show! ${showResource.data}")
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
        lifecycleScope.launchWhenStarted {
            // Contains all UI elements for the Episode Details View
            val mainGroup = bindings.episodedetailsactivityInner.episodedetailsactivityMainGroup
            mainGroup.visibility = View.VISIBLE

            viewModel.episode.collectLatest { episodeResource ->

                val episode = episodeResource.data

                when (episodeResource) {
                    is Resource.Loading -> {
                        toggleProgressBar(true)
                        Log.d(TAG, "collectEpisode: Loading Episode...")
                    }
                    is Resource.Success -> {
                        toggleProgressBar(false)

                        mainGroup.visibility = View.VISIBLE
                        bindings.wpisodedetailsactivityCollapsingToolbarLayout.title = episode?.name ?: "Unknown"

                        displayEpisode(episode)

                    }
                    is Resource.Error -> {
                        toggleProgressBar(false)

                        // Try to display cached episode if available
                        if(episodeResource.data != null) {
                            mainGroup.visibility = View.VISIBLE

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

    private fun toggleProgressBar(isRefreshing: Boolean) {
        val actionButtonProgressbar = episodeDetailsActionButtonsFragment.view?.findViewById<ProgressBar>(R.id.actionbutton_loading_progress)
        val episodeDetailsOverViewProgressBar = episodeDetailsOverviewFragment.view?.findViewById<ProgressBar>(R.id.episodedetailsoverview_progressbar)

            if(swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }

            if(isRefreshing) {
                progressBar.visibility = View.VISIBLE
                actionButtonProgressbar?.visibility = View.VISIBLE
                episodeDetailsOverViewProgressBar?.visibility = View.VISIBLE
            } else {
                 progressBar.visibility = View.GONE
                actionButtonProgressbar?.visibility = View.GONE
                episodeDetailsOverViewProgressBar?.visibility = View.GONE
            }
    }

    private fun getWatchedEpisodes(show: TmShow?) {
        if(show == null) {
            return
        }

        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodeStats.collectLatest { watchedEpisodes ->
                val foundShow = watchedEpisodes.find { it.show_trakt_id == show.trakt_id }

                if(foundShow?.last_watched_at != null) {
                    bindings.episodedetailsactivityInner.episodedetailsactivityTotalPlays.visibility = View.VISIBLE
                    bindings.episodedetailsactivityInner.episodedetailsactivityTotalPlays.text = "Total plays: ${foundShow.plays} \nLast watched at: ${getFormattedDate(foundShow.last_watched_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)!!, sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT)!!)}"
                } else {
                    bindings.episodedetailsactivityInner.episodedetailsactivityTotalPlays.visibility = View.GONE
                }
            }
        }
    }

//    private fun getEvents() {
//        lifecycleScope.launchWhenStarted {
//            viewModel.events.collectLatest { event ->
//                when(event) {
//                    is EpisodeDetailsViewModel.Event.DeleteWatchedHistoryItem -> {
//                        val eventResource = event.syncResponse
//
//                        if(eventResource is Resource.Success) {
//                            val syncResponse = eventResource.data
//
//                            if(syncResponse?.deleted?.episodes ?: 0 > 0) {
//                                displayMessageToast("Successfully removed play", Toast.LENGTH_SHORT)
//                            } else {
//                                displayMessageToast("Didn't remove play", Toast.LENGTH_SHORT)
//                            }
//
//                        } else if (eventResource is Resource.Error) {
//                            showErrorMessageToast(event.syncResponse.error, "Error deleting watched history item")
//                        }
//                    }
//                    is EpisodeDetailsViewModel.Event.AddCheckinEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.AddListEntryEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.AddRatingsEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.AddToWatchedHistoryEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.CancelCheckinEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.DeleteRatingsEvent -> TODO()
//                    is EpisodeDetailsViewModel.Event.RemoveListEntryEvent -> TODO()
//                }
//            }
//        }
//    }

    private fun displayShow(show: TmShow?) {
        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityShowTitle.text = show?.name ?: "Unknown"

            if(show?.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + show.poster_path)
                    .into(episodedetailsactivityPoster)
            } else {
                episodedetailsactivityPoster.visibility = View.GONE
            }

            episodedetailsactivityShowTitle.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id, show?.name)
            }

            episodedetailsactivityPoster.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id, show?.name,)
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
        }

        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityTitle.text = episode.name

            episodedetailsactivityShowSeasonEpisode.text = "Season ${episode.season_number} Episode ${episode.episode_number}"

            if(episode.runtime != null) {
                episodedetailsactivityRuntime.text = "Runtime: ${calculateRuntime(episode.runtime)}"
            }


            if (episode.air_date != null) {
                bindings.episodedetailsactivityInner.episodedetailsactivityFirstAired.visibility = View.VISIBLE

                episodedetailsactivityFirstAired.text = "Aired: " + DateFormatUtils.format(
                    episode.air_date,
                    sharedPreferences.getString(
                        "date_format",
                        AppConstants.DEFAULT_DATE_TIME_FORMAT
                    )
                )
            }

            if(episode.trakt_rating != 0.0) {
                episodedetailsactivityTraktRating.visibility = View.VISIBLE

                episodedetailsactivityTraktRating.text = "Trakt rating: ${String.format("%.1f", episode?.trakt_rating)}"
            } else {
                episodedetailsactivityTraktRating.visibility = View.GONE
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

//    private fun handleWatchedEpisodeDelete(watchedEpisode: WatchedEpisode, showConfirmation: Boolean) {
//        val syncItem = SyncItems().ids(watchedEpisode.id)
//
//        if(!showConfirmation) {
//            viewModel.removeWatchedHistoryItem(syncItem)
//            return
//        }
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Delete ${watchedEpisode.episode_title} from your watched history?")
//            .setMessage("Are you sure you want to remove ${watchedEpisode.episode_title} from your Trakt History?")
//            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
//                viewModel.removeWatchedHistoryItem(syncItem)
//            })
//            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
//                dialogInterface.dismiss()
//            })
//            .create()
//
//        dialog.show()
//    }



    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
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

    companion object {
        const val EPISODE_DATA_KEY = "episode_data_key"
    }
}