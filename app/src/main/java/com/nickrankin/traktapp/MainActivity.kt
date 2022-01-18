package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RelativeLayout
import android.widget.SearchView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.databinding.ActivityMainBinding
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.search.ShowSearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var bindings: ActivityMainBinding

    @Inject
    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        val navView: BottomNavigationView = bindings.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_watching_shows, R.id.navigation_collected_shows, R.id.navigation_show_reccomendations
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
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

}