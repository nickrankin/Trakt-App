package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.movies.MovieDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"

@HiltViewModel
class ShowDetailsActionButtonsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ShowDetailsActionButtonsRepository,
    private val statsRepository: StatsRepository,
    private val listsRepository: TraktListsRepository,
    private val listEntryRepository: ListEntryRepository
) : ViewModel() {

    private var showDataModel: ShowDataModel? = savedStateHandle.get<ShowDataModel>(
        ShowDetailsActivity.SHOW_DATA_KEY)


    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    val listsWithEntries = listsRepository.listsWithEntries

    fun getRatings(traktId: Int) = repository.getRatings(traktId)

    fun addRating(tmShow: TmShow, newRating: Int) = viewModelScope.launch {
        eventsChannel.send(Event.AddRatingEvent(repository.setRatings(tmShow, newRating, false), newRating))
    }

    fun deleteRating(tmShow: TmShow) = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingEvent(repository.setRatings(tmShow, -1, true))) }

    fun addToCollection(tmShow: TmShow) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(repository.addToCollection(tmShow))) }

    val showsCollectedStatus = repository.getCollectedShowFlow(showDataModel?.traktId ?: -1).map { collectedShow ->
        collectedShow != null
    }

    fun addListEntry(type: String, traktId: Int, traktList: TraktList) = viewModelScope.launch { eventsChannel.send(
       Event.AddListEntryEvent(listEntryRepository.addListEntry(type, traktId, traktList))) }
    fun removeListEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventsChannel.send(
        Event.RemoveListEntryEvent(listEntryRepository.removeEntry(listTraktId, listEntryTraktId, type))) }

    fun removeFromCollection() = viewModelScope.launch { eventsChannel.send(Event.RemoveFromCollectionEvent(repository.removeFromCollection(showDataModel?.traktId ?: -1))) }


    fun onStart() {
        viewModelScope.launch {
            eventsChannel.send(Event.RefreshShowStatsEvent(statsRepository.refreshShowStats(false)))
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            eventsChannel.send(Event.RefreshShowStatsEvent(statsRepository.refreshShowStats(true)))
        }
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): Event()
        data class DeleteRatingEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RefreshShowStatsEvent(val refreshShowStatsResponse: Resource<Boolean>): Event()
        data class AddListEntryEvent(val addListEntryResponse: Resource<SyncResponse>): Event()
        data class RemoveListEntryEvent(val removeListEntryResponse: Resource<SyncResponse?>): Event()
    }
}