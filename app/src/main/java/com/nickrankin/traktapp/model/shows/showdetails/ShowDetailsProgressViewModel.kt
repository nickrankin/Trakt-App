package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsProgressRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsProgressView"
@HiltViewModel
class ShowDetailsProgressViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: ShowDetailsProgressRepository) : ViewModel() {

    val progress = repository.progressChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: -1
    private val tmndbId: Int? = savedStateHandle.get(ShowDetailsRepository.SHOW_TMDB_ID_KEY)

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    init {
        viewModelScope.launch {
            repository.refreshShowProgress(traktId)
        }
    }

    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getWatchedEpisodes(true, traktId).map {
            if (it is Resource.Success) {
                it.data = it.data?.sortedBy { watchedEpisode ->
                    watchedEpisode.watched_at
                }
            }
            it
        }
    }

    fun removeWatchedEpisode(syncItems: SyncItems) = viewModelScope.launch {
        eventsChannel.send(Event.DeleteWatchedHistoryItemEvent(repository.removeWatchedEpisode(syncItems)))
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Called")
        viewModelScope.launch {
            launch {
                refreshEventChannel.send(true)
            }
            launch {
                repository.refreshShowProgress(traktId)
            }
        }
    }

    sealed class Event {
        data class DeleteWatchedHistoryItemEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

}