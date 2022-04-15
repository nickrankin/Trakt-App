package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.movies.MovieDetailsActionButtonRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.nickrankin.traktapp.repo.ratings.MovieRatingsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MovieDetailsActionButto"
@HiltViewModel
class MovieDetailsActionButtonsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val collectedMoviesRepository: CollectedMoviesRepository,
    private val movieRatingsRepository: MovieRatingsRepository,
    private val repository: MovieDetailsActionButtonRepository
) : ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val traktId = savedStateHandle.get<Int>(MovieDetailsRepository.MOVIE_TRAKT_ID_KEY)

    val collectedMovie = refreshEvent.flatMapLatest { shouldRefresh ->
        collectedMoviesRepository.getCollectedMovies(shouldRefresh).map { collectedMoviesResource ->
            if(collectedMoviesResource is Resource.Success) {
                collectedMoviesResource.data?.find { it.trakt_id == traktId }
            } else {
                null
            }
        }
    }

    fun addToCollection() = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(collectedMoviesRepository.addCollectedMovie(traktId ?: 0))) }
    fun deleteFromCollection() = viewModelScope.launch { eventsChannel.send(Event.RemoveFromCollectionEvent(collectedMoviesRepository.removeCollectedMovie(traktId ?: -1))) }

    val movieRating = refreshEvent.flatMapLatest { shouldRefresh ->
        movieRatingsRepository.getRatings(shouldRefresh, traktId ?: -1)
    }

    fun addRating(newRating: Int) = viewModelScope.launch { eventsChannel.send(Event.AddRatingEvent(movieRatingsRepository.addRating(traktId ?: -1, newRating), newRating)) }

    fun deleteRating() = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingEvent(movieRatingsRepository.deleteRating(traktId ?: -1))) }



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

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): Event()
        data class DeleteRatingEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

}