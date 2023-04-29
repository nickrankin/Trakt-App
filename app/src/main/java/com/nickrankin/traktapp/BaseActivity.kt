package com.nickrankin.traktapp

import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.nickrankin.traktapp.databinding.ActivitySplitviewBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.*
import com.nickrankin.traktapp.ui.OnSearchByGenre
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.lists.TraktListsActivity
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsFragment
import com.nickrankin.traktapp.ui.settings.SettingsActivity
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment
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

    private lateinit var application: TmApplication
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    protected var isLoggedIn = false

    private var errorHandled = false

//    @Inject
//    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getTmApplication()

        // Load the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)




//        lifecycleScope.launchWhenStarted {
//            trackedEpisodeAlarmScheduler.scheduleAllAlarms()
//
//        }


    }

    private fun getTmApplication() {
        try {
            application = getApplication() as TmApplication
        } catch(e: Throwable) {
            Log.e(TAG, "getTmApplication: Error getting application", )
            e.printStackTrace()
        }
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


    open fun onRefresh() {
        errorHandled = false
    }

    override fun handleError(throwable: Throwable?, customMessage: String?) {
        synchronized(this) {
            if(!errorHandled) {
                val errorMessage = StringBuilder()
                if(customMessage != null) {
                    errorMessage.append(customMessage)
                    errorMessage.append(": ")
                }

                if(throwable is HttpException) {
                    if(throwable.code() == 401) {
                        // Try refreshing access token...
                        Log.e(TAG, "handleError: 401 HTTP Code, access token refresh needed", )
                    } else {
                        errorMessage.append("HTTP Response code ${throwable.code()}")
                    }
                } else if(throwable is IOException) {
                    errorMessage.append("Please check your network connection (${throwable.localizedMessage})")
                } else {
                    errorMessage.append(throwable?.localizedMessage)
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                errorHandled = true
            }
        }

        throwable?.printStackTrace()

    }
}

interface OnNavigateToEntity {
    fun enableOverviewLayout(isEnabled: Boolean)
    fun navigateToFragment(fragmentTag: String)
    fun navigateToMovie(movieDataModel: MovieDataModel)
    fun navigateToShow(showDataModel: ShowDataModel)
    fun navigateToSeason(seasonDataModel: SeasonDataModel)
    fun navigateToEpisode(episodeDataModel: EpisodeDataModel)
    fun navigateToPerson(personDataModel: PersonDataModel)
}