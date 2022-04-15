package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: MovieDetailsRepository): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()

    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val movieTraktId = savedStateHandle.get<Int>(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY) ?: -1

    val userRatings = savedStateHandle.getLiveData<Double>(MovieDetailsRepository.USER_RATINGS_KEY)

    init {
        viewModelScope.launch {
            getUserRatings()
        }
    }

    val movie = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMovieSummary(movieTraktId, shouldRefresh)
    }

    private suspend fun getUserRatings() {
        val rating = repository.getTraktUserRatings(movieTraktId)

        if(rating is Resource.Success) {
            if(rating.data != 0.0) {
                savedStateHandle.set(MovieDetailsRepository.USER_RATINGS_KEY, rating.data)
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

            getUserRatings()
        }
    }
}