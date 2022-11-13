package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.ISortable
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.TrendingMoviesRepository
import com.uwetrottmann.trakt5.entities.TrendingMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendingMoviesViewModel @Inject constructor(private val repository: TrendingMoviesRepository): ViewSwitcherViewModel(), ISortable<TrendingMovie> {

    val sortingChannel = Channel<ISortable.Sorting>()
    val sorting = sortingChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ISortable.Sorting(
            TOTAL_WATCHING_SORT_BY, ISortable.SORT_ORDER_DESC))

    val trendingMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        sorting.flatMapLatest { sorting ->
            repository.getTrendingMovies(shouldRefresh).mapLatest { trendingMoviesResource ->
                if(trendingMoviesResource is Resource.Success) {
                    trendingMoviesResource.data = sortList(trendingMoviesResource.data ?: emptyList(), sorting)
                }

                trendingMoviesResource
            }
        }
    }

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sorting.value, sortingChannel, sortBy)
        }
    }

    override fun sortList(
        list: List<TrendingMovie>,
        sorting: ISortable.Sorting
    ): List<TrendingMovie> {
        val sortBy = sorting.sortBy
        val sortHow = sorting.sortHow

        return when(sortBy) {
            ISortable.SORT_BY_TITLE -> {
                var sortedMovies = list.sortedBy { it.movie?.title }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            ISortable.SORT_BY_YEAR -> {
                var sortedMovies = list.sortedBy { it.movie?.year }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            TOTAL_WATCHING_SORT_BY -> {
                var sortedMovies = list.sortedBy { it.watchers }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            else -> {
                var sortedMovies = list.sortedBy { it.watchers }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
        }
    }

    companion object {
        const val TOTAL_WATCHING_SORT_BY = "total_watching"
    }

}