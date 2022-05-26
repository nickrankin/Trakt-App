package com.nickrankin.traktapp.ui.movies.moviedetails

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.MovieDetailsActionButtonsViewModel
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.nickrankin.traktmanager.ui.dialoguifragments.WatchedDatePickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"

@AndroidEntryPoint
class MovieDetailsActionButtonsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val viewModel: MovieDetailsActionButtonsViewModel by activityViewModels()

    @Inject
    lateinit var shedPreferences: SharedPreferences

    private lateinit var bindings: ActionButtonsFragmentBinding

    private var isLoggedIn = false

    // Dialogs
    private lateinit var addToCollectionDialog: AlertDialog
    private lateinit var removeFromCollectionDialog: AlertDialog
    private lateinit var ratingDialog: RatingPickerFragment
    private lateinit var addToWatchedHistoryFragment: WatchedDatePickerFragment
    private lateinit var listsDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ActionButtonsFragmentBinding.inflate(inflater)


        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.e(TAG, "onViewCreated:setupListsDialog HERE", )

        super.onViewCreated(view, savedInstanceState)

        isLoggedIn = shedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

        if(isLoggedIn) {
            setupRatingButton()
            setupCollectionButton()
        }
    }

    fun initFragment(movie: TmMovie?) {
        if(movie == null) {
            return
        }

        if (isLoggedIn) {

            setupAddHistoryButton(movie)

            // Dialogs
            initAddToCollectionDialogs(movie)
            initRatingsDialog(movie)

            // Buttons
            setupCheckinButton(movie)
            setupAddListsButton(movie)

            // Get Data
            getRatings(movie)
            getCollectedStatus(movie)
            getEvents(movie)
        }
    }

    private fun setupAddListsButton(tmMovie: TmMovie) {
        val addListsButton = bindings.actionbuttonLists
        addListsButton.visibility = View.VISIBLE

        setupListsDialog(tmMovie)

        Log.d(TAG, "setupAddListsButton: HERE")
        addListsButton.setOnClickListener {
            listsDialog.show()
        }
    }

    private fun setupCheckinButton(movie: TmMovie) {
        val checkinButton = bindings.actionbuttonCheckin
        checkinButton.visibility = View.VISIBLE

        val checkinDialog = AlertDialog.Builder(requireContext())
            .setTitle("Start watching ${movie.title}?")
            .setMessage("Do you want to start watching ${movie.title}?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.checkin(false)
                dialogInterface.dismiss()
            })
            .setNegativeButton("Cancel",  DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        checkinButton.setOnClickListener {
            checkinDialog.show()
        }
    }

    private fun setupAddHistoryButton(movie: TmMovie) {
        val addToHistoryButton = bindings.actionbuttonAddHistory
        addToHistoryButton.visibility = View.VISIBLE

        addToWatchedHistoryFragment = WatchedDatePickerFragment() { watchedDate ->
            viewModel.addToWatchedHistory(movie, watchedDate)
        }

        addToHistoryButton.setOnClickListener {
            addToWatchedHistoryFragment.show(requireActivity().supportFragmentManager, "Date Picker Fragment")
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

    private fun getRatings(movie: TmMovie) {
        lifecycleScope.launchWhenStarted {
            viewModel.movieRatings.collectLatest { ratings ->
                val rating = ratings.find { it.trakt_id == movie.trakt_id }

                    if (rating != null) {
                        bindings.actionbuttonRateText.text = "${rating.rating}"
                    } else {
                        bindings.actionbuttonRateText.text = " - "

                    }
            }
        }
    }

    private fun showAlreadyCheckedInDialog(movie: TmMovie) {
        val alreadyCheckedInDialog = AlertDialog.Builder(requireContext())
            .setTitle("Already watching something")
            .setMessage("You are already watching something on Trakt. Cancel what you are watching and start watching ${movie.title}?")
            .setPositiveButton("Yes") { dialogInterface, i ->
                viewModel.checkin(true)
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create()

        alreadyCheckedInDialog.show()
    }


    private fun getCollectedStatus(movie: TmMovie) {
        val collectedShowButton = bindings.actionbuttonAddToCollection
        val collectedShowButtonText = bindings.actionbuttonCollectedTextview

        lifecycleScope.launchWhenStarted {
            viewModel.collectedMovieStats(movie.trakt_id).collectLatest { collectedMovieStats ->

                if (collectedMovieStats != null) {
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

    private fun initAddToCollectionDialogs(movie: TmMovie) {
        val collectedShowButton = bindings.actionbuttonAddToCollection
        val addCollectionProgressBar = bindings.actionButtonAddToCollectionProgressbar

        // Add to Collection
        addToCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to add ${movie.title} to your collection?")
            .setMessage("Do you want to add ${movie.title} to your Trakt Collection?")
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
            .setTitle("Do you want to remove ${movie.title} to your collection?")
            .setMessage("Do you want to remove ${movie.title} to your Trakt Collection?")
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

    private fun initRatingsDialog(movie: TmMovie) {
        ratingDialog = RatingPickerFragment(callback = { newRating ->
            Log.d(TAG, "initDialogs: Updating rating with $newRating")
            bindings.actionbuttonRate.isEnabled = false
            bindings.actionButtonRateProgressbar.visibility = View.VISIBLE

            if (newRating != -1) {
                Log.d(TAG, "Calling viewModel.updateRating")
                viewModel.addRating(newRating, movie.tmdb_id, movie.title)
            } else {
                Log.d(TAG, "initDialogs: Deleting rating")
                viewModel.deleteRating()
            }
        }, movie.title)
    }

    private fun setupListsDialog(movie: TmMovie) {
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
                        Log.d(TAG, "setupListsDialog: List Entry TraktId: ${it?.list_entry_trakt_id} TRAKT ID: ${movie.trakt_id}")
                         it?.list_entry_trakt_id == movie.trakt_id } != null

                    checkbox.setOnClickListener {
                        val checkbox = it as CheckBox

                        if(checkbox.isChecked) {
                            viewModel.addListEntry("movie", movie.trakt_id, listWithEntries.list)
                        } else {
                            viewModel.removeListEntry(listWithEntries.list.trakt_id, movie.trakt_id, Type.MOVIE)
                        }

                    }

                    layout.addView(checkbox)
                }
            }
        }
    }

    private fun getEvents(movie: TmMovie) {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is MovieDetailsActionButtonsViewModel.Event.AddListEntryEvent -> {
                        val syncResponse = event.addListEntryResponse

                        when(syncResponse) {
                            is Resource.Success -> {
                                if(syncResponse.data?.added?.movies ?: 0 > 0) {
                                    displayMessageToast("Successfully added ${movie.title} to the list", Toast.LENGTH_SHORT)
                                }
                            }
                            is Resource.Error -> {
                                displayMessageToast("Error adding ${movie.title} to the list ${syncResponse.error?.message}", Toast.LENGTH_LONG)
                                syncResponse.error?.printStackTrace()
                                Log.e(TAG, "getEvents: ${syncResponse.error?.message}", )
                            }
                        }

                    }
                    is MovieDetailsActionButtonsViewModel.Event.RemoveListEntryEvent -> {
                        val syncResponse = event.removeListEntryResponse

                        when(syncResponse) {
                            is Resource.Success -> {
                                if(syncResponse.data?.deleted?.movies ?: 0 > 0) {
                                    displayMessageToast("Successfully removed ${movie.title} from the list", Toast.LENGTH_SHORT)
                                }
                            }
                            is Resource.Error -> {
                                displayMessageToast("Error removing ${movie.title} from the list ${syncResponse.error?.message}", Toast.LENGTH_LONG)
                                syncResponse.error?.printStackTrace()
                                Log.e(TAG, "getEvents: ${syncResponse.error?.message}", )
                            }
                        }
                    }
                    is MovieDetailsActionButtonsViewModel.Event.AddToHistoryEvent -> {
                        val syncResponse = event.addHistoryResponse

                        when(syncResponse) {
                            is Resource.Success -> {
                                if(syncResponse.data?.added?.movies ?: 0 > 0) {
                                    displayMessageToast("Added ${movie.title} to your watched history successfully!", Toast.LENGTH_SHORT)
                                }
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "getEvents: Error adding to watch history", )
                                syncResponse.error?.printStackTrace()
                                displayMessageToast("Error adding ${movie.title} to your watched History", Toast.LENGTH_SHORT)
                            }
                        }
                    }

                    is MovieDetailsActionButtonsViewModel.Event.CheckinEvent -> {
                        when(event.movieCheckinResponse) {
                            is Resource.Success -> {
                                displayMessageToast("You are watching ${movie.title}", Toast.LENGTH_SHORT)
                            }
                            is Resource.Error -> {
                                val error = event.movieCheckinResponse.error
                                if(error is HttpException) {
                                    if(error.code() == 409) {
                                        Log.d(TAG, "getEvents: User already checked in")
                                        // User already checked in to something
                                        showAlreadyCheckedInDialog(movie)
                                    } else {
                                        displayMessageToast("An error occurred checking in to ${movie.title}", Toast.LENGTH_SHORT)
                                        error.printStackTrace()
                                    }
                                }
                                error?.printStackTrace()

                            }
                        }
                    }
                    is MovieDetailsActionButtonsViewModel.Event.AddRatingEvent -> {
                        val syncResponse = event.syncResponse

                        when (syncResponse) {
                            is Resource.Success -> {
                                // Disable progressbar and re-enable button
                                bindings.actionbuttonRate.isEnabled = true
                                bindings.actionButtonRateProgressbar.visibility = View.GONE

                                if (syncResponse.data?.added?.movies ?: 0 > 0) {
                                    displayMessageToast(
                                        "Successfully rated ${movie.title} with ${
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
                                    "Error rating ${movie.title}. Error ${syncResponse.error?.localizedMessage}",
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
                                    "Reset ${movie.title} Rating successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            displayMessageToast(
                                "Error resetting rating ${movie.title}. Error ${syncResponse.error?.localizedMessage}",
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
                                    "${movie.title} Added to collection successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            displayMessageToast(
                                "Error adding ${movie.title} to collection. Error ${syncResponse.error?.localizedMessage}",
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
                                    "${movie.title} deleted from collection successfully!",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        } else if (syncResponse is Resource.Error) {
                            bindings.actionbuttonAddToCollection.isEnabled = true
                            bindings.actionButtonAddToCollectionProgressbar.visibility = View.GONE

                            displayMessageToast(
                                "Error deleting ${movie.title} from collection. Error ${syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )

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
        fun newInstance() = MovieDetailsActionButtonsFragment()
    }

}