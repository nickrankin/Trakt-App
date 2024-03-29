package com.nickrankin.traktapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Operation
import androidx.work.WorkInfo
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.MainActivityRepository
import com.nickrankin.traktapp.repo.auth.AuthRepository
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.ShowStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivityViewModel"

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val showsOverviewRepository: ShowsOverviewRepository,
    private val authRepository: AuthRepository,
    private val mainActivityRepository: MainActivityRepository
) : ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val watchedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        mainActivityRepository.getLatestMovies(shouldRefresh)
    }

    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        mainActivityRepository.getLatestEpisodes(shouldRefresh)
    }

    val upcomingShows = refreshEvent.flatMapLatest { shouldRefresh ->
        showsOverviewRepository.getLatestShows(shouldRefresh)
    }

    val userStats = refreshEvent.flatMapLatest { shouldRefresh ->
        authRepository.getUserStats(shouldRefresh)
    }


    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh(movieRefreshState: (state: LiveData<WorkInfo>) -> Unit, showRefreshState: (state: LiveData<WorkInfo>) -> Unit) {
        viewModelScope.launch {
            refreshEventChannel.send(true)
        }
    }
}