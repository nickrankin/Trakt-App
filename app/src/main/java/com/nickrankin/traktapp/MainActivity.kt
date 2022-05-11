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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.navigation.NavigationView
import com.nickrankin.traktapp.adapter.home.LastWatchedHistoryAdapter
import com.nickrankin.traktapp.adapter.home.UpcomingEpisodesAdapter
import com.nickrankin.traktapp.api.services.trakt.model.stats.UserStats
import com.nickrankin.traktapp.databinding.ActivityMainBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.calculateRuntimeWithDays
import com.nickrankin.traktapp.model.MainActivityViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.nickrankin.traktapp.ui.search.ShowSearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "MainActivity"
@AndroidEntryPoint
class MainActivity: BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var bindings: ActivityMainBinding
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var tmdbImageLoader: TmdbImageLoader

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var upcomingShowsRecyclerView: RecyclerView
    private lateinit var lastWatchedMoviesRecycler: RecyclerView
    private lateinit var lastWatchedEpisodesRecycler: RecyclerView

    private lateinit var upcomingEpisodesAdapter: UpcomingEpisodesAdapter
    private lateinit var lastWatchedMoviesAdapter: LastWatchedHistoryAdapter
    private lateinit var lastWatchedEpisodesAdapter: LastWatchedHistoryAdapter


//    @Inject
//    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    private fun getUserStats() {
        lifecycleScope.launchWhenStarted {
            viewModel.userStats.collectLatest { userStatsResource ->
                when(userStatsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getUserStats: Loading UserStats")
                    }
                    is Resource.Success -> {
                        bindUserStats(userStatsResource.data)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "getUserStats: Error getting User Stats ${userStatsResource.error?.message}", )
                        userStatsResource.error?.printStackTrace()
                    }

                }
            }
        }
    }

    private fun getUpcomingEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.upcomingShows.collectLatest { upcomingEpisodes ->
                when(upcomingEpisodes) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getUpcomingEpisodes: Loading Upcoming episodes")
                        bindings.homeNextAiringProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        if(swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        Log.d(TAG, "getUpcomingEpisodes: Got ${upcomingEpisodes.data?.size} episodes")
                        bindings.homeNextAiringProgressbar.visibility = View.GONE

                        upcomingEpisodesAdapter.submitList(upcomingEpisodes.data?.sortedBy { it.first_aired })
                    }
                    is Resource.Error -> {
                        if(swipeRefreshLayout.isRefreshing) {
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
                if(it is Resource.Success) {
                    if(swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    bindings.homeWatchedMoviesProgressbar.visibility = View.GONE
                    Log.e(TAG, "getLastWatchedMovies: ${it.data?.size}", )
                    lastWatchedMoviesAdapter.submitList(it.data)
                }
            }
        }
    }

    private fun getLastWatchedEpisodes() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest {
                if(it is Resource.Success) {
                    if(swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }

                    bindings.homeWatchedShowsProgressbar.visibility = View.GONE

                    Log.e(TAG, "getLastWatchedEpisodes: ${it.data?.size}", )

                    lastWatchedEpisodesAdapter.submitList(it.data)
                }
            }
        }
    }

    private fun bindUserStats(userStats: UserStats?) {
        if(userStats == null) {
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

        upcomingEpisodesAdapter = UpcomingEpisodesAdapter(sharedPreferences, tmdbImageLoader) { historyEntry, action, position ->
            val intent = Intent(this, EpisodeDetailsActivity::class.java)
            intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, historyEntry.show_trakt_id)
            intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, historyEntry.episode_season)
            intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, historyEntry.episode_number)

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

        lastWatchedMoviesAdapter = LastWatchedHistoryAdapter(sharedPreferences, tmdbImageLoader) { historyEntry, action, position ->
            val intent = Intent(this, MovieDetailsActivity::class.java)

            intent.putExtra(MovieDetailsRepository.MOVIE_TITLE_KEY, historyEntry.movie?.title)
            intent.putExtra(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY, historyEntry.movie?.ids?.trakt ?: 0)

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

        lastWatchedEpisodesAdapter = LastWatchedHistoryAdapter(sharedPreferences, tmdbImageLoader) { historyEntry, action, position ->
            val intent = Intent(this, EpisodeDetailsActivity::class.java)
            intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, historyEntry.show?.ids?.trakt ?: 0)
            intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, historyEntry.episode?.season)
            intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, historyEntry.episode?.number)

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
                intent.putExtra(MoviesMainActivity.MOVIE_INITIAL_TAB, MoviesMainActivity.TAG_WATCHED_MOVIES)

            startActivity(intent)

        }
        bindings.homeWatchedShowsAllBtn.setOnClickListener {
            val intent = Intent(this, ShowsMainActivity::class.java)
            intent.putExtra(ShowsMainActivity.SHOW_CURRENT_FRAGMENT_TAG, ShowsMainActivity.WATCHED_SHOWS_TAG)

            startActivity(intent)

        }

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        (menu?.findItem(R.id.mainmenu_search)?.actionView as SearchView).apply {
            startSearch(this)
        }


        return true
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

    private fun startSearch(searchView: SearchView) {
        val intent = Intent(this, ShowSearchResultsActivity::class.java)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {

                intent.putExtra(SearchManager.QUERY, query)

                startActivity(intent)

                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
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