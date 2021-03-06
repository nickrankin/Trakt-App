package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.navigation.NavigationView
import com.nickrankin.traktapp.adapter.home.LastWatchedHistoryAdapter
import com.nickrankin.traktapp.adapter.home.UpcomingEpisodesAdapter
import com.nickrankin.traktapp.api.services.trakt.model.stats.UserStats
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.databinding.ActivityMainBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.calculateRuntimeWithDays
import com.nickrankin.traktapp.model.MainActivityViewModel
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: ActivityMainBinding
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var upcomingShowsRecyclerView: RecyclerView
    private lateinit var lastWatchedMoviesRecycler: RecyclerView
    private lateinit var lastWatchedEpisodesRecycler: RecyclerView

    private lateinit var upcomingEpisodesAdapter: UpcomingEpisodesAdapter
    private lateinit var lastWatchedMoviesAdapter: LastWatchedHistoryAdapter<WatchedMoviesStats>
    private lateinit var lastWatchedEpisodesAdapter: LastWatchedHistoryAdapter<WatchedEpisodeStats>


//    @Inject
//    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Use logged in: ${sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)}")
        Log.d(TAG, "onCreate: User logged in as ${sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "Unknown")}. Access token ${sharedPreferences.getString(AuthActivity.ACCESS_TOKEN_KEY, "empty")}")

        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

