package com.nickrankin.traktapp.ui.shows.showdetails

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.stats.model.WatchedSeasonStats
import com.nickrankin.traktapp.databinding.ShowDetailsProgressFragmentBinding
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsFragmentsViewModel
import com.nickrankin.traktapp.ui.IOnStatistcsRefreshListener
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

private const val TAG = "HERE"
@AndroidEntryPoint
class ShowDetailsProgressFragment: Fragment(), IOnStatistcsRefreshListener {
    private lateinit var bindings: ShowDetailsProgressFragmentBinding

    private val viewModel: ShowDetailsFragmentsViewModel by activityViewModels()
    private lateinit var progressCardview: CardView
    private lateinit var progressItemContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val allSeasonsToggleChannel = Channel<Boolean>()
    private val allSeasonsToggled = allSeasonsToggleChannel.receiveAsFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = ShowDetailsProgressFragmentBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressCardview = bindings.showdetailsprogressfragmentCardview
        progressItemContainer = bindings.showdetailsprogressfragmentContainer

        progressCardview.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                allSeasonsToggleChannel.send(!allSeasonsToggled.value)
            }

            toggleProgress()
        }

        getProgress()
    }
    
    private fun getProgress() {
        progressBar = bindings.showdetailsprogressfragmentLoadingProgressbar

        lifecycleScope.launchWhenStarted { 
            viewModel.overallSeasonStats().collectLatest { seasonData ->
                progressItemContainer.removeAllViews()

                allSeasonsToggleChannel.send(false)

                var totalEpisodes = 0
                var totalWatched = 0
                
                seasonData.map { stats ->
                    totalEpisodes += stats.aired
                    totalWatched += stats.completed
                }

                val overallProgressPercentage = calculateProgress(totalWatched.toDouble(), totalEpisodes.toDouble())

                bindings.showdetailsprogressfragmentOverview.apply {
                    showdetailsprogressfragmentProgressTitle.text = "Overall progress ($overallProgressPercentage%)"
                    showdetailsprogressfragmentProgressbar.progress = overallProgressPercentage
                }

                seasonData.map { seasonStat ->
                    progressItemContainer.addView(buildProgressItem(seasonStat))
                }

            }
        }
    }

    private fun toggleProgress() {
        lifecycleScope.launchWhenStarted {
            allSeasonsToggled.collectLatest { isToggled ->
                if(isToggled) {
                    progressItemContainer.visibility = View.VISIBLE
                } else {
                    progressItemContainer.visibility = View.GONE
                }
            }
        }

    }

    private fun buildProgressItem(watchedSeasonStats: WatchedSeasonStats): View {
        val progressView = layoutInflater.inflate(R.layout.item_progress_layout_item, null)

        val title = progressView.findViewById<TextView>(R.id.showdetailsprogressfragment_progress_title)
        val progressBar = progressView.findViewById<ProgressBar>(R.id.showdetailsprogressfragment_progressbar)

        val progressPercent = calculateProgress(watchedSeasonStats.completed.toDouble(), watchedSeasonStats.aired.toDouble())

        title.text = "Season ${watchedSeasonStats.season} ($progressPercent)"
        progressBar.progress = progressPercent

        progressView.setOnClickListener {
            val seasonEpisodesIntent = Intent(activity, SeasonEpisodesActivity::class.java)
            seasonEpisodesIntent.putExtra(SeasonEpisodesActivity.SEASON_DATA_KEY,
            SeasonDataModel(
                watchedSeasonStats.show_trakt_id,
                null,
                watchedSeasonStats.season,
                ""
            ))

            startActivity(seasonEpisodesIntent)

        }

        return progressView
    }

    override fun onRefresh(isRefreshing: Boolean) {
        if(this.isAdded) {
            if(isRefreshing) {
                progressBar.visibility = View.VISIBLE
                progressBar.bringToFront()
            }
        } else {
            progressBar.visibility = View.GONE
        }
    }


    companion object {
        fun newInstance() = ShowDetailsProgressFragment()
    }
}