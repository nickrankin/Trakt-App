package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.adapter.shows.EpisodesAdapter
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.databinding.ActivitySeasonEpisodesBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.SeasonEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val SELECTED_SEASON = "selected_season"
private const val TAG = "SeasonEpisodesActivity"
@AndroidEntryPoint
class SeasonEpisodesActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener,
    OnNavigateToEpisode {
    private lateinit var bindings: ActivitySeasonEpisodesBinding
    private val viewModel: SeasonEpisodesViewModel by viewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EpisodesAdapter

    private var seasonBarSetup = false

    private lateinit var seasonDataModel: SeasonDataModel
    private var selectedSeason = 0

    private var showTitle = "Show"
    private var seasonTitle = "Season Episodes"

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivitySeasonEpisodesBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        swipeLayout = bindings.seasonepisodesactivitySwipeRefreshLayout
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.seasonepisodesactivityProgressbar

        setSupportActionBar(bindings.seasonepisodesactivityToolbar.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(intent.extras?.containsKey(SEASON_DATA_KEY) == false || intent.extras?.getParcelable<SeasonDataModel>(
                SEASON_DATA_KEY) == null) {
            throw RuntimeException("Error loading Season Data. SeasonDataModel must be included with Intent!")
        }

        seasonDataModel = intent.extras?.getParcelable<SeasonDataModel>(SEASON_DATA_KEY)!!

        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_SEASON)) {
            selectedSeason = savedInstanceState.getInt(SELECTED_SEASON)
            viewModel.switchSeason(selectedSeason)
        }

        initRecycler()

        getShow()
        getSeason()
        getEpisodes()

        setupSeasonSwitcher()
    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when (showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getShow: Loading show ..")
                    }
                    is Resource.Success -> {
                        if (show != null) {
                            showTitle = show.name

                            updateTitle()
                        }
                    }
                    is Resource.Error -> {

                        if (show != null) {
                            showTitle = show.name

                            updateTitle()
                        }

                        showErrorSnackbarRetryButton(showResource.error, bindings.seasonepisodesactivitySwipeRefreshLayout) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun getSeason() {
        lifecycleScope.launchWhenStarted {
            viewModel.currentSeason.collectLatest { seasonResource ->
                val season = seasonResource.data

                when (seasonResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getSeason: Loading season ...")
                    }
                    is Resource.Success -> {

                        // Season Stats can be empty initially if user hasn't watched any of show before. See SeasonStatsRepository -> getWatchedSeasonStatsPerShow()
                        if(season?.isNotEmpty() == true) {
                            Log.d(TAG, "getSeason: Got season ${season.first().season.name}")

                            displaySeason(season.first())

                        }

                    }
                    is Resource.Error -> {
                        if(season?.isNotEmpty() == true) {
                            displaySeason(season.first())

                        }
                        showErrorSnackbarRetryButton(seasonResource.error, bindings.seasonepisodesactivitySwipeRefreshLayout) {
                            viewModel.onRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun displaySeason(seasonData: TmSeasonAndStats?) {
        if(seasonData == null) {
            return
        }

        val season = seasonData.season

        bindings.seasonepisodeactivityMainGroup.visibility = View.VISIBLE

            seasonTitle = season.name
            updateTitle()

            bindings.seasonepisodeactivitySeasonTitle.text = season.name
            bindings.seasonepisodeactivitySeasonOverview.text = season.overview

            if (season.air_date != null) {
                bindings.seasonepisodeactivitySeasonAired.text = "First aired ${
                    
                    season.air_date.format(
                        DateTimeFormatter.ofPattern(
                            sharedPreferences.getString(
                                "date_format",
                                AppConstants.DEFAULT_DATE_TIME_FORMAT
                            )
                        )
                    )
                }"
            } else {
                bindings.seasonepisodeactivitySeasonAired.visibility = View.GONE
            }
            bindings.seasonepisodeactivitySeasonEpisodeCount.text =
                "${season.episode_count} Episodes"

            if (season.poster_path != null) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + season.poster_path)
                    .into(bindings.seasonepisodeactivitySeasonPoster)
            }
    }

    private fun getEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.episodes.collectLatest { episodesResource ->
                when (episodesResource) {
                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        Log.d(TAG, "collectEpisodes: Episodes loading")
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        val data = episodesResource.data

                        if (data?.isNotEmpty() == true) {
                            adapter.submitList(data.sortedBy {
                                it.episode.episode_number
                            })
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        handleError(episodesResource.error, null)
                    }
                }
            }
        }
    }

    private fun setupSeasonSwitcher() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasons.collectLatest { seasonResource ->
                when(seasonResource) {
                    is Resource.Success -> {
                        setupSeasonSpinner(seasonResource.data ?: emptyList())
                    }
                    else -> {}

                }
            }
        }

    }

    private fun setupSeasonSpinner(seasonsData: List<TmSeasonAndStats>) {
        val seasonIntro = "Season - "
        if (!seasonBarSetup) {
            val seasonSwitcherTitle = bindings.seasonepisodeactivitySeasonSwitcherTitle
            val seasonSpinner = bindings.seasonepisodeactivitySeasonSwitcher

            seasonSwitcherTitle.visibility = View.VISIBLE

            val seasonsList: MutableList<String> = mutableListOf()

            seasonsData.sortedBy { it.season.season_number }.map { tmSeason ->
                seasonsList.add("$seasonIntro${tmSeason.season.season_number}")
            }

            seasonSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasonsList)

            seasonSpinner.setSelection(seasonsList.indexOf(seasonIntro + seasonDataModel.seasonNumber) )

            seasonSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener
            {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    // Reset the view and enable progressbar
                    progressBar.visibility = View.VISIBLE
                    adapter.submitList(emptyList())

                    val currentSeason = (p0?.selectedItem as String).replace(seasonIntro, "").toInt()
                    Log.d(TAG, "onItemSelected: $currentSeason", )

                    displaySeason(seasonsData.find { it.season.season_number == currentSeason })

                    viewModel.switchSeason(currentSeason)

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // TODO("Not yet implemented")
                }

            }

            seasonBarSetup = true
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.seasonepisodesactivityRecyclerview
        val layoutManager = LinearLayoutManager(this)

        adapter = EpisodesAdapter(sharedPreferences, glide, callback = { selectedEpisodeData ->
            val selectedEpisode = selectedEpisodeData.episode
            navigateToEpisode(
                selectedEpisode.show_trakt_id,
                selectedEpisode.show_tmdb_id,
                selectedEpisode.season_number ?: 0,
                selectedEpisode.episode_number ?: 0,
                ""
            )
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        showTitle: String?
    ) {
        val intent = Intent(this, EpisodeDetailsActivity::class.java)

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                showTitle
            ))

        // No need to force refresh of watched shows as this was done in previous step (ShowDetailsActivity) so assume the watched show data in cache is up to date
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, false)

        startActivity(intent)
    }

    private fun updateTitle() {
        // Title consists of Show name and Season Name. To prevent colision sync title update to ensure both titles are displayed successfully
        synchronized(this) {
            supportActionBar?.title = "$seasonTitle - $showTitle"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_SEASON, selectedSeason)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModel.onRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val SEASON_DATA_KEY = "season_data_key"
    }
}