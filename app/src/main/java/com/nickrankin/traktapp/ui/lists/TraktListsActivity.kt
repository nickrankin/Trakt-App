package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
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
import com.nickrankin.traktapp.model.lists.ListEntryViewModel
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

    private lateinit var progressBar: ProgressBar
    private lateinit var addListFab: FloatingActionButton

    private lateinit var addListDialog: AddEditListDialog

    private lateinit var bindings: ActivityTraktListsBinding

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivityTraktListsBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        swipeLayout = bindings.traktlistsactivitySwipeRefresh
        swipeLayout.setOnRefreshListener(this)

        progressBar = bindings.traktlistsactivityProgressbar

        setSupportActionBar(bindings.toolbarLayout.toolbar)

        supportActionBar?.title = "My Lists"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initRecycler()

        addListFab = bindings.traktlistsactivityAddList
        createAddToListDialog()

        addListFab.setOnClickListener { addListDialog.add() }

        getLists()

        getEvents()
    }

    private fun getLists() {
        lifecycleScope.launchWhenStarted {
            viewModel.lists.collectLatest { listsResource ->
                when (listsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getLists: Loading ...")
                        progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }
                        Log.d(TAG, "getLists: Got ${listsResource.data?.size} lists")

                        traktListsAdapter.submitList(listsResource.data)

                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
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
                                displayMessageToast(
                                    "Added list ${listResource.data?.name} successfully",
                                    Toast.LENGTH_SHORT
                                )
                            }
                            is Resource.Error -> {
                                displayMessageToast(
                                    "Error adding list. ${listResource.error?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                )
                                listResource.error?.printStackTrace()
                                Log.e(
                                    TAG,
                                    "getEvents: Error adding list ${listResource.error?.message}",
                                )
                            }
                        }
                    }
                    is TraktListsViewModel.Event.EditListEvent -> {
                        when (val listResource = event.editListResource) {
                            is Resource.Success -> {
                                displayMessageToast(
                                    "Successfully edited list ${listResource.data?.name}",
                                    Toast.LENGTH_SHORT
                                )
                            }
                            is Resource.Error -> {
                                displayMessageToast(
                                    "Error editing list. ${listResource.error?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                )
                                listResource.error?.printStackTrace()
                                Log.e(
                                    TAG,
                                    "getEvents: Error editing list ${listResource.error?.message}",
                                )
                            }
                        }
                    }
                    is TraktListsViewModel.Event.DeleteListEvent -> {
                        when (val listResource = event.deleteListResource) {
                            is Resource.Success -> {
                                displayMessageToast(
                                    "Successfully removed list.",
                                    Toast.LENGTH_SHORT
                                )
                            }
                            is Resource.Error -> {
                                displayMessageToast(
                                    "Error removing list. ${listResource.error?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                )
                                listResource.error?.printStackTrace()
                                Log.e(
                                    TAG,
                                    "getEvents: Error removing list ${listResource.error?.message}",
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

        traktListsAdapter = TraktListsAdapter(sharedPreferences) { action, selectedList ->

            when (action) {
                TraktListsAdapter.OPEN_LIST -> {
                    val intent = Intent(this, ListItemsActivity::class.java)
                    intent.putExtra(ListEntryViewModel.LIST_ID_KEY, selectedList.trakt_id)
                    intent.putExtra(ListEntryViewModel.LIST_NAME_KEY, selectedList.name)
                    startActivity(intent)
                }
                TraktListsAdapter.EDIT_LIST -> {
                    addListDialog.edit(selectedList)
                }
                TraktListsAdapter.DELETE_LIST -> {
                    deleteList(selectedList)
                }
                else -> {
                    val intent = Intent(this, ListItemsActivity::class.java)
                    intent.putExtra(ListEntryViewModel.LIST_ID_KEY, selectedList.trakt_id)
                    intent.putExtra(ListEntryViewModel.LIST_NAME_KEY, selectedList.name)
                    startActivity(intent)
                }

            }

        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = traktListsAdapter
    }

    private fun deleteList(traktList: com.nickrankin.traktapp.dao.lists.model.TraktList) {
        AlertDialog.Builder(this)
            .setTitle("Delete List: ${traktList.name}")
            .setMessage("Are you sure you wish to delete the list: ${traktList.name}? This will also remove all ${traktList.item_count} items from the list.")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.deleteList(traktList.trakt_id.toString())
                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()
            .show()
    }

    private fun createAddToListDialog() {

        val addNewListLayout = AddListLayoutBinding.inflate(layoutInflater)

        addListDialog = AddEditListDialog(addNewListLayout)

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

    inner class AddEditListDialog(addNewListLayout: AddListLayoutBinding) : AlertDialog(this) {

        val listNameEditText: EditText = addNewListLayout.newlistName
        val listDescriptionEditText: EditText = addNewListLayout.newlistDesc
        val listAllowCommentsSwitch: SwitchCompat = addNewListLayout.newlistAllowComments
        val listDisplayNumbersSwitch: SwitchCompat = addNewListLayout.newlistDisplayNumbers
        val listPrivacySpinner: Spinner = addNewListLayout.newlistPrivacy
        val listSortBySpinner: Spinner = addNewListLayout.newlistSortBy
        val listSortHowSpinner: Spinner = addNewListLayout.newlistSortHow

        val saveListButton: Button = addNewListLayout.newlistSave

        init {
            setView(addNewListLayout.root)

            createSpinnerArrayAdapter(R.array.privacy_array, listPrivacySpinner)
            createSpinnerArrayAdapter(R.array.sort_by, listSortBySpinner)
            createSpinnerArrayAdapter(R.array.sort_how, listSortHowSpinner)

            create()
        }

        fun add() {
            //setTitle("New List")

            clearFields()

            saveListButton.setOnClickListener {
                Log.d(TAG, "createAddToListDialog: List button save checked")
                saveList(false, null)
            }

            show()
        }

        fun edit(traktList: com.nickrankin.traktapp.dao.lists.model.TraktList?) {
            //setTitle("Edit ${traktList?.name ?: "Unknown List"}")

            listNameEditText.text = SpannableStringBuilder(traktList?.name)
            listDescriptionEditText.text = SpannableStringBuilder(traktList?.description)
            listAllowCommentsSwitch.isActivated = traktList?.allow_comments ?: false
            listDisplayNumbersSwitch.isActivated = traktList?.display_numbers ?: false

            listPrivacySpinner.setSelection(
                getSelectedAdapterPosition(
                    listPrivacySpinner,
                    traktList?.privacy?.name
                )
            )
            listSortBySpinner.setSelection(
                getSelectedAdapterPosition(
                    listSortBySpinner,
                    traktList?.sortBy?.name
                )
            )
            listSortHowSpinner.setSelection(
                getSelectedAdapterPosition(
                    listSortHowSpinner,
                    traktList?.sortHow?.name
                )
            )

            saveListButton.setOnClickListener {
                saveList(true, traktList?.list_slug)
            }

            show()
        }

        private fun saveList(isEdit: Boolean, listSLug: String?) {
            val traktList = TraktList()
            traktList.name = listNameEditText.text.toString()
            traktList.description = listDescriptionEditText.text.toString()
            traktList.allow_comments = listAllowCommentsSwitch.isChecked
            traktList.display_numbers = listDisplayNumbersSwitch.isChecked


            traktList.privacy = ListPrivacy.valueOf(listPrivacySpinner.selectedItem.toString())
            traktList.sort_by = SortBy.valueOf(listSortBySpinner.selectedItem.toString())
            traktList.sort_how = SortHow.valueOf(listSortHowSpinner.selectedItem.toString())

            if (validateFields()) {
                if (isEdit) {
                    viewModel.editList(traktList, listSLug ?: "NULL")
                } else {
                    Log.d(TAG, "createAddToListDialog: List saved successfully ")
                    viewModel.addList(traktList)
                }
            }

            addListDialog.dismiss()
        }

        private fun clearFields() {
            listNameEditText.text.clear()
            listDescriptionEditText.text.clear()

            listAllowCommentsSwitch.isActivated = false
            listDisplayNumbersSwitch.isActivated = false

            listPrivacySpinner.setSelection(0)
            listSortBySpinner.setSelection(0)
            listSortHowSpinner.setSelection(0)
        }

        private fun validateFields(): Boolean {
            return if (listNameEditText.text.isEmpty() || listPrivacySpinner.selectedItemPosition == 0 || listSortBySpinner.selectedItemPosition == 0 || listSortHowSpinner.selectedItemPosition == 0) {
                Log.e(TAG, "createAddToListDialog: Validation error occurred.")
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
                false
            } else {
                true
            }
        }

        private fun getSelectedAdapterPosition(spinner: Spinner, value: String?): Int {
            val adapter: ArrayAdapter<String> = spinner.adapter as ArrayAdapter<String>

            return if (value != null) adapter.getPosition(value) else 0
        }

        private fun createSpinnerArrayAdapter(@ArrayRes textArrayResId: Int, spinner: Spinner) {
            ArrayAdapter.createFromResource(
                context, textArrayResId, android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                spinner.adapter = adapter
            }
        }

    }
}