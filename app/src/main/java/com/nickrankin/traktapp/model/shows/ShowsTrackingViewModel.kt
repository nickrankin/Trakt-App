package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.Sorting
import com.nickrankin.traktapp.model.search.ShowSearchViewModel
import com.nickrankin.traktapp.repo.shows.ShowsTrackingRepository
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.uwetrottmann.trakt5.entities.SearchResult
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.internal.ChannelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowsTrackingViewModel"

@HiltViewModel
class ShowsTrackingViewModel @Inject constructor(
    private val showsTrackingRepository: ShowsTrackingRepository,
    private val collectedShowsRepository: CollectedShowsRepository,
    override val traktApi: TraktApi
) : ShowSearchViewModel(traktApi) {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()

    private val filterTextChannel = Channel<String>()
    private val filterText = filterTextChannel.receiveAsFlow()

    private val searchQueryChannel = Channel<String>()
    private val searchQuery = searchQueryChannel.receiveAsFlow()

    private val sortingChannel = Channel<Sorting>()
    private val sorting = sortingChannel.receiveAsFlow()

    val trackedShows = sorting.combine(showsTrackingRepository.getTrackedShows()) { sorting, trackedShows ->
            Log.d(TAG, "Triggered (getTrackedShows): $sorting ")

        Log.d(TAG, "Triggered the shows  (getTrackedShows): $trackedShows ")

        if(trackedShows.isNotEmpty()) {
            Log.d(TAG, "Returning the shows  (getTrackedShows): ${            
                sortList(trackedShows, sorting.sortBy, sorting.sortHow)
            } ")

            sortList(trackedShows, sorting.sortBy, sorting.sortHow)
            } else {
            Log.e(TAG, "Returning $trackedShows: ", )
                trackedShows
            }

    }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val collectedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        combine(
            trackedShows,
            collectedShowsRepository.getCollectedShows(shouldRefresh),
            filterText) { trackedShows, collectedShowsResource, filterText ->
            if (collectedShowsResource is Resource.Success) {
                val collectedShows = collectedShowsResource.data

                val filteredShows: MutableList<CollectedShow> = mutableListOf()
                Log.d(TAG, "Filtering for $filterText: ")

                // Filter the shows depending on applied filters
                filteredShows.addAll(collectedShows?.filter { collectedShow ->
                    collectedShow.show_title.contains(filterText, true)
                }!!.toList())

                // Check for already tracked shows and remove them from CollectedShows list
                filteredShows.removeAll(
                    collectedShows.filter { collectedShow -> trackedShows.find { trackedShow -> trackedShow.trackedShow.trakt_id == collectedShow.show_trakt_id } != null }
                )

                Resource.Success(filteredShows)

            } else {
                collectedShowsResource
            }
        }
    }

    private fun sortList(trackedShows: List<TrackedShowWithEpisodes>, sortedBy: String, sortHow: String): List<TrackedShowWithEpisodes> {
        Log.d(TAG, "sortList: Sorting $sortedBy , $sortHow")

        return when(sortedBy) {
            Sorting.SORT_BY_TITLE -> {
                val sortedShows =trackedShows.sortedBy { it.trackedShow.title }

                if(sortHow == Sorting.SORT_ORDER_DESC) {
                    Log.d(TAG, "sortList: Returning ${sortedShows.reversed()}")
                    sortedShows.reversed()
                } else {
                    Log.d(TAG, "sortList: Returning ${sortedShows}")

                    sortedShows
                }
            }
            Sorting.SORT_BY_NEXT_AIRING -> {
                val sortedShows = trackedShows.sortedBy { it.episodes.isNotEmpty() }


                if(sortHow == Sorting.SORT_ORDER_DESC) {
                    Log.d(TAG, "sortList: Returning ${sortedShows.reversed()}")
                    sortedShows.reversed()
                } else {
                    Log.d(TAG, "sortList: Returning ${sortedShows}")

                    sortedShows
                }
            }
            Sorting.SORT_BY_TRACKED_AT -> {
                val sortedShows = trackedShows.sortedBy { it.trackedShow.tracked_on }


                if(sortHow == Sorting.SORT_ORDER_DESC) {
                    Log.d(TAG, "sortList: Returning ${sortedShows.reversed()}")
                    sortedShows.reversed()
                } else {
                    Log.d(TAG, "sortList: Returning ${sortedShows}")

                    sortedShows
                }
            }
            Sorting.SORT_BY_YEAR -> {
                val sortedShows = trackedShows.sortedBy { it.trackedShow.releaseDate?.year }


                if(sortHow == Sorting.SORT_ORDER_DESC) {
                    Log.d(TAG, "sortList: Returning ${sortedShows.reversed()}")
                    sortedShows.reversed()
                } else {
                    Log.d(TAG, "sortList: Returning ${sortedShows}")

                    sortedShows
                }
            }
            else -> {
                Log.e(TAG, "sortList: getTrackedShows HERER", )
                trackedShows.sortedBy {  it.trackedShow.title  }
            }
        }
    }


    fun applySorting(sorting: Sorting) {
        Log.d(TAG, "applySorting: Sorting by ${sorting.sortBy} // ${sorting.sortHow}")
        viewModelScope.launch {
            sortingChannel.send(sorting)
        }
    }

    val searchResults = searchQuery.flatMapLatest { query ->
        doSearch(query)
    }

    fun newSearch(searchQuery: String) = viewModelScope.launch { if(searchQuery.isNotBlank()) searchQueryChannel.send(searchQuery) }

    fun filterCollectedShows(text: String) {
        viewModelScope.launch {
            filterTextChannel.send(text)
        }
    }

    fun getUpcomingEpisodes(showTraktId: Int) = viewModelScope.launch { showsTrackingRepository.refreshUpcomingEpisodes(showTraktId) }

    fun addTrackedShow(trackedShow: TrackedShow) =
        viewModelScope.launch { showsTrackingRepository.insertTrackedShow(trackedShow) }

    fun stopTracking(trackedShow: TrackedShow) =
        viewModelScope.launch { showsTrackingRepository.deleteTrackedShow(trackedShow) }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

}