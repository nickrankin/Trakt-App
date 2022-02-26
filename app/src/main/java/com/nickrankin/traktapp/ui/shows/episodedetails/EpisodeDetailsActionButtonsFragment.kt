package com.nickrankin.traktapp.ui.shows.episodedetails

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
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.episodedetails.EpisodeDetailsActionButtonsViewModel
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.nickrankin.traktmanager.ui.dialoguifragments.WatchedDatePickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException

private const val TAG = "EpisodeDetailsActionBut"
@AndroidEntryPoint
class EpisodeDetailsActionButtonsFragment(): Fragment(), OnEpisodeChangeListener {

    private val viewModel: EpisodeDetailsActionButtonsViewModel by activityViewModels()
    private lateinit var bindings: ActionButtonsFragmentBinding
    private var episode: TmEpisode? = null

    private var checkinDialog: AlertDialog? = null
    private var cancelCheckinDialog: AlertDialog? = null
    private var addWatchedHistoryDialog: WatchedDatePickerFragment? = null
    private var ratingPickerFragment: RatingPickerFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindings = ActionButtonsFragmentBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun bindEpisode(episode: TmEpisode) {
        this.episode = episode

        viewModel.setEpisodeTraktId(episode.episode_trakt_id)
            Log.e(TAG, "onEpisodeIdChanged: EpisodeID is now $episode", )

        // Only setup the buttons once we have Episode Trakt ID
        // Setup the buttons
        //setupAddCollectionButton()
        setupCheckinButton()
        setupAddToHistoryButton()
        setupRatingButton()

        // Get Data
        getRatings()
        getEvents()

    }

