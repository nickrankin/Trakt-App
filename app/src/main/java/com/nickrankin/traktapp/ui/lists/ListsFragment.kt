package com.nickrankin.traktapp.ui.lists

import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.lists.TraktListsAdapter
import com.nickrankin.traktapp.databinding.AddListLayoutBinding
import com.nickrankin.traktapp.databinding.FragmentListsBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.lists.TraktListsViewModel
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "ListsFragment"
@AndroidEntryPoint
class ListsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var _bindings: FragmentListsBinding? = null
    private val bindings get() = _bindings!!

    private val viewModel: TraktListsViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var traktListsAdapter: TraktListsAdapter

    private lateinit var addListFab: FloatingActionButton

    private lateinit var addListDialog: AddEditListDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = FragmentListsBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val swipeLayout = bindings.traktlistsfragmentSwipeLayout
        swipeLayout.setOnRefreshListener(this)

        initRecycler()

        addListFab = bindings.traktlistsfragmentAddList
        createAddToListDialog()

        addListFab.setOnClickListener { addListDialog.add() }

        updateTitle("My Lists")

        setHasOptionsMenu(true)

        if(!isLoggedIn) {
            handleLoggedOutState(this.id)
        }

        getLists()
        getEvents()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater
            .inflate(R.menu.lists_menu, menu)
    }

    private fun getLists() {
        val progressBar = bindings.traktlistsfragmentProgressbar
        val messageContainerTextView = bindings.traktlistsfragmentMessageContainer
        val swipeLayout = bindings.traktlistsfragmentSwipeLayout

        lifecycleScope.launchWhenStarted {
            viewModel.lists.collectLatest { listsResource ->
                when (listsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getLists: Loading ...")
                        progressBar.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        val lists = listsResource.data ?: emptyList()

                        Log.d(TAG, "getLists: Got ${lists.size} lists")

                        if(lists.isNotEmpty()) {
                            messageContainerTextView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            traktListsAdapter.submitList(lists)
                        } else {
                            messageContainerTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE

                            messageContainerTextView.text = "You have no lists yet. Why not make one?"
                        }


                    }
                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        if (swipeLayout.isRefreshing) {
                            swipeLayout.isRefreshing = false
                        }

                        val lists = listsResource.data ?: emptyList()

                        if(lists.isNotEmpty()) {
                            messageContainerTextView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            traktListsAdapter.submitList(lists)
                        } else {
                            messageContainerTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE

                            messageContainerTextView.text = "You have no lists yet. Why not make one?"
                        }

                        (activity as IHandleError).showErrorSnackbarRetryButton(listsResource.error, bindings!!.traktlistsfragmentSwipeLayout) {
                            viewModel.onRefresh()
                        }
                    }
                    else -> {}
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
                                (activity as IHandleError).handleError(event.addedListResource.error, "Error adding list")
                            }
                            else -> {}

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
                                (activity as IHandleError).handleError(event.editListResource.error, "Error editing list")
                            }
                            else -> {}

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
                                (activity as IHandleError).handleError(event.deleteListResource.error, "Error removing list")

                            }
                            else -> {}
                        }
                    }
                    else -> {}

                }
            }
        }
    }

    private fun initRecycler() {
        recyclerView = bindings!!.traktlistsfragmentRecyclerview

        val lm = LinearLayoutManager(requireContext())

        traktListsAdapter = TraktListsAdapter(sharedPreferences) { action, selectedList ->



            when (action) {
                TraktListsAdapter.OPEN_LIST -> {
                    viewModel.switchList(selectedList.trakt_id)
                }
                TraktListsAdapter.EDIT_LIST -> {
                    addListDialog.edit(selectedList)
                }
                TraktListsAdapter.DELETE_LIST -> {
                    deleteList(selectedList)
                }
                else -> {
//                    fragmentManager.beginTransaction()
//                        .replace(this.id, listItemsFragment)
//                        .addToBackStack("list_items")
//                        .commit()
                }

            }

        }

        recyclerView.layoutManager = lm
        recyclerView.adapter = traktListsAdapter
    }

    private fun deleteList(traktList: com.nickrankin.traktapp.dao.lists.model.TraktList) {
        AlertDialog.Builder(requireContext())
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

    private fun displayMessageToast(text: String, length: Int) {
        Toast.makeText(requireContext(), text, length).show()
    }

    inner class AddEditListDialog(addNewListLayout: AddListLayoutBinding) : AlertDialog(requireContext()) {

        val listNameEditText: EditText = addNewListLayout.newlistName
        val listDescriptionEditText: EditText = addNewListLayout.newlistDesc
        val listAllowCommentsSwitch: SwitchCompat = addNewListLayout.newlistAllowComments
        val listDisplayNumbersSwitch: SwitchCompat = addNewListLayout.newlistDisplayNumbers
        val listPrivacySpinner: Spinner = addNewListLayout.newlistPrivacy

        val saveListButton: Button = addNewListLayout.newlistSave

        init {
            setView(addNewListLayout.root)

            createSpinnerArrayAdapter(R.array.privacy_array, listPrivacySpinner)
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
                    traktList?.privacy?.name ?: ListPrivacy.PRIVATE.name
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

            if (validateFields()) {

                traktList.privacy = ListPrivacy.valueOf(listPrivacySpinner.selectedItem.toString())
                traktList.sort_by = SortBy.ADDED
                traktList.sort_how = SortHow.DESC

                addListDialog.dismiss()

                if (isEdit) {
                    viewModel.editList(traktList, listSLug ?: "NULL")
                } else {
                    Log.d(TAG, "createAddToListDialog: List saved successfully ")
                    viewModel.addList(traktList)
                }
            }




        }

        private fun clearFields() {
            listNameEditText.text.clear()
            listDescriptionEditText.text.clear()

            listAllowCommentsSwitch.isActivated = false
            listDisplayNumbersSwitch.isActivated = false

            listPrivacySpinner.setSelection(0)
        }

        private fun validateFields(): Boolean {
            return if (listNameEditText.text.isEmpty() || listPrivacySpinner.selectedItemPosition == 0) {
                Log.e(TAG, "createAddToListDialog: Validation error occurred.")
                if (listNameEditText.text.isEmpty()) {
                    listNameEditText.error = "Enter a name for your new list!"
                }
                if (listPrivacySpinner.selectedItemPosition == 0) {
                    val selectedView: TextView = listPrivacySpinner.selectedView as TextView
                    selectedView.error = "Choose a privacy option"
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.listsmenu_sort_created -> {
                viewModel.changeOrdering(TraktListsViewModel.SORT_MENU_CREATED)
            }
            R.id.listsmenu_sort_numitems -> {
                viewModel.changeOrdering(TraktListsViewModel.SORT_MENU_NUM_ITEMS)

            }
            R.id.listsmenu_sort_name -> {
                viewModel.changeOrdering(TraktListsViewModel.SORT_MENU_TITLE)

            }
            else -> {
                Log.e(TAG, "onOptionsItemSelected: Invalid menu item ${item.itemId}", )
            }
        }

        return false
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModel.onRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }


    companion object {
        @JvmStatic
        fun newInstance() = ListsFragment()
    }
}