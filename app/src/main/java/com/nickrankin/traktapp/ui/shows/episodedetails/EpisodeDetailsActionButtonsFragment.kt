package com.nickrankin.traktapp.ui.shows.episodedetails

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.BaseFragment
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.Response
import com.nickrankin.traktapp.helper.getSyncResponse
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.nickrankin.traktmanager.ui.dialoguifragments.WatchedDatePickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActionBut"
@AndroidEntryPoint
class EpisodeDetailsActionButtonsFragment(): BaseFragment() {

    private val viewModel: EpisodeDetailsViewModel by activityViewModels()

    private lateinit var bindings: ActionButtonsFragmentBinding

    private lateinit var addCollectionButton: RelativeLayout

    private var checkinDialog: AlertDialog? = null
    private var cancelCheckinDialog: AlertDialog? = null
    private var addWatchedHistoryDialog: WatchedDatePickerFragment? = null
    private var ratingPickerFragment: RatingPickerFragment? = null
    private lateinit var listsDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ActionButtonsFragmentBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAddCollectionButton()

        getEpisode()

    }

    private fun getEpisode() {
        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episodeResource ->
                when(episodeResource) {
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        val episode = episodeResource.data

                        if(episode != null) {
                            if(isLoggedIn) {
                                setupCollectionButton(episode)
                                setupCheckinButton(episode)
                                setupAddToHistoryButton()
                                setupRatingButton(episode)
                                setupAddListsButton(episode)

                                // Get Data
                                getRatings(episode)
                                getEvents(episode)
                            }
                        }
                    }
                    is Resource.Error -> TODO()

                }


            }
        }
    }

    private fun setupCollectionButton(episode: TmEpisode?) {
        if(episode == null) {
            return
        }

        addCollectionButton = bindings.actionbuttonAddToCollection

        getCollectionStatus(episode)

    }

    private fun getCollectionStatus(episode: TmEpisode) {
        val collectedButtonLabel = addCollectionButton.findViewById<TextView>(R.id.actionbutton_collected_textview)
        lifecycleScope.launchWhenStarted {
            viewModel.collectedEpisodes.collectLatest { collectedEpisodes ->
                val collectedEpisode = collectedEpisodes.filter { it.trakt_id == episode.episode_trakt_id }

                if(collectedEpisode.isNotEmpty()) {
                    collectedButtonLabel.text = "Remove from Collection"

                    addCollectionButton.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Remove from Collection")
                            .setMessage("Are you sure you want to remove ${episode.name} from your Collection?")
                            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i -> viewModel.removeFromCollection(episode.episode_trakt_id) })
                            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                            .create()
                            .show()
                    }
                } else {
                    collectedButtonLabel.text = "Add to Collection"

                    addCollectionButton.setOnClickListener {
                        viewModel.addToCollection(episode)
                    }
                }
            }
        }
    }

    private fun setupAddListsButton(tmEpisode: TmEpisode) {
        val addListsButton = bindings.actionbuttonLists
        addListsButton.visibility = View.VISIBLE

        setupListsDialog(tmEpisode)

        Log.d(TAG, "setupAddListsButton: HERE")
        addListsButton.setOnClickListener {
            listsDialog.show()
        }
    }

    private fun setupCheckinButton(episode: TmEpisode) {
        val checkinButton = bindings.actionbuttonCheckin
        checkinButton.visibility = View.VISIBLE

        initCheckinDialog(episode)
        initCancelCheckinDialog(episode)
        initAddWatchedHistoryDialog(episode)

        checkinButton.setOnClickListener { checkinDialog?.show() }
    }

    private fun setupAddCollectionButton() {
        val addCollectionButton = bindings.actionbuttonAddToCollection
        addCollectionButton.visibility = View.VISIBLE
    }

    private fun setupAddToHistoryButton() {
        val addHistoryButton = bindings.actionbuttonAddHistory
        addHistoryButton.visibility = View.VISIBLE

        addHistoryButton.setOnClickListener { addWatchedHistoryDialog?.show(requireActivity().supportFragmentManager, "Add To History dialog") }
    }

    private fun setupRatingButton(episode: TmEpisode) {
        val ratingButton = bindings.actionbuttonRate
        val ratingProgressBar = bindings.actionButtonRateProgressbar

        ratingButton.visibility = View.VISIBLE
        ratingProgressBar.visibility = View.GONE
        bindings.actionbuttonRateText.text = " - "

        initRatingDialog(episode)

        ratingButton.setOnClickListener {
            ratingProgressBar.visibility = View.VISIBLE
            ratingPickerFragment?.show(requireActivity().supportFragmentManager, "add_rating_fragment")

        }
    }

    private fun getRatings(episode: TmEpisode) {
        lifecycleScope.launchWhenStarted {
            viewModel.ratings.collectLatest  { ratings ->
                val rating = ratings.find { it.trakt_id == episode.episode_trakt_id }?.rating ?: 0
                Log.d(TAG, "getRatings: Got rating $rating")

                if(rating > 0) {
                    bindings.actionbuttonRateText.text = "$rating"

                } else {
                    bindings.actionbuttonRateText.text = " - "

                }

            }
        }

    }

    private fun getEvents(episode: TmEpisode) {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is EpisodeDetailsViewModel.Event.AddCheckinEvent -> {
                        val checkInResponse = event.checkinResponse
                        val checkinButton = bindings.actionbuttonCheckin
                        val checkinProgressBar = bindings.actionButtonCheckinProgressbar

                        if(checkInResponse is Resource.Success) {
                            checkinButton.isEnabled = true
                            checkinProgressBar.visibility = View.GONE

                            displayMessageToast("You are watching ${episode.name}", Toast.LENGTH_SHORT)
                        } else if(checkInResponse is Resource.Error) {
                            val exception = checkInResponse.error

                            Log.e(TAG, "getEvents: Caught an exception ${exception?.localizedMessage}", )

                            if(exception is HttpException && exception.code() == 409) {
                                    // The user is already watching something, so give the user options
                                    cancelCheckinDialog?.show()

                            } else {
                                checkinButton.isEnabled = true
                                checkinProgressBar.visibility = View.GONE

                                (activity as IHandleError).showErrorMessageToast(checkInResponse.error, "Error checking in")
                            }
                        }
                    }
                    is EpisodeDetailsViewModel.Event.CancelCheckinEvent -> {
                        val shouldCheckinCurrentEpisode = event.checkinCurrentEpisode
                        val response = event.cancelCheckinResult

                        val checkinButton = bindings.actionbuttonCheckin
                        val checkinProgressBar = bindings.actionButtonCheckinProgressbar

                        checkinButton.isEnabled = true
                        checkinProgressBar.visibility = View.GONE

                        if(response is Resource.Success) {
                            if(shouldCheckinCurrentEpisode) {
                                // Past checkins are now cleared, so checkin the current episode
                                checkin(episode)
                            } else {
                                displayMessageToast("Successfully cleared active Trakt Checkins", Toast.LENGTH_SHORT)
                            }

                        } else if(response is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(response.error, "Error  deleting checkin")

                        }
                    }
                    is EpisodeDetailsViewModel.Event.AddToWatchedHistoryEvent -> {
                        val addHistoryButton = bindings.actionbuttonAddHistory
                        val addHistoryProgressBar = bindings.actionButtonAddHistoryProgressbar

                        addHistoryButton.isEnabled = true
                        addHistoryProgressBar.visibility = View.GONE

                        val syncResponseResource = event.syncResponse

                        if(syncResponseResource is Resource.Success) {


                            when(val syncResponse = getSyncResponse(syncResponseResource.data, Type.EPISODE)) {
                                Response.ADDED_OK -> {
                                    displayMessageToast("Successfully added ${episode.name ?: "episode"} to your watch history!", Toast.LENGTH_SHORT)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current Episode could not be found on Trakt", Toast.LENGTH_LONG)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("An error occorred adding item to your history", Toast.LENGTH_LONG)
                                }
                                else -> {

                                }
                            }

                        } else if(syncResponseResource is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(syncResponseResource.error, "Error adding episode to your watched history")

                        }
                    }
                    is EpisodeDetailsViewModel.Event.AddRatingsEvent -> {
                        val syncResponse = event.syncResponse.data?.first
                        val newRating = event.syncResponse.data?.second

                        if(event.syncResponse is Resource.Success) {

                            when(getSyncResponse(syncResponse, Type.EPISODE)) {
                                Response.ADDED_OK -> {
                                    displayMessageToast("Successfully rated ${episode.name ?: "Unknown"} with ${Rating.fromValue(newRating ?: 7).name} ($newRating)", Toast.LENGTH_LONG)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current Episode could not be found on Trakt", Toast.LENGTH_LONG)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Could not add rating, please try again later", Toast.LENGTH_LONG)
                                }
                                else -> {

                                }
                            }

                        } else if(event.syncResponse is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(event.syncResponse.error, "Error adding rating")

                        }

                        bindings.actionButtonRateProgressbar.visibility = View.GONE
                    }

                    is EpisodeDetailsViewModel.Event.DeleteRatingsEvent -> {
                        val syncResponse = event.syncResponse.data

                        if(event.syncResponse is Resource.Success) {
                            when(getSyncResponse(syncResponse, Type.EPISODE)) {
                                Response.DELETED_OK -> {
                                    displayMessageToast("Successfully reset rating!", Toast.LENGTH_LONG)
                                }
                                Response.NOT_FOUND -> {
                                    displayMessageToast("Current Episode could not be found on Trakt", Toast.LENGTH_LONG)
                                }
                                Response.ERROR -> {
                                    displayMessageToast("Could not delete rating, please try again later", Toast.LENGTH_LONG)
                                }
                                else -> {

                                }
                            }

                        } else if(event.syncResponse is Resource.Error) {
                            (activity as IHandleError).showErrorMessageToast(event.syncResponse.error, "Error removing rating")
                        }
                    }
                    else -> {}

                }
            }
        }
    }

    private fun initCheckinDialog(episode: TmEpisode) {

        if(checkinDialog != null) {
            return
        }

        checkinDialog = AlertDialog.Builder(requireContext())
            .setTitle("Start watching ${episode.name}")
            .setMessage("Do you want to start watching ${episode.name}?")
            .setPositiveButton("Yes" ) { dialogInterface, i -> checkin(episode) }
            .setNegativeButton("Cancel" ) { dialogInterface, i -> dialogInterface.dismiss() }
            .create()
    }

    private fun initCancelCheckinDialog(episode: TmEpisode) {

        if(cancelCheckinDialog != null) {
            return
        }

        cancelCheckinDialog = AlertDialog.Builder(requireContext())
            .setTitle("You are already watching something. What do you want to do?")
            .setItems(arrayOf("Start watching ${episode.name ?: " this episode"}", "Nothing", "Just cancel checkin")) { dialogInterface, i ->
                val checkinButton = bindings.actionbuttonCheckin
                val checkinProgressBar = bindings.actionButtonCheckinProgressbar

                checkinButton.isEnabled = false
                checkinProgressBar.visibility = View.VISIBLE
                when(i) {
                    0 -> {
                        cancelCheckin(true)
                    }
                    1 -> {
                        checkinButton.isEnabled = true
                        checkinProgressBar.visibility = View.GONE
                        dialogInterface.dismiss()
                    }
                    2 -> {
                        cancelCheckin(false)
                    }
                    else -> {
                        checkinButton.isEnabled = true
                        checkinProgressBar.visibility = View.GONE

                        dialogInterface.dismiss()
                    }
                }
            }
            .create()
    }

    private fun setupListsDialog(tmEpisode: TmEpisode) {
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
                        it?.list_entry_trakt_id == tmEpisode.episode_trakt_id } != null

                    checkbox.setOnClickListener {
                        val checkbox = it as CheckBox

                        if(checkbox.isChecked) {
                            viewModel.addListEntry("episode", tmEpisode.episode_trakt_id, listWithEntries.list)
                        } else {
                            viewModel.removeListEntry(listWithEntries.list.trakt_id, tmEpisode.episode_trakt_id, Type.EPISODE)
                        }
                    }

                    layout.addView(checkbox)
                }
            }
        }
    }

    private fun initAddWatchedHistoryDialog(episode: TmEpisode) {

        if(addWatchedHistoryDialog != null) {
            return
        }

        addWatchedHistoryDialog = WatchedDatePickerFragment(onWatchedDateChanged = { watchedAt ->
            val addHistoryButton = bindings.actionbuttonAddHistory
            val addHistoryProgressBar = bindings.actionButtonAddHistoryProgressbar

            addHistoryButton.isEnabled = false
            addHistoryProgressBar.visibility = View.VISIBLE

            viewModel.addToWatchedHistory(episode, watchedAt)
        })
    }

    private fun initRatingDialog(episode: TmEpisode) {

        if(ratingPickerFragment != null) {
            return
        }

        ratingPickerFragment = RatingPickerFragment(callback = { newRating ->
            if(newRating != -1) {
                // New rating to be added
                viewModel.addRating(newRating, episode.episode_trakt_id ?: -1)
            } else {
                // User reset rating
                viewModel.resetRating(episode.episode_trakt_id ?: -1)
            }
        }, episode.name ?: "Episode")
    }

    private fun checkin(episode: TmEpisode) {
        viewModel.checkin(episode.episode_trakt_id ?: -1)
    }

    private fun cancelCheckin(checkinCurrentEpisode: Boolean) {
        viewModel.cancelCheckin(checkinCurrentEpisode)
    }

    private fun displayMessageToast(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    companion object {
        fun newInstance() = EpisodeDetailsActionButtonsFragment()
    }
}