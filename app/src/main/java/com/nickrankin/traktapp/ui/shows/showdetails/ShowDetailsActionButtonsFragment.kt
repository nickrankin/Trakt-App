package com.nickrankin.traktapp.ui.shows.showdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.stats.model.CollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.databinding.LayoutActionButtonsBinding
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.ui.ActionButtonsBaseFragment
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import org.threeten.bp.OffsetDateTime

private const val TAG = "ShowDetailsActionButton"
@AndroidEntryPoint
class ShowDetailsActionButtonsFragment : ActionButtonsBaseFragment() {

    private val viewModel: ShowDetailsViewModel by activityViewModels()

    private var _bindings: LayoutActionButtonsBinding? = null
    private val bindings get() = _bindings!!

    private val traktRatingChannel = Channel<Double?>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bindings = LayoutActionButtonsBinding.inflate(inflater)

        return bindings.root
    }

    override fun setup(config: (bindings: LayoutActionButtonsBinding, traktId: Int, title: String, type: Type, enableCheckinButton: Boolean, enableHistoryButton: Boolean) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.show.collectLatest { showResource ->
                when(showResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "setup: Loading Show")
                    }
                    is Resource.Success -> {
                        val show = showResource.data

                        if(show != null) {
                            config(
                                bindings,
                                show.trakt_id,
                                show.name,
                                Type.SHOW,
                                false,
                                false
                            )
                        }

                        traktRatingChannel.send(show?.trakt_rating)
                    }
                    is Resource.Error -> {
                        handleError(showResource.error, null)
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

    override fun updatePlayCount(onPlayCountUpdated: (List<HistoryEntry>) -> Unit) {
        lifecycleScope.launchWhenStarted { 
            viewModel.playCount.collectLatest { playcountResource ->
                when(playcountResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "updatePlayCount: Loading playcount")
                    }
                    is Resource.Success -> {
                        onPlayCountUpdated(playcountResource.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        handleError(playcountResource.error, null)
                    }
                }
            }
        }
    }

    override fun removeHistoryEntry(historyEntry: HistoryEntry) {
       viewModel.removeHistoryEntry(historyEntry)
    }

    override fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime) {
       viewModel.addToWatchedHistory(traktId, watchedDate)
    }

    override fun updateRatingText(onRatingChanged: (ratingStats: RatingStats?) -> Unit) {
        lifecycleScope.launchWhenStarted {
            viewModel.ratings.collectLatest { ratingResource ->
                when(ratingResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "updateRatingText: Loading ratings")
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

    override fun setTraktRating(onRatingChanged: (traktRating: Double?) -> Unit) {
        lifecycleScope.launchWhenStarted {
            traktRatingChannel.receiveAsFlow().collectLatest { traktRating ->
                onRatingChanged(traktRating)
            }
        }
    }

    override fun setNewRating(traktId: Int, newRating: Int) {
        viewModel.addRating(newRating, traktId)
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
            viewModel.collectionStatus.collectLatest { collectionStatusResource ->
                when(collectionStatusResource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "getCollectedStats: Loading Collection Status")
                    }
                    is Resource.Success -> {
                        oncollectedStateChanged(collectionStatusResource.data)
                    }
                    is Resource.Error -> {
                        handleError(collectionStatusResource.error, null)
                    }
                }
            }
        }
    }

    override fun addToCollection(traktId: Int) {
        viewModel.addToCollection(traktId)
    }

    override fun removeFromCollection(traktId: Int) {
        viewModel.deleteFromCollection(traktId)
    }

    override fun getLists(onListsChanged: (listEntries: List<Pair<TraktList, List<TraktListEntry>>>) -> Unit) {
     lifecycleScope.launchWhenStarted { 
         viewModel.lists.collectLatest { listsResource ->
             when(listsResource) {
                 is Resource.Loading -> {
                     Log.d(TAG, "getLists: Loading lists ...")
                 }
                 is Resource.Success -> {
                     onListsChanged(listsResource.data ?: emptyList())
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
        fun newInstance() = ShowDetailsActionButtonsFragment()
    }
}