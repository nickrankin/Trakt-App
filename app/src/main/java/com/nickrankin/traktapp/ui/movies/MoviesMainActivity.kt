package com.nickrankin.traktapp.ui.movies

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.ActivityMoviesMainBinding
import com.nickrankin.traktapp.ui.movies.collected.CollectedMoviesFragment
import com.nickrankin.traktapp.ui.movies.watched.WatchedMoviesFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MoviesMainActivity"
private const val CURRENT_TAB = "key_current_tab"
@AndroidEntryPoint
class MoviesMainActivity : BaseActivity(), TabLayout.OnTabSelectedListener {
    private lateinit var bindings: ActivityMoviesMainBinding
    private lateinit var fragmentContainer: LinearLayout

    private lateinit var navTabs: TabLayout

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var currentFragmentTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityMoviesMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        toolbar = bindings.moviesmainactivityToolbar.toolbar

        fragmentContainer = bindings.moviesmainactivityContainer

        navTabs = bindings.moviesmainactivityNavigationTabs
        navTabs.addOnTabSelectedListener(this)


        setupDrawerLayout()

        currentFragmentTag = savedInstanceState?.getString(CURRENT_TAB, "") ?: ""

        if(currentFragmentTag.isBlank()) {
            supportFragmentManager.beginTransaction()
                .add(bindings.moviesmainactivityContainer.id, CollectedMoviesFragment.newInstance(), "collected")
                .commit()

            currentFragmentTag = "collected"
        } else {
            selectCurrentTab()
        }



    }

    private fun selectCurrentTab() {
        when(currentFragmentTag) {
            "collected" -> {
                navTabs.selectTab(navTabs.getTabAt(0))
            }
            "watched" -> {
                navTabs.selectTab(navTabs.getTabAt(1))
            }
            "recommended" -> {
                navTabs.selectTab(navTabs.getTabAt(2))
            }
            "trending" -> {
                navTabs.selectTab(navTabs.getTabAt(3))

            }
        }
    }

    private fun setupDrawerLayout() {
        setSupportActionBar(toolbar)

        navView = bindings.moviesmainactivityNavView
        drawerLayout = bindings.moviesmainactivityDrawer

        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when(tab?.position) {
            0 -> {
                Log.d(TAG, "onTabSelected: Collected")

                supportFragmentManager.beginTransaction()
                    .replace(bindings.moviesmainactivityContainer.id, CollectedMoviesFragment.newInstance())
                    .commit()

                currentFragmentTag = "collected"
            }
            1 -> {
                Log.d(TAG, "onTabSelected: Watched")

                supportFragmentManager.beginTransaction()
                    .replace(bindings.moviesmainactivityContainer.id, WatchedMoviesFragment.newInstance())
                    .commit()

                currentFragmentTag = "watched"
            }
            2 -> {
                Log.d(TAG, "onTabSelected: Recommended")

                supportFragmentManager.beginTransaction()
                    .replace(bindings.moviesmainactivityContainer.id, RecommendedMoviesFragment.newInstance())
                    .commit()

                currentFragmentTag = "recommended"
            }
            3 -> {
                Log.d(TAG, "onTabSelected: Trending")

                supportFragmentManager.beginTransaction()
                    .replace(bindings.moviesmainactivityContainer.id, TrendingMoviesFragment.newInstance())
                    .commit()

                currentFragmentTag = "trending"
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
       // TODO("Not yet implemented")
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
      //  TODO("Not yet implemented")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CURRENT_TAB, currentFragmentTag)
        super.onSaveInstanceState(outState)
    }
}