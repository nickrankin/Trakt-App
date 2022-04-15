package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectedMoviesViewMode"
@HiltViewModel
class CollectedMoviesViewModel @Inject constructor(private val repository: CollectedMoviesRepository): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private var sortBy = "collected_at"
    private var sortHow = SortHow.DESC


    val collectedShows = refreshEvent.flatMapLatest { shouldRefresh ->
        Log.d(TAG, "Getting movies: ")
        repository.getCollectedMovies(shouldRefresh).map { resource ->
            if(resource is Resource.Success) {
                resource.data = sortMovies(resource.data)
            }
            resource
        }
    }

    fun sortMovies(sortBy: String) {
        if(this.sortBy == sortBy) {
            if(sortHow == SortHow.DESC) {
                this.sortHow = SortHow.ASC
            } else {
                this.sortHow = SortHow.DESC
            }
        }

        this.sortBy = sortBy

        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    private fun sortMovies(movies: List<CollectedMovie>?): List<CollectedMovie>? {
        return when(sortBy) {
            SORT_COLLECTED_AT -> {
                if(sortHow == SortHow.DESC) {
                    movies?.sortedBy { it.collected_at }?.reversed()
                } else {
                    movies?.sortedBy { it.collected_at }
                }
            }
            SORT_TITLE -> {
                if(sortHow == SortHow.DESC) {
                    movies?.sortedBy { it.title }?.reversed()
                } else {
                    movies?.sortedBy { it.title }
                }
            }
            SORT_YEAR -> {
                if(sortHow == SortHow.DESC) {
                    movies?.sortedBy { it.release_date?.year }?.reversed()
                } else {
                    movies?.sortedBy { it.release_date?.year }
                }
            }
            else -> {
                movies
            }
        }
    }


    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
        }
    }

    companion object {
        const val SORT_TITLE = "title"
        const val SORT_COLLECTED_AT = "collected_at"
        const val SORT_YEAR = "year"
    }
}