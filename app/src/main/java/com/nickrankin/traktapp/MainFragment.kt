package com.nickrankin.traktapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.WorkInfo
import com.nickrankin.traktapp.adapter.home.LastWatchedHistoryAdapter
import com.nickrankin.traktapp.adapter.home.UpcomingEpisodesAdapter
import com.nickrankin.traktapp.dao.auth.model.Stats
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.databinding.ActivityMainBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.calculateRuntimeWithDays
import com.nickrankin.traktapp.model.MainActivityViewModel
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRemoteMediator
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MainFragment"
@AndroidEntryPoint
class MainFragment: BaseFragment(), SwipeRefreshLayout.OnRefreshListener {
    private var _bindings: ActivityMainBinding? = null
    private val bindings get() = _bindings!!

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: MainActivityViewModel by activityViewModels()

    private lateinit var upcomingShowsRecyclerView: RecyclerView
    private lateinit var lastWatchedMoviesRecycler: RecyclerView
    private lateinit var lastWatchedEpisodesRecycler: RecyclerView

    private lateinit var upcomingEpisodesAdapter: UpcomingEpisodesAdapter
    private lateinit var lastWatchedMoviesAdapter: LastWatchedHistoryAdapter<MovieWatchedHistoryEntry>
    private lateinit var lastWatchedEpisodesAdapter: LastWatchedHistoryAdapter<EpisodeWatchedHistoryEntry>

    private var isRefreshingMovies = false
    private var isRefreshingShows = false


//    @Inject
//    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _bindings = ActivityMainBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as OnNavigateToEntity).enableOverviewLayout(false)

        updateTitle(AppConstants.APP_TITLE)