//        lifecycleScope.launchWhenStarted {
//            trackedEpisodeAlarmScheduler.scheduleAllAlarms()
//
//        }

        bindings = ActivityMainBinding.inflate(layoutInflater)
        toolbar = bindings.toolbarLayout.toolbar

        swipeRefreshLayout = bindings.homeSwipeLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        setContentView(bindings.root)

        setupDrawerLayout()
        setupActionButtons()

        initUpcomingEpisodesRecyclerView()
        initLastWatchedMoviesRecyclerView()
        initLastWatchedShowsRecyclerView()

        getUserStats()

        getUpcomingEpisodes()
        getLastWatchedMovies()
        getLastWatchedEpisodes()
        getEvents()
    }

    private fun getUserStats() {
//        lifecycleScope.launchWhenStarted {
//            viewModel.userStats.collectLatest { userStatsResource ->
//                when (userStatsResource) {
//                    is Resource.Loading -> {
//                        Log.d(TAG, "getUserStats: Loading UserStats")
//                    }
//                    is Resource.Success -> {
//                        bindUserStats(userStatsResource.data)
//                    }
//                    is Resource.Error -> {
//                        Log.e(
//                            TAG,
//                            "getUserStats: Error getting User Stats ${userStatsResource.error?.message}",
//                        )
//                        userStatsResource.error?.printStackTrace()
//                    }
//                }
//            }
//        }
    }

    private fun getUpcomingEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.upcomingShows.collectLatest { upcomingEpisodes ->
                when (upcomingEpisodes) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getUpcomingEpisodes: Loading Upcoming episodes")
                        bindings.homeNextAiringProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        Log.d(
                            TAG,
                            "getUpcomingEpisodes: Got ${upcomingEpisodes.data?.size} episodes"
                        )
                        bindings.homeNextAiringProgressbar.visibility = View.GONE

                        upcomingEpisodesAdapter.submitList(upcomingEpisodes.data?.sortedBy { it.first_aired })
                    }
                    is Resource.Error -> {
                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        upcomingEpisodes.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getLastWatchedMovies() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedMovies.collectLatest {
                    if (swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    bindings.homeWatchedMoviesProgressbar.visibility = View.GONE
                    Log.e(TAG, "getLastWatchedMovies: ${it.size}")
                    lastWatchedMoviesAdapter.submitList(it)
            }
        }
    }

    private fun getLastWatchedEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest {
                    if (swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    bindings.homeWatchedShowsProgressbar.visibility = View.GONE

                    Log.e(TAG, "getLastWatchedEpisodes: ${it.size}")

                    lastWatchedEpisodesAdapter.submitList(it)
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is MainActivityViewModel.Event.RefreshMovieStatsEvent -> {
                        when(event.refreshStatus) {
                            is Resource.Success -> {
                                Log.d(TAG, "getEvents: Refreshed Movies successfully")
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "getEvents: Error refreshing movies", )
                                event.refreshStatus.error?.printStackTrace()
                            }
                            else -> {}
                        }
                    }

                    is MainActivityViewModel.Event.RefreshShowStatsEvent -> {
                        when(event.refreshStatus) {
                            is Resource.Success -> {
                                Log.d(TAG, "getEvents: Refreshed Shows successfully")
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "getEvents: Error refreshing Shows", )
                                event.refreshStatus.error?.printStackTrace()
                            }
                            else -> {}

                        }
                    }
                }
            }
        }
    }

    private fun bindUserStats(userStats: UserStats?) {
        if (userStats == null) {
            return
        }
        bindings.apply {
            val movieStats = userStats.movies
            val showsStats = userStats.shows
            val episodesStats = userStats.episodes

            homeStatsMoviesCollected.text = "Collected: ${movieStats.collected}"
            homeStatsMoviesPlays.text = "Plays: ${movieStats.plays}"
            homeStatsMoviesDuration.text = "${calculateRuntimeWithDays(movieStats.minutes)}"

            homeStatsShowsCollected.text = "Collected  ${showsStats.collected}"
            homeStatsShowsPlays.text = "Plays: ${episodesStats.plays}"
            homeStatsShowsDuration.text = "${calculateRuntimeWithDays(episodesStats.minutes)}"

        }
    }

    private fun initUpcomingEpisodesRecyclerView() {
        upcomingShowsRecyclerView = bindings.homeNextAiringRecyclerview

        val lm = FlexboxLayoutManager(this)

        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.NOWRAP

        upcomingEpisodesAdapter = UpcomingEpisodesAdapter(
            sharedPreferences,
            tmdbImageLoader
        ) { historyEntry, action, position ->
            val intent = Intent(this, EpisodeDetailsActivity::class.java)
            intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
                EpisodeDataModel(
                    historyEntry.show_trakt_id,
                    historyEntry.show_tmdb_id,
                    historyEntry.episode_season,
                    historyEntry.episode_number,
                    historyEntry.show_title
                )
            )

            startActivity(intent)
        }

        upcomingShowsRecyclerView.layoutManager = lm
        upcomingShowsRecyclerView.adapter = upcomingEpisodesAdapter

    }

    private fun initLastWatchedMoviesRecyclerView() {
        lastWatchedMoviesRecycler = bindings.homeWatchedMoviesRecyclerview

        val lm = FlexboxLayoutManager(this)

        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.NOWRAP

        val comparator = object : DiffUtil.ItemCallback<WatchedMoviesStats>() {
            override fun areItemsTheSame(oldItem: WatchedMoviesStats, newItem: WatchedMoviesStats): Boolean {
                return oldItem == oldItem
            }

            override fun areContentsTheSame(oldItem: WatchedMoviesStats, newItem: WatchedMoviesStats): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }

        lastWatchedMoviesAdapter = LastWatchedHistoryAdapter(
            comparator,
            sharedPreferences,
            tmdbImageLoader
        ) { watchedMovie, action, position ->
            val intent = Intent(this, MovieDetailsActivity::class.java)

            intent.putExtra(MovieDetailsActivity.MOVIE_DATA_KEY,
                MovieDataModel(
                    watchedMovie.trakt_id,
                    watchedMovie.tmdb_id,
                    watchedMovie.title,
                    0
                )
            )

            startActivity(intent)
        }

        lastWatchedMoviesRecycler.layoutManager = lm
        lastWatchedMoviesRecycler.adapter = lastWatchedMoviesAdapter

    }

    private fun initLastWatchedShowsRecyclerView() {
        lastWatchedEpisodesRecycler = bindings.homeWatchedShowsRecyclerview

        val lm = FlexboxLayoutManager(this)

        lm.flexDirection = FlexDirection.ROW
        lm.flexWrap = FlexWrap.NOWRAP

        val comparator = object : DiffUtil.ItemCallback<WatchedEpisodeStats>() {
            override fun areItemsTheSame(
                oldItem: WatchedEpisodeStats,
                newItem: WatchedEpisodeStats
            ): Boolean {
                return oldItem == oldItem
            }

            override fun areContentsTheSame(
                oldItem: WatchedEpisodeStats,
                newItem: WatchedEpisodeStats
            ): Boolean {
                return oldItem.id == newItem.id
            }
        }

        lastWatchedEpisodesAdapter = LastWatchedHistoryAdapter(
            comparator,
            sharedPreferences,
            tmdbImageLoader
        ) { watchedEpisode, action, position ->
            val intent = Intent(this, EpisodeDetailsActivity::class.java)

            intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
                EpisodeDataModel(
                    watchedEpisode.show_trakt_id,
                    watchedEpisode.show_tmdb_id,
                    watchedEpisode.season,
                    watchedEpisode.episode,
                    watchedEpisode.show_title
                ))

            startActivity(intent)
        }

        lastWatchedEpisodesRecycler.layoutManager = lm
        lastWatchedEpisodesRecycler.adapter = lastWatchedEpisodesAdapter
    }

    private fun setupActionButtons() {
        bindings.homeNextAiringAllBtn.setOnClickListener {
            val intent = Intent(this, ShowsMainActivity::class.java)
            //intent.putExtra(ShowsMainActivity.SHOW_CURRENT_FRAGMENT_TAG, ShowsMainActivity.UPCOMING_SHOWS_TAG)

            startActivity(intent)
        }

        bindings.homeWatchedMoviesAllBtn.setOnClickListener {
            val intent = Intent(this, MoviesMainActivity::class.java)
            intent.putExtra(
                MoviesMainActivity.MOVIE_INITIAL_TAB,
                MoviesMainActivity.TAG_WATCHED_MOVIES
            )

            startActivity(intent)

        }
        bindings.homeWatchedShowsAllBtn.setOnClickListener {
            val intent = Intent(this, ShowsMainActivity::class.java)
            intent.putExtra(
                ShowsMainActivity.SHOW_CURRENT_FRAGMENT_TAG,
                ShowsMainActivity.WATCHED_SHOWS_TAG
            )

            startActivity(intent)

        }

    }

    private fun setupDrawerLayout() {
        setSupportActionBar(toolbar)

        navView = bindings.mainDrawer
        drawerLayout = bindings.drawerLayout

        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mainmenu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            else -> {

            }
        }
        return false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        super.onNavigationItemSelected(item)

        drawerLayout.closeDrawer(Gravity.LEFT)

        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }
}