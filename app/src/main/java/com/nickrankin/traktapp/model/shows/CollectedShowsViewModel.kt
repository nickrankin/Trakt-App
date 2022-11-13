package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.ISortable
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.model.shows.ShowsTrackingViewModel
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
class CollectedShowsViewModel @Inject constructor(val repository: CollectedShowsRepository, private val showStatsRepository: ShowStatsRepository): ViewSwitcherViewModel(), ISortable<CollectedShow> {

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private val sortingChannel = Channel<ISortable.Sorting>()
    private val sorting = sortingChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, ISortable.Sorting(SORT_COLLECT_AT, ISortable.SORT_ORDER_DESC))

    @ExperimentalCoroutinesApi
    val collectedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        sorting.flatMapLatest { sorting ->
            repository.getCollectedShows(shouldRefresh).map { collectedShowsResource ->

                if(collectedShowsResource is Resource.Success) {
                    collectedShowsResource.data = sortList(collectedShowsResource.data ?: emptyList(), sorting)
                }

                collectedShowsResource
            }
        }
    }

    fun deleteShowFromCollection(collectedShow: CollectedShow) = viewModelScope.launch { eventChannel.send(Event.DELETE_COLLECTION_EVENT(repository.removeFromCollection(collectedShow))) }

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sorting.value, sortingChannel, sortBy)
        }
    }

    override fun sortList(
        list: List<CollectedShow>,
        sorting: ISortable.Sorting
    ): List<CollectedShow> {
        val sortBy = sorting.sortBy
        val sortHow = sorting.sortHow

        return when (sortBy) {
            ISortable.SORT_BY_TITLE -> {
                val sortedShows = list.sortedBy { it.show_title }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            ISortable.SORT_BY_YEAR -> {
                val sortedShows = list.sortedBy { it.airedDate }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            SORT_COLLECT_AT -> {
                val sortedShows = list.sortedBy { it.collected_at }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            else -> {
                list.sortedBy { it.show_title }
            }
        }
    }

    sealed class Event {
        data class DELETE_COLLECTION_EVENT(val syncResponse: Resource<SyncResponse>): Event()
    }

    companion object {
        const val SORT_COLLECT_AT = "collected_at"
    }

}