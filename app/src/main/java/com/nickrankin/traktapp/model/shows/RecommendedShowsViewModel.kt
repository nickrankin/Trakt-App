package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.shows.suggested.RecommendedShowsRepository
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PAGE_LIMIT = 10
@HiltViewModel
class RecommendedShowsViewModel @Inject constructor(private val repository: RecommendedShowsRepository): ViewSwitcherViewModel() {

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val suggestedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSuggestedShows(shouldRefresh)
    }

    fun addToCollection(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(repository.addToCollection(syncItems))) }

    fun removeFromSuggestions(show: Show) = viewModelScope.launch {
        val response = repository.removeSuggestion(show)
        eventsChannel.send(Event.RemoveSuggestionEvent(response))
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveSuggestionEvent(val removedSuccessfully: Resource<Boolean>): Event()
    }
}