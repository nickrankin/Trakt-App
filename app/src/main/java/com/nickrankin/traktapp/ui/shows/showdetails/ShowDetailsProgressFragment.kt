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
import com.nickrankin.traktapp.dao.show.TmSeasonAndStats
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.ShowDetailsProgressFragmentBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsProgressViewModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.OnNavigateToEpisode
import com.uwetrottmann.trakt5.entities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.ZoneId
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


        // Get data
        getProgress()
        getEvents()
    }

    private fun getProgress() {
        lifecycleScope.launchWhenStarted {
            viewModel.seasons.collectLatest { seasonsResource ->
                when (seasonsResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getProgress: Got progress")
                        bindings.apply {
                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.VISIBLE
                            showdetailsprogressfragmentContainer.visibility = View.GONE
                        }

                    }
                    is Resource.Success -> {
                        bindings.apply {
                            showdetailsprogressfragmentMainGroup.visibility = View.VISIBLE

                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.GONE
                            showdetailsprogressfragmentContainer.visibility =
                                if (progressVisibilityToggle) View.VISIBLE else View.GONE

                        }

                        val seasons = seasonsResource.data?.sortedBy { it.season.season_number }

                        // Every season object has every seasons stats for that show, as we use Show Trakt ID as PK for DAO. So we only need to use the first entry to calculate an overall progress
                        val seasonStatsData = seasons?.first()?.watchedSeasonStats

                        if (seasons != null) {
                            var totalAired = 0
                            var complete = 0

                            seasonStatsData?.map { seasonStat ->
                                totalAired += seasonStat?.aired ?: 0
                                complete += seasonStat?.completed ?: 0
                            }

                            val overallProgress = calculateProgress(
                                complete.toDouble() ?: 0.0,
                                totalAired.toDouble() ?: 0.0
                            )

                            // Overall progress
                            bindings.showdetailsprogressfragmentOverallprogress.apply {
                                showdetailsprogressfragmentProgressTitle.text =
                                    "Overall Progress ($overallProgress%)"
                                showdetailsprogressfragmentProgressbarOverall.progress =
                                    overallProgress
                            }

                            //Show individual Seasons progress
                            seasons.map { season ->
                                progressBarContainerLayout.addView(buildProgressItem(season))
                            }

                        } else {
                            Log.d(TAG, "getProgress: Show progress is null")
                        }
                    }
                    is Resource.Error -> {
                        bindings.apply {
                            showdetailsprogressfragmentMainGroup.visibility = View.GONE

                            showdetailsprogressfragmentLoadingProgressbar.visibility = View.GONE
                            showdetailsprogressfragmentContainer.visibility = View.GONE
                        }

                        Log.e(
                            TAG,
                            "getProgress: Error getting progress ${seasonsResource.error?.localizedMessage}",
                        )
                        seasonsResource.error?.printStackTrace()
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

    private fun buildProgressItem(tmSeasonAndStats: TmSeasonAndStats): ConstraintLayout {
        val stats = tmSeasonAndStats.watchedSeasonStats.find { it?.season ?: 0 == tmSeasonAndStats.season.season_number }
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
            calculateProgress(stats?.completed?.toDouble() ?: 0.0, stats?.aired?.toDouble() ?: 0.0)

        titleField.text = "Season ${stats?.season} Progress ($overallProgress%)"
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

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                showTraktId,
                showTmdbId,
                seasonNumber,
                episodeNumber,
                language
            )
        )
        // No need to force refresh of watched shows as this was done in this activity so assume the watched show data in cache is up to date
        intent.putExtra(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, false)

        startActivity(intent)
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