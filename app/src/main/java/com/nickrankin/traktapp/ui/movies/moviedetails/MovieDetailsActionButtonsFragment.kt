package com.nickrankin.traktapp.ui.movies.moviedetails

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
import com.nickrankin.traktapp.model.movies.MovieDetailsActionButtonsViewModel
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"

@AndroidEntryPoint
class MovieDetailsActionButtonsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: MovieDetailsActionButtonsViewModel by activityViewModels()

    @Inject
    lateinit var shedPreferences: SharedPreferences

    private lateinit var bindings: ActionButtonsFragmentBinding

    private var isLoggedIn = false

    private lateinit var movieTitle: String

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

        movieTitle = arguments?.getString(MovieDetailsRepository.MOVIE_TITLE_KEY, "Unknown") ?: "Unknown"

        if (isLoggedIn) {
            // Dialogs
            initAddToCollectionDialogs()
            initRatingsDialog()

            // Buttons
            setupRatingButton()
            setupCollectionButton()

            // Get Data
            getRatings()
            getCollectedStatus()
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
        lifecycleScope.launchWhenStarted {
            viewModel.movieRating.collectLatest { movieRatingResource ->

                if (movieRatingResource is Resource.Success) {
                    val rating = movieRatingResource.data
                    if (rating != null) {
                        bindings.actionbuttonRateText.text = "${rating.rating}"
                    } else {
                        bindings.actionbuttonRateText.text = " - "

                    }
                }

            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is MovieDetailsActionButtonsViewModel.Event.AddRatingEvent -> {
                        val syncResponse = event.syncResponse

                        when (syncResponse) {
                            is Resource.Success -> {
                                // Disable progressbar and re-enable button
                                bindings.actionbuttonRate.isEnabled = true
                                bindings.actionButtonRateProgressbar.visibility = View.GONE

                                if (syncResponse.data?.added?.movies ?: 0 > 0) {
                                    displayMessageToast(
                                        "Successfully rated $movieTitle with ${
                                            Rating.fromValue(
                                                event.newRating
                                            ).name
                                        } (${event.newRating})", Toast.LENGTH_SHORT
                                    )
                                }
                            }
                            is Resource.Error -> {
                                // Disable progressbar and re-enable button
                                bindings.actionbuttonRate.isEnabled = true
                                bindings.actionButtonRateProgressbar.visibility = View.GONE

                                displayMessageToast(
                                    "Error rating $movieTitle. Error ${syncResponse.error?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                )
                            }
                        }
                    }
                    is MovieDetailsActionButtonsViewModel.Event.DeleteRatingEvent -> {
                        val syncResponse = event.syncResponse

                        bindings.actionbuttonRate.isEnabled = true
                        bindings.actionButtonRateProgressbar.visibility = View.GONE

                        if (syncResponse is Resource.Success) {
                            if (syncResponse.data?.deleted?.movies ?: 0 > 0) {
                                displayMessageToast(
                                    "Reset $movieTitle Rating successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            displayMessageToast(
                                "Error resetting rating $movieTitle. Error ${syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )

                            syncResponse.error?.printStackTrace()
                        }
                    }

                    is MovieDetailsActionButtonsViewModel.Event.AddToCollectionEvent -> {
                        val syncResponse = event.syncResponse

                        if (syncResponse is Resource.Success) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            if (syncResponse.data?.added?.movies ?: 0 > 0) {
                                displayMessageToast(
                                    "$movieTitle Added to collection successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            displayMessageToast(
                                "Error adding $movieTitle to collection. Error ${syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )

                            syncResponse.error?.printStackTrace()
                        }
                    }

                    is MovieDetailsActionButtonsViewModel.Event.RemoveFromCollectionEvent -> {
                        val syncResponse = event.syncResponse

                        if (syncResponse is Resource.Success) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            if (syncResponse.data?.deleted?.movies ?: 0 > 0) {
                                displayMessageToast(
                                    "$movieTitle deleted from collection successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            displayMessageToast(
                                "Error deleting $movieTitle from collection. Error ${syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )

                            syncResponse.error?.printStackTrace()
                        }
                    }

                }

            }
        }
    }


    private fun getCollectedStatus() {
        val collectedShowButton = bindings.actionbuttonAddToCollection
        val collectedShowButtonText = bindings.actionbuttonCollectedTextview

        lifecycleScope.launchWhenStarted {
            viewModel.collectedMovie.collectLatest { collectedMovie ->

                if (collectedMovie != null) {
                    Log.d(TAG, "getCollectedStatus: Movie is in collection")
                    collectedShowButtonText.text = "Remove from Collection"
                    collectedShowButton.setOnClickListener {
                        removeFromCollectionDialog.show()
                    }
                } else {
                    Log.d(TAG, "getCollectedStatus: Movie is not in collection")
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
            .setTitle("Do you want to add $movieTitle to your collection?")
            .setMessage("Do you want to add $movieTitle to your Trakt Collection?")
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
            .setTitle("Do you want to remove $movieTitle to your collection?")
            .setMessage("Do you want to remove $movieTitle to your Trakt Collection?")
            .setPositiveButton("Remove", DialogInterface.OnClickListener { dialogInterface, i ->
                collectedShowButton.isEnabled = false
                addCollectionProgressBar.visibility = View.VISIBLE

                viewModel.deleteFromCollection()
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()
    }

    private fun initRatingsDialog() {
        ratingDialog = RatingPickerFragment(callback = { newRating ->
            Log.d(TAG, "initDialogs: Updating rating with $newRating")
            bindings.actionbuttonRate.isEnabled = false
            bindings.actionButtonRateProgressbar.visibility = View.VISIBLE

            if (newRating != -1) {
                Log.d(TAG, "Calling viewModel.updateRating")
                viewModel.addRating(newRating)
            } else {
                Log.d(TAG, "initDialogs: Deleting rating")
                viewModel.deleteRating()
            }
        }, movieTitle)
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
        fun newInstance() = MovieDetailsActionButtonsFragment()
    }

}