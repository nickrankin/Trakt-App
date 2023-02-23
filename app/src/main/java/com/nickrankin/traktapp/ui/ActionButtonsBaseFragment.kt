package com.nickrankin.traktapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.adapter.history.WatchedHistoryEntryAdapter
import com.nickrankin.traktapp.adapter.lists.ListsCheckboxAdapter
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.databinding.DialogListsBinding
import com.nickrankin.traktapp.databinding.LayoutActionButtonsBinding
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.nickrankin.traktmanager.ui.dialoguifragments.WatchedDatePickerFragment
import com.uwetrottmann.trakt5.entities.BaseCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat

private const val TAG = "ActionButtonsBaseFragme"

@AndroidEntryPoint
abstract class ActionButtonsBaseFragment: BaseFragment() {

    private lateinit var bindings: LayoutActionButtonsBinding
    private var progressBar: ProgressBar? = null

    private lateinit var watchedHistoryAdapter: WatchedHistoryEntryAdapter
    private lateinit var listsDialogProgressBar: ProgressBar

    protected var traktId: Int = -1
    private lateinit var type: Type
    private lateinit var title: String

    private lateinit var typeString: String

    abstract fun setup(config: (bindings: LayoutActionButtonsBinding, traktId: Int, title: String, type: Type) -> Unit)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup { bindings, traktId, title, type ->
            this.bindings = bindings
            this.traktId = traktId
            this.title = title
            this.type = type

            initWatchedHistoryAdapter()
            displayPlayCount()

            progressBar = bindings.actionbuttonsProgressbar

            setTypeString()

            setupDialogs()

            getEvents()
        }

    }

    private fun setupDialogs() {
        setupCheckinDialogButton()
        setupAddHistoryButton()
        setupRatingButton()
        setupAddToCollectionButton()
        setupListsDialog()
    }

    private fun setTypeString() {
        typeString = when(type) {
            Type.MOVIE -> {
                "Movie"
            }
            Type.SHOW -> {
                "Show"
            }
            Type.EPISODE -> {
                "Episode"
            }
            Type.PERSON -> {
                "Person"
            }
            Type.LIST -> {
                "List"
            }
        }
    }

    private fun initWatchedHistoryAdapter() {
        watchedHistoryAdapter = WatchedHistoryEntryAdapter(sharedPreferences) { watchedHistoryEntry ->
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to remove play ${getFormattedDateTime(watchedHistoryEntry.watched_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))} ")
                .setPositiveButton("Yes") { dialog, _ ->
                    removeHistoryEntry(watchedHistoryEntry)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    abstract fun updatePlayCount(onPlayCountUpdated: (List<HistoryEntry>) -> Unit)
    abstract fun removeHistoryEntry(historyEntry: HistoryEntry)


    private fun displayPlayCount() {
        updatePlayCount { historyEntries ->
            if(historyEntries.isNotEmpty()) {
                bindings.apply {
                    actionbuttonsLastWatched.text = "Last watched: ${getFormattedDateTime(historyEntries.first().watched_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"

                    actionbuttonsAllPlays.visibility = View.VISIBLE
                    actionbuttonsAllPlays.text = "Plays (${historyEntries.size})"

                    actionbuttonsAllPlays.setOnClickListener {
                        getAllPlaysDialog().show()
                    }
                }
                watchedHistoryAdapter.submitList(historyEntries.sortedBy { it.watched_date }.reversed())
            }
        }
    }

    private fun getAllPlaysDialog(): AlertDialog {
        val watchedHistoryRecyclerView = RecyclerView(requireContext())
        val lm = LinearLayoutManager(requireContext())

        watchedHistoryRecyclerView.layoutManager = lm
        watchedHistoryRecyclerView.adapter = watchedHistoryAdapter

        return AlertDialog.Builder(requireContext())
            .setTitle("All Plays")
            .setView(watchedHistoryRecyclerView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun setupAddHistoryButton() {

        val button = bindings.actionbuttonsHistory


        button.setOnClickListener {
            getAddHistoryDialog { watchedDate ->
                button.isEnabled = false
                setProgressBar(true)

                addToWatchedHistory(traktId, watchedDate)
            }.show(
                requireActivity().supportFragmentManager,
                "Date Picker Fragment"
            )
        }
    }

    private fun getAddHistoryDialog(onWatchedDateChanged: (watchedDate: OffsetDateTime) -> Unit): WatchedDatePickerFragment {
        return WatchedDatePickerFragment(onWatchedDateChanged)
    }

    abstract fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime)

    private fun setupRatingButton() {
        val ratingDialog = getRatingPickerDialog(title) { newRating ->
            setProgressBar(true)
            when (newRating) {
                RatingPickerFragment.RATING_RESET -> {
                    Log.d(TAG, "initDialogs: Deleting rating")
                    deleteRating(traktId)

                }
                RatingPickerFragment.DIALOG_CLOSED -> {
                    setProgressBar(false)
                }
                else -> {
                    Log.d(TAG, "initDialogs: Updating rating with $newRating")

                    setNewRating(traktId, newRating)
                }
            }
        }


        val traktRatingTextView = bindings.actionbuttonsTraktRating
        val ratingTextView = bindings.actionbuttonsRating

        // Initial text
        ratingTextView.text = " - "

        bindings.actionbuttonsRatingGroup.referencedIds.forEach { viewId ->
            bindings.actionbuttonsRatingGroup.rootView.findViewById<View>(viewId).setOnClickListener {
                ratingDialog.show(requireActivity().supportFragmentManager, "rating_dialog")

            }
        }

        setTraktRating { traktRating ->
            val decimalFromatter = DecimalFormat("#.#")
            decimalFromatter.roundingMode = RoundingMode.CEILING

            if(traktRating != null) {
                traktRatingTextView.text =
                    "Trakt Rating: ${ decimalFromatter.format(traktRating) }"
            } else {
                traktRatingTextView.text =
                    "Trakt Rating: Not Rated "
            }
        }

        updateRatingText { rating ->
            if (rating != null) {
                ratingTextView.text = "${rating.rating}"
            } else {
                ratingTextView.text = " - "

            }
        }
    }

    private fun getRatingPickerDialog(
        title: String,
        callback: (newRating: Int) -> Unit
    ): RatingPickerFragment {
        return RatingPickerFragment(callback, title)
    }

    abstract fun updateRatingText(onRatingChanged: (ratingStats: RatingStats?) -> Unit)
    abstract fun setTraktRating(onRatingChanged: (traktRating: Double?) -> Unit)

    abstract fun setNewRating(traktId: Int, newRating: Int)
    abstract fun deleteRating(traktId: Int)

    private fun setupCheckinDialogButton() {
        val button = bindings.actionbuttonsCheckin

        button.setOnClickListener {
            getCheckinDialog(traktId, title).show()
        }
    }

    private fun getCheckinDialog(traktId: Int, title: String): AlertDialog {
        val button = bindings.actionbuttonsCheckin

        return AlertDialog.Builder(requireContext())
            .setTitle("Start watching ${title}?")
            .setMessage("Do you want to start watching ${title}?")
            .setPositiveButton("Yes") { dialogInterface, _ ->
                setProgressBar(true)
                button.isEnabled = false

                checkin(traktId)

                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel")  { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.create()
    }

    abstract fun checkin(traktId: Int)
    abstract fun overrideCheckin(traktId: Int)

    private fun showAlreadyCheckedInDialog(traktId: Int, title: String) {
        val alreadyCheckedInDialog = AlertDialog.Builder(requireContext())
            .setTitle("Already watching something")
            .setMessage("You are already watching something on Trakt. Cancel what you are watching and start watching ${title}?")
            .setPositiveButton("Yes") { dialogInterface, i ->

                overrideCheckin(traktId)

                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create()

        alreadyCheckedInDialog.show()
    }

    private fun setupAddToCollectionButton() {
        val button = bindings.actionbuttonsCollection

        // Add to Collection
        val addToCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to add ${title} to your collection?")
            .setMessage("Do you want to add ${title} to your Trakt Collection?")
            .setPositiveButton("Add") { dialogInterface, i ->
                button.isEnabled = false
                addToCollection(traktId)
                setProgressBar(true)
            }
            .setNegativeButton("Cancel", { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        // Remove from collection
        val removeFromCollectionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Do you want to remove ${title} to your collection?")
            .setMessage("Do you want to remove ${title} to your Trakt Collection?")
            .setPositiveButton("Remove") { dialogInterface, i ->
                button.isEnabled = false
                removeFromCollection(traktId)
                setProgressBar(true)
            }
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create()


        getCollectedStats(traktId) { collectedStats ->
            if (collectedStats != null) {
                Log.d(TAG, "getCollectedStatus: $typeString is in collection")
                button.text = "Remove from Collection"
                button.setOnClickListener {
                    removeFromCollectionDialog.show()
                }
            } else {
                Log.d(TAG, "getCollectedStatus: $typeString is not in collection")
                button.text = "Add To Collection"
                button.setOnClickListener {
                    addToCollectionDialog.show()
                }
            }
        }
    }

    abstract fun getCollectedStats(traktId: Int, oncollectedStateChanged: (collectedStats: CollectedStats?) -> Unit)
    abstract fun addToCollection(traktId: Int)
    abstract fun removeFromCollection(traktId: Int)

    protected fun setProgressBar(isSpinning: Boolean) {
        synchronized(this) {
            progressBar!!.visibility = if (isSpinning) View.VISIBLE else View.GONE
        }
    }

    private fun setupListsDialog() {
        val button = bindings.actionbuttonsLists
        val layout = DialogListsBinding.inflate(layoutInflater)
        val listsdialog = getListsDialog(layout.root)
        val listsCheckboxAdapter = ListsCheckboxAdapter(traktId, type) { traktList, isChecked ->
            listsDialogProgressBar.visibility = View.VISIBLE

            if(isChecked) {
                addListEntry(traktId, traktList)
            } else {
                removeListEntry(traktId, traktList)
            }
        }

        listsDialogProgressBar = layout.listdialogProgressbar
        val recyclerView = layout.listdialogRecycler
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = listsCheckboxAdapter

        button.setOnClickListener {
            listsdialog.show()
        }

        getLists { listEntries ->
            listsDialogProgressBar.visibility = View.GONE
            listsCheckboxAdapter.submitList(listEntries)
        }
    }

    abstract fun getLists(onListsChanged: (listEntries: List<Pair<TraktList, List<TraktListEntry>>>) -> Unit)
    abstract fun addListEntry(traktId: Int, traktList: TraktList)
    abstract fun removeListEntry(traktId: Int, traktList: TraktList)

    private fun getListsDialog(layout: ViewGroup): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Lists")
            .setPositiveButton("Close") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .setView(layout)
            .create()
    }

    abstract fun onNewEvent(onNewEvent: (event: ActionButtonEvent) -> Unit)

    private fun getEvents() {
        onNewEvent { event ->
            when(event) {
                is ActionButtonEvent.AddHistoryEntryEvent -> {
                    handleAddToHistoryEvent(checkEventResult(event.syncResponse))
                }
                is ActionButtonEvent.AddRatingEvent -> {
                    handleRatingAddedEvent(event.newRating, checkEventResult(event.syncResponse))
                }
                is ActionButtonEvent.AddToCollectionEvent -> {
                    handleAddCollectionEvent(checkEventResult(event.syncResponse))
                }
                is ActionButtonEvent.CheckinEvent -> {
                    when(event.baseCheckin) {
                        is Resource.Loading -> {}
                        is Resource.Success -> handleCheckinEventSuccess(event.baseCheckin.data)
                        is Resource.Error -> {
                            handleError(event.baseCheckin.error, null)
                            handleCheckinEventError(event.baseCheckin.error)
                        }
                    }
                }
                is ActionButtonEvent.DeleteCheckinEvent -> {
                    when(event.wasDeleted) {
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            Log.d(TAG, "getEvents: Checkins deleted ok")
                        }
                        is Resource.Error -> {
                            handleError(event.wasDeleted.error, null)

                        }
                    }
                }
                is ActionButtonEvent.RemoveFromCollectionEvent -> {
                    handleRemoveCollectionEvent(checkEventResult(event.syncResponse))
                }
                is ActionButtonEvent.RemoveHistoryEntryEvent -> {
                    handleDeleteFromHistoryEvent(checkEventResult(event.syncResponse))
                }
                is ActionButtonEvent.RemoveRatingEvent -> {
                    handleDeleteRatingEvent(checkEventResult(event.syncResponse))
                }
            }
        }
    }

    private fun checkEventResult(eventResource: Resource<SyncResponse>): SyncResponse? {
        return when(eventResource) {
            is Resource.Loading -> {
                Log.d(TAG, "checkEventResult: Event loading")

                null
            }
            is Resource.Success -> {
                eventResource.data
            }
            is Resource.Error -> {
                handleError(eventResource.error, null)

                null
            }

        }
    }

    private fun handleRatingAddedEvent(
        newRating: Int,
        syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.ADDED_OK -> {
                displayMessageToast(
                    "Successfully rated ${title} with ${
                        Rating.fromValue(
                            newRating
                        ).name
                    } (${newRating})", Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                Log.e(
                    TAG,
                    "handleRatingAddedEvent: $typeString with ${traktId} not found on Trakt!",
                )
                displayMessageToast(
                    "Current $typeString could not be found on Trakt (ID: ${traktId})",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }
    }

    private fun handleDeleteRatingEvent(syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.DELETED_OK -> {
                displayMessageToast(
                    "Reset ${title} Rating successfully!",
                    Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                displayMessageToast(
                    "Current $typeString could not be found on Trakt (ID: ${traktId})",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }
    }

    private fun handleCheckinEventSuccess(baseCheckinResponse: BaseCheckinResponse?) {
        displayMessageToast(
            "You are watching ${title}",
            Toast.LENGTH_SHORT
        )
    }

    private fun handleCheckinEventError(error: Throwable?) {
        if (error is HttpException) {
            if (error.code() == 409) {
                Log.d(TAG, "getEvents: User already checked in")
                // User already checked in to something
                showAlreadyCheckedInDialog(traktId, title)
            } else {
                (activity as IHandleError).handleError(
                    error,
                    "Error checking in to ${title} (HTTP Response ${error.code()})"
                )
            }
        } else if (error is IOException) {
            (activity as IHandleError).handleError(
                error,
                "Error checking in to ${title}. Please check your network connection."
            )
        } else {
           handleError(
                error,
                "Error checking in to ${title}. Error: ${error?.localizedMessage}"
            )
        }
    }


    private fun handleAddCollectionEvent(syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.ADDED_OK -> {
                displayMessageToast(
                    "${title} Added to collection successfully!",
                    Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                displayMessageToast(
                    "Current $typeString could not be found on Trakt",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }
    }

    private fun handleRemoveCollectionEvent(syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.DELETED_OK -> {
                displayMessageToast(
                    "$title deleted from collection successfully!",
                    Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                displayMessageToast(
                    "Current $typeString could not be found on Trakt",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }
    }

    private fun handleAddToHistoryEvent(syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.ADDED_OK -> {
                displayMessageToast(
                    "Added ${title} to your watched history successfully!",
                    Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                displayMessageToast(
                    "Current $typeString could not be found on Trakt",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }

    }

    private fun handleDeleteFromHistoryEvent(syncResponse: SyncResponse?) {
        when (getSyncResponse(syncResponse, type)) {
            Response.DELETED_OK -> {
                displayMessageToast(
                    "Removed ${title} from your watched history successfully!",
                    Toast.LENGTH_SHORT
                )
            }
            Response.NOT_FOUND -> {
                displayMessageToast(
                    "Current $typeString could not be found on Trakt",
                    Toast.LENGTH_SHORT
                )
            }
            Response.ERROR -> {
                Log.e(TAG, "getEvents: SyncResponse cannot be NULL")
            }
            else -> {
                Log.e(TAG, "getEvents: Invalid Response")
            }
        }

    }

    protected fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

}