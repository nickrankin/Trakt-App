package com.nickrankin.traktapp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.auth.AuthRepository
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivityViewModel"

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val watchedMoviesRepository: WatchedMoviesRepository,
    private val watchedEpisodesRepository: WatchedEpisodesRepository,
    private val showsOverviewRepository: ShowsOverviewRepository,
    private val authRepository: AuthRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val watchedMovies = statsRepository.watchedMoviesStats.map { watchedMovieStats ->
        if(watchedMovieStats.isNotEmpty() || watchedMovieStats.size > 8) {
            watchedMovieStats.sortedBy {it.last_watched_at }.reversed().subList(0, 8)
        } else
            watchedMovieStats.sortedBy {it.last_watched_at }.reversed()
    }


    val watchedEpisodes = statsRepository.watchedEpisodeStats.map { watchedEpisodesStats ->
        if(watchedEpisodesStats.isNotEmpty() || watchedEpisodesStats.size > 8) {
            watchedEpisodesStats.sortedBy {it.last_watched_at }.reversed().subList(0, 8)
        } else
            watchedEpisodesStats.sortedBy {it.last_watched_at }.reversed()
    }

    val upcomingShows = refreshEvent.flatMapLatest { shouldRefresh ->
        showsOverviewRepository.getMyShows(shouldRefresh)
    }.map { calendarEntriesResource ->
        if (calendarEntriesResource is Resource.Success) {

            var data = calendarEntriesResource.data

            showsOverviewRepository.removeAlreadyAiredEpisodes(data ?: emptyList())

            if (data?.size ?: 0 >= 4) {
                data = data?.subList(0, 4)
            }
            calendarEntriesResource.data = data
        }
        calendarEntriesResource
    }

//    val userStats = refreshEvent.flatMapLatest { shouldRefresh ->
//        authRepository.getUserStats(shouldRefresh)
//    }


    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
//            eventsChannel.send(Event.RefreshMovieStatsEvent(statsRepository.refreshMovieStats(false)))
//            eventsChannel.send(Event.RefreshShowStatsEvent(statsRepository.refreshShowStats(false)))
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
//            eventsChannel.send(Event.RefreshMovieStatsEvent(statsRepository.refreshMovieStats(true)))
//            eventsChannel.send(Event.RefreshShowStatsEvent(statsRepository.refreshShowStats(true)))
        }
    }

    sealed class Event {
        data class RefreshMovieStatsEvent(val refreshStatus: Resource<Boolean>): Event()
        data class RefreshShowStatsEvent(val refreshStatus: Resource<Boolean>): Event()
    }

}