    private fun setupCheckinButton() {
        val checkinButton = bindings.actionbuttonCheckin
        checkinButton.visibility = View.VISIBLE

        initCheckinDialog()
        initCancelCheckinDialog()
        initAddWatchedHistoryDialog()

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

    private fun setupRatingButton() {
        val ratingButton = bindings.actionbuttonRate
        ratingButton.visibility = View.VISIBLE
        bindings.actionbuttonRateText.text = " - "

        initRatingDialog()

        ratingButton.setOnClickListener {
            ratingPickerFragment?.show(requireActivity().supportFragmentManager, "add_rating_fragment")

        }
    }

    private fun getRatings() {
        viewModel.ratings.observe(viewLifecycleOwner) { rating ->
            Log.d(TAG, "getRatings: Got rating $rating")

            if(rating > 0) {
                bindings.actionbuttonRateText.text = "$rating"

            } else {
                bindings.actionbuttonRateText.text = " - "

            }

        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is EpisodeDetailsActionButtonsViewModel.Event.AddCheckinEvent -> {
                        val checkInResponse = event.checkinResponse
                        val checkinButton = bindings.actionbuttonCheckin
                        val checkinProgressBar = bindings.actionButtonCheckinProgressbar

                        if(checkInResponse is Resource.Success) {
                            checkinButton.isEnabled = true
                            checkinProgressBar.visibility = View.GONE

                            displayMessageToast("You are watching ${episode?.name}", Toast.LENGTH_SHORT)
                        } else if(checkInResponse is Resource.Error) {
                            val exception = checkInResponse.error

                            Log.e(TAG, "getEvents: Caught an exception ${exception?.localizedMessage}", )

                            if(exception is HttpException) {
                                val errorCode = exception.code()
                                if(errorCode == 409) {
                                    // The user is already watching something, so give the user options
                                    cancelCheckinDialog?.show()
                                } else {
                                    checkinButton.isEnabled = true
                                    checkinProgressBar.visibility = View.GONE

                                    // Some other HTTP Error occurred
                                    displayMessageToast("An HTTP error occurred while trying to checkin to this episode (code: $errorCode) ${exception?.localizedMessage}", Toast.LENGTH_LONG)

                                }
                            } else {
                                checkinButton.isEnabled = true
                                checkinProgressBar.visibility = View.GONE

                                displayMessageToast("An error occurred while trying to checkin to this episode ${exception?.localizedMessage}", Toast.LENGTH_LONG)
                            }
                        }
                    }
                    is EpisodeDetailsActionButtonsViewModel.Event.CancelCheckinEvent -> {
                        val shouldCheckinCurrentEpisode = event.checkinCurrentEpisode
                        val response = event.cancelCheckinResult

                        val checkinButton = bindings.actionbuttonCheckin
                        val checkinProgressBar = bindings.actionButtonCheckinProgressbar

                        checkinButton.isEnabled = true
                        checkinProgressBar.visibility = View.GONE

                        if(response is Resource.Success) {
                            if(shouldCheckinCurrentEpisode) {
                                // Past checkins are now cleared, so checkin the current episode
                                checkin()
                            } else {
                                displayMessageToast("Successfully cleared active Trakt Checkins", Toast.LENGTH_SHORT)
                            }

                        } else if(response is Resource.Error) {
                            response.error?.printStackTrace()
                            displayMessageToast("Error cancelling checkins. ${response.error?.localizedMessage}", Toast.LENGTH_LONG)
                        }
                    }
                    is EpisodeDetailsActionButtonsViewModel.Event.AddToWatchedHistoryEvent -> {
                        val addHistoryButton = bindings.actionbuttonAddHistory
                        val addHistoryProgressBar = bindings.actionButtonAddHistoryProgressbar

                        addHistoryButton.isEnabled = true
                        addHistoryProgressBar.visibility = View.GONE

                        val syncResponse = event.syncResponse

                        if(syncResponse is Resource.Success) {
                            val data = syncResponse.data

                            if(data?.added?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully added ${episode?.name ?: "episode"} to your watch history!", Toast.LENGTH_SHORT)
                            } else {
                                displayMessageToast("Unable to add current episode to watched history", Toast.LENGTH_LONG)
                            }
                        } else if(syncResponse is Resource.Error) {
                            syncResponse.error?.printStackTrace()

                            displayMessageToast("Error adding episode to your watched history", Toast.LENGTH_LONG)
                        }
                    }
                    is EpisodeDetailsActionButtonsViewModel.Event.AddRatingsEvent -> {
                        val syncResponse = event.syncResponse.data?.first
                        val newRating = event.syncResponse.data?.second

                        if(event.syncResponse is Resource.Success) {
                            if(syncResponse?.added?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully rated ${episode?.name ?: "Unknown"} with ${Rating.fromValue(newRating ?: 7).name} ($newRating)", Toast.LENGTH_LONG)

                                viewModel.updateRatings(newRating ?: 0)
                            } else {
                                displayMessageToast("Could not add rating, please try again later", Toast.LENGTH_LONG)
                            }

                        } else if(event.syncResponse is Resource.Error) {
                            displayMessageToast("Error adding rating. Error: ${event.syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)
                            event.syncResponse.error?.printStackTrace()
                        }
                    }

                    is EpisodeDetailsActionButtonsViewModel.Event.DeleteRatingsEvent -> {
                        val syncResponse = event.syncResponse.data

                        if(event.syncResponse is Resource.Success) {
                            if(syncResponse?.deleted?.episodes ?: 0 > 0) {
                                displayMessageToast("Successfully reset rating!", Toast.LENGTH_LONG)

                                viewModel.updateRatings(null)
                            } else {
                                displayMessageToast("Could not delete rating, please try again later", Toast.LENGTH_LONG)
                            }

                        } else if(event.syncResponse is Resource.Error) {
                            displayMessageToast("Error deleting rating. Error: ${event.syncResponse.error?.localizedMessage}", Toast.LENGTH_LONG)
                            event.syncResponse.error?.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun initCheckinDialog() {

        if(checkinDialog != null) {
            return
        }

        checkinDialog = AlertDialog.Builder(requireContext())
            .setTitle("Start watching ${episode?.name}")
            .setMessage("Do you want to start watching ${episode?.name}?")
            .setPositiveButton("Yes" ) { dialogInterface, i -> checkin() }
            .setNegativeButton("Cancel" ) { dialogInterface, i -> dialogInterface.dismiss() }
            .create()
    }

    private fun initCancelCheckinDialog() {

        if(cancelCheckinDialog != null) {
            return
        }

        cancelCheckinDialog = AlertDialog.Builder(requireContext())
            .setTitle("You are already watching something. What do you want to do?")
            .setItems(arrayOf("Start watching ${episode?.name ?: " this episode"}", "Nothing", "Just cancel checkin")) { dialogInterface, i ->
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

    private fun initAddWatchedHistoryDialog() {

        if(addWatchedHistoryDialog != null) {
            return
        }

        addWatchedHistoryDialog = WatchedDatePickerFragment(onWatchedDateChanged = { watchedAt ->
            val addHistoryButton = bindings.actionbuttonAddHistory
            val addHistoryProgressBar = bindings.actionButtonAddHistoryProgressbar

            addHistoryButton.isEnabled = false
            addHistoryProgressBar.visibility = View.VISIBLE

            viewModel.addToWatchedHistory(episode!!, watchedAt)
        })
    }

    private fun initRatingDialog() {

        if(ratingPickerFragment != null) {
            return
        }

        ratingPickerFragment = RatingPickerFragment(callback = { newRating ->
            if(newRating != -1) {
                // New rating to be added
                viewModel.addRating(newRating, episode?.episode_trakt_id ?: -1)
            } else {
                // User reset rating
                viewModel.resetRating(episode?.episode_trakt_id ?: -1)
            }
        }, episode?.name ?: "Episode")
    }

    private fun checkin() {
        viewModel.checkin(episode?.episode_trakt_id ?: -1)
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

interface OnEpisodeChangeListener {
    fun bindEpisode(episode: TmEpisode)
}