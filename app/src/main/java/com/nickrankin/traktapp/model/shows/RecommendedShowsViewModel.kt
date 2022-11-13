package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.ISortable
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
class RecommendedShowsViewModel @Inject constructor(private val repository: RecommendedShowsRepository): ViewSwitcherViewModel(), ISortable<Show> {

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val sortingChannel = Channel<ISortable.Sorting>()
    private val sorting = sortingChannel.receiveAsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ISortable.Sorting(ISortable.SORT_BY_TITLE, ISortable.SORT_ORDER_ASC)
        )

    val suggestedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        sorting.flatMapLatest { sorting ->
            repository.getSuggestedShows(shouldRefresh).mapLatest { suggestedShowsResource ->
                if(suggestedShowsResource is Resource.Success) {
                    suggestedShowsResource.data = sortList(suggestedShowsResource.data ?: emptyList(), sorting)
                }

                suggestedShowsResource
            }
        }
    }

    fun addToCollection(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(repository.addToCollection(syncItems))) }

    fun removeFromSuggestions(show: Show) = viewModelScope.launch {
        val response = repository.removeSuggestion(show)
        eventsChannel.send(Event.RemoveSuggestionEvent(response))
    }

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sorting.value, sortingChannel, sortBy)
        }
    }

    override fun sortList(list: List<Show>, sorting: ISortable.Sorting): List<Show> {
        val sortBy = sorting.sortBy
        val sortHow = sorting.sortHow

        return when (sortBy) {
            ISortable.SORT_BY_TITLE -> {
                val sortedShows = list.sortedBy { it.title }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            ISortable.SORT_BY_YEAR -> {
                val sortedShows = list.sortedBy { it.year }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            RECOMMENDED_AT_SORT_BY -> {
                val sortedShows = list.sortedBy { it.updated_at }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            else -> {
                list.sortedBy { it.title }
            }
        }
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveSuggestionEvent(val removedSuccessfully: Resource<Boolean>): Event()
    }

    companion object {
        const val RECOMMENDED_AT_SORT_BY = "recommended_at_sorting"
    }
}