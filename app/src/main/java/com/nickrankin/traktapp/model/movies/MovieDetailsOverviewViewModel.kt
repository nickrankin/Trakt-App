package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.movies.MovieDetailsOverviewRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewVie"
@HiltViewModel
class MovieDetailsOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: MovieDetailsOverviewRepository): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val traktId = savedStateHandle.get<Int>(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY) ?: -1
    private var tmdbId: Int? = 0

    val credits = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMovieCredits(traktId, tmdbId, shouldRefresh)
    }

    fun setTmdbId(tmdbId: Int?) {
        this.tmdbId = tmdbId
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