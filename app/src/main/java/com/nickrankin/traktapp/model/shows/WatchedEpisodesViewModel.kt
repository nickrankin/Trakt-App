package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchedEpisodesViewModel @Inject constructor(private val repository: WatchedEpisodesRepository): ViewSwitcherViewModel() {

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.watchedEpisodes(shouldRefresh)
    }

    fun removeFromWatchedHistory(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(
        Event.RemoveWatchedHistoryEvent(repository.deleteFromWatchedHistory(syncItems))
    ) }

    sealed class Event {
        data class RemoveWatchedHistoryEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

}