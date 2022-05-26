package com.nickrankin.traktapp.ui.shows

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.ActivityShowsMainBinding
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ClassCastException


private const val TAG = "ShowsMainActivity"
@AndroidEntryPoint
class ShowsMainActivity : BaseActivity(), OnTitleChangeListener, TabLayout.OnTabSelectedListener {
    private lateinit var bindings: ActivityShowsMainBinding
    private lateinit var fragmentContainer: LinearLayout

    private lateinit var navTabs: TabLayout

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var currentFragmentTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityShowsMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        fragmentContainer = bindings.showsmainactivityContainer

        navTabs = bindings.showsmainactivityNavigationTabs

        toolbar = bindings.showsmainactivityToolbar.toolbar

        setSupportActionBar(toolbar)

        // Search defaults to shows
        setSearchType(SearchResultsActivity.TYPE_SHOW_KEY)

        setupDrawerLayout()

        navTabs.addOnTabSelectedListener(this)

        currentFragmentTag = savedInstanceState?.getString(SHOW_CURRENT_FRAGMENT_TAG) ?: ""

        navigateToFragment()

    }

    private fun navigateToFragment() {

        // Navigation triggered to a particular fragment/tab
        if(intent.hasExtra(SHOW_CURRENT_FRAGMENT_TAG)) {
            currentFragmentTag = intent.getStringExtra(SHOW_CURRENT_FRAGMENT_TAG) ?: ""
        }

        if(currentFragmentTag.isNotBlank()) {
            navTabs.selectTab(navTabs.getTabAt(selectTabByTag(currentFragmentTag)))
        } else {
            supportFragmentManager.beginTransaction()
                .add(fragmentContainer.id, ShowsUpcomingFragment.newInstance(), UPCOMING_SHOWS_TAG)
                .commit()

            currentFragmentTag = UPCOMING_SHOWS_TAG
        }

    }

    private fun selectTabByTag(tag: String): Int {
        var tabPos = -1
        when(tag) {
            UPCOMING_SHOWS_TAG -> {
                tabPos = 0
            }
            WATCHED_SHOWS_TAG -> {
                tabPos = 1
            }
            TRACKING_SHOWS_TAG -> {
                tabPos = 2
            }
            COLLECTED_SHOWS_TAG -> {
                tabPos = 3
            }
            SUGGESTED_SHOWS_TAG -> {
                tabPos = 4
            }
            else -> {
                tabPos = 0
            }
        }

        return  tabPos
    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }

    private fun setupDrawerLayout() {
        setSupportActionBar(toolbar)

        navView = bindings.showsmainactivityNavView
        drawerLayout = bindings.showsmainactivityDrawer

        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
                Log.d(TAG, "onTabSelected: Upcoming")
                supportFragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, ShowsUpcomingFragment.newInstance(), UPCOMING_SHOWS_TAG)
                    .commit()

                currentFragmentTag = UPCOMING_SHOWS_TAG
            }
            1 -> {
                Log.d(TAG, "onTabSelected: Watched")
                supportFragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, WatchingFragment.newInstance(), WATCHED_SHOWS_TAG)
                    .commit()

                currentFragmentTag = WATCHED_SHOWS_TAG
            }
            2 -> {
                Log.d(TAG, "onTabSelected: Tracking")
                supportFragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, ShowsTrackingFragment.newInstance(), TRACKING_SHOWS_TAG)
                    .commit()

                currentFragmentTag = TRACKING_SHOWS_TAG
            }
            3 -> {
                Log.d(TAG, "onTabSelected: Collected")
                supportFragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, CollectedShowsFragment.newInstance(), COLLECTED_SHOWS_TAG)
                    .commit()

                currentFragmentTag = COLLECTED_SHOWS_TAG
            }
            4 -> {
                Log.d(TAG, "onTabSelected: Recommended")
                supportFragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, ShowsRecommendedFragment.newInstance(), SUGGESTED_SHOWS_TAG)
                    .commit()

                currentFragmentTag = SUGGESTED_SHOWS_TAG
            }
            else -> {
                Log.e(TAG, "onTabSelected: ELSE")
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> {
            }
            1 -> {
            }
            2 -> {
            }
            3 -> {
            }
            4 -> {
            }
            else -> {
            }
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        try {
            Log.d(TAG, "onTabReselected: Current Fragment Tag $currentFragmentTag")
            (supportFragmentManager.findFragmentByTag(currentFragmentTag) as SwipeRefreshLayout.OnRefreshListener).let { refreshFragment ->
                Log.d(TAG, "onTabReselected: Refreshing Fragment $currentFragmentTag")
                refreshFragment.onRefresh()
            }
        } catch (e: ClassCastException) {
            Log.e(
                TAG,
                "onTabReselected: Cannot Cast ${supportFragmentManager.findFragmentByTag(currentFragmentTag)?.javaClass?.name} as SwipeRefreshLayout.OnRefreshListener",
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SHOW_CURRENT_FRAGMENT_TAG, currentFragmentTag)

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val SHOW_CURRENT_FRAGMENT_TAG = "current_fragment"
        const val UPCOMING_SHOWS_TAG = "upcoming"
        const val TRACKING_SHOWS_TAG = "tracking"
        const val COLLECTED_SHOWS_TAG = "collected"
        const val WATCHED_SHOWS_TAG = "watched"
        const val SUGGESTED_SHOWS_TAG = "suggested"
    }
}