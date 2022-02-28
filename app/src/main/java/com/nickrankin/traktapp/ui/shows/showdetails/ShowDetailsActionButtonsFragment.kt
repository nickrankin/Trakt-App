package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"
@AndroidEntryPoint
class ShowDetailsActionButtonsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: ShowDetailsActionButtonsViewModel by activityViewModels()

    @Inject
    lateinit var shedPreferences: SharedPreferences

    private lateinit var bindings: ActionButtonsFragmentBinding

    private var isLoggedIn = false

    private lateinit var showTitle: String

    // Dialogs
    private lateinit var addToCollectionDialog: AlertDialog
    private lateinit var removeFromCollectionDialog: AlertDialog
    private lateinit var ratingDialog: RatingPickerFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ActionButtonsFragmentBinding.inflate(inflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isLoggedIn = shedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        showTitle = activity?.intent?.getStringExtra(ShowDetailsRepository.SHOW_TITLE_KEY) ?: "Unknown"

        if(isLoggedIn) {
            // Dialogs
            initAddToCollectionDialogs()
            initRatingsDialog()

            // Buttons
            setupRatingButton()
            setupCollectionButton()

            // Get Data
            getRatings()
            getCollectedShowStatus()
            getEvents()
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

    private fun getRatings() {
        viewModel.ratings.observe(viewLifecycleOwner) { rating ->
            if(rating != null) {
                bindings.actionbuttonRateText.text = "$rating"
            } else {
                bindings.actionbuttonRateText.text = " - "

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

    private fun initAddToCollectionDialogs() {
        val collectedShowButton = bindings.actionbuttonAddToCollection
        val addCollectionProgressBar = bindings.actionButtonAddToCollectionProgressbar

        // Add to Collection
        addToCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to add $showTitle to your collection?")
            .setMessage("Do you want to add $showTitle to your Trakt Collection?")
            .setPositiveButton("Add", DialogInterface.OnClickListener { dialogInterface, i ->
                collectedShowButton.isEnabled = false
                addCollectionProgressBar.visibility = View.VISIBLE

                viewModel.addToCollection()
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

    private fun initRatingsDialog() {
        ratingDialog = RatingPickerFragment(callback = {newRating ->
            Log.d(TAG, "initDialogs: Updating rating with $newRating")
            bindings.actionbuttonRate.isEnabled = false
            bindings.actionButtonRateProgressbar.visibility = View.VISIBLE

            if(newRating != -1) {
                Log.d(TAG, "Calling viewModel.updateRating")
                viewModel.addRating(newRating)
            } else {
                Log.d(TAG, "initDialogs: Deleting rating")
                viewModel.deleteRating()
            }
        }, showTitle)
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is ShowDetailsActionButtonsViewModel.Event.AddToCollectionEvent -> {
                        val syncResponse = event.syncResponse

                        // Disable progressbar and re-enable button
                        bindings.actionbuttonAddToCollection.isEnabled = true
                        bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                        if(syncResponse is Resource.Success) {
                            if(syncResponse.data?.added?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully added $showTitle to your Trakt collection!", Toast.LENGTH_SHORT)
                            } else {
                                displayMessageToast("Did  not add $showTitle to your Trakt collection", Toast.LENGTH_SHORT)
                            }
                        } else if(syncResponse is Resource.Error) {
                            displayMessageToast("Error adding $showTitle to your collection. ${syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.RemoveFromCollectionEvent -> {

                        // Reenable button and hide progressbar
                        bindings.actionbuttonAddToCollection.isEnabled = true
                        bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                        val syncResponse = event.syncResponse
                        if(syncResponse is Resource.Success) {
                            if(syncResponse.data?.deleted?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully removed $showTitle from your Trakt collection!", Toast.LENGTH_SHORT)
                            } else {
                                displayMessageToast("Did  not remove $showTitle from your Trakt collection", Toast.LENGTH_SHORT)
                            }
                        } else if(syncResponse is Resource.Error) {
                            displayMessageToast("Error removing $showTitle from your collection. ${syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.AddRatingEvent -> {
                        val syncResponse = event.syncResponse

                        bindings.actionbuttonRate.isEnabled = true
                        bindings.actionButtonRateProgressbar.visibility = View.GONE

                        if(syncResponse is Resource.Success) {
                            viewModel.updateRatingDisplay(event.newRating)
                            displayMessageToast("Rated $showTitle with ${ event.newRating } (${ Rating.fromValue(event.newRating).name }) successfully!", Toast.LENGTH_SHORT)
                        } else if(syncResponse is Resource.Error) {
                            displayMessageToast("Error rating $showTitle. Error ${syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)

                            syncResponse.error?.printStackTrace()
                        }
                    }
                    is ShowDetailsActionButtonsViewModel.Event.DeleteRatingEvent -> {
                        val syncResponse = event.syncResponse

                        bindings.actionbuttonRate.isEnabled = true
                        bindings.actionButtonRateProgressbar.visibility = View.GONE

                        if(syncResponse is Resource.Success) {
                            viewModel.updateRatingDisplay(null)

                            displayMessageToast("Reset $showTitle Rating successfully!", Toast.LENGTH_SHORT)
                        } else if(syncResponse is Resource.Error) {
                            displayMessageToast("Error resetting rating $showTitle. Error ${syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)

                            syncResponse.error?.printStackTrace()
                        }
                    }
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