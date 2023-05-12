package com.nickrankin.traktapp.ui.lists

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.SplitViewActivity
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
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