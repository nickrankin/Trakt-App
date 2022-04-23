package com.nickrankin.traktapp.ui.lists

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.lists.TraktListsAdapter
import com.nickrankin.traktapp.databinding.ActivityTraktListsBinding
import com.nickrankin.traktapp.databinding.AddListLayoutBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "TraktListsActivity"

@AndroidEntryPoint
class TraktListsActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: TraktListsViewModel by viewModels()

    private lateinit var swipeLayout: SwipeRefreshLayout

    private lateinit var recyclerView: RecyclerView
    private lateinit var traktListsAdapter: TraktListsAdapter

    private lateinit var addListFab: FloatingActionButton
    private lateinit var addListDialog: AlertDialog

    private lateinit var bindings: ActivityTraktListsBinding

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityTraktListsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        swipeLayout = bindings.traktlistsactivitySwipeRefresh
        swipeLayout.setOnRefreshListener(this)

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.title = "My Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initRecycler()

        addListFab = bindings.traktlistsactivityAddList
        createAddToListDialog()

        addListFab.setOnClickListener { addListDialog.show() }

        getLists()

        getEvents()
    }

    private fun getLists() {
        lifecycleScope.launchWhenStarted {
            viewModel.lists.collectLatest { listsResource ->
                when (listsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getLists: Loading ...")
                    }
                    is Resource.Success -> {
                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.d(TAG, "getLists: Got ${listsResource.data?.size} lists")

                        traktListsAdapter.submitList(listsResource.data)

                    }
                    is Resource.Error -> {
                        if(swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.e(TAG, "getLists: Error getting list ${listsResource.error?.message}")
                        listsResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is TraktListsViewModel.Event.AddListEvent -> {
                        when (val listResource = event.addedListResource) {
                            is Resource.Success -> {
                                displayMessageToast("Added list ${listResource.data?.name} successfully", Toast.LENGTH_SHORT)
                            }
                            is Resource.Error -> {
                                displayMessageToast("Error adding list. ${listResource.error?.localizedMessage}", Toast.LENGTH_LONG)
                                listResource.error?.printStackTrace()
                                Log.e(
                                    TAG,
                                    "getEvents: Error adding list ${listResource.error?.message}",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings.traktlistsactivityRecyclerview

        val lm = LinearLayoutManager(this)

        traktListsAdapter = TraktListsAdapter(sharedPreferences)

        recyclerView.layoutManager = lm
        recyclerView.adapter = traktListsAdapter
    }

    private fun createAddToListDialog() {

        val addNewListLayout = AddListLayoutBinding.inflate(layoutInflater)

        val listNameEditText: EditText = addNewListLayout.newlistName
        val listDescriptionEditText: EditText = addNewListLayout.newlistDesc
        val listAllowCommentsSwitch: SwitchCompat = addNewListLayout.newlistAllowComments
        val listDisplayNumbersSwitch: SwitchCompat = addNewListLayout.newlistDisplayNumbers
        val listPrivacySpinner: Spinner = addNewListLayout.newlistPrivacy
        val listSortBySpinner: Spinner = addNewListLayout.newlistSortBy
        val listSortHowSpinner: Spinner = addNewListLayout.newlistSortHow

        val saveListButton: Button = addNewListLayout.newlistSave


        createSpinnerArrayAdapter(R.array.privacy_array, listPrivacySpinner)
        createSpinnerArrayAdapter(R.array.sort_by, listSortBySpinner)
        createSpinnerArrayAdapter(R.array.sort_how, listSortHowSpinner)

        addListDialog = AlertDialog.Builder(this)
            .setTitle("New List")
            .setView(addNewListLayout.root)
            .create()

        saveListButton.setOnClickListener {
            Log.d(TAG, "createAddToListDialog: List button save checked")

            val traktList = TraktList()
            traktList.name = listNameEditText.text.toString()
            traktList.description = listDescriptionEditText.text.toString()
            traktList.allow_comments = listAllowCommentsSwitch.isChecked
            traktList.display_numbers = listDisplayNumbersSwitch.isChecked

            // Error validation on the Name EditText and Spinners.
            if (listNameEditText.text.isEmpty() || listPrivacySpinner.selectedItemPosition == 0 || listSortBySpinner.selectedItemPosition == 0 || listSortHowSpinner.selectedItemPosition == 0) {
                Log.e(TAG, "createAddToListDialog: Validation error occurred.", )
                if (listNameEditText.text.isEmpty()) {
                    listNameEditText.error = "Enter a name for your new list!"
                }
                if (listPrivacySpinner.selectedItemPosition == 0) {
                    val selectedView: TextView = listPrivacySpinner.selectedView as TextView
                    selectedView.error = "Choose a privacy option"
                }
                if (listSortBySpinner.selectedItemPosition == 0) {
                    val selectedView: TextView = listSortBySpinner.selectedView as TextView
                    selectedView.error = "Choose a Sort By option"
                }
                if (listSortHowSpinner.selectedItemPosition == 0) {
                    val selectedView: TextView = listSortHowSpinner.selectedView as TextView
                    selectedView.error = "Choose a Sort How option"
                }
            } else {
                traktList.privacy = ListPrivacy.valueOf(listPrivacySpinner.selectedItem.toString())
                traktList.sort_by = SortBy.valueOf(listSortBySpinner.selectedItem.toString())
                traktList.sort_how = SortHow.valueOf(listSortHowSpinner.selectedItem.toString())
                Log.d(TAG, "createAddToListDialog: List saved successfully ")
                viewModel.addList(traktList)

                addListDialog.dismiss()
            }
        }
    }

    private fun createSpinnerArrayAdapter(@ArrayRes textArrayResId: Int, spinner: Spinner) {
        ArrayAdapter.createFromResource(
            this, textArrayResId, android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()

        return true
    }

    private fun displayMessageToast(text: String, length: Int) {
        Toast.makeText(this, text, length).show()
    }
}