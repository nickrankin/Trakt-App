package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.helper.ISortable
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.movies.CollectedMoviesViewModel
import com.nickrankin.traktapp.model.search.SearchViewModel
import com.nickrankin.traktapp.repo.shows.ShowsTrackingRepository
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.uwetrottmann.trakt5.enums.SortHow
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowsTrackingViewModel"

@HiltViewModel
class ShowsTrackingViewModel @Inject constructor(
    private val showsTrackingRepository: ShowsTrackingRepository,
    private val collectedShowsRepository: CollectedShowsRepository,
    override val traktApi: TraktApi
) : SearchViewModel(traktApi), ISortable<TrackedShowWithEpisodes> {

    private val filterTextChannel = Channel<String>()
    private val filterText = filterTextChannel.receiveAsFlow()

    private val searchQueryChannel = Channel<String>()
    private val searchQuery = searchQueryChannel.receiveAsFlow()

    private val sortingChannel = Channel<ISortable.Sorting>()
    private val sorting = sortingChannel.receiveAsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ISortable.Sorting(SORT_BY_NEXT_AIRING, ISortable.SORT_ORDER_DESC)
        )

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    val trackedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        sorting.flatMapLatest { sorting ->
            showsTrackingRepository.getTrackedShows(shouldRefresh)
                .mapLatest { trackedShowsResource ->
                    if (trackedShowsResource is Resource.Success) {
                        val shows = trackedShowsResource.data

                        trackedShowsResource.data = sortList(shows ?: emptyList(), sorting)
                    }
                    trackedShowsResource
                }
        }
    }.shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val collectedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        combine(
            trackedShows,
            collectedShowsRepository.getCollectedShows(shouldRefresh),
            filterText
        ) { trackedShows, collectedShowsResource, filterText ->
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
                    collectedShows.filter { collectedShow -> trackedShows.data?.find { trackedShow -> trackedShow.trackedShow.trakt_id == collectedShow.show_trakt_id } != null }
                )

                Resource.Success(filteredShows)

            } else {
                collectedShowsResource
            }
        }
    }

    val searchResults = searchQuery.flatMapLatest { query ->
        doSearch(query, Type.SHOW)
    }

    fun newSearch(searchQuery: String) =
        viewModelScope.launch { if (searchQuery.isNotBlank()) searchQueryChannel.send(searchQuery) }

    fun filterCollectedShows(text: String) {
        viewModelScope.launch {
            filterTextChannel.send(text)
        }
    }

    fun getUpcomingEpisodesPerShow(showTraktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            Event.UpdateTrackedEpisodeDataEvent(
                showsTrackingRepository.refreshUpcomingEpisodesPerShow(
                    showTraktId
                )
            )
        )
    }

    fun addTrackedShow(trackedShow: TrackedShow) =
        viewModelScope.launch { showsTrackingRepository.insertTrackedShow(trackedShow) }

    fun stopTracking(trackedShow: TrackedShow) =
        viewModelScope.launch { showsTrackingRepository.deleteTrackedShow(trackedShow) }

    suspend fun removeExpiredTrackedEpisodes(showTraktId: Int) =
        showsTrackingRepository.removeExpiredTrackedShows(showTraktId)

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sorting.value, sortingChannel, sortBy)
        }
    }

    override fun sortList(list: List<TrackedShowWithEpisodes>, sorting: ISortable.Sorting): List<TrackedShowWithEpisodes> {
        val sortBy = sorting.sortBy
        val sortHow = sorting.sortHow

        return when (sortBy) {
            ISortable.SORT_BY_TITLE -> {
                val sortedShows = list.sortedBy { it.trackedShow.title }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            SORT_BY_NEXT_AIRING -> {
                val sortedShows = list.sortedBy { it.episodes.isNotEmpty() }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            ISortable.SORT_BY_YEAR -> {
                val sortedShows = list.sortedBy { it.trackedShow.releaseDate }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            SORT_BY_TRACKED_AT -> {
                val sortedShows = list.sortedBy { it.trackedShow.tracked_on }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            else -> {
                list.sortedBy { it.trackedShow.title }
            }
        }
    }



    sealed class Event {
        data class UpdateTrackedEpisodeDataEvent(val episodesResource: Resource<TmShow?>) : Event()
    }


        companion object {
            const val SORT_BY_TRACKED_AT = "tracked_on"
            const val SORT_BY_NEXT_AIRING = "next_up"
        }
}