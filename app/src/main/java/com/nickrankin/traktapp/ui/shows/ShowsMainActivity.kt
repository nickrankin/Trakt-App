package com.nickrankin.traktapp.ui.shows

import android.app.SearchManager
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.ClassCastException


private const val TAG = "ShowsMainActivity"

@AndroidEntryPoint
class ShowsMainActivity : SplitViewActivity(),
    OnTitleChangeListener {

    private lateinit var showTabsFragment: ShowTabsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent.hasExtra(EpisodeDetailsFragment.EPISODE_DATA_KEY)) {
            // User has click on a upcoming episode pending intent, bring him to the episode overview
            val episodeDataModel = intent.getParcelableExtra<EpisodeDataModel>(EpisodeDetailsFragment.EPISODE_DATA_KEY)

            if(episodeDataModel != null) {
                navigateToEpisode(episodeDataModel)
            }
        }

        // Search defaults to shows
        setSearchType(SearchResultsActivity.TYPE_SHOW_KEY)

        initNavTabs()

        currentFragmentTag = savedInstanceState?.getString(SHOW_CURRENT_FRAGMENT_TAG) ?: ""

    }

    override fun onResume() {
        super.onResume()

        navigateToFragment()
    }

    private fun initNavTabs() {
        showTabsFragment = ShowTabsFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityHeader.id, showTabsFragment)
            .commit()
    }


    private fun navigateToFragment() {

        // Navigation triggered to a particular fragment/tab
        if (intent.hasExtra(SHOW_CURRENT_FRAGMENT_TAG)) {
            currentFragmentTag = intent.getStringExtra(SHOW_CURRENT_FRAGMENT_TAG) ?: ""
        }

        if (currentFragmentTag.isNotBlank()) {
            showTabsFragment.selectTab(selectTabByTag(currentFragmentTag))
        } else {

            super.navigateToFragment(PROGRESS_SHOWS_TAG)
            currentFragmentTag = PROGRESS_SHOWS_TAG
        }

    }

    private fun selectTabByTag(tag: String): Int {
        var tabPos = -1
        when (tag) {
            PROGRESS_SHOWS_TAG -> {
                tabPos = 0

            }
            UPCOMING_SHOWS_TAG -> {
                tabPos = 1

            }
            WATCHED_SHOWS_TAG -> {
                tabPos = 2

            }
            TRACKING_SHOWS_TAG -> {
                tabPos = 3

            }
            COLLECTED_SHOWS_TAG -> {
                tabPos = 4

            }
            SUGGESTED_SHOWS_TAG -> {
                tabPos = 5
            }
            else -> {
                tabPos = 0
            }
        }

        return tabPos
    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }


    override fun navigateToFragment(fragmentTag: String) {
        when (fragmentTag) {
            PROGRESS_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Progress")
                super.navigateToFragment(PROGRESS_SHOWS_TAG)

                currentFragmentTag = PROGRESS_SHOWS_TAG
            }
            UPCOMING_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Upcoming")

                super.navigateToFragment(UPCOMING_SHOWS_TAG)

                currentFragmentTag = UPCOMING_SHOWS_TAG
            }
            WATCHED_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Watched")

                super.navigateToFragment(WATCHED_SHOWS_TAG)

                currentFragmentTag = WATCHED_SHOWS_TAG
            }
            TRACKING_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Tracking")

                super.navigateToFragment(TRACKING_SHOWS_TAG)

                currentFragmentTag = TRACKING_SHOWS_TAG
            }
            COLLECTED_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Collected")

                super.navigateToFragment(COLLECTED_SHOWS_TAG)

                currentFragmentTag = COLLECTED_SHOWS_TAG
            }
            SUGGESTED_SHOWS_TAG -> {
                Log.d(TAG, "onTabSelected: Recommended")

                super.navigateToFragment(SUGGESTED_SHOWS_TAG)

                currentFragmentTag = SUGGESTED_SHOWS_TAG
            }
            else -> {
                Log.e(TAG, "onTabSelected: Fragment $fragmentTag not supported")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SHOW_CURRENT_FRAGMENT_TAG, currentFragmentTag)

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val SHOW_CURRENT_FRAGMENT_TAG = "current_fragment"
        const val PROGRESS_SHOWS_TAG = "progress_shows"
        const val UPCOMING_SHOWS_TAG = "upcoming_shows"
        const val TRACKING_SHOWS_TAG = "tracking_shows"
        const val COLLECTED_SHOWS_TAG = "collected_shows"
        const val WATCHED_SHOWS_TAG = "watched_shows"
        const val SUGGESTED_SHOWS_TAG = "suggested_shows"
        const val SEASON_EPISODES_TAG = "season_episodes_tag"
    }
}

