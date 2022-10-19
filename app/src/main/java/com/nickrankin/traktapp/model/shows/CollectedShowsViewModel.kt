package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.nickrankin.traktapp.repo.stats.ShowStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectedShowsViewModel @Inject constructor(val repository: CollectedShowsRepository, private val showStatsRepository: ShowStatsRepository): ViewSwitcherViewModel() {

    private val sortingToggleChannel = Channel<String>()
    private val sortingToggle = sortingToggleChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private var sortBy = SORT_COLLECT_AT
    private var sortHow = SortHow.ASC

    init {
        // Initial sort
        sortShows(SORT_COLLECT_AT)
    }

    @ExperimentalCoroutinesApi
    val collectedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        sortingToggle.flatMapLatest { sortBy ->
            repository.getCollectedShows(shouldRefresh).map { collectedShows ->
                if(collectedShows is Resource.Success) {
                    var sortedShows = collectedShows.data

                    sortedShows = when(sortBy) {
                        SORT_TITLE -> {
                            sortedShows?.sortedBy { it.show_title }
                        }
                        SORT_COLLECT_AT -> {
                            sortedShows?.sortedBy { it.collected_at }
                        }
                        SORT_YEAR -> {
                            sortedShows?.sortedBy { it.airedDate }
                        }
                        else -> {
                            sortedShows?.sortedBy { it.show_title }
                        }
                    }

                    if(sortHow == SortHow.DESC) {
                        sortedShows = sortedShows?.reversed()
                    }

                    Resource.Success(sortedShows)
                } else {
                    collectedShows
                }
            }
        }
    }

    fun sortShows(sortBy: String) {
        if(this.sortBy == sortBy) {
            if(sortHow == SortHow.DESC) {
                sortHow = SortHow.ASC
            } else {
                sortHow = SortHow.DESC
            }
        }

        this.sortBy = sortBy

        viewModelScope.launch {
            sortingToggleChannel.send(sortBy)
        }
    }

    override fun onRefresh() {
        super.onRefresh()

        viewModelScope.launch {

        }
    }

    fun deleteShowFromCollection(collectedShow: CollectedShow) = viewModelScope.launch { eventChannel.send(Event.DELETE_COLLECTION_EVENT(repository.removeFromCollection(collectedShow))) }


    sealed class Event {
        data class DELETE_COLLECTION_EVENT(val syncResponse: Resource<SyncResponse>): Event()
    }

    companion object {
        const val SORT_TITLE = "title"
        const val SORT_COLLECT_AT = "collected_at"
        const val SORT_YEAR = "first_aired"
    }

}