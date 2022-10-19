package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.RecommendedMoviesRepository
import com.uwetrottmann.trakt5.entities.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendedMoviesViewModel @Inject constructor(private val repository: RecommendedMoviesRepository): ViewSwitcherViewModel() {

    private val eventChannel = Channel<Event>()

    val event = eventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val recommendedMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getRecommendedMovies(shouldRefresh)
    }

    fun removeRecommendedMovie(movie: Movie) = viewModelScope.launch { eventChannel.send(Event.RemoveRecommendationEvent(repository.deleteRecommendedMovie(movie))) }

    sealed class Event {
        data class RemoveRecommendationEvent(val response: Resource<Boolean>): Event()
    }

}