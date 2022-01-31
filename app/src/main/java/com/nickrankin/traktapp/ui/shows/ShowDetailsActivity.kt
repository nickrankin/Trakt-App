package com.nickrankin.traktapp.ui.shows

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.gson.Gson
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.CastCreditsAdapter
import com.nickrankin.traktapp.adapter.credits.CrewCreditsAdapter
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.adapter.shows.SeasonsAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ActivityShowDetailsBinding
import com.nickrankin.traktapp.databinding.DialogWatchedHistoryItemsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.CrewMember
import com.uwetrottmann.tmdb2.entities.Person
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Rating
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateFormatUtils
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "ShowDetailsActivity"

@AndroidEntryPoint
class ShowDetailsActivity : AppCompatActivity(), OnNavigateToEpisode {
    private lateinit var bindings: ActivityShowDetailsBinding

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castCreditsAdapter: CastCreditsAdapter

    private lateinit var crewRecyclerView: RecyclerView
    private lateinit var crewCreditsAdapter: CrewCreditsAdapter

    private lateinit var seasonsRecyclerView: RecyclerView
    private lateinit var seasonsAdapter: SeasonsAdapter

    private lateinit var ratingsDialog: RatingPickerFragment
    private lateinit var ratingText: TextView

    private var isLoggedIn: Boolean = false

    private var showTraktId = 0

    private lateinit var addCollectionButton: CardView

    private var show: TmShow? = null

    private var isCollected = false
    private var isTracked = false

    private val viewModel: ShowDetailsViewModel by viewModels()

