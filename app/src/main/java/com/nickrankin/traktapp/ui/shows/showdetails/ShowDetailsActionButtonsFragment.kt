package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.Response
import com.nickrankin.traktapp.helper.getSyncResponse
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"
@AndroidEntryPoint
class ShowDetailsActionButtonsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: ShowDetailsActionButtonsViewModel by activityViewModels()

    private lateinit var bindings: ActionButtonsFragmentBinding

    private lateinit var collectedShowButton: RelativeLayout
    private lateinit var addCollectionProgressBar: ProgressBar

    // Dialogs
    private lateinit var addToCollectionDialog: AlertDialog
    private lateinit var removeFromCollectionDialog: AlertDialog
    private lateinit var ratingDialog: RatingPickerFragment
    private lateinit var listsDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ActionButtonsFragmentBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isLoggedIn) {
            // Buttons
            setupRatingButton()
            setupCollectionButton()
            setupCollectedButton()

            // Get Data
            getCollectedShowStatus()
        }
    }

    private fun setupCollectedButton() {
        collectedShowButton = bindings.actionbuttonAddToCollection
        addCollectionProgressBar = bindings.actionButtonAddToCollectionProgressbar
    }

    fun initFragment(tmShow: TmShow?) {
        if(tmShow == null) {
            return
        }

        initRatingsDialog(tmShow)
        initAddToCollectionDialogs(tmShow)
        getRatings(tmShow)
        getEvents(tmShow)
        setupAddListsButton(tmShow)

    }

    private fun setupAddListsButton(tmShow: TmShow) {
        val addListsButton = bindings.actionbuttonLists
        addListsButton.visibility = View.VISIBLE

        setupListsDialog(tmShow)

        Log.d(TAG, "setupAddListsButton: HERE")
        addListsButton.setOnClickListener {
            listsDialog.show()
        }
    }

    private fun setupRatingButton() {
        val ratingButton = bindings.actionbuttonRate
        ratingButton.visibility = View.VISIBLE

        // Initial text
        bindings.actionbuttonRateText.text = " - "

        ratingButton.setOnClickListener {
            ratingDialog.show(requireActivity().supportFragmentManager, "rating_dialog")
        }
    }

    private fun setupCollectionButton() {
        val collectionButton = bindings.actionbuttonAddToCollection
        collectionButton.visibility = View.VISIBLE
    }

    private fun getRatings(tmShow: TmShow) {
        lifecycleScope.launchWhenStarted {
            viewModel.getRatings(tmShow.trakt_id).collectLatest { ratingStats ->

                val rating = ratingStats?.rating

                if(rating != null && rating != 0) {
                    bindings.actionbuttonRateText.text = "$rating"
                } else {
                    bindings.actionbuttonRateText.text = " - "
                }
            }
        }

    }

    private fun getCollectedShowStatus() {
        val collectedShowButton = bindings.actionbuttonAddToCollection
        val collectedShowButtonText = bindings.actionbuttonCollectedTextview

        lifecycleScope.launchWhenStarted {
            val isShowCollected = viewModel.showsCollectedStatus.collectLatest { isShowCollected ->
                Log.d(TAG, "getCollectedShowStatus: Show collection status is $isShowCollected")

                if(isShowCollected) {
                    collectedShowButtonText.text = "Remove from Collection"
                    collectedShowButton.setOnClickListener {
                        removeFromCollectionDialog.show()
                    }
                } else {
                    collectedShowButtonText.text = "Add To Collection"
                    collectedShowButton.setOnClickListener {
                        addToCollectionDialog.show()
                    }
                }
            }
        }
    }

    private fun initAddToCollectionDialogs(tmShow: TmShow) {
        val showTitle = tmShow.name

        // Add to Collection
        addToCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to add $showTitle to your collection?")
            .setMessage("Do you want to add $showTitle to your Trakt Collection?")
            .setPositiveButton("Add", DialogInterface.OnClickListener { dialogInterface, i ->
                collectedShowButton.isEnabled = false
                addCollectionProgressBar.visibility = View.VISIBLE

                viewModel.addToCollection(tmShow)
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        // Remove from collection
        removeFromCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to remove $showTitle to your collection?")
            .setMessage("Do you want to remove $showTitle to your Trakt Collection?")
            .setPositiveButton("Remove", DialogInterface.OnClickListener { dialogInterface, i ->
                collectedShowButton.isEnabled = false
                addCollectionProgressBar.visibility = View.VISIBLE

                viewModel.removeFromCollection()
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()
    }

    private fun initRatingsDialog(tmShow: TmShow) {
        val showTitle = tmShow.name

        ratingDialog = RatingPickerFragment(callback = {newRating ->
            Log.d(TAG, "initDialogs: Updating rating with $newRating")
            bindings.actionbuttonRate.isEnabled = false
            bindings.actionButtonRateProgressbar.visibility = View.VISIBLE

            if(newRating != -1) {
                Log.d(TAG, "Calling viewModel.updateRating")
                viewModel.addRating(tmShow, newRating)
            } else {
                Log.d(TAG, "initDialogs: Deleting rating")
                viewModel.deleteRating(tmShow)
            }
        }, showTitle)
    }

    private fun getEvents(tmShow: TmShow) {
        val showTitle = tmShow.name

        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is ShowDetailsActionButtonsViewModel.Event.AddToCollectionEvent -> {
                        val syncResponseResource = event.syncResponse

                        // Disable progressbar and re-enable button
                        bindings.actionbuttonAddToCollection.isEnabled = true
                        bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                        if(syncResponseResource is Resource.Success) {

                            when(getSyncResponse(syncResponseResource.data, Type.SHOW)) {
                                Response.ADDED_OK -> {
                                    displayMessageToast("Successfully added $showTitle to your Trakt collection!", Toast.LENGTH_SHORT)

                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current item was not found on Trakt", Toast.LENGTH_SHORT)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Did  not add $showTitle to your Trakt collection", Toast.LENGTH_SHORT)

                                }
                                else -> {}
                            }
                        } else if(syncResponseResource is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error adding $showTitle to your collection.")
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.RemoveFromCollectionEvent -> {

                        // Reenable button and hide progressbar
                        bindings.actionbuttonAddToCollection.isEnabled = true
                        bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                        val syncResponseResource = event.syncResponse
                        if(syncResponseResource is Resource.Success) {

                            when(getSyncResponse(syncResponseResource.data, Type.SHOW)) {
                                Response.DELETED_OK -> {
                                    displayMessageToast("Successfully removed $showTitle from your Trakt collection!", Toast.LENGTH_SHORT)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current item was not found on Trakt", Toast.LENGTH_SHORT)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Did  not remove $showTitle from your Trakt collection", Toast.LENGTH_SHORT)

                                }

                                else -> {}
                            }
                        } else if(syncResponseResource is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error removing $showTitle from your Trakt collection!")
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.AddRatingEvent -> {
                        val syncResponseResource = event.syncResponse

                        bindings.actionbuttonRate.isEnabled = true
                        bindings.actionButtonRateProgressbar.visibility = View.GONE

                        if(syncResponseResource is Resource.Success) {

                            when(getSyncResponse(syncResponseResource.data, Type.SHOW)) {
                                Response.ADDED_OK -> {
                                    displayMessageToast("Rated $showTitle with ${ event.newRating } (${ Rating.fromValue(event.newRating).name }) successfully!", Toast.LENGTH_SHORT)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current item was not found on Trakt", Toast.LENGTH_SHORT)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Error rating $showTitle.", Toast.LENGTH_SHORT)
                                }
                                else -> {

                                }
                            }

                        } else if(syncResponseResource is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error rating $showTitle.")
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.DeleteRatingEvent -> {
                        val syncResponseResource = event.syncResponse

                        bindings.actionbuttonRate.isEnabled = true
                        bindings.actionButtonRateProgressbar.visibility = View.GONE

                        if(syncResponseResource is Resource.Success) {

                            when(getSyncResponse(syncResponseResource.data, Type.SHOW)) {
                                Response.ADDED_OK -> {
                                    displayMessageToast("Reset $showTitle Rating successfully!", Toast.LENGTH_SHORT)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current item was not found on Trakt", Toast.LENGTH_SHORT)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Error resetting the rating for $showTitle.", Toast.LENGTH_SHORT)
                                }
                                else -> {

                                }
                            }

                            displayMessageToast("Reset $showTitle Rating successfully!", Toast.LENGTH_SHORT)
                        } else if(syncResponseResource is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error resetting rating $showTitle.")

                        }
                    }
                    else -> {}

                }
            }
        }
    }

    private fun setupListsDialog(tmShow: TmShow) {
        Log.e(TAG, "setupListsDialog: Called", )
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        listsDialog = AlertDialog.Builder(requireContext())
            .setTitle("Lists")
            .setPositiveButton("Close", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .setView(layout)
            .create()

        lifecycleScope.launchWhenStarted {
            viewModel.listsWithEntries.collectLatest { listEntries->
                layout.removeAllViews()
                listEntries.map { listWithEntries ->
                    val checkbox = CheckBox(layout.context)
                    checkbox.text = listWithEntries.list.name

                    // If we find current movie in list, checkbox should be ticked
                    checkbox.isChecked = listWithEntries.entries.find {
                        Log.d(TAG, "setupListsDialog: List Entry TraktId: ${it?.list_entry_trakt_id} TRAKT ID: ${tmShow.trakt_id}")
                        it?.list_entry_trakt_id == tmShow.trakt_id } != null

                    checkbox.setOnClickListener {
                        val checkbox = it as CheckBox

                        if(checkbox.isChecked) {
                            viewModel.addListEntry("show", tmShow.trakt_id, listWithEntries.list)
                        } else {
                            viewModel.removeListEntry(listWithEntries.list.trakt_id, tmShow.trakt_id, Type.SHOW)
                        }
                    }

                    layout.addView(checkbox)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        viewModel.onRefresh()
    }

    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    companion object {
        fun newInstance() = ShowDetailsActionButtonsFragment()
    }

}