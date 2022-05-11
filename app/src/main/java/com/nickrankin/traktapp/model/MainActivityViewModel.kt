package com.nickrankin.traktapp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.shows.ShowCalendarEntriesAdapter
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.auth.AuthRepository
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.shows.ShowsTrackingRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.nickrankin.traktapp.ui.shows.ShowsUpcomingFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivityViewModel"
@HiltViewModel
class MainActivityViewModel @Inject constructor(private val watchedMoviesRepository: WatchedMoviesRepository, private val watchedEpisodesRepository: WatchedEpisodesRepository, private val showsOverviewRepository: ShowsOverviewRepository, private val authRepository: AuthRepository): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
        private val refreshEvent = refreshEventChannel.receiveAsFlow()
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val watchedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        watchedMoviesRepository.getLatestWatchedMovies(shouldRefresh)
    }

    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        watchedEpisodesRepository.getLatestWatchedEpisodes(shouldRefresh)
    }

    val upcomingShows = refreshEvent.flatMapLatest { shouldRefresh ->
        showsOverviewRepository.getMyShows(shouldRefresh)
    }.map { calendarEntriesResource ->
        if(calendarEntriesResource is Resource.Success) {

            var data = calendarEntriesResource.data

            showsOverviewRepository.removeAlreadyAiredEpisodes(data ?: emptyList())

            if(data?.size ?: 0 >= 4) {
                data = data?.subList(0, 4)
            }
            calendarEntriesResource.data = data
        }
        calendarEntriesResource

    }

    val userStats = refreshEvent.flatMapLatest { shouldRefresh ->
        authRepository.getUserStats(shouldRefresh)
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

}