package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.lists.TraktListsActivity
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.text.StringBuilder

private const val TAG = "BaseActivity"
@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, IHandleError {

    private var searchType: String? = null
    private lateinit var searchMenuItem: MenuItem
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    protected var isLoggedIn = false

//    @Inject
//    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)



//        lifecycleScope.launchWhenStarted {
//            trackedEpisodeAlarmScheduler.scheduleAllAlarms()
//
//        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.mainmenu_search)!!

        ((searchMenuItem).actionView as SearchView).apply {
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
                // Collapse the search view when submitted
                searchMenuItem.collapseActionView()
                searchView.onActionViewCollapsed()

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

    override fun showErrorSnackbarRetryButton(throwable: Throwable?, view: View, retryCallback: () -> Unit) {
        Log.e(TAG, "handleError: Error occurred ", )
        throwable?.printStackTrace()

        var errorMessage = StringBuilder()
        errorMessage.append("Error: ")

        // Standard HTTPException handling
        if(throwable is HttpException) {
            if(throwable.code() == 401) {
                // Try refreshing access token...
                handle401Error()
            } else {
                errorMessage.append(" Web Server responded with HTTP Error Code ${throwable.code()}")
            }
        } else if(throwable is IOException) {
            errorMessage.append(" A Network issue occurred. Please check your network connection.")
        } else {
            // Some other kind of error
            errorMessage.append(throwable?.message)
        }

        Snackbar.make(view, errorMessage, Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry", View.OnClickListener { retryCallback() })
            .show()
    }

    override fun showErrorMessageToast(throwable: Throwable?, customMessage: String) {
        throwable?.printStackTrace()
        val errorMessage = StringBuilder()
        errorMessage.append(customMessage)

        if(throwable is HttpException) {
            if(throwable.code() == 401) {
                // Try refreshing access token...
                handle401Error()
            } else {
                errorMessage.append(": HTTP Response code ${throwable.code()}")
            }
        } else if(throwable is IOException) {
            errorMessage.append(": Please check your network connection")
        } else {
            errorMessage.append(throwable?.localizedMessage)
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun handle401Error() {
        // TODO
    }
}