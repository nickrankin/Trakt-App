package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.nickrankin.traktapp.databinding.ActivityMainBinding
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.search.ShowSearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var bindings: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

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

        setSupportActionBar(toolbar)

        setupDrawerLayout()

        setContentView(bindings.root)
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
        when(item.itemId) {
            R.id.navdrawer_home -> {
                startActivity(Intent(this, BaseActivity::class.java))
                return true
            }
            R.id.navdrawer_shows -> {
                startActivity(Intent(this, ShowsMainActivity::class.java))
                return true
            }

            R.id.navdrawer_movies -> {
                startActivity(Intent(this, MoviesMainActivity::class.java))
                return true
            }

            R.id.navdrawer_lists -> {

            }
        }

        return false
    }
}