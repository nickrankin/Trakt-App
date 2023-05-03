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
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.databinding.ActionButtonsFragmentBinding
import com.nickrankin.traktapp.databinding.LayoutActionButtonsBinding
import com.nickrankin.traktapp.helper.IHandleError
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.Response
import com.nickrankin.traktapp.helper.getSyncResponse
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.shows.EpisodeDetailsViewModel
import com.nickrankin.traktapp.ui.ActionButtonsBaseFragment
import com.nickrankin.traktapp.ui.dialog.RatingPickerFragment
import com.nickrankin.traktmanager.ui.dialoguifragments.WatchedDatePickerFragment
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActionBut"

@AndroidEntryPoint
class EpisodeDetailsActionButtonsFragment : ActionButtonsBaseFragment() {

    private val viewModel: EpisodeDetailsViewModel by activityViewModels()

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

    override fun setup(config: (bindings: LayoutActionButtonsBinding, traktId: Int, title: String, type: Type) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.episode.collectLatest { episodeResource ->
                when (episodeResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getEpisode: Loading Episode")
                    }
                    is Resource.Success -> {
                        val episode = episodeResource.data

                        if (episode != null) {
                            if (isLoggedIn) {
                                config(
                                    bindings,
                                    episode.episode_trakt_id,
                                    episode.name
                                        ?: "S${episode.season_number}E${episode.episode_number}",
                                    Type.EPISODE
                                )
                            }

                            traktRatingChannel.send(episode.trakt_rating)
                        }
                    }
                    is Resource.Error -> {
                        handleError(episodeResource.error, null)
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
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        Log.e(TAG, "updatePlayCount: ${watchedEpisodesResource.data}")
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
                    is Resource.Loading -> {
                    }
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
                    is Resource.Loading -> {}
                    is Resource.Success -> {
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
        viewModel.removeFromCollection(traktId)
    }

    override fun getLists(onListsChanged: (listEntries: List<Pair<TraktList, List<TraktListEntry>>>) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.lists.collectLatest { listsResource ->
                when(listsResource) {
                    is Resource.Loading -> {

                    }
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