    private lateinit var watchedHistoryRecyclerView: RecyclerView
    private lateinit var watchedHistoryItemAdapter: EpisodeWatchedHistoryItemAdapter

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityShowDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.showdetailsactivityToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showTraktId = intent.getIntExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, 0)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        addCollectionButton =
            bindings.showdetailsactivityInner.showdetailsactivityCollectedButton.collectedbuttonCardview

        ratingText =
            bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonRateText

        initSeasonsRecycler()

        setupRatingsButton()
        updateWatchedProgress()
        displayNextEpisode()
        setupTrackingButton()

        lifecycleScope.launchWhenStarted {
            launch {
                collectShow()
            }
            launch {
                collectSeasons()
            }
            launch {
                collectEvents()
            }
            if (isLoggedIn) {
                launch {
                    collectWatchedStatus()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.entity_details_menu, menu)
        return true
    }

    private suspend fun collectShow() {
        viewModel.show.collectLatest { showResource ->
            when (showResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectShow: Show loading")
                }
                is Resource.Success -> {
                    show = showResource.data

                    displayShow(show)

                    // Populate the cast  members
                    if (show?.created_by?.isNotEmpty() == true) {
                        displayCrew(convertPersonToCrew(show!!.created_by))
                    }
                    if (show?.credits?.cast?.isNotEmpty() == true) {
                        displayCast(show?.credits)
                    }

                    if (isLoggedIn) {
                        setupActionButtons(show)

                        lifecycleScope.launchWhenStarted {
                            collectCollectedShows(show)
                        }
                    }
                }
                is Resource.Error -> {
                    displayToastMessage("Error getting Show Details. ${showResource.error?.localizedMessage}", Toast.LENGTH_LONG)
                    Log.e(
                        TAG,
                        "collectShow: Couldn't get the show. ${showResource.error?.localizedMessage}",
                    )
                    showResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun convertPersonToCrew(personList: List<Person?>): List<CrewMember> {
        val crewMemberList: MutableList<CrewMember> = mutableListOf()

        personList.map { person ->
            val crewMember = CrewMember().apply {
                id = person?.id
                job = "Director"
                name = person?.name
                profile_path = person?.profile_path ?: ""
            }
            crewMemberList.add(
                crewMember
            )
        }
        return crewMemberList
    }

    private suspend fun collectSeasons() {
            lifecycleScope.launchWhenStarted {
                viewModel.seasons.collectLatest { seasonsList ->
                    seasonsAdapter.submitList(seasonsList.data?.sortedBy { it.season_number })
                }
            }

    }

    private suspend fun collectEvents() {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ShowDetailsViewModel.Event.AddToCollectionEvent -> {
                    if (event.syncResponse is Resource.Success) {
                        if (event.syncResponse.data?.added?.episodes ?: 0 > 0) {
                            displayToastMessage(
                                "${show?.name} added to your collection successfully!",
                                Toast.LENGTH_LONG
                            )
                        }
                    } else if (event.syncResponse is Resource.Error) {
                        Log.e(TAG, "collectEvents: ${event.syncResponse.error?.localizedMessage}")
                    }
                }
                is ShowDetailsViewModel.Event.RemoveFromCollectionEvent -> {

                    if (event.syncResponse is Resource.Success) {
                        if (event.syncResponse.data?.deleted?.episodes ?: 0 > 0) {
                            displayToastMessage(
                                "${show?.name} was removed from your collection successfully!",
                                Toast.LENGTH_LONG
                            )
                        }
                    } else if (event.syncResponse is Resource.Error) {
                        Log.e(TAG, "collectEvents: ${event.syncResponse.error?.localizedMessage}")
                    }
                }
                is ShowDetailsViewModel.Event.RatingSetEvent -> {
                    if (event.syncResponse is Resource.Success) {
                        val syncResponse = event.syncResponse.data

                        if (syncResponse?.added?.shows ?: 0 > 0) {
                            viewModel.refreshRating(event.newRating)
                            displayToastMessage(
                                "Rated ${show?.name} with ${Rating.fromValue(event.newRating).name} (${event.newRating}) successfully!",
                                Toast.LENGTH_LONG
                            )
                        } else {
                            viewModel.refreshRating(-1)
                            displayToastMessage("Rating successfully reset!", Toast.LENGTH_LONG)
                        }

                    } else if (event.syncResponse is Resource.Error) {
                        Log.e(TAG, "collectEvents: ${event.syncResponse.error?.localizedMessage}")

                    }
                }
                is ShowDetailsViewModel.Event.DeleteWatchedEpisodeEvent -> {
                    val syncResponse = event.syncResponse.data

                    if(event.syncResponse is Resource.Success) {
                        if(syncResponse?.deleted?.episodes ?: 0 > 0) {
                            displayToastMessage("Successfully removed play", Toast.LENGTH_LONG)
                        } else if(syncResponse?.not_found?.episodes?.isNotEmpty() == true) {
                            displayToastMessage("Could not locate play with this ID, error deleting watched history.", Toast.LENGTH_LONG)
                        }
                    } else {
                        displayToastMessage("Error removing play ${event.syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun setupActionButtons(show: TmShow?) {
        bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonToolbar.visibility =
            View.VISIBLE
        bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonCheckin.visibility =
            View.GONE

        ratingsDialog = RatingPickerFragment(callback = { newRating ->
            setRating(newRating)
        }, show?.name ?: "Unknown")

        bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonRate.setOnClickListener {
            ratingsDialog.show(supportFragmentManager, "ratings_dialog")
        }
    }

    private fun displayShow(tmShow: TmShow?) {
        bindings.showdetailsactivityCollapsingToolbarLayout.title = tmShow?.name

        if (tmShow?.poster_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + tmShow.poster_path)
                .into(bindings.showdetailsactivityBackdrop)
        }

        bindings.showdetailsactivityInner.apply {
            if (tmShow?.poster_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + tmShow.poster_path)
                    .into(showdetailsactivityPoster)
            }

            showdetailsactivityTitle.text = tmShow?.name
            if(tmShow?.first_aired != null) {
                showdetailsactivityFirstAired.text = "Premiered: " + DateFormatUtils.format(
                    tmShow?.first_aired,
                    sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)
                )
            }

            showdetailsactivityOverview.text = tmShow?.overview
        }
    }

    private fun displayCrew(crewList: List<CrewMember>) {
        bindings.showdetailsactivityInner.showdetailsactivityCrewTitle.visibility = View.VISIBLE

        crewRecyclerView = bindings.showdetailsactivityInner.showdetailsactivityCrewRecycler
        crewRecyclerView.visibility = View.VISIBLE

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        crewCreditsAdapter = CrewCreditsAdapter(glide)

        crewRecyclerView.layoutManager = layoutManager
        crewRecyclerView.adapter = crewCreditsAdapter

        crewCreditsAdapter.updateCredits(crewList)
    }

    private fun displayCast(credits: Credits?) {
        bindings.showdetailsactivityInner.showdetailsactivityCastTitle.visibility = View.VISIBLE

        castRecyclerView = bindings.showdetailsactivityInner.showdetailsactivityCastRecycler
        castRecyclerView.visibility = View.VISIBLE

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        castCreditsAdapter = CastCreditsAdapter(glide)

        castRecyclerView.layoutManager = layoutManager
        castRecyclerView.adapter = castCreditsAdapter

        castCreditsAdapter.updateCredits(credits?.cast ?: emptyList())
    }

    private fun setRating(newRating: Int) {
        val syncItems = SyncItems()

        var resetRatings = false

        if (newRating == -1) {
            resetRatings = true
            syncItems.apply {
                shows = listOf(SyncShow().id(ShowIds.trakt(showTraktId)))
            }

        } else {
            syncItems.apply {
                shows = listOf(
                    SyncShow().ratedAt(OffsetDateTime.now()).rating(Rating.fromValue(newRating))
                        .id(ShowIds.trakt(showTraktId))
                )
            }
        }
        viewModel.setRatings(syncItems, resetRatings)
    }

    private fun setupTrackingButton() {
        val showTrackingButton =
            bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonTrack
        val showTrackingIcon =
            bindings.showdetailsactivityInner.showdetailsactivityActionButtons.actionbuttonTrackImageview
        val colorGreen = ContextCompat.getColor(this, R.color.green)
        val colorRed = ContextCompat.getColor(this, R.color.red)

        showTrackingButton.setOnClickListener {
            showTrackingButton.isEnabled = false
            if (isTracked) {
                viewModel.setTracking(showTraktId, -1)
            } else {
                viewModel.setTracking(showTraktId, -1)
            }
        }

        viewModel.trackingLiveData.observe(this, { isTracking ->
            Log.e(TAG, "setupTrackingButton: HERE", )
            if(isTracking) {
                showTrackingIcon.setColorFilter(colorGreen)
            } else {
                showTrackingIcon.setColorFilter(colorRed)
            }
            Log.e(TAG, "setupTrackingButton: HERE")
            // Prevent users spamming the Tracking button. Only re-enable when we receive tracking status in getTrackedStatus()
            showTrackingButton.isEnabled = true
            isTracked = isTracking

        })
    }

    private fun setupCollectedButton(show: TmShow?, collectedShow: CollectedShow?) {
        bindings.showdetailsactivityInner.showdetailsactivityCollectedButton.apply {
            collectedbuttonCollectedAt.text =
                "Collected at: " + collectedShow?.collected_at?.format(
                    DateTimeFormatter.ofPattern(
                        sharedPreferences.getString(
                            "date_format",
                            AppConstants.DEFAULT_DATE_TIME_FORMAT
                        )
                    )
                )
        }

        addCollectionButton.setOnClickListener {
            manageCollection(show, collectedShow)
        }
    }

    private fun setupWatchedButton() {
        Log.d(TAG, "setupWatchedButton: Setting up watched button")
        val watchedButton = bindings.showdetailsactivityInner.showdetailsactivityWatchedButton
        val allButton = watchedButton.watchedbuttonAll

        watchedButton.watchedbuttonCardview.visibility = View.VISIBLE


        val dialogView = DialogWatchedHistoryItemsBinding.inflate(LayoutInflater.from(this))
        watchedHistoryRecyclerView = dialogView.watchedHistoryItemRecyclerview
        val layoutManager = LinearLayoutManager(this)
        watchedHistoryItemAdapter = EpisodeWatchedHistoryItemAdapter(callback = { selectedEpisode ->
            handleWatchedShowDelete(selectedEpisode)
        })

        val watchedHistoryDialog = AlertDialog.Builder(this)
            .setTitle("Watched Shows")
            .setView(dialogView.root)
            .setNegativeButton("Close", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        watchedHistoryRecyclerView.layoutManager = layoutManager
        watchedHistoryRecyclerView.adapter = watchedHistoryItemAdapter

        allButton.setOnClickListener {
            watchedHistoryDialog.show()
        }
    }

    private fun setupRatingsButton() {
        viewModel.state.getLiveData<Int>("rating").observe(this, { rating ->
            if (rating != -1) {
                ratingText.text = rating.toString()
            } else {
                ratingText.text = " - "
            }
        })
    }


    private suspend fun collectWatchedStatus() {
        viewModel.watchedEpisodes.collectLatest { watchedShowsResource ->
            when (watchedShowsResource) {
                is Resource.Success -> {
                    val watchedEpisodes = watchedShowsResource.data

                    if(watchedEpisodes?.isNotEmpty() == true) {
                        setupWatchedButton()
                    }

                    if (watchedEpisodes?.isNotEmpty() == true) {
                        val watchedButton =
                            bindings.showdetailsactivityInner.showdetailsactivityWatchedButton

                        val lastWatchedEpisode = watchedEpisodes.first()

                        watchedButton.apply {
                            watchedButton.watchedbuttonEpisodeTitle.text =
                                lastWatchedEpisode.episode_title

                            watchedButton.watchedbuttonCollectedAt.text =
                                "Last watched at: " + lastWatchedEpisode?.watched_at?.format(
                                    DateTimeFormatter.ofPattern(
                                        sharedPreferences.getString(
                                            "date_format",
                                            AppConstants.DEFAULT_DATE_TIME_FORMAT
                                        )
                                    )
                                )
                        }

                        watchedHistoryItemAdapter.submitList(watchedEpisodes)
                    }

                }
                is Resource.Error -> {
                    Log.e(TAG, "collectWatchedStatus: Error getting watched Status", )
                    watchedShowsResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun handleWatchedShowDelete(watchedEpisode: WatchedEpisode) {
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

    private fun initSeasonsRecycler() {
        seasonsRecyclerView = bindings.showdetailsactivityInner.showdetailsactivitySeasonsRecycler
        val layoutManager = LinearLayoutManager(this)
        seasonsAdapter = SeasonsAdapter(glide, callback = { selectedSeason ->
            navigateToSeason(selectedSeason.show_trakt_id, selectedSeason.show_tmdb_id ?: 0, selectedSeason.season_number ?: 0)
        })

        seasonsRecyclerView.layoutManager = layoutManager
        seasonsRecyclerView.adapter = seasonsAdapter
    }

    private fun navigateToSeason(showTraktId: Int, showTmdbId: Int, seasonNumber: Int) {
        val intent = Intent(this, SeasonEpisodesActivity::class.java)
        intent.putExtra(SeasonEpisodesRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(SeasonEpisodesRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(SeasonEpisodesRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(SeasonEpisodesRepository.LANGUAGE_KEY, "en")

        startActivity(intent)
    }

    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String?) {
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

    private fun manageCollection(show: TmShow?, collectedShow: CollectedShow?) {
        // To prevent user spamming the button, deactivate it while item is remove from collection
        addCollectionButton.isEnabled = false

        val syncItems = SyncItems().apply {
            shows = listOf(SyncShow().id(ShowIds.tmdb(show?.tmdb_id ?: 0)))
        }

        if (!isCollected) {
            showDialog("Add ${show?.name} to your collection",
                "Would you like to add ${show?.name} to your Trakt Collection?",
                { dialogInterface, i ->
                    viewModel.addToCollection(syncItems)
                },
                { dialogInterface, i ->
                    dialogInterface.dismiss()

                    // Reenable the button
                    addCollectionButton.isEnabled = true
                })

        } else {
            showDialog("Remove ${show?.name} from your collection",
                "Would you like to remove ${show?.name} from your Trakt Collection?",
                { dialogInterface, i ->
                    viewModel.removeFromCollection(collectedShow, syncItems)
                },
                { dialogInterface, i ->
                    dialogInterface.dismiss()
                    addCollectionButton.isEnabled = true
                })
        }
    }

    private fun showDialog(
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

    private suspend fun collectCollectedShows(show: TmShow?) {
        viewModel.collectedShow.collectLatest { collectedShowResource ->
            addCollectionButton.isEnabled = true

            when (collectedShowResource) {
                is Resource.Success -> {
                    val collectedShow = collectedShowResource.data?.first()
                    if (collectedShow != null) {
                        isCollected = true
                        setupCollectedButton(show, collectedShow)
                        Log.d(
                            TAG,
                            "collectCollectedShows: ${collectedShowResource.data?.toString()}"
                        )
                    } else {
                        isCollected = false
                        setupCollectedButton(show, null)

                    }
                }
            }
        }
    }

    private fun updateWatchedProgress() {
        val progressText = bindings.showdetailsactivityInner.showdetailsactivityProgressTitle
        val progressBar = bindings.showdetailsactivityInner.showdetailsactivityShowProgress

        viewModel.progressLiveData.observe(this) { progress ->
            progressText.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            progressText.text = "Show Progress ($progress% watched)"

            progressBar.progress = progress
        }
    }

    private fun displayNextEpisode() {
        viewModel.nextEpisodeLiveData.observe(this) {
            if (it.isNotEmpty()) {


                val traktEpisode = gson.fromJson(it, Episode::class.java)

                lifecycleScope.launchWhenStarted {
                    viewModel.episode(
                        intent.getIntExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, 0),
                        intent.getIntExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, -1),
                        traktEpisode?.season ?: 0,
                        traktEpisode?.number ?: 0,
                    ).collectLatest { episodeResource ->
                        when (episodeResource) {
                            is Resource.Success -> {

                                Log.d(
                                    TAG,
                                    "displayNextEpisode: Got episode ${episodeResource.data}"
                                )

                                // Enable Next episode title and layout (CardView)
                                bindings.showdetailsactivityInner.showdetailsactivityNextEpisodeTitle.visibility =
                                    View.VISIBLE
                                val cardView =
                                    findViewById<CardView>(R.id.showdetailsactivity_next_episode)
                                cardView.visibility = View.VISIBLE

                                val episode = episodeResource.data

                                bindings.showdetailsactivityInner.showdetailsactivityNextEpisode.apply {
                                    episodeitemName.text = episode?.name
                                    episodeitemNumber.text =
                                        "S${episode?.season_number}E${episode?.episode_number}"
                                    if (episode?.air_date != null) {
                                        episodeitemAirDate.text =
                                            "Aired: " + DateFormatUtils.format(
                                                episode.air_date,
                                                sharedPreferences.getString(
                                                    "date_format",
                                                    AppConstants.DEFAULT_DATE_TIME_FORMAT
                                                )
                                            )

                                    }
                                    episodeitemOverview.text = episode?.overview

                                    if (episode?.still_path?.isNotEmpty() == true) {
                                        glide
                                            .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                                            .into(episodeitemStillImageview)
                                    }
                                }

                                cardView.setOnClickListener {
                                    navigateToEpisode(
                                        intent.getIntExtra(
                                            ShowDetailsRepository.SHOW_TRAKT_ID_KEY,
                                            0
                                        ),
                                        showTraktId,
                                        episode?.season_number ?: 0,
                                        episode?.episode_number ?: 0,
                                        episode?.language
                                    )
                                }
                            }
                            is Resource.Error -> {
                                Log.e(
                                    TAG,
                                    "displayNextEpisode: Error getting next episode ${episodeResource.error?.localizedMessage}"
                                )
                                episodeResource.error?.printStackTrace()
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()

        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    private fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.entitydetailsmenu_refresh -> {
                onRefresh()
            }
        }

        return false
    }

    private fun displayToastMessage(message: String, length: Int) {
        Toast.makeText(this, message, length).show()
    }

    companion object {

    }
}