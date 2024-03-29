package com.nickrankin.traktapp.ui.shows.episodedetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.FragmentEpisodeDetailsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import com.nickrankin.traktapp.ui.shows.OnNavigateToShow
import com.nickrankin.traktapp.ui.shows.showdetails.EpisodeCreditsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "EpisodeDetailsFragment"

private const val FRAGMENT_ACTION_BUTTONS = "action_buttons_fragment"

@AndroidEntryPoint
class EpisodeDetailsFragment() : BaseFragment(), OnNavigateToShow,
    SwipeRefreshLayout.OnRefreshListener {

    private var _bindings: FragmentEpisodeDetailsBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: EpisodeDetailsViewModel by viewModels()

    private var episodeTitle = ""

    private var episodeDataModel: EpisodeDataModel? = null

    private var shouldDismissEpisodeNotification = false

    @Inject
    lateinit var trackedEpisodesAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentEpisodeDetailsBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (arguments?.containsKey(EPISODE_DATA_KEY) == false || arguments?.getParcelable<EpisodeDataModel>(
                EPISODE_DATA_KEY
            ) == null
        ) {
            throw RuntimeException("EpisodeDataModel must be passed to this Activity.")
        }

        episodeDataModel = arguments?.getParcelable<EpisodeDataModel>(EPISODE_DATA_KEY)
        
        val isClickedFromNotification = arguments?.getBoolean(CLICK_FROM_NOTIFICATION_KEY) ?: false
        
        if(isClickedFromNotification) {
            Log.d(TAG, "onViewCreated: Episode clicked from notification")

            shouldDismissEpisodeNotification = true
        }

        viewModel.switchEpisodeDataModel(episodeDataModel!!)

        (activity as OnNavigateToEntity).enableOverviewLayout(true)

        initFragments()

        getShow()
        getEpisode()
    }

    override fun onResume() {
        super.onResume()

        if(episodeTitle.isNotBlank()) {
            // In situation fragment got cached, make sure the title is changed when switched
            updateTitle(episodeTitle)
        }
    }

    private fun initFragments() {
            childFragmentManager.beginTransaction()
                .replace(
                    bindings.episodedetailsactivityActionButtons.id,
                    EpisodeDetailsActionButtonsFragment.newInstance(),
                    FRAGMENT_ACTION_BUTTONS
                )
                .replace(
                    bindings.episodedetailsactivityCastCrew.id,
                    EpisodeCreditsFragment.newInstance()
                )
                .commit()

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

                        if (isLoggedIn) {
                            getWatchedEpisodes(show)
                        }
                    }
                    is Resource.Error -> {

                        // Try display cached show if available
                        if (show != null) {
                            displayShow(show)
                        }

                        showErrorSnackbarRetryButton(
                            showResource.error,
                            bindings.root
                        ) {
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
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        episodeTitle = episode?.name ?: "Unknown"

                        // If user reaches episode via the notification, we dismiss the notification at this point
                        if(shouldDismissEpisodeNotification) {
                            val episodeTraktId = episodeResource.data?.episode_trakt_id
                            if(episodeTraktId != null) {
                                trackedEpisodesAlarmScheduler.dismissNotification(episodeTraktId, true)
                            } else {
                                Log.e(TAG, "dismissEipsodeNotifications: Episode Trakt Id cannot be null", )
                            }
                        }

                        updateTitle(episodeTitle)

                        displayEpisode(episode)
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        // Try to display cached episode if available
                        if (episodeResource.data != null) {
                            displayEpisode(episode)
                        }

                        showErrorSnackbarRetryButton(
                            episodeResource.error,
                            bindings.root
                        ) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }



    private fun getWatchedEpisodes(show: TmShow?) {
        if (show == null) {
            return
        }

        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodeStats.collectLatest { watchedEpisodes ->
                val foundShow = watchedEpisodes.find { it.show_trakt_id == show.trakt_id }
            }
        }
    }

    private fun displayShow(show: TmShow?) {

        if (show == null) {
            return
        }

        bindings.apply {

            if (show.poster_path != null && show.poster_path != "") {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + show.poster_path)
                    .into(episodedetailsactivityPoster)
            }

            episodedetailsactivityShowTitle.text = show.name

            episodedetailsactivityShowTitle.setOnClickListener {
                navigateToShow(show.trakt_id, show.tmdb_id, show.name)
            }

            episodedetailsactivityPosterCardview.setOnClickListener {
                navigateToShow(show.trakt_id, show.tmdb_id, show.name)
            }
        }
    }

    private fun displayEpisode(episode: TmEpisode?) {
        if (episode == null) {
            return
        }

        if (episode.still_path?.isNotEmpty() == true) {
            glide
                .load(AppConstants.TMDB_POSTER_URL + episode.still_path)
                .into(bindings.episodedetailsactivityBackdrop)
        } else {
            bindings.episodedetailsactivityBackdrop.scaleType = ImageView.ScaleType.FIT_XY
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
                                AppConstants.DATE_FORMAT,
                                AppConstants.DEFAULT_DATE_TIME_FORMAT
                            )
                        )
                    )
                }"
            }

            if (episode.runtime != null) {
                episodedetailsactivityRuntime.text = "Runtime ${calculateRuntime(episode.runtime)}"
            }

        }
    }

    override fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?) {

        (activity as OnNavigateToEntity).navigateToShow(
            ShowDataModel(
                traktId, tmdbId, title
            )
        )

    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }


    companion object {
        @JvmStatic
        fun newInstance() =
            EpisodeDetailsFragment()

        const val EPISODE_DATA_KEY = "episode_data_key"
        const val CLICK_FROM_NOTIFICATION_KEY = "click_notification_key"
    }
}