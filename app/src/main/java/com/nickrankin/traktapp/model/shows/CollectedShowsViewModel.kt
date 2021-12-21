package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectedShowsViewModel @Inject constructor(val repository: CollectedShowsRepository): ViewModel() {

    private val showRefreshEventChannel = Channel<Boolean>()
    private val showRefreshEvent = showRefreshEventChannel.receiveAsFlow()

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private var sortBy = SortBy.ADDED
    private var sortHow = SortHow.DESC

    @ExperimentalCoroutinesApi
    val collectedShows = showRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getCollectedShows(shouldRefresh).map { showsResource ->
            // Sort only when we receive some shows
            if(showsResource is Resource.Success) {
                showsResource.data = doSorting(showsResource.data ?: emptyList())
            }
            showsResource
        }
    }

    fun sortShows(sortBy: SortBy) {
        if(this.sortBy == sortBy) {
            if(sortHow == SortHow.DESC) {
                sortHow = SortHow.ASC
            } else {
                sortHow = SortHow.DESC
            }
        }

        this.sortBy = sortBy

        viewModelScope.launch {
            showRefreshEventChannel.send(false)
        }
    }

    private fun doSorting(shows: List<CollectedShow>): List<CollectedShow> {
        var sortedShows: List<CollectedShow> = listOf()
        when(sortBy) {
            SortBy.TITLE -> {
                sortedShows = shows.sortedBy {show -> show.show_title }
            }
            SortBy.ADDED -> {
                sortedShows = shows.sortedBy {show -> show.collected_at }
            }

        }

        if(sortHow == SortHow.DESC) {
            sortedShows = sortedShows.reversed()
        }

        return sortedShows
    }

    fun deleteShowFromCollection(collectedShow: CollectedShow) = viewModelScope.launch { eventChannel.send(Event.DELETE_COLLECTION_EVENT(repository.removeFromCollection(collectedShow))) }


    fun onStart() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(false)
            }

        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(true)
            }
        }
    }

    sealed class Event {
        data class DELETE_COLLECTION_EVENT(val syncResponse: Resource<SyncResponse>): Event()
    }

}