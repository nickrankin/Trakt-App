package com.nickrankin.traktapp

import android.app.SearchManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.nickrankin.traktapp.databinding.ActivitySplitviewBinding
import com.nickrankin.traktapp.model.SplitViewViewModel
import com.nickrankin.traktapp.model.datamodel.*
import com.nickrankin.traktapp.ui.OnSearchByGenre
import com.nickrankin.traktapp.ui.auth.NotLoggedInFragment
import com.nickrankin.traktapp.ui.movies.MoviesMainActivity
import com.nickrankin.traktapp.ui.movies.RecommendedMoviesFragment
import com.nickrankin.traktapp.ui.movies.TrendingMoviesFragment
import com.nickrankin.traktapp.ui.movies.collected.CollectedMoviesFragment
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.movies.watched.WatchedMoviesFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsFragment
import com.nickrankin.traktapp.ui.shows.*
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsOverviewFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodePagerFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment

private const val TAG = "SplitViewActivity"

open class SplitViewActivity : BaseActivity(), OnNavigateToEntity,
    OnSearchByGenre, SwipeRefreshLayout.OnRefreshListener {
    protected lateinit var bindings: ActivitySplitviewBinding

    protected lateinit var fragmentContainer: LinearLayout

    private val viewModel: SplitViewViewModel by viewModels()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    protected lateinit var toolbar: Toolbar
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navView: NavigationView
    private var isLandscape = false
    protected lateinit var currentFragmentTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivitySplitviewBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        swipeRefreshLayout = bindings.splitviewactivitySwiperefreshlayour
        swipeRefreshLayout.setOnRefreshListener(this)


        isLandscape = bindings.splitviewactivitySecondContainer.tag == "landscape_tag"

        fragmentContainer = bindings.splitviewactivityFirstContainer


        toolbar = bindings.splitviewactivityToolbar.toolbar

        setSupportActionBar(toolbar)
        setupDrawerLayout()

        Log.e(TAG, "onCreate: backstack is ${supportFragmentManager.backStackEntryCount}")


        supportFragmentManager.fragments.map {
            Log.d(TAG, "onCreate: Fragment in backstack is ${it.javaClass.name} // ${it.tag}")

        }

        supportFragmentManager.addOnBackStackChangedListener {
            Log.d(TAG, "onCreate: -------------------------START-----------------------")
            supportFragmentManager.fragments.forEach {
                Log.d(TAG, "onCreate: Current Fragment backstack: ${it.javaClass.name}")
            }
            Log.d(TAG, "onCreate: -------------------------ENDS-----------------------\n")
        }


        handlePrimaryFragmentSwitch()
        handleSecondaryFragmentSwitch()
    }

    private fun setupDrawerLayout() {
        setSupportActionBar(toolbar)

        navView = bindings.splitviewactivityNavView
        drawerLayout = bindings.splitviewactivityDrawer

        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }

    fun displayEpisode(episodeFragment: EpisodeDetailsFragment) {
        Log.d(TAG, "displayEpisode: Displaying single episode")
        supportFragmentManager.beginTransaction()
            .replace(
                bindings.splitviewactivityFirstContainer.id,
                episodeFragment,
                PRIMARY_FRAGMENT_TAG
            )
            .setReorderingAllowed(true)
//                    .setPrimaryNavigationFragment(fragment)
            .commit()
    }

    override fun navigateToFragment(fragmentTag: String, loginRequired: Boolean) {
        if(loginRequired && !isLoggedIn) {
            Log.d(TAG, "navigateToFragment: You must be logged in to perform this action", )

            viewModel.switchPrimaryFragment(FRAGMENT_NO_LOGIN_TAG)
            return
        }
        viewModel.switchPrimaryFragment(fragmentTag)
    }

    override fun navigateToMovie(movieDataModel: MovieDataModel) {
        viewModel.switchSecondaryFragment(movieDataModel)
    }

    override fun navigateToShow(showDataModel: ShowDataModel) {
        viewModel.switchSecondaryFragment(showDataModel)
    }

    override fun navigateToSeason(seasonDataModel: SeasonDataModel) {
        viewModel.switchSecondaryFragment(seasonDataModel)
    }

    override fun navigateToEpisode(episodeDataModel: EpisodeDataModel) {
        viewModel.switchSecondaryFragment(episodeDataModel)
    }

    override fun navigateToPerson(personDataModel: PersonDataModel) {
        viewModel.switchSecondaryFragment(personDataModel)
    }

    private fun handlePrimaryFragmentSwitch() {
        viewModel.currentPrimaryFragment.observe(this) { fragmentTag ->

            val fragment: Fragment? = when (fragmentTag) {
                MainActivity.MAIN_ACTIVITY_TAG -> {
                    MainFragment.newInstance()
                }
                MoviesMainActivity.TAG_COLLECTED_MOVIES -> {
                    CollectedMoviesFragment.newInstance()
                }
                MoviesMainActivity.TAG_SUGGESTED_MOVIES -> {
                    RecommendedMoviesFragment.newInstance()
                }
                MoviesMainActivity.TAG_TRENDING_MOVIES -> {
                    TrendingMoviesFragment.newInstance()
                }
                MoviesMainActivity.TAG_WATCHED_MOVIES -> {
                    WatchedMoviesFragment.newInstance()
                }
                ShowsMainActivity.TRACKING_SHOWS_TAG -> {
                    ShowsTrackingFragment.newInstance()
                }
                ShowsMainActivity.SUGGESTED_SHOWS_TAG -> {
                    ShowsRecommendedFragment.newInstance()
                }
                ShowsMainActivity.WATCHED_SHOWS_TAG -> {
                    WatchedEpisodesFragment.newInstance()
                }
                ShowsMainActivity.UPCOMING_SHOWS_TAG -> {
                    ShowsUpcomingFragment.newInstance()
                }
                ShowsMainActivity.COLLECTED_SHOWS_TAG -> {
                    CollectedShowsFragment.newInstance()
                }
                ShowsMainActivity.PROGRESS_SHOWS_TAG -> {
                    ShowsProgressFragment.newInstance()
                }
                FRAGMENT_NO_LOGIN_TAG -> {
                    NotLoggedInFragment.newInstance()
                }

                else -> {
                    Log.e(TAG, "handlePrimaryFragmentSwitch: Tag $fragmentTag not recognised!")
                    null
                }
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(
                        bindings.splitviewactivityFirstContainer.id,
                        fragment,
                        PRIMARY_FRAGMENT_TAG
                    )
                    .setReorderingAllowed(true)
//                    .setPrimaryNavigationFragment(fragment)
                    .commit()
            }

        }
    }

    private fun handleSecondaryFragmentSwitch() {
        viewModel.currentSecondaryFragment.observe(this) { baseDataModel ->
            when (baseDataModel) {
                is MovieDataModel -> {
                    val currentTag = "movie"

                    val fragment = MovieDetailsFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putParcelable(MovieDetailsFragment.MOVIE_DATA_KEY, baseDataModel)
                    fragment.arguments = bundle

                    performFragmentNav(fragment, currentTag)

                }
                is ShowDataModel -> {
                    val currentTag = "show"

                    val fragment = ShowDetailsFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putParcelable(ShowDetailsFragment.SHOW_DATA_KEY, baseDataModel)
                    fragment.arguments = bundle

                    performFragmentNav(fragment, currentTag)
                }
                is SeasonDataModel -> {
                    val currentTag = "season"

                    val fragment = SeasonEpisodesFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putParcelable(SeasonEpisodesFragment.SEASON_DATA_KEY, baseDataModel)
                    fragment.arguments = bundle

                    performFragmentNav(fragment, currentTag)
                }
                is EpisodeDataModel -> {
                    val currentTag = "episode"

                    val fragment = EpisodePagerFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putParcelable(EpisodeDetailsFragment.EPISODE_DATA_KEY, baseDataModel)
                    fragment.arguments = bundle

                    performFragmentNav(fragment, currentTag)
                }
                is PersonDataModel -> {
                    val currentTag = "person"

                    val fragment = PersonOverviewFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putInt(PersonOverviewFragment.PERSON_ID_KEY, baseDataModel.traktId)
                    fragment.arguments = bundle

                    performFragmentNav(fragment, currentTag)
                }
            }

        }
    }

    private fun performFragmentNav(fragment: Fragment?, tag: String) {
        if (fragment != null) {
            if (isLandscape) {
                supportFragmentManager.beginTransaction()
                    .replace(bindings.splitviewactivitySecondContainer.id, fragment, tag)
                    .setReorderingAllowed(true)
                    .addToBackStack(SECONDARY_FRAGMENT_TAG)
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(bindings.splitviewactivityFirstContainer.id, fragment, tag)
                    .setReorderingAllowed(true)
                    .addToBackStack(SECONDARY_FRAGMENT_TAG)
                    .commit()
            }
        }
    }

    override fun enableOverviewLayout(isEnabled: Boolean) {
        if (isEnabled) {
            displaySecondLevelLayout()
        } else {
            if (!isLandscape) {
                bindings.splitviewactivityFirstContainer.visibility = View.VISIBLE
                bindings.splitviewactivitySecondContainer.visibility = View.GONE
            } else {
                bindings.splitviewactivityFirstContainer.visibility = View.VISIBLE
                bindings.splitviewactivitySecondContainer.visibility = View.VISIBLE
            }
            displayDrawerLayout()
        }
    }

    private fun displayDrawerLayout() {
        bindings.splitviewactivityHeader.visibility = View.VISIBLE
        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener(this)
    }

    private fun displaySecondLevelLayout() {
        // In landscape mode we do not show back arrow
        if (isLandscape) {
            return
        }

        bindings.splitviewactivityHeader.visibility = View.GONE


        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        toolbar.setNavigationOnClickListener {
            onSupportNavigateUp()
        }
    }

    override fun onRefresh() {
        val primaryFragment = supportFragmentManager.findFragmentById(bindings.splitviewactivityFirstContainer.id)
        val secondaryFragment = supportFragmentManager.findFragmentById(bindings.splitviewactivitySecondContainer.id)

        if(primaryFragment != null) {
            try {
                (primaryFragment as SwipeRefreshLayout.OnRefreshListener).onRefresh()
                Log.d(TAG, "onRefresh: Refresh called for ${primaryFragment.javaClass.name}")

            } catch (cce: ClassCastException) {
                Log.e(
                    TAG,
                    "onRefresh: Cannot cast ${primaryFragment.javaClass.name} to SwipeRefreshLayout.OnRefreshListener "
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "onRefresh: Primary Fragment is null, skipping refresh")
        }

        if(secondaryFragment != null) {
            try {
                (secondaryFragment as SwipeRefreshLayout.OnRefreshListener).onRefresh()
                Log.d(TAG, "onRefresh: Refresh called for ${secondaryFragment.javaClass.name}")

            } catch (cce: ClassCastException) {
                Log.e(
                    TAG,
                    "onRefresh: Cannot cast ${secondaryFragment.javaClass.name} to SwipeRefreshLayout.OnRefreshListener "
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "onRefresh: Secondary fragment is null, skipping refresh")
        }


//        try {
//            val secondaryFragment = supportFragmentManager.findFragmentByTag(SECONDARY_FRAGMENT_TAG)
//
//            if(secondaryFragment != null) {
//                (secondaryFragment as SwipeRefreshLayout.OnRefreshListener).onRefresh()
//            }
//
//
//        } catch(cce: ClassCastException) {
//            Log.e(TAG, "onRefresh: Cannot cast secondary fragment primary fragment to SwipeRefreshLayout.OnRefreshListener ")
//        } catch(e: Exception) {
//            e.printStackTrace()
//        }

        swipeRefreshLayout.isRefreshing = false

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()

        return super.onSupportNavigateUp()
    }

    override fun doSearchWithGenre(query: String, genre: String) {
        val searchResultsFragment = SearchResultsFragment.newInstance()
        val bundle = Bundle()
        bundle.putString(SearchManager.QUERY, query)
        bundle.putBoolean(SearchResultsActivity.SHOW_CHIPS, false)
        bundle.putString(SearchResultsActivity.GENRE_KEY, genre)

        searchResultsFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityFirstContainer.id, searchResultsFragment)
            .commit()
    }

    companion object {
        const val PRIMARY_FRAGMENT_TAG = "primary_fragment"
        const val SECONDARY_FRAGMENT_TAG = "secondary_fragment"
        const val FRAGMENT_NO_LOGIN_TAG = "not_logged_in_fragment"
    }

}