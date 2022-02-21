package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.history.EpisodeWatchedHistoryItemAdapter
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ShowDetailsProgressFragmentBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsProgressViewModel
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.shows.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToEpisode
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "ShowDetailsProgressFrag"

@AndroidEntryPoint
class ShowDetailsProgressFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener,
    OnNavigateToEpisode {
    private val viewModel: ShowDetailsProgressViewModel by activityViewModels()
    private lateinit var bindings: ShowDetailsProgressFragmentBinding

    private lateinit var overallProgressCardView: CardView
    private var progressVisibilityToggle = false
    private lateinit var progressBarContainerLayout: LinearLayout

    private lateinit var watchedHistoryItemsDialog: AlertDialog
    private lateinit var episodeWatchedHistoryItemAdapter: EpisodeWatchedHistoryItemAdapter

    private var showTraktId: Int = 0
    private var showTmdbId: Int? = null


    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var glide: RequestManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        bindings = ShowDetailsProgressFragmentBinding.inflate(layoutInflater)

        return bindings.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Variables
        showTraktId = activity?.intent?.getIntExtra(ShowDetailsRepository.SHOW_TRAKT_ID_KEY, -1) ?: -1
        showTmdbId = activity?.intent?.getIntExtra(ShowDetailsRepository.SHOW_TMDB_ID_KEY, -1) ?: -1

        // Views
        overallProgressCardView = bindings.showdetailsprogressfragmentOverallprogressCardview
        progressBarContainerLayout = bindings.showdetailsprogressfragmentContainer

        // Season progress toggle
        overallProgressCardView.setOnClickListener { toggleSeasonProgress() }
        // TODO Clicking will go to relevant Season page
        progressBarContainerLayout.setOnClickListener { }

        initWatchedHistoryRecycler()

        // Get data
        getProgress()
        getLastWatched()
        getEvents()
    }

    private fun getProgress() {
        lifecycleScope.launchWhenStarted {
            viewModel.progress.collectLatest { progressResource ->
                when (progressResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getProgress: Got progress")
                        bindings.apply {
                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.VISIBLE
                            showdetailsprogressfragmentContainer.visibility = View.GONE
                        }

                    }
                    is Resource.Success -> {
                        bindings.apply {
                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.GONE
                            showdetailsprogressfragmentContainer.visibility =
                                if (progressVisibilityToggle) View.VISIBLE else View.GONE

                        }

                        val progress = progressResource.data

                        if (progress != null) {
                            val overallProgress = calculateProgress(
                                progress.completed?.toDouble() ?: 0.0,
                                progress.aired?.toDouble() ?: 0.0
                            )

                            // Overall progress
                            bindings.showdetailsprogressfragmentOverallprogress.apply {
                                showdetailsprogressfragmentProgressTitle.text =
                                    "Overall Progress ($overallProgress%)"
                                showdetailsprogressfragmentProgressbarOverall.progress =
                                    overallProgress
                            }

                            // Up Next
                            if(progress.next_episode != null) {
                                displayNextEpisode(progress.next_episode)
                            }

                            //Show individual Seasons progress
                            progress.seasons?.map { baseSeason ->
                                progressBarContainerLayout.addView(buildProgressItem(baseSeason))
                            }

                        } else {
                            Log.d(TAG, "getProgress: Show progress is null")
                        }
                    }
                    is Resource.Error -> {
                        bindings.apply {
                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.GONE
                            showdetailsprogressfragmentContainer.visibility = View.GONE
                        }

                        Log.e(
                            TAG,
                            "getProgress: Error getting progress ${progressResource.error?.localizedMessage}",
                        )
                        progressResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when(event) {
                    is ShowDetailsProgressViewModel.Event.DeleteWatchedHistoryItemEvent -> {
                        val syncResponse = event.syncResponse.data

                        if (event.syncResponse is Resource.Success) {
                            if (syncResponse?.deleted?.episodes ?: 0 > 0) {
                                displayToastMessage("Successfully removed play", Toast.LENGTH_LONG)
                            } else if (syncResponse?.not_found?.episodes?.isNotEmpty() == true) {
                                displayToastMessage(
                                    "Could not locate play with this ID, error deleting watched history.",
                                    Toast.LENGTH_LONG
                                )
                            }
                        } else {
                            displayToastMessage(
                                "Error removing play ${event.syncResponse.error?.localizedMessage}",
                                Toast.LENGTH_LONG
                            )
                        }
                    }
                }
            }
        }
    }

    private fun displayNextEpisode(nextEpisode: Episode?) {
        bindings.showdetailsprogressfragmentNextEpisode.apply {
            episodeitemCardview.visibility = View.VISIBLE

            episodeitemUpNextTitle.text = "Up next to Watch"

            episodeitemName.text = nextEpisode?.title

            if (nextEpisode?.first_aired != null) {
                episodeitemAirDate.visibility = View.VISIBLE

                episodeitemAirDate.text = "First aired: " + nextEpisode.first_aired?.format(
                    DateTimeFormatter.ofPattern(
                        sharedPreferences.getString(
                            "date_format",
                            AppConstants.DEFAULT_DATE_TIME_FORMAT
                        )
                    )
                )
            }

            if (nextEpisode?.season != null && nextEpisode.number != null) {
                episodeitemNumber.text = "S${nextEpisode.season}E${nextEpisode.number}"
            }

            if (nextEpisode?.overview != null) {
                episodeitemOverview.visibility = View.VISIBLE

                episodeitemOverview.text = nextEpisode.overview
            }

            root.setOnClickListener {
                navigateToEpisode(showTraktId, showTmdbId, nextEpisode?.season ?: 0, nextEpisode?.number ?: 0, null)
            }
        }
    }

    private fun getLastWatched() {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest { watchedEpisodesResource ->
                when(watchedEpisodesResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "displayWatchedEpisodes: Loading Watched Episodes")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "displayWatchedEpisodes: Got watched episodes")
                        val watchedEpisodes = watchedEpisodesResource.data
                            ?.sortedBy { it.watched_at }?.reversed()

                        if(watchedEpisodes?.isNotEmpty() == true) {

                            episodeWatchedHistoryItemAdapter.submitList(watchedEpisodes)

                            val lastWatched = watchedEpisodes.first()

                            displayLastWatched(lastWatched)
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "displayWatchedEpisodes: Error getting watched episodes. ${watchedEpisodesResource.error?.localizedMessage}", )
                        watchedEpisodesResource.error?.printStackTrace()
                    }
                }
            }
        }
    }

    private fun displayLastWatched(lastWatched: WatchedEpisode?) {
        bindings.showdetailsprogressfragmentLastWatched.apply {
            episodeitemCardview.visibility = View.VISIBLE

            episodeitemUpNextTitle.text = "Last Watched"
            episodeitemName.text = lastWatched?.episode_title

            if (lastWatched?.watched_at != null) {
                episodeitemAirDate.visibility = View.VISIBLE

                episodeitemAirDate.text = "Last watched: " + lastWatched.watched_at?.format(
                    DateTimeFormatter.ofPattern(
                        sharedPreferences.getString(
                            "date_format",
                            AppConstants.DEFAULT_DATE_TIME_FORMAT
                        )
                    )
                )
            }

            if (lastWatched?.episode_season != null && lastWatched.episode_number != null) {
                episodeitemNumber.text = "S${lastWatched.episode_season}E${lastWatched.episode_number}"
            }

            if (lastWatched?.episode_overview != null) {
                episodeitemOverview.visibility = View.VISIBLE

                episodeitemOverview.text = lastWatched.episode_overview
            }

            root.setOnClickListener {
                watchedHistoryItemsDialog.show()
            }
        }
    }

    private fun initWatchedHistoryRecycler() {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_watched_history_items, bindings.root, false)

        watchedHistoryItemsDialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .setTitle("Watched History")
            .setNegativeButton("Close", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            }).create()

        val recyclerView = dialogLayout.findViewById<RecyclerView>(R.id.watched_history_item_recyclerview)
        val layoutManager = LinearLayoutManager(requireContext())

        episodeWatchedHistoryItemAdapter = EpisodeWatchedHistoryItemAdapter(callback = { watchedEpisode ->
            handleWatchedShowDelete(watchedEpisode)
        })

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = episodeWatchedHistoryItemAdapter



    }

    private fun buildProgressItem(season: BaseSeason): ConstraintLayout {
        val overallProgressLayout = layoutInflater.inflate(
            R.layout.item_progress_layout_item,
            progressBarContainerLayout,
            false
        ) as ConstraintLayout

        val titleField =
            overallProgressLayout.findViewById<TextView>(R.id.showdetailsprogressfragment_progress_title)
        val progressBarField =
            overallProgressLayout.findViewById<ProgressBar>(R.id.showdetailsprogressfragment_progressbar_overall)


        val overallProgress =
            calculateProgress(season.completed?.toDouble() ?: 0.0, season.aired?.toDouble() ?: 0.0)

        titleField.text = "Season ${season.number} Progress ($overallProgress%)"
        progressBarField.progress = overallProgress

        return overallProgressLayout
    }

    private fun toggleSeasonProgress() {
        progressVisibilityToggle = !progressVisibilityToggle

        if (progressVisibilityToggle) {
            progressBarContainerLayout.visibility = View.VISIBLE
        } else {
            progressBarContainerLayout.visibility = View.GONE
        }
    }

    override fun navigateToEpisode(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String?
    ) {
        val intent = Intent(requireContext(), EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY, showTraktId)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, showTmdbId)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, seasonNumber)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, episodeNumber)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, language)

        // No need to force refresh of watched shows as this was done in this activity so assume the watched show data in cache is up to date
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, false)

        startActivity(intent)
    }

    private fun handleWatchedShowDelete(watchedEpisode: WatchedEpisode) {
        val syncItem = SyncItems().ids(watchedEpisode.id)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete ${watchedEpisode.episode_title} from your watched history?")
            .setMessage("Are you sure you want to remove ${watchedEpisode.episode_title} from your Trakt History?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.removeWatchedEpisode(syncItem)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            .create()

        dialog.show()
    }

    private fun displayToastMessage(message: String, length: Int) {
        Toast.makeText(requireContext(), message, length).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onRefresh() {
        Log.d(TAG, "onRefresh: Refreshing Show Progress")

        // To prevent duplication of Season specific progress
        progressBarContainerLayout.removeAllViews()

        viewModel.onRefresh()
    }

    companion object {
        fun newInstance() = ShowDetailsProgressFragment()
    }


}