package com.nickrankin.traktapp.ui.shows

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.EpisodesAdapter
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.databinding.ActivitySeasonEpisodesBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.SeasonEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "SeasonEpisodesActivity"

@AndroidEntryPoint
class SeasonEpisodesActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    OnNavigateToEpisode {
    private lateinit var bindings: ActivitySeasonEpisodesBinding
    private val viewModel: SeasonEpisodesViewModel by viewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EpisodesAdapter

    private var seasonBarSetup = false

    private var showTitle = "Show"
    private var seasonTitle = "Season Episodes"
    private var seasonNumber = 0

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

        seasonNumber = intent.extras?.getInt(SeasonEpisodesRepository.SEASON_NUMBER_KEY) ?: 0

        initRecycler()

        getShow()
        getSeasons()
        getEpisodes()

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
                        Log.e(
                            TAG,
                            "getShow: Error getting the Show ${showResource.error?.localizedMessage}"
                        )
                        if (show != null) {
                            showTitle = show.name

                            updateTitle()
                        }
                    }
                }
            }
        }
    }

    private fun getSeasons() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasons.collectLatest { seasonResource ->
                val season = seasonResource.data?.find { tmSeason ->
                    tmSeason.season_number == seasonNumber
                }

                when (seasonResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getSeason: Loading season ...")
                    }
                    is Resource.Success -> {

                        Log.d(TAG, "getSeason: Got season ${season?.name}")

                        setupSeasonSwitcher(seasonResource.data ?: emptyList())

                        if (season != null) {
                            seasonTitle = season.name
                            updateTitle()

                            bindings.seasonepisodeactivitySeasonTitle.text = season.name
                            bindings.seasonepisodeactivitySeasonOverview.text = season.overview

                            if (season.air_date != null) {
                                bindings.seasonepisodeactivitySeasonAired.text = "First aired ${
                                    DateFormatUtils.format(
                                        season.air_date,
                                        sharedPreferences.getString(
                                            "date_format",
                                            AppConstants.DEFAULT_DATE_TIME_FORMAT
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
                    }
                    is Resource.Error -> {
                        Log.e(
                            TAG,
                            "getSeason: Error getting season data ${seasonResource.error?.localizedMessage}"
                        )
                        seasonTitle = season?.name ?: "Episodes"

                        if (season != null) {
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
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        val data = episodesResource.data

                        if (data?.isNotEmpty() == true) {
                            adapter.submitList(data.sortedBy {
                                it.episode_number
                            })
                        }
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        episodesResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun setupSeasonSwitcher(seasons: List<TmSeason>) {
        if (!seasonBarSetup) {
            val seasonSwitcherTitle = bindings.seasonepisodeactivitySeasonSwitcherTitle
            val seasonButtonContainer =
                bindings.seasonepisodeactivitySeasonSwitcher as FlexboxLayout
            seasonButtonContainer.flexDirection = FlexDirection.ROW
            seasonButtonContainer.justifyContent = JustifyContent.FLEX_START
            seasonButtonContainer.flexWrap = FlexWrap.WRAP

            seasonSwitcherTitle.visibility = View.VISIBLE

            seasons.sortedBy { it.season_number }.map { tmSeason ->
                val button = getSeasonButton(seasonButtonContainer.context, tmSeason.season_number)

                button.setOnClickListener {
                    seasonNumber = tmSeason.season_number
                    viewModel.switchSeason(tmSeason.season_number)
                }
                seasonButtonContainer.addView(button)
            }
            seasonBarSetup = true
        }
    }

    private fun getSeasonButton(context: Context, seasonNumber: Int): TextView {
        val button = TextView(context)
        button.text = "$seasonNumber"
        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        params.setMargins(0, 0, 12, 16)

        params.width = 125
        params.height = 125

        button.layoutParams = params

        button.gravity = Gravity.CENTER
        button.typeface = Typeface.DEFAULT_BOLD
        button.textAlignment = View.TEXT_ALIGNMENT_CENTER
        button.setBackgroundResource(R.drawable.button_round)

        return button

    }

    private fun initRecycler() {
        recyclerView = bindings.seasonepisodesactivityRecyclerview
        val layoutManager = LinearLayoutManager(this)

        adapter = EpisodesAdapter(sharedPreferences, glide, callback = { selectedEpisode ->
            navigateToEpisode(
                selectedEpisode.show_trakt_id,
                selectedEpisode.show_tmdb_id,
                selectedEpisode.season_number ?: 0,
                selectedEpisode.episode_number ?: 0,
                "en"
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
        language: String?
    ) {
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