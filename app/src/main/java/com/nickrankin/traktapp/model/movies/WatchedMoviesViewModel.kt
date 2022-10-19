package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WatchedMoviesViewModel"
@HiltViewModel
class WatchedMoviesViewModel @Inject constructor(private val repository: WatchedMoviesRepository, private val movieStatsRepository: MovieStatsRepository): ViewSwitcherViewModel() {

    val ratedMoviesStats = movieStatsRepository.ratedMoviesStats
    val collectedMoviesStats = movieStatsRepository.collectedMoviesStats

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    val watchedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.watchedMovies(shouldRefresh)
    }.cachedIn(viewModelScope)


    fun removeFromWatchedHistory(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(
        Event.RemoveWatchedHistoryEvent(repository.deleteFromWatchedHistory(syncItems))
    ) }

    override fun onRefresh() {
        super.onRefresh()
        viewModelScope.launch {
            movieStatsRepository.refreshWatchedMovies()
        }
    }

    sealed class Event {
        data class RemoveWatchedHistoryEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

}