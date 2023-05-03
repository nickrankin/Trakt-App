package com.nickrankin.traktapp.ui.shows.episodedetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActivityEpisodeDetailsBinding
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

private const val TAG = "EpisodeDetailsActivity"

private const val FRAGMENT_ACTION_BUTTONS = "action_buttons_fragment"

@AndroidEntryPoint
class EpisodeDetailsFragment : BaseFragment(), OnNavigateToShow,
    SwipeRefreshLayout.OnRefreshListener {

    private var _bindings: ActivityEpisodeDetailsBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: EpisodeDetailsViewModel by activityViewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var watchedEpisodesRecyclerView: RecyclerView
    private lateinit var watchedEpisodesAdapter: EpisodeWatchedHistoryItemAdapter

    private var episodeDataModel: EpisodeDataModel? = null

    @Inject
    lateinit var trackedEpisodesAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = ActivityEpisodeDetailsBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = bindings.episodedetailsactivitySwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)


        if (arguments?.containsKey(EPISODE_DATA_KEY) == false || arguments?.getParcelable<EpisodeDataModel>(
                EPISODE_DATA_KEY
            ) == null
        ) {
            throw RuntimeException("EpisodeDataModel must be passed to this Activity.")
        }

        episodeDataModel = arguments!!.getParcelable<EpisodeDataModel>(EPISODE_DATA_KEY)!!

        viewModel.switchEpisodeDataModel(episodeDataModel!!)

        (activity as OnNavigateToEntity).enableOverviewLayout(true)

        initFragments()

        getShow()
        getEpisode()


        getSeasonEpisodes()
    }

    private fun dismissEipsodeNotifications() {
        val episodeTraktId = arguments?.getInt(TrackedEpisodeNotificationsBuilder.EPISODE_TRAKT_ID)
        Log.d(
            TAG,
            "dismissEipsodeNotifications: Dismissing notifications for episode ${episodeTraktId}",
        )

        lifecycleScope.launchWhenStarted {
            trackedEpisodesAlarmScheduler.dismissNotification(episodeTraktId ?: 0, true)
        }
    }

    private fun initFragments() {

        if (isLoggedIn) {
            parentFragmentManager.beginTransaction()
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
                            bindings.episodedetailsactivitySwipeLayout
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
                        if (!swipeRefreshLayout.isRefreshing) {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE

                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        updateTitle(episode?.name ?: "Unknown")

                        displayEpisode(episode)
                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE

                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                        // Try to display cached episode if available
                        if (episodeResource.data != null) {
                            displayEpisode(episode)
                        }

                        showErrorSnackbarRetryButton(
                            episodeResource.error,
                            bindings.episodedetailsactivitySwipeLayout
                        ) {
                            viewModel.onRefresh()
                        }

                    }
                }
            }
        }
    }

    private fun getSeasonEpisodes() {
        val nextEpisodeButton = bindings.episodedetailsactivityNextEpisode
        val prevEpisodeButton = bindings.episodedetailsactivityPreviousEpisode



        lifecycleScope.launchWhenStarted {
            viewModel.seasonEpisodes.collectLatest { episodeNumberAndseasonEpisodesResource ->
                val currentEpisodeNumber = episodeNumberAndseasonEpisodesResource.first
                val seasonEpisodesResource = episodeNumberAndseasonEpisodesResource.second
                Log.d(TAG, "getSeasonEpisodes: currentEpisodeNumber $currentEpisodeNumber")

                if (currentEpisodeNumber == -1) {
                    Log.e(TAG, "getSeasonEpisodes: Current episode is not correct")

                    prevEpisodeButton.visibility = View.GONE
                    nextEpisodeButton.visibility = View.GONE
                }

                when (seasonEpisodesResource) {
                    is Resource.Loading -> {
                        prevEpisodeButton.visibility = View.GONE
                        nextEpisodeButton.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        val seasonEpisodeData = seasonEpisodesResource.data
                        if (currentEpisodeNumber > 1) {
                            prevEpisodeButton.visibility = View.VISIBLE

                            val previousEpisode =
                                seasonEpisodeData?.find { it.episode.episode_number == currentEpisodeNumber - 1 }
                            prevEpisodeButton.text =
                                "Episode ${previousEpisode?.episode?.episode_number?.toString()}"

                            prevEpisodeButton.setOnClickListener {
                                viewModel.switchEpisodeDataModel(
                                    getEpisodeDataModelByNumber(
                                        previousEpisode?.episode?.episode_number
                                    )
                                )
                            }
                        } else {
                            prevEpisodeButton.visibility = View.GONE
                        }

                        // We have more Episodes after this Episode
                        if (currentEpisodeNumber < (seasonEpisodeData?.size ?: 0)) {
                            nextEpisodeButton.visibility = View.VISIBLE

                            val nextEpisode =
                                seasonEpisodeData?.find { it.episode.episode_number == currentEpisodeNumber + 1 }

                            nextEpisodeButton.text =
                                "Episode ${nextEpisode?.episode?.episode_number}"

                            nextEpisodeButton.setOnClickListener {
                                viewModel.switchEpisodeDataModel(
                                    getEpisodeDataModelByNumber(
                                        nextEpisode?.episode?.episode_number
                                    )
                                )
                            }
                        } else {
                            nextEpisodeButton.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        handleError(seasonEpisodesResource.error, null)
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
            bindings.episodedetailsactivityBackdrop.visibility = View.GONE
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

    private fun getEpisodeDataModelByNumber(newEpisodeNumber: Int?): EpisodeDataModel? {
        if (episodeDataModel == null) {
            Log.e(TAG, "getEpisodeDataModelByNumber: Episode Data model cannot be null")
            return null
        }

        if (newEpisodeNumber == null) {
            Log.e(TAG, "getEpisodeDataModelByNumber: Episode number cannot be null")
            return null
        }
        return EpisodeDataModel(
            episodeDataModel!!.traktId,
            episodeDataModel!!.tmdbId,
            episodeDataModel!!.seasonNumber,
            newEpisodeNumber,
            episodeDataModel!!.showTitle
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
    }
}