package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.CollectedMoviesAdapter
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.helper.ISortable
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectedMoviesViewMode"
@HiltViewModel
class CollectedMoviesViewModel @Inject constructor(private val repository: CollectedMoviesRepository, private val movieStatsRepository: MovieStatsRepository): ViewSwitcherViewModel(), ISortable<CollectedMovie> {

    private val sortingChangeChannel = Channel<ISortable.Sorting>()
    private val sortingChange = sortingChangeChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, ISortable.Sorting(SORT_COLLECTED_AT, ISortable.SORT_ORDER_DESC))

    val collectedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        sortingChange.flatMapLatest { sortBy ->
            repository.getCollectedMovies(shouldRefresh)
                .map { resource ->
                if(resource is Resource.Success) {

                    resource.data = sortList(resource.data ?: emptyList(), sortBy)

                }
                    resource
            }
        }
    }

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sortingChange.value, sortingChangeChannel, sortBy)
        }
    }

    override fun sortList(
        list: List<CollectedMovie>,
        sorting: ISortable.Sorting
    ): List<CollectedMovie> {
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
                val sortedShows = list.sortedBy { it.release_date }

                if (sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedShows.reversed()
                } else {
                    sortedShows
                }
            }
            SORT_COLLECTED_AT -> {
                val sortedShows = list.sortedBy { it.collected_at }

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

    suspend fun deleteCollectedMovie(traktId: Int) = repository.removeCollectedMovie(traktId)

    override fun onRefresh() {
        super.onRefresh()
        viewModelScope.launch {
            movieStatsRepository.refreshCollectedMovieStats()
        }
    }

    companion object {
        const val SORT_COLLECTED_AT = "collected_at"
    }
}