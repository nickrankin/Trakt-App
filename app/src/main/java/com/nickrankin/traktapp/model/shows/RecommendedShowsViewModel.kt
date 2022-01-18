package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.suggested.RecommendedShowsRepository
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PAGE_LIMIT = 10
@HiltViewModel
class RecommendedShowsViewModel @Inject constructor(private val repository: RecommendedShowsRepository): ViewModel() {

    val suggestedShows = repository.suggestedShowsChannel.receiveAsFlow()
    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.getSuggestedShows()
        }
    }

    fun addToCollection(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(repository.addToCollection(syncItems))) }

    fun removeFromSuggestions(traktId: String) = viewModelScope.launch {
        val response = repository.removeSuggestion(traktId)
        eventsChannel.send(Event.RemoveSuggestionEvent(response.first, response.second))
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveSuggestionEvent(val removedSuccessfully: Boolean, var t: Throwable?): Event()
    }
}