package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.nickrankin.traktapp.ui.lists.TraktListsActivity
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "BaseActivity"
@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var searchType: String? = null

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
        val intent = Intent(this, SearchResultsActivity::class.java)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {

                intent.putExtra(SearchManager.QUERY, query)
                intent.putExtra(SearchResultsActivity.SEARCH_TYPE_KEY, searchType ?: SearchResultsActivity.TYPE_MOVIE_KEY)

                startActivity(intent)

                return false
            }
        })
    }

    protected fun setSearchType(type: String?) {
        this.searchType = type
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
                    startActivity(Intent(this, MainActivity::class.java))
            }
            R.id.navdrawer_shows -> {
                startActivity(Intent(this, ShowsMainActivity::class.java))
            }

            R.id.navdrawer_movies -> {
                startActivity(Intent(this, MoviesMainActivity::class.java))
            }

            R.id.navdrawer_lists -> {
                startActivity(Intent(this, TraktListsActivity::class.java))
            }
        }

        return false
    }
}