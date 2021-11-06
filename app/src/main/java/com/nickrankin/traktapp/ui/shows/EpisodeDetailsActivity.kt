package com.nickrankin.traktapp.ui.shows

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.credits.CastCreditsAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.CastMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateFormatUtils
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActivity"
@AndroidEntryPoint
class EpisodeDetailsActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityEpisodeDetailsBinding
    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CastCreditsAdapter

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

        lifecycleScope.launch {
            launch { collectShow() }
            launch { collectEpisode() }
        }
    }

    private suspend fun collectShow() {
        viewModel.show.collectLatest { showResource ->
            when(showResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectShow: Loading show..")
                }
                is Resource.Success -> {
                    Log.d(TAG, "collectShow: Got show! ${showResource.data}")
                }
                is Resource.Error -> {
                    Log.e(TAG, "collectShow: Error getting show. ${showResource.error?.localizedMessage}")
                    showResource.error?.printStackTrace()
                }
            }
        }
    }

    private suspend fun collectEpisode() {
        viewModel.episode.collectLatest { episodeResource ->
            when(episodeResource) {
                is Resource.Loading -> {
                    Log.d(TAG, "collectEpisode: Loading Episode...")
                }
                is Resource.Success -> {
                    Log.d(TAG, "collectEpisode: Got episode ${episodeResource.data}")

                    bindings.wpisodedetailsactivityCollapsingToolbarLayout.title = episodeResource.data?.name

                    bindEpisode(episodeResource.data)
                }
                is Resource.Error -> {
                    Log.e(TAG, "collectEpisode: Error getting episode. ${episodeResource.error?.localizedMessage}")
                    episodeResource.error?.printStackTrace()
                }
            }
        }
    }

    private fun bindEpisode(episode: TmEpisode?) {
        if(episode?.still_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                .into(bindings.episodedetailsactivityBackdrop)
        }

        bindings.episodedetailsactivityInner.apply {
            episodedetailsactivityTitle.text = episode?.name
            episodedetailsactivityOverview.text = episode?.overview

            if(episode?.air_date != null) {
                episodedetailsactivityFirstAired.text = DateFormatUtils.format(episode.air_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT))

            }
        }

        if(episode?.guest_stars?.isNotEmpty() == true) {
            displayCast(episode.guest_stars)
        }
    }

    private fun displayCast(guestStars: List<CastMember>) {
        bindings.episodedetailsactivityInner.episodedetailsactivityCastTitle.visibility = View.VISIBLE
        bindings.episodedetailsactivityInner.episodedetailsactivityCastRecycler.visibility = View.VISIBLE

        if(guestStars.isNotEmpty()) {
            adapter.updateCredits(guestStars)

        }
    }

    private fun initRecycler() {
        recyclerView = bindings.episodedetailsactivityInner.episodedetailsactivityCastRecycler
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

        adapter = CastCreditsAdapter(glide)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
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