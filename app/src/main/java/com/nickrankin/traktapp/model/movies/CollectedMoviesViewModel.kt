package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.adapter.movies.CollectedMoviesAdapter
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
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
class CollectedMoviesViewModel @Inject constructor(private val repository: CollectedMoviesRepository, private val movieStatsRepository: MovieStatsRepository): ViewSwitcherViewModel() {

    private val sortingChangeChannel = Channel<String>()
    private val sortingChange = sortingChangeChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private var currentSorting = SORT_COLLECTED_AT
    private var sortHow = SortHow.ASC

    init {
        // Initially sort movies by collected at
        sortMovies(SORT_COLLECTED_AT)
    }


    val collectedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        sortingChange.flatMapLatest { sortBy ->
            repository.getCollectedMovies(shouldRefresh)
                .map { resource ->
                if(resource is Resource.Success) {
                    var sortedData = resource.data

                    sortedData = when(sortBy) {
                        SORT_TITLE -> {
                            sortedData?.sortedBy { it.title }
                        }
                        SORT_COLLECTED_AT -> {
                            sortedData?.sortedBy { it.collected_at }
                        }
                        SORT_YEAR -> {
                            sortedData?.sortedBy { it.release_date }
                        }
                        else -> {
                            sortedData?.sortedBy { it.collected_at }
                        }
                    }

                    if(sortHow == SortHow.DESC) {
                        sortedData = sortedData?.reversed()
                    }

                    Resource.Success(sortedData)
                } else {
                    resource
                }
            }
        }
    }

    fun sortMovies(sortBy: String) {
        // If user chooses the same sorting by, reverse the sorting..
        if(this.currentSorting == sortBy) {
            if(sortHow == SortHow.DESC) {
                this.sortHow = SortHow.ASC
            } else {
                this.sortHow = SortHow.DESC
            }
        }

        // Keep track current sorting in the ViewModel
        this.currentSorting = sortBy

        viewModelScope.launch {
            sortingChangeChannel.send(sortBy)
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
        const val SORT_TITLE = "title"
        const val SORT_COLLECTED_AT = "collected_at"
        const val SORT_YEAR = "year"
    }
}