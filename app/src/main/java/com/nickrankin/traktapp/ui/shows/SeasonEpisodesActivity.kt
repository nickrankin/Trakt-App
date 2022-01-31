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
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.SeasonEpisodesViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

        lifecycleScope.launch {
            launch {
                getSeason()
            }
            launch {
                collectEpisodes()
            }
        }
    }

    private suspend fun getSeason() {
        viewModel.season().collectLatest { season ->
            Log.e(TAG, "getSeason: Got season ${season?.name}" )
            updateTitle(season?.name ?: "Episodes")

        }
    }

    private suspend fun collectEpisodes() {
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
                        adapter.submitList(data)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    if(swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                    //Log.e(TAG, "collectEpisodes: Error getting resource ${episodesResource.error?.localizedMessage}" )
                    episodesResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun updateTitle(title: String) {
        supportActionBar?.title = title
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