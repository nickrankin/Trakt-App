package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.credits.CastCreditsAdapter
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.trakt5.entities.EpisodeCheckin
import com.uwetrottmann.trakt5.entities.EpisodeIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.enums.Rating
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActivity"

@AndroidEntryPoint
class EpisodeDetailsActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityEpisodeDetailsBinding
    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: CastCreditsAdapter

    private lateinit var watchedEpisodesRecyclerView: RecyclerView
    private lateinit var watchedEpisodesAdapter: EpisodeWatchedHistoryItemAdapter

    private var isLoggedIn = false

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityEpisodeDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.episodedetailsactivityToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        initRecycler()

        collectRatings()

        lifecycleScope.launchWhenStarted {
            launch { collectShow() }
            launch { collectEpisode() }
            launch { collectEvents() }
        }
    }


    private suspend fun collectShow() {
        viewModel.show.collectLatest { showResource ->
            when (showResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectShow: Loading show..")
                }
                is Resource.Success -> {

                    Log.d(TAG, "collectShow: Got show! ${showResource.data}")

                    displayShow(showResource.data)
                }
                is Resource.Error -> {
                    Log.e(
                        TAG,
                        "collectShow: Error getting show. ${showResource.error?.localizedMessage}"
                    )
                    showResource.error?.printStackTrace()
                }
            }
        }
    }

    private suspend fun collectEpisode() {
        viewModel.episode.collectLatest { episodeResource ->
            when (episodeResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectEpisode: Loading Episode...")
                }
                is Resource.Success -> {
                    val episode = episodeResource.data

                    bindings.wpisodedetailsactivityCollapsingToolbarLayout.title = episode?.name ?: "Unknown"

                    displayEpisode(episode)

                    lifecycleScope.launch {
                        launch { collectedWatchedEpisodes(episode?.tmdb_id ?: 0) }

                    }

                    if (isLoggedIn) {
                        setupActionButtons(episode)

                    }
                }
                is Resource.Error -> {
                    Log.e(
                        TAG,
                        "collectEpisode: Error getting episode. ${episodeResource.error?.localizedMessage}"
                    )
                    episodeResource.error?.printStackTrace()
                }
            }
        }
    }

    private  suspend fun collectedWatchedEpisodes(episodeId: Int) {
        viewModel.watchedEpisodes.collectLatest { watchedEpisodesResource ->
            if(watchedEpisodesResource is Resource.Success) {

                val playedEpisodes = watchedEpisodesResource.data?.filter {
                    it.episode_tmdb_id == episodeId
                }?.sortedBy { it.watched_at }?.reversed()

                bindings.episodedetailsactivityInner.episodedetailsactivityTotalPlays.text = "Plays: ${if(playedEpisodes?.size?: 0 > 0) playedEpisodes?.size else "Not watched"}"

                if(playedEpisodes != null && playedEpisodes.isNotEmpty()) {
                    bindings.episodedetailsactivityInner.episodedetailsactivityWatchedTitle.visibility = View.VISIBLE
                    bindings.episodedetailsactivityInner.episodedetailsactivityWatchedRecyclerview.visibility = View.VISIBLE

                    watchedEpisodesAdapter.submitList(playedEpisodes)
                }
                //Log.e(TAG, "collectedWatchedEpisodes: Watched at ${filtered?.first()}", )
            } else if (watchedEpisodesResource is Resource.Error) {
                Log.e(TAG, "collectedWatchedEpisodes: Error getting watched episodes ${watchedEpisodesResource.error?.localizedMessage}", )
            }
        }
    }

    private suspend fun collectEvents() {
        viewModel.events.collectLatest { event ->
            when(event) {
                is EpisodeDetailsViewModel.Event.AddRatingsEvent -> {
                    when(event.syncResponse) {
                        is Resource.Success -> {
                            if (event.syncResponse.data?.added?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully rated Episode!", Toast.LENGTH_LONG)
                            }
                        }
                        is Resource.Error -> {
                            displayMessageToast(
                                "Error rating Episode! Error ${event.syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )
                        }
                    }
                }
                is EpisodeDetailsViewModel.Event.AddCheckinEvent -> {
                    when (event.checkinResponse) {
                        is Resource.Success -> {
                            val episodeName = event.episodeName

                            if (event.checkinResponse.data != null) {

                                displayMessageToast(
                                    "You are watching ${episodeName ?: "Unknown Episode"}",
                                    Toast.LENGTH_LONG
                                )
                            } else {
                                displayAlertDialog(
                                    "Active Checkins found",
                                    "You are currently watching something else on Trakt. Delete all active checkins now and checkin to episode ${episodeName}?",
                                    { dialogInterface, i ->
                                        lifecycleScope.launch {
                                            val checkinsCleared = clearActiveCheckins()

                                            if (checkinsCleared) {
                                                viewModel.checkin(episodeName, event.episodeCheckin)
                                            }
                                        }
                                    },
                                    { dialogInterface, i ->
                                        dialogInterface.dismiss()
                                    }
                                )
                            }
                        }
                        is Resource.Error -> {
                            event.checkinResponse.error?.printStackTrace()
                            displayMessageToast(
                                "Error checking in to episode. Error: ${event.checkinResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )

                        }
                    }
                }
                is EpisodeDetailsViewModel.Event.DeleteWatchedEpisodeEvent -> {
                    val syncResponseResource = event.syncResponse

                    if(syncResponseResource is Resource.Success) {
                        if(syncResponseResource.data?.deleted?.episodes ?: 0 > 0) {
                            displayMessageToast("Successfully deleted watched episode from your Trakt library!", Toast.LENGTH_LONG)
                        } else {
                            displayMessageToast("Could not delete watched Episode. Please try again later.", Toast.LENGTH_LONG)
                        }
                    } else if(syncResponseResource is Resource.Error) {
                        syncResponseResource.error?.printStackTrace()
                        displayMessageToast("Error removing watched episode. ${syncResponseResource.error?.localizedMessage}", Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun setupActionButtons(episode: TmEpisode?) {
        bindings.episodedetailsactivityInner.episodedetailsactivityActionButtons.actionbuttonToolbar.visibility =
            View.VISIBLE

        // Hide collect + Tracking button
        bindings.episodedetailsactivityInner.episodedetailsactivityCollectedButton.collectedbuttonCardview.visibility =
            View.GONE
        bindings.episodedetailsactivityInner.episodedetailsactivityActionButtons.actionbuttonTrack.visibility =
            View.GONE

        val ratingsButton =
            bindings.episodedetailsactivityInner.episodedetailsactivityActionButtons.actionbuttonRate

        ratingsButton.setOnClickListener {
            addRatings(episode)
        }


        setupCheckin(episode)

    }

    private fun addRatings(episode: TmEpisode?) {

        RatingPickerFragment({ newRating ->
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode().rating(
                        Rating.fromValue(newRating)
                    )
                        .id(EpisodeIds.tmdb(episode?.tmdb_id ?: 0))
                )
            }

            viewModel.addRatings(syncItems)


        }, "${episode?.name ?: "Unknown Episode"}")
            .show(supportFragmentManager, "Ratings dialog")
    }

    private fun collectRatings() {
        val ratingTextView =
            bindings.episodedetailsactivityInner.episodedetailsactivityActionButtons.actionbuttonRateText

        ratingTextView.text = " - "
        viewModel.ratings.observe(this, {rating ->
            if (rating != -1) {
                ratingTextView.text = rating?.toString()
            } else {
                ratingTextView.text = " - "
            }

        })
    }

    private fun displayShow(show: TmShow?) {
        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityShowTitle.text = show?.name ?: "Unknown"

            if(show?.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + show.poster_path)
                    .into(episodedetailsactivityShowPoster)
            } else {
                episodedetailsactivityShowPoster.visibility = View.GONE
            }

            episodedetailsactivityShowTitle.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id ?: 0, show?.languages?.first() ?: "en")
            }

            episodedetailsactivityShowPoster.setOnClickListener {
                navigateToShow(show?.trakt_id ?: 0, show?.tmdb_id ?: 0, show?.languages?.first() ?: "en")
            }
        }
    }

    private fun displayEpisode(episode: TmEpisode?) {
        if (episode?.still_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                .into(bindings.episodedetailsactivityBackdrop)
        }

        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityTitle.text = episode?.name
            episodedetailsactivityOverview.text = episode?.overview

            if (episode?.air_date != null) {
                episodedetailsactivityFirstAired.text = "Aired: " + DateFormatUtils.format(
                    episode.air_date,
                    sharedPreferences.getString(
                        "date_format",
                        AppConstants.DEFAULT_DATE_TIME_FORMAT
                    )
                )
            }
        }

        if (episode?.guest_stars?.isNotEmpty() == true) {
            displayCast(episode.guest_stars)
        }
    }

    private fun displayCast(guestStars: List<CastMember>) {
        bindings.episodedetailsactivityInner.episodedetailsactivityCastTitle.visibility =
            View.VISIBLE
        bindings.episodedetailsactivityInner.episodedetailsactivityCastRecycler.visibility =
            View.VISIBLE

        if (guestStars.isNotEmpty()) {
            castAdapter.updateCredits(guestStars)

        }
    }

    private fun setupCheckin(episode: TmEpisode?) {
        val checkinButton =
            bindings.episodedetailsactivityInner.episodedetailsactivityActionButtons.actionbuttonCheckin
        val episodeCheckin = EpisodeCheckin.Builder(
            SyncEpisode().id(EpisodeIds.tmdb(episode?.tmdb_id ?: 0)),
            AppConstants.APP_VERSION,
            AppConstants.APP_DATE
        ).build()

        checkinButton.setOnClickListener {
                viewModel.checkin(episode?.name ?: "Unknown", episodeCheckin)
        }
    }

    private suspend fun clearActiveCheckins(): Boolean {
        val result = viewModel.deleteCheckins()
        return if (result is Resource.Success) {
            displayMessageToast("Succesfully cleared Trakt checkins", Toast.LENGTH_LONG)

            true
        } else {
            Log.e(TAG, "clearActiveCheckins: Error clearing checkins")
            result.error?.printStackTrace()
            displayMessageToast(
                "Error clearing active Trakt checkins. Error: ${result.error?.localizedMessage}",
                Toast.LENGTH_LONG
            )

            false
        }
    }

    private fun handleWatchedEpisodeDelete(watchedEpisode: WatchedEpisode) {
        val syncItem = SyncItems().ids(watchedEpisode.id)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete ${watchedEpisode.episode_title} from your watched history?")
            .setMessage("Are you sure you want to remove ${watchedEpisode.episode_title} from your Trakt History?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeWatchedEpisode(syncItem)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        dialog.show()
    }

    private fun navigateToShow(traktId: Int, tmdbId: Int, langauge: String) {
        val intent = Intent(this, ShowDetailsActivity::class.java)
        intent.putExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, traktId)
        intent.putExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, tmdbId)
        intent.putExtra(ShowDetailsRepository.SHOW_LANGUAGE_KEY, langauge)

        startActivity(intent)
    }
    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    private fun displayAlertDialog(
        title: String,
        message: String,
        okCallback: DialogInterface.OnClickListener,
        cancelCallback: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes", okCallback)
            .setNegativeButton("No", cancelCallback)
            .show()
    }


    private fun initRecycler() {
        castRecyclerView = bindings.episodedetailsactivityInner.episodedetailsactivityCastRecycler
        val castLayoutManager = LinearLayoutManager(this)
        castLayoutManager.orientation = LinearLayoutManager.HORIZONTAL

        castAdapter = CastCreditsAdapter(glide)

        castRecyclerView.layoutManager = castLayoutManager
        castRecyclerView.adapter = castAdapter

        val watchedEpisodesLayoutManager = LinearLayoutManager(this)

        watchedEpisodesAdapter = EpisodeWatchedHistoryItemAdapter(callback = { selectedEpisode ->
            handleWatchedEpisodeDelete(selectedEpisode)
        })

        watchedEpisodesRecyclerView = bindings.episodedetailsactivityInner.episodedetailsactivityWatchedRecyclerview

        watchedEpisodesRecyclerView.layoutManager = watchedEpisodesLayoutManager
        watchedEpisodesRecyclerView.adapter = watchedEpisodesAdapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }
}