package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.ISortable
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.RecommendedMoviesRepository
import com.uwetrottmann.trakt5.entities.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendedMoviesViewModel @Inject constructor(private val repository: RecommendedMoviesRepository): ViewSwitcherViewModel(), ISortable<Movie> {

    private val eventChannel = Channel<Event>()

    val event = eventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val sortingChannel = Channel<ISortable.Sorting>()
    val sorting = sortingChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ISortable.Sorting(SORT_BY_LAST_SUGGESTED, ISortable.SORT_ORDER_DESC))

    val recommendedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        sorting.flatMapLatest { sorting ->
            repository.getRecommendedMovies(shouldRefresh).mapLatest { moviesResource ->

                if(moviesResource is Resource.Success) {
                    moviesResource.data = sortList(moviesResource.data ?: emptyList(), sorting)
                }
                moviesResource
            }
        }
    }

    override fun applySorting(sortBy: String) {
        viewModelScope.launch {
            updateSorting(sorting.value, sortingChannel, sortBy)
        }
    }

    override fun sortList(list: List<Movie>, sorting: ISortable.Sorting): List<Movie> {
        val sortBy = sorting.sortBy
        val sortHow = sorting.sortHow

        return when(sortBy) {
            ISortable.SORT_BY_TITLE -> {
                var sortedMovies = list.sortedBy { it.title }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            ISortable.SORT_BY_YEAR -> {
                var sortedMovies = list.sortedBy { it.year }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            SORT_BY_LAST_SUGGESTED -> {
                var sortedMovies = list.sortedBy { it.updated_at }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
            else -> {
                var sortedMovies = list.sortedBy { it.updated_at }

                if(sortHow == ISortable.SORT_ORDER_DESC) {
                    sortedMovies = sortedMovies.reversed()
                }

                sortedMovies
            }
        }
    }

    fun removeRecommendedMovie(movie: Movie) = viewModelScope.launch { eventChannel.send(Event.RemoveRecommendationEvent(repository.deleteRecommendedMovie(movie))) }

    sealed class Event {
        data class RemoveRecommendationEvent(val response: Resource<Boolean>): Event()
    }

    companion object {
        const val SORT_BY_LAST_SUGGESTED = "last_suggested"
    }

}