        Log.d(
            TAG,
            "onCreate: Use logged in: ${
                sharedPreferences.getBoolean(
                    AuthActivity.IS_LOGGED_IN,
                    false
                )
            }"
        )
        Log.d(
            TAG,
            "onCreate: User logged in as ${
                sharedPreferences.getString(
                    AuthActivity.USER_SLUG_KEY,
                    "Unknown"
                )
            }. Access token ${sharedPreferences.getString(AuthActivity.ACCESS_TOKEN_KEY, "empty")}"
        )

        // Load the default preferences
        PreferenceManager.setDefaultValues(requireContext(), R.xml.root_preferences, false)

        setupActionButtons()

        initUpcomingEpisodesRecyclerView()
        initLastWatchedMoviesRecyclerView()
        initLastWatchedShowsRecyclerView()

        if(isLoggedIn) {
            getUserStats()
            getUpcomingEpisodes()
            getLastWatchedMovies()
            getLastWatchedEpisodes()
        } else {
            // If user is not logged in, do not show logon only features
            bindings.homeStatsLayout.visibility = View.GONE
            bindings.homeStatsMessageContainer.visibility = View.VISIBLE
            bindings.homeStatsMessageContainer.text = getString(R.string.login_trakt_required)

            bindings.homeAiringLayout.visibility = View.GONE
            bindings.homeAiringMessageContainer.visibility = View.VISIBLE
            bindings.homeAiringMessageContainer.text = getString(R.string.login_trakt_required)

            bindings.homeWatchedMoviesLayout.visibility = View.GONE
            bindings.homeWatchedMoviesMessageContainer.visibility = View.VISIBLE
            bindings.homeWatchedMoviesMessageContainer.text = getString(R.string.login_trakt_required)

            bindings.homeWatchedShowsLayout.visibility = View.GONE
            bindings.homeWatchedShowsMessageContainer.visibility = View.VISIBLE
            bindings.homeWatchedShowsMessageContainer.text = getString(R.string.login_trakt_required)
        }
    }

    private fun getUserStats() {
        lifecycleScope.launchWhenStarted {
            viewModel.userStats.collectLatest { userStatsResource ->
                when (userStatsResource) {
                    is Resource.Loading -> {
                        bindings.homeStatsProgressbar.visibility = View.VISIBLE

                    }
                    is Resource.Success -> {
                        bindings.homeStatsProgressbar.visibility = View.GONE
                        bindings.homeStatsLayout.visibility = View.VISIBLE
                        bindings.homeStatsMessageContainer.visibility = View.GONE

                        bindUserStats(userStatsResource.data)
                    }
                    is Resource.Error -> {
                        bindings.homeStatsProgressbar.visibility = View.GONE

                        if(userStatsResource.data == null) {
                            bindings.homeStatsLayout.visibility = View.GONE
                            bindings.homeStatsMessageContainer.visibility = View.VISIBLE
                            bindings.homeStatsMessageContainer.text = getString(R.string.unable_load_stats)
                        }

                        handleError(userStatsResource.error, null)
                    }
                }
            }
        }
    }

    private fun getUpcomingEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.upcomingShows.collectLatest { upcomingEpisodes ->
                when (upcomingEpisodes) {
                    is Resource.Loading -> {
                        bindings.homeNextAiringProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        bindings.homeNextAiringProgressbar.visibility = View.GONE

                        if(upcomingEpisodes.data.isNullOrEmpty()) {
                            bindings.homeAiringLayout.visibility = View.GONE
                            bindings.homeAiringMessageContainer.visibility = View.VISIBLE
                            bindings.homeAiringMessageContainer.text = getString(R.string.airing_empty)
                        } else {
                            bindings.homeAiringLayout.visibility = View.VISIBLE
                            bindings.homeAiringMessageContainer.visibility = View.GONE

                            upcomingEpisodesAdapter.submitList(upcomingEpisodes.data?.sortedBy { it.first_aired })
                        }
                    }
                    is Resource.Error -> {
                        bindings.homeNextAiringProgressbar.visibility = View.GONE

                        if(upcomingEpisodes.data.isNullOrEmpty()) {
                            bindings.homeAiringLayout.visibility = View.GONE
                            bindings.homeAiringMessageContainer.visibility = View.VISIBLE
                            bindings.homeAiringMessageContainer.text = getString(R.string.airing_empty)
                        } else {
                            bindings.homeAiringLayout.visibility = View.VISIBLE
                            bindings.homeAiringMessageContainer.visibility = View.GONE

                            upcomingEpisodesAdapter.submitList(upcomingEpisodes.data?.sortedBy { it.first_aired })
                        }


                        handleError(upcomingEpisodes.error, null)

                    }
                }
            }
        }
    }

    private fun getLastWatchedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedMovies.collectLatest { latestMoviesResource ->
                when(latestMoviesResource) {
                    is Resource.Loading -> {
                        bindings.homeWatchedMoviesProgressbar.visibility = View.VISIBLE

                    }
                    is Resource.Success -> {
                        bindings.homeWatchedMoviesProgressbar.visibility = View.GONE

                        val watchedMovies = latestMoviesResource.data

                        if(watchedMovies.isNullOrEmpty()) {
                            bindings.homeWatchedMoviesLayout.visibility = View.GONE
                            bindings.homeWatchedMoviesMessageContainer.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesMessageContainer.text = getString(R.string.no_watched_movies)

                        } else {
                            bindings.homeWatchedMoviesLayout.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesMessageContainer.visibility = View.GONE
                            lastWatchedMoviesAdapter.submitList(latestMoviesResource.data)
                        }
                    }
                    is Resource.Error -> {
                        bindings.homeWatchedMoviesProgressbar.visibility = View.GONE


                        val watchedMovies = latestMoviesResource.data

                        if(watchedMovies.isNullOrEmpty()) {
                            bindings.homeWatchedMoviesLayout.visibility = View.GONE
                            bindings.homeWatchedMoviesMessageContainer.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesMessageContainer.text = getString(R.string.no_watched_movies)

                        } else {
                            bindings.homeWatchedMoviesLayout.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesMessageContainer.visibility = View.GONE
                            lastWatchedMoviesAdapter.submitList(latestMoviesResource.data)
                        }
                        handleError(latestMoviesResource.error, null)
                    }

                }
            }
        }
    }

    private fun getLastWatchedEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest { watchedEpisodesResource ->

                when(watchedEpisodesResource) {
                    is Resource.Loading -> {
                        bindings.homeWatchedShowsProgressbar.visibility = View.VISIBLE
                    }

                    is Resource.Success -> {
                        bindings.homeWatchedShowsProgressbar.visibility = View.GONE

                        val watchedEpisodes = watchedEpisodesResource.data

                        if(watchedEpisodes.isNullOrEmpty()) {
                            bindings.homeWatchedShowsLayout.visibility = View.GONE
                            bindings.homeWatchedShowsMessageContainer.visibility = View.VISIBLE
                            bindings.homeWatchedShowsMessageContainer.text = getString(R.string.no_watched_episodes)
                        } else {
                            bindings.homeWatchedShowsLayout.visibility = View.VISIBLE
                            bindings.homeWatchedShowsMessageContainer.visibility = View.GONE

                            lastWatchedEpisodesAdapter.submitList(watchedEpisodesResource.data)
                        }

                    }
                    is Resource.Error -> {
                        bindings.homeWatchedShowsProgressbar.visibility = View.GONE

                        val watchedEpisodes = watchedEpisodesResource.data

                        if(watchedEpisodes.isNullOrEmpty()) {
                            bindings.homeWatchedShowsLayout.visibility = View.GONE
                            bindings.homeWatchedShowsMessageContainer.visibility = View.VISIBLE
                            bindings.homeWatchedShowsMessageContainer.text = getString(R.string.no_watched_episodes)
                        } else {
                            bindings.homeWatchedShowsLayout.visibility = View.VISIBLE
                            bindings.homeWatchedShowsMessageContainer.visibility = View.GONE

                            lastWatchedEpisodesAdapter.submitList(watchedEpisodesResource.data)
                        }

                        handleError(watchedEpisodesResource.error, null)

                    }

                }

            }
        }
    }


    private fun bindUserStats(userStats: Stats?) {
        if (userStats == null) {
            return
        }
        bindings.apply {

            homeStatsMoviesCollected.text = "Collected: ${userStats.collected_movies}"
            homeStatsMoviesPlays.text = "Plays: ${userStats.played_movies}"
            homeStatsMoviesDuration.text =
                calculateRuntimeWithDays(userStats.watched_movies_duration)

            homeStatsShowsCollected.text = "Collected  ${userStats.collected_shows}"
            homeStatsShowsPlays.text = "Plays: ${userStats.played_shows}"
            homeStatsShowsDuration.text =
                calculateRuntimeWithDays(userStats.watched_episodes_duration)

        }
    }

    private fun initUpcomingEpisodesRecyclerView() {
        upcomingShowsRecyclerView = bindings.homeNextAiringRecyclerview

        val lm = LinearLayoutManager(requireContext())

        upcomingEpisodesAdapter = UpcomingEpisodesAdapter(
            sharedPreferences,
            tmdbImageLoader
        ) { historyEntry, action, position ->

            (activity as OnNavigateToEntity).navigateToEpisode(
                EpisodeDataModel(
                    historyEntry.show_trakt_id,
                    historyEntry.show_tmdb_id,
                    historyEntry.episode_season,
                    historyEntry.episode_number,
                    historyEntry.show_title
                )
            )

        }

        upcomingShowsRecyclerView.layoutManager = lm
        upcomingShowsRecyclerView.adapter = upcomingEpisodesAdapter

    }

    private fun initLastWatchedMoviesRecyclerView() {
        lastWatchedMoviesRecycler = bindings.homeWatchedMoviesRecyclerview

        val lm = LinearLayoutManager(requireContext())

        val comparator = object : DiffUtil.ItemCallback<MovieWatchedHistoryEntry>() {
            override fun areItemsTheSame(
                oldItem: MovieWatchedHistoryEntry,
                newItem: MovieWatchedHistoryEntry
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: MovieWatchedHistoryEntry,
                newItem: MovieWatchedHistoryEntry
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }

        lastWatchedMoviesAdapter = LastWatchedHistoryAdapter(
            comparator,
            sharedPreferences,
            tmdbImageLoader
        ) { watchedMovie ->

            (activity as OnNavigateToEntity).navigateToMovie(
                MovieDataModel(
                    watchedMovie.trakt_id,
                    watchedMovie.tmdb_id,
                    watchedMovie.title,
                    0
                )
            )
        }

        lastWatchedMoviesRecycler.layoutManager = lm
        lastWatchedMoviesRecycler.adapter = lastWatchedMoviesAdapter

    }

    private fun initLastWatchedShowsRecyclerView() {
        lastWatchedEpisodesRecycler = bindings.homeWatchedShowsRecyclerview

        val lm = LinearLayoutManager(requireContext())

        val comparator = object : DiffUtil.ItemCallback<EpisodeWatchedHistoryEntry>() {
            override fun areItemsTheSame(
                oldItem: EpisodeWatchedHistoryEntry,
                newItem: EpisodeWatchedHistoryEntry
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: EpisodeWatchedHistoryEntry,
                newItem: EpisodeWatchedHistoryEntry
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }

        lastWatchedEpisodesAdapter = LastWatchedHistoryAdapter(
            comparator,
            sharedPreferences,
            tmdbImageLoader
        ) { watchedEpisode ->

            (activity as OnNavigateToEntity).navigateToEpisode(
                EpisodeDataModel(
                    watchedEpisode.show_trakt_id,
                    watchedEpisode.show_tmdb_id,
                    watchedEpisode.season,
                    watchedEpisode.episode,
                    watchedEpisode.title
                )
            )
        }

        lastWatchedEpisodesRecycler.layoutManager = lm
        lastWatchedEpisodesRecycler.adapter = lastWatchedEpisodesAdapter
    }

    private fun setupActionButtons() {
        bindings.homeNextAiringAllBtn.setOnClickListener {
            val intent = Intent(requireContext(), ShowsMainActivity::class.java)
            //intent.putExtra(ShowsMainActivity.SHOW_CURRENT_FRAGMENT_TAG, ShowsMainActivity.UPCOMING_SHOWS_TAG)

            startActivity(intent)
        }

        bindings.homeWatchedMoviesAllBtn.setOnClickListener {
            val intent = Intent(requireContext(), MoviesMainActivity::class.java)
            intent.putExtra(
                MoviesMainActivity.MOVIE_INITIAL_TAB,
                MoviesMainActivity.TAG_WATCHED_MOVIES
            )

            startActivity(intent)

        }
        bindings.homeWatchedShowsAllBtn.setOnClickListener {
            val intent = Intent(requireContext(), ShowsMainActivity::class.java)
            intent.putExtra(
                ShowsMainActivity.SHOW_CURRENT_FRAGMENT_TAG,
                ShowsMainActivity.WATCHED_SHOWS_TAG
            )

            startActivity(intent)

        }

    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {

        isRefreshingMovies = true
        isRefreshingShows = true

        // Force refresh of Watched Movies and Shows
        sharedPreferences.edit()
            .putBoolean(WatchedMoviesRemoteMediator.WATCHED_MOVIES_FORCE_REFRESH_KEY, true)
            .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
            .apply()

        viewModel.onRefresh(
            movieRefreshState = { movieRefreshStateLiveData ->
                movieRefreshStateLiveData.observe(this) { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            bindings.homeWatchedMoviesProgressbar.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesProgressbar.bringToFront()
                        }
                        WorkInfo.State.RUNNING -> {
                            bindings.homeWatchedMoviesProgressbar.visibility = View.VISIBLE
                            bindings.homeWatchedMoviesProgressbar.bringToFront()
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            bindings.homeWatchedMoviesProgressbar.visibility = View.GONE

                            isRefreshingMovies = false
                        }
                        WorkInfo.State.FAILED -> {
                            bindings.homeWatchedMoviesProgressbar.visibility = View.GONE
                            isRefreshingMovies = false

                        }
                        else -> {
                            bindings.homeWatchedMoviesProgressbar.visibility = View.GONE
                            isRefreshingMovies = false
                        }
                    }
                }
            },
            showRefreshState = { showRefreshStateLiveData ->
                showRefreshStateLiveData.observe(this) { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            Log.e(TAG, "onRefresh: enqueued")
                            bindings.homeWatchedShowsProgressbar.visibility = View.VISIBLE
                            bindings.homeWatchedShowsProgressbar.bringToFront()

                        }
                        WorkInfo.State.RUNNING -> {
                            Log.e(TAG, "onRefresh: running")
                            bindings.homeWatchedShowsProgressbar.visibility = View.VISIBLE
                            bindings.homeWatchedShowsProgressbar.bringToFront()
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            bindings.homeWatchedShowsProgressbar.visibility = View.GONE
                            isRefreshingShows = false
                        }
                        WorkInfo.State.FAILED -> {
                            bindings.homeWatchedShowsProgressbar.visibility = View.GONE
                            isRefreshingShows = false
                        }
                        else -> {
                            bindings.homeWatchedShowsProgressbar.visibility = View.GONE
                            isRefreshingShows = false
                        }
                    }
                }
            }
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}