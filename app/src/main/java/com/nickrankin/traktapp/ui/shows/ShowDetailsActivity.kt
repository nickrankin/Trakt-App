package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.credits.CastCreditsAdapter
import com.nickrankin.traktapp.adapter.shows.SeasonsAdapter
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityShowDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.trakt5.entities.Episode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "ShowDetailsActivity"

@AndroidEntryPoint
class ShowDetailsActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityShowDetailsBinding

    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castCreditsAdapter: CastCreditsAdapter

    private lateinit var seasonsRecyclerView: RecyclerView
    private lateinit var seasonsAdapter: SeasonsAdapter

    private var isLoggedIn: Boolean = false

    private var showTmdbId: Int = 0

    private val viewModel: ShowDetailsViewModel by viewModels()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityShowDetailsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.showdetailsactivityToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showTmdbId = intent.getIntExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, 0)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        initSeasonsRecycler()

        lifecycleScope.launch {
            launch {
                collectShow()
            }
            launch {
                collectSeasons()
            }
            if(isLoggedIn) {
                launch {
                    getProgress()
                }
            }
        }
    }

    private suspend fun collectShow() {
        viewModel.show.collectLatest { showResource ->
            when (showResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectShow: Show loading")
                }
                is Resource.Success -> {
                    Log.d(TAG, "collectShow: Got show ${showResource.data}")

                    bindTvShowProperties(showResource.data)
                }
                is Resource.Error -> {
                    Log.e(
                        TAG,
                        "collectShow: Couldn't get the show. ${showResource.error?.localizedMessage}",
                    )
                    showResource.error?.printStackTrace()
                }
            }
        }
    }

    private suspend fun collectSeasons() {
        viewModel.seasons.collectLatest { seasonsList ->
            seasonsAdapter.updateSeasons(seasonsList.sortedBy { it.season_number })
        }
    }

    private suspend fun getProgress() {
        viewModel.progress.collectLatest { baseShow ->
            // Update show progress Progressbar
            updateWatchedProgress(
                baseShow.completed?.toDouble() ?: 0.0,
                baseShow.aired?.toDouble() ?: 0.0
            )

            // Update next airing episode
            if (baseShow.next_episode != null) {
                displayNextEpisode(baseShow.next_episode!!)
            }
        }
    }

    private fun bindTvShowProperties(tmShow: TmShow?) {
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
            showdetailsactivityFirstAired.text = "Premiered: " + DateFormatUtils.format(
                tmShow?.first_aired,
                sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)
            )
            showdetailsactivityOverview.text = tmShow?.overview
        }

        // Populate the cast members
        if(tmShow?.credits?.cast?.isNotEmpty() == true) {
            populateCast(tmShow.credits)
        }
    }

    private fun populateCast(credits: Credits?) {
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

    private fun updateWatchedProgress(completed: Double, aired: Double) {
        val progressText = bindings.showdetailsactivityInner.showdetailsactivityProgressTitle
        val progressBar = bindings.showdetailsactivityInner.showdetailsactivityShowProgress

        progressText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        val progress = calculateProgress(completed, aired)

        Log.e(TAG, "getProgress: process is $progress")

        progressText.text = "Show Progress ($progress% watched)"

        progressBar.progress = progress
    }

    private fun displayNextEpisode(traktEpisode: Episode) {
        bindings.showdetailsactivityInner.showdetailsactivityNextEpisodeTitle.visibility =
            View.VISIBLE
        val cardView = findViewById<CardView>(R.id.showdetailsactivity_next_episode)

        cardView.visibility = View.VISIBLE

        lifecycleScope.launchWhenStarted {
            viewModel.episode(showTmdbId, traktEpisode.season ?: 0, traktEpisode.number ?: 0, "en")
                .collectLatest { episodeResource ->
                    when (episodeResource) {
                        is Resource.Loading -> {
                            Log.d(TAG, "displayNextEpisode: Loading next episode")
                        }
                        is Resource.Success -> {
                            Log.d(TAG, "displayNextEpisode: Got episode ${episodeResource.data}")

                            val episode = episodeResource.data

                            bindings.showdetailsactivityInner.showdetailsactivityNextEpisode.apply {
                                episodeitemName.text = episode?.name
                                episodeitemNumber.text =
                                    "S${episode?.season_number}E${episode?.episode_number}"
                                if (episode?.air_date != null) {
                                    episodeitemAirDate.text = "Aired: " + DateFormatUtils.format(
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
                                    showTmdbId,
                                    episode?.season_number ?: 0,
                                    episode?.episode_number ?: 0
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
                    }
                }
        }

    }

    private fun initSeasonsRecycler() {
        seasonsRecyclerView = bindings.showdetailsactivityInner.showdetailsactivitySeasonsRecycler
        val layoutManager = LinearLayoutManager(this)
        seasonsAdapter = SeasonsAdapter(glide, callback = { selectedSeason ->
            navigateToSeason(selectedSeason.show_tmdb_id, selectedSeason.season_number ?: 0)
        })

        seasonsRecyclerView.layoutManager = layoutManager
        seasonsRecyclerView.adapter = seasonsAdapter
    }

    private fun navigateToSeason(showTmdbId: Int, seasonNumber: Int) {
        val intent = Intent(this, SeasonEpisodesActivity::class.java)
        intent.putExtra(SeasonEpisodesRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(SeasonEpisodesRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(SeasonEpisodesRepository.LANGUAGE_KEY, "en")

        startActivity(intent)
    }

    private fun navigateToEpisode(showTmdbId: Int, seasonNumber: Int, episodeNumber: Int) {
        val intent = Intent(this, EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, "en")

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
}