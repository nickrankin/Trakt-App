package com.nickrankin.traktapp.ui.shows.episodedetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.databinding.LayoutActionButtonsBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.ui.ActionButtonsBaseFragment
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime

private const val TAG = "EpisodeDetailsActionBut"

@AndroidEntryPoint
class EpisodeDetailsActionButtonsFragment : ActionButtonsBaseFragment() {

    private val viewModel: EpisodeDetailsViewModel by viewModels(
        ownerProducer = {
            requireParentFragment()
        }
    )

    private var _bindings: LayoutActionButtonsBinding? = null
    private val bindings get() = _bindings!!

    private val traktRatingChannel = Channel<Double>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = LayoutActionButtonsBinding.inflate(layoutInflater)

        return bindings.root
    }

    override fun setup(config: (bindings: LayoutActionButtonsBinding, traktId: Int, title: String, type: Type, enableCheckinButton: Boolean, enableHistoryButton: Boolean) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episodeResource ->
                when (episodeResource) {
                    is Resource.Loading -> {
                        bindings.actionbuttonsProgressbar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        val episode = episodeResource.data

                        if (episode != null) {
                                config(
                                    bindings,
                                    episode.episode_trakt_id,
                                    episode.name
                                        ?: "S${episode.season_number}E${episode.episode_number}",
                                    Type.EPISODE,
                                    true,
                                    true
                                )

                            traktRatingChannel.send(episode.trakt_rating)

                            bindings.actionbuttonsProgressbar.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        handleError(episodeResource.error, null)
                        bindings.actionbuttonsProgressbar.visibility = View.GONE
                    }

                }
            }
        }
    }

    override fun onNewEvent(onNewEvent: (event: ActionButtonEvent) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                onNewEvent(event)
            }
        }
    }

    override fun setTraktRating(onRatingChanged: (traktRating: Double?) -> Unit) {
        lifecycleScope.launchWhenStarted {
            traktRatingChannel.receiveAsFlow().collectLatest { traktRating ->
                onRatingChanged(traktRating)
            }
        }

    }

    override fun updatePlayCount(onPlayCountUpdated: (List<HistoryEntry>) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.watchedEpisodes.collectLatest { watchedEpisodesResource ->
                when (watchedEpisodesResource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        onPlayCountUpdated(watchedEpisodesResource.data!!)
                    }
                    is Resource.Error -> {
                        Log.e(
                            TAG,
                            "updatePlayCount: Error getting play history, ${watchedEpisodesResource.error}",
                        )
                    }
                }
            }
        }
    }

    override fun removeHistoryEntry(historyEntry: HistoryEntry) {
        viewModel.deleteFromHistory(historyEntry.history_id)
    }

    override fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime) {
        viewModel.addToHistory(traktId, watchedDate)
    }

    override fun updateRatingText(
        onRatingChanged: (ratingStats: RatingStats?) -> Unit
    ) {
        lifecycleScope.launchWhenStarted {
            viewModel.rating.collectLatest { ratingResource ->
                when(ratingResource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        onRatingChanged(ratingResource.data)
                    }
                    is Resource.Error -> {
                        handleError(ratingResource.error, null)
                    }

                }
            }
        }
    }

    override fun setNewRating(traktId: Int, newRating: Int) {
        viewModel.addRating(traktId, newRating, OffsetDateTime.now())
    }

    override fun deleteRating(traktId: Int) {
        viewModel.deleteRating(traktId)
    }

    override fun checkin(traktId: Int) {
        viewModel.checkin(traktId, false)
    }

    override fun overrideCheckin(traktId: Int) {
        viewModel.checkin(traktId, true)
    }

    override fun getCollectedStats(
        traktId: Int,
        oncollectedStateChanged: (collectedStats: CollectedStats?) -> Unit
    ) {
        lifecycleScope.launchWhenStarted {

            viewModel.collectionStatus.collectLatest { collectedEpisodesStatsResource ->
                when (collectedEpisodesStatsResource) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        Log.e(TAG, "getCollectedStats: HERE ${collectedEpisodesStatsResource.data}", )
                        oncollectedStateChanged(collectedEpisodesStatsResource.data)
                    }
                    is Resource.Error -> {
                        handleError(collectedEpisodesStatsResource.error, null)
                    }
                }
            }
        }
    }

    override fun addToCollection(traktId: Int) {
        viewModel.addToCollection(traktId)
    }

    override fun removeFromCollection(traktId: Int) {
        lifecycleScope.launchWhenStarted {
            val episode = viewModel.episode.first().data
            
            if(episode != null) {
                viewModel.removeFromCollection(traktId, episode.show_trakt_id, episode.season_number ?: 0, episode.episode_number ?: 0)
            } else {
                Log.e(TAG, "removeFromCollection: Cannot remove episode (episode cannt be null)", )
            }
        }

    }

    override fun getLists(onListsChanged: (listEntries: List<Pair<TraktList, List<TraktListEntry>>>) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.lists.collectLatest { listsResource ->
                when(listsResource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val lists = listsResource.data

                        if(lists != null) {
                            onListsChanged(lists)
                        }

                    }
                    is Resource.Error -> {
                        handleError(listsResource.error, null)
                    }

                }

            }
        }
    }
    override fun addListEntry(traktId: Int, traktList: TraktList) {
        viewModel.addListEntry(traktId, traktList.trakt_id)
    }

    override fun removeListEntry(traktId: Int, traktList: TraktList) {
        viewModel.removeListEntry(traktId, traktList.trakt_id)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }

    companion object {
        fun newInstance() = EpisodeDetailsActionButtonsFragment()
    }
}