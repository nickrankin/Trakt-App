package com.nickrankin.traktapp.model.movies

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.helper.TmdbToTraktIdHelper
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.repo.movies.MovieActionButtonsRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsOverviewRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "MovieDetailsViewModel"

@HiltViewModel
open class MovieDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieDetailsRepository,
    private val movieActionButtonsRepository: MovieActionButtonsRepository,
    private val movieDetailsOverviewRepository: MovieDetailsOverviewRepository,
    private val tmdbToTraktIdHelper: TmdbToTraktIdHelper

) : ViewModel() {
    private val eventsChannel = Channel<ActionButtonEvent>()
    val events = eventsChannel.receiveAsFlow()

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val movieDataModelChangedChannel = Channel<MovieDataModel>()
    private val movieDataModelChanged = movieDataModelChangedChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val movie = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDataModelChanged.flatMapLatest { movieDataModel ->
            repository.getMovieSummary(movieDataModel.traktId, shouldRefresh)
        }
    }


    val watchedMovieHistoryEntries = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDataModelChanged.flatMapLatest { movieDataModel ->
            movieActionButtonsRepository.getPlaybackHistory(movieDataModel.traktId, shouldRefresh)
        }
    }

    val listsAndEntries = refreshEvent.flatMapLatest { shouldRefresh ->
        movieActionButtonsRepository.getTraktListsAndItems(shouldRefresh)
    }

    // Overview Fragment
    val credits = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDataModelChanged.flatMapLatest { movieDataModel ->
            movieDetailsOverviewRepository.getCast(
                movieDataModel.traktId,
                movieDataModel.tmdbId ?: 0,
                shouldRefresh
            )
        }

    }

    suspend fun personTmdbIdToTrakt(tmdbId: Int) =
        tmdbToTraktIdHelper.getTraktPersonByTmdbId(tmdbId)


    val collectedMovieStats = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDataModelChanged.flatMapLatest { movieDataModel ->
            movieActionButtonsRepository.getCollectedStats(movieDataModel.traktId, shouldRefresh)
        }
    }

    fun addToCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddToCollectionEvent(
                movieActionButtonsRepository.addToCollection(
                    traktId
                )
            )
        )
    }

    fun deleteFromCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveFromCollectionEvent(
                movieActionButtonsRepository.removeFromCollection(
                    traktId
                )
            )
        )
    }

    fun addListEntry(itemTraktId: Int, listTraktId: Int) = viewModelScope.launch {
        movieActionButtonsRepository.addToList(
            itemTraktId,
            listTraktId
        )
    }

    fun removeListEntry(itemTraktId: Int, listTraktId: Int) =
        viewModelScope.launch {

            movieActionButtonsRepository.removeFromList(
                itemTraktId,
                listTraktId
            )
        }

    val movieRatings = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDataModelChanged.flatMapLatest { movieDataModel ->
            movieActionButtonsRepository.getRatings(movieDataModel.traktId, shouldRefresh)
        }
    }

    fun addRating(newRating: Int, traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddRatingEvent(
                movieActionButtonsRepository.addRating(traktId, newRating, OffsetDateTime.now()),
                newRating
            )
        )
    }

    fun deleteRating(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveRatingEvent(
                movieActionButtonsRepository.deleteRating(
                    traktId
                )
            )
        )
    }

    fun checkin(traktId: Int, cancelActiveCheckins: Boolean) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.CheckinEvent(
                movieActionButtonsRepository.checkin(
                    traktId,
                    cancelActiveCheckins
                )
            )
        )
    }

    fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddHistoryEntryEvent(
                movieActionButtonsRepository.addToHistory(
                    traktId,
                    watchedDate
                )
            )
        )
    }

    fun removeHistoryEntry(historyEntry: HistoryEntry) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveHistoryEntryEvent(
                movieActionButtonsRepository.removeFromHistory(historyEntry.history_id)
            )
        )
    }

    val videoStreamingServices = movieDataModelChanged.mapLatest { movieDataModel ->
        repository.getVideoStreamingServices(movieDataModel.tmdbId, movieDataModel.movieTitle)
    }

    fun switchMovie(movieDataModel: MovieDataModel) {
        viewModelScope.launch {
            movieDataModelChangedChannel.send(movieDataModel)
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
            // statsWorkRefreshHelper.refreshMovieStats()

        }
    }

    fun resetRefreshState() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }
}