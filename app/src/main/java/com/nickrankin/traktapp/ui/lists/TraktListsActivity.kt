package com.nickrankin.traktapp.ui.lists

import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.OnNavigateToEntity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.SplitViewActivity
import com.nickrankin.traktapp.adapter.lists.TraktListsAdapter
import com.nickrankin.traktapp.databinding.ActivitySplitviewBinding
import com.nickrankin.traktapp.databinding.ActivityTraktListsBinding
import com.nickrankin.traktapp.databinding.AddListLayoutBinding
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.nickrankin.traktapp.ui.OnSearchByGenre
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsFragment
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import com.nickrankin.traktapp.ui.search.SearchResultsActivity
import com.nickrankin.traktapp.ui.search.SearchResultsFragment
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsFragment
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "TraktListsActivity"

@AndroidEntryPoint
class TraktListsActivity : SplitViewActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: TraktListsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "My Lists"

        displayAllLists()
        setupDrawerLayout()

        getActiveList()
    }

    private fun getActiveList() {

        lifecycleScope.launchWhenStarted {
            viewModel.activeList.collectLatest { activeListId ->

                Log.e(TAG, "getActiveList: Active list is $activeListId")

                if(activeListId != null) {
                    navigateList(activeListId)
                }
            }
        }
    }

    private fun setupDrawerLayout() {
        setSupportActionBar(toolbar)

        navView = bindings.splitviewactivityNavView
        drawerLayout = bindings.splitviewactivityDrawer

        navView.setNavigationItemSelectedListener(this)
    }



    private fun navigateList(traktListId: Int) {
        val listItemsFragment = ListItemsFragment.newInstance()

        val bundle = Bundle()

        listItemsFragment.arguments = bundle
        bundle.putInt(TraktListsViewModel.LIST_ID_KEY, traktListId)

        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityFirstContainer.id, listItemsFragment)
            .commit()
    }

    private fun displayAllLists() {
        supportFragmentManager.beginTransaction()
            .replace(bindings.splitviewactivityFirstContainer.id, ListsFragment.newInstance())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }


    override fun onRefresh() {
        super.onRefresh()
    }

    override fun onBackPressed() {
//        // Need to use getId on fragment to solve configuration change bug in popBackStack method
//        // https://stackoverflow.com/questions/40834313/supportfragmentmanager-popbackstack-screen-rotation
//        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
//            getSupportFragmentManager().popBackStack(getSupportFragmentManager()
//                .getBackStackEntryAt(0).getId(),
//                FragmentManager.POP_BACK_STACK_INCLUSIVE)
//        } else {
//            super.onBackPressed();
//        }
    }
}