package com.nickrankin.traktapp.ui.movies

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.SplitViewActivity
import com.nickrankin.traktapp.databinding.ActivitySplitviewBinding
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.ui.OnSearchByGenre
import com.nickrankin.traktapp.ui.movies.collected.CollectedMoviesFragment
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.movies.watched.WatchedMoviesFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "splitviewmoviesactivity"
private const val CURRENT_TAB = "key_current_tab"

@AndroidEntryPoint
class MoviesMainActivity : SplitViewActivity(), OnTitleChangeListener, OnNavigateToEntity, OnSearchByGenre {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setupDrawerLayout()

        initNavTabs()

        // Search defaults to movies
        setSearchType(SearchResultsActivity.TYPE_MOVIE_KEY)

        currentFragmentTag = savedInstanceState?.getString(CURRENT_TAB, "") ?: ""

    }

    override fun onResume() {
        super.onResume()
        setupDisplay()


    }

    private fun initNavTabs() {
        val navTabsFragment = MovieTabsFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityHeader.id, navTabsFragment)
            .commit()
    }

    private fun setupDisplay() {
        // If intent contains Fragment tag, we load that Fragment
        if (intent.hasExtra(MOVIE_INITIAL_TAB)) {
            currentFragmentTag = intent.getStringExtra(MOVIE_INITIAL_TAB) ?: ""
        }

        if (currentFragmentTag.isBlank()) {

            super.navigateToFragment(TAG_COLLECTED_MOVIES, true)

            currentFragmentTag = TAG_COLLECTED_MOVIES
        } else {
            selectCurrentTab()
        }

    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }

    private fun selectCurrentTab() {
        when (currentFragmentTag) {
//            TAG_COLLECTED_MOVIES -> {
//                navTabs.selectTab(navTabs.getTabAt(0))
//            }
//            TAG_WATCHED_MOVIES -> {
//                navTabs.selectTab(navTabs.getTabAt(1))
//            }
//            TAG_SUGGESTED_MOVIES -> {
//                navTabs.selectTab(navTabs.getTabAt(2))
//            }
//            TAG_TRENDING_MOVIES -> {
//                navTabs.selectTab(navTabs.getTabAt(3))
//
//            }
        }
    }

    private fun setupDrawerLayout() {
        navView = bindings.splitviewactivityNavView
        drawerLayout = bindings.splitviewactivityDrawer

        displayDrawerLayout()
    }

    private fun displayDrawerLayout() {
        bindings.splitviewactivityHeader.visibility = View.VISIBLE
        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }


    override fun navigateToFragment(fragmentTag: String, loginRequired: Boolean) {
        when (fragmentTag) {
            TAG_COLLECTED_MOVIES -> {
                Log.d(TAG, "onTabSelected: Collected")

                super.navigateToFragment(TAG_COLLECTED_MOVIES, true)

//                supportFragmentManager.beginTransaction()
//                    .replace(
//                        bindings.splitviewactivityFirstContainer.id,
//                        CollectedMoviesFragment.newInstance(),
//                        TAG_COLLECTED_MOVIES
//                    )
//                    .commit()

                currentFragmentTag = TAG_COLLECTED_MOVIES
            }
            TAG_WATCHED_MOVIES -> {
                Log.d(TAG, "onTabSelected: Watched")

                super.navigateToFragment(TAG_WATCHED_MOVIES, true)

//
//                supportFragmentManager.beginTransaction()
//                    .replace(
//                        bindings.splitviewactivityFirstContainer.id,
//                        WatchedMoviesFragment.newInstance(),
//                        TAG_WATCHED_MOVIES
//                    )
//                    .commit()

                currentFragmentTag = TAG_WATCHED_MOVIES
            }
            TAG_SUGGESTED_MOVIES -> {
                Log.d(TAG, "onTabSelected: Recommended")

                super.navigateToFragment(TAG_SUGGESTED_MOVIES, true)

//                supportFragmentManager.beginTransaction()
//                    .replace(
//                        bindings.splitviewactivityFirstContainer.id,
//                        RecommendedMoviesFragment.newInstance(),
//                        TAG_SUGGESTED_MOVIES
//                    )
//                    .commit()

                currentFragmentTag = TAG_SUGGESTED_MOVIES
            }
            TAG_TRENDING_MOVIES -> {
                Log.d(TAG, "onTabSelected: Trending")

                super.navigateToFragment(TAG_TRENDING_MOVIES, false)

//                supportFragmentManager.beginTransaction()
//                    .replace(
//                        bindings.splitviewactivityFirstContainer.id,
//                        TrendingMoviesFragment.newInstance(),
//                        TAG_TRENDING_MOVIES
//                    )
//                    .commit()

                currentFragmentTag = TAG_TRENDING_MOVIES
            }
            else -> {
                Log.e(TAG, "navigateToFragment: Invalid tag $fragmentTag")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CURRENT_TAB, currentFragmentTag)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val MOVIE_INITIAL_TAB = "initial_tab"
        const val TAG_COLLECTED_MOVIES = "collected"
        const val TAG_WATCHED_MOVIES = "watched"
        const val TAG_SUGGESTED_MOVIES = "suggested"
        const val TAG_TRENDING_MOVIES = "trending_movies"

    }

}
