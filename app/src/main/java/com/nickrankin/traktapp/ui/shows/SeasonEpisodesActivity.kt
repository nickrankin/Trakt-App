package com.nickrankin.traktapp.ui.shows

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.shows.EpisodesAdapter
import com.nickrankin.traktapp.databinding.ActivitySeasonEpisodesBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.SeasonEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import org.threeten.bp.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "SeasonEpisodesActivity"
@AndroidEntryPoint
class SeasonEpisodesActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, OnNavigateToEpisode {
    private lateinit var bindings: ActivitySeasonEpisodesBinding
    private val viewModel: SeasonEpisodesViewModel by viewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EpisodesAdapter

    private var showTitle = "Show"
    private var seasonTitle = "Season Episodes"


    @Inject
    lateinit var sharedPreferences: SharedPreferences
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

        initRecycler()

        getShow()
        getSeason()
        getEpisodes()

    }

    private fun getShow() {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                val show = showResource.data
                when(showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getShow: Loading show ..")
                    }
                    is Resource.Success -> {
                        if(show != null) {
                            showTitle = show.name

                            updateTitle()
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getShow: Error getting the Show ${showResource.error?.localizedMessage}", )
                        if(show != null) {
                            showTitle = show.name

                            updateTitle()
                        }
                    }
                }
            }
        }
    }

    private fun getSeason() {
        lifecycleScope.launchWhenStarted {
            viewModel.season.collectLatest { seasonResource ->
                val season = seasonResource.data?.first()
                when(seasonResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getSeason: Loading season ...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "getSeason: Got season ${season?.name}" )

                        if(season != null) {
                            seasonTitle = season.name
                            updateTitle()

                            bindings.seasonepisodeactivitySeasonTitle.text = season.name
                            bindings.seasonepisodeactivitySeasonOverview.text = season.overview
                            if(season.air_date != null) {
                                bindings.seasonepisodeactivitySeasonAired.text = "First aired ${DateFormatUtils.format(season.air_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT))}"
                            } else {
                                bindings.seasonepisodeactivitySeasonAired.visibility = View.GONE
                            }
                            bindings.seasonepisodeactivitySeasonEpisodeCount.text = "${season.episode_count} Episodes"

                            if(season.poster_path != null) {
                                glide
                                    .load(AppConstants.TMDB_POSTER_URL + season.poster_path)
                                    .into(bindings.seasonepisodeactivitySeasonPoster)
                            }

                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getSeason: Error getting season data ${seasonResource.error?.localizedMessage}", )
                        seasonTitle = season?.name ?: "Episodes"

                        if(season != null) {
                            seasonTitle = season.name
                            updateTitle()
                        }
                    }
                }
            }
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
                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        val data = episodesResource.data

                        if(data?.isNotEmpty() == true) {
                            adapter.submitList(data.sortedBy {
                                it.episode_number
                            })
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        episodesResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.seasonepisodesactivityRecyclerview
        val layoutManager = LinearLayoutManager(this)

        adapter = EpisodesAdapter(sharedPreferences, glide, callback = {selectedEpisode ->
            navigateToEpisode(selectedEpisode.show_trakt_id, selectedEpisode.show_tmdb_id ?: -1, selectedEpisode.season_number ?: 0, selectedEpisode.episode_number ?: 0, "en")
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    override fun navigateToEpisode(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String?) {
        val intent = Intent(this, EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, language)

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


    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}