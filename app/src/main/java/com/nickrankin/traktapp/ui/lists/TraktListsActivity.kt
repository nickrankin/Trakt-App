package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nickrankin.traktapp.BaseActivity
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.lists.TraktListsAdapter
import com.nickrankin.traktapp.databinding.ActivityTraktListsBinding
import com.nickrankin.traktapp.databinding.AddListLayoutBinding
import com.nickrankin.traktapp.helper.OnTitleChangeListener
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "TraktListsActivity"

@AndroidEntryPoint
class TraktListsActivity : BaseActivity(), OnTitleChangeListener {


    private lateinit var bindings: ActivityTraktListsBinding
    private val viewModel: TraktListsViewModel by viewModels()
    private var listsFragment: ListsFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityTraktListsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.title = "My Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        displayAllLists()

        getActiveList()
    }

    private fun getActiveList() {

        lifecycleScope.launchWhenStarted {
            viewModel.activeList.collectLatest { activeListId ->
                if(activeListId != null) {

                    navigateList(activeListId)
                } else {
                    supportFragmentManager.popBackStack()

                    displayAllLists()
                }
            }
        }
    }

    private fun navigateList(traktListId: Int) {
        val listItemsFragment = ListItemsFragment.newInstance()

        val bundle = Bundle()

        listItemsFragment.arguments = bundle
        bundle.putInt(TraktListsViewModel.LIST_ID_KEY, traktListId)

        supportFragmentManager.beginTransaction()
            .replace(bindings.traktlistsactivityFragmentContainer.id, listItemsFragment)
            .addToBackStack("list_items")
            .commit()
    }

    private fun displayAllLists() {
        supportFragmentManager.beginTransaction()
            .replace(bindings.traktlistsactivityFragmentContainer.id, getListsFragment())
            .commit()
    }

    override fun onTitleChanged(newTitle: String) {
        supportActionBar?.title = newTitle
    }

    override fun onSupportNavigateUp(): Boolean {
        viewModel.switchList(null)
        onBackPressed()
        return true
    }

    private fun getListsFragment(): ListsFragment {
        if(listsFragment == null) {
            listsFragment = ListsFragment.newInstance()
        }

        return listsFragment!!
    }

    override fun onDestroy() {
        super.onDestroy()
        
        listsFragment = null
    }

    override fun onBackPressed() {
        // Need to use getId on fragment to solve configuration change bug in popBackStack method
        // https://stackoverflow.com/questions/40834313/supportfragmentmanager-popbackstack-screen-rotation
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(getSupportFragmentManager()
                .getBackStackEntryAt(0).getId(),
                FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else {
            super.onBackPressed();
        }
    }
}