package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.TmdbToTraktIdHelper
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsActionButtonRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsOverviewRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.nickrankin.traktapp.repo.ratings.MovieRatingsRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.movies.moviedetails.MovieDetailsActivity
import com.uwetrottmann.trakt5.entities.MovieCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieDetailsRepository,
    private val traktListsRepository: TraktListsRepository,
    private val statsWorkRefreshHelper: StatsWorkRefreshHelper,
    private val movieRatingsRepository: MovieRatingsRepository,
    private val movieDetailsOverviewRepository: MovieDetailsOverviewRepository,
    private val collectedMoviesRepository: CollectedMoviesRepository,
    private val movieDetailsActionButtonRepository: MovieDetailsActionButtonRepository,
    private val listsRepository: TraktListsRepository,
    private val listEntryRepository: ListEntryRepository,
    private val movieStatsRepository: MovieStatsRepository,
    private val tmdbToTraktIdHelper: TmdbToTraktIdHelper

) : ViewModel() {
    private val movieDataModel: MovieDataModel? =
        savedStateHandle.get(MovieDetailsActivity.MOVIE_DATA_KEY)

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)


    val movie = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMovieSummary(movieDataModel?.traktId ?: 0, shouldRefresh)
    }

    val watchedMovieStats = movieStatsRepository.watchedMoviesStats.map { watchedMovies ->
        watchedMovies.find { it.trakt_id == movieDataModel?.traktId ?: 0 }
    }

    // Overview Fragment
    val credits = refreshEvent.flatMapLatest { shouldRefresh ->
        movieDetailsOverviewRepository.getCast(movieDataModel?.traktId ?: 0, movieDataModel?.tmdbId ?: 0, shouldRefresh)
    }

    suspend fun personTmdbIdToTrakt(tmdbId: Int) = tmdbToTraktIdHelper.getTraktPersonByTmdbId(tmdbId)


    // Action Button Fragment
    val listsWithEntries = listEntryRepository.listEntries



    suspend fun collectedMovieStats(traktId: Int) = movieStatsRepository.getCollectedMovieStatsById(traktId)

    fun addToCollection(traktId: Int) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(collectedMoviesRepository.addCollectedMovie(traktId ?: 0))) }
    fun deleteFromCollection(traktId: Int) = viewModelScope.launch { eventsChannel.send(Event.RemoveFromCollectionEvent(collectedMoviesRepository.removeCollectedMovie(traktId ?: -1))) }

    fun addListEntry(type: String, traktId: Int, traktList: TraktList) = viewModelScope.launch { eventsChannel.send(Event.AddListEntryEvent(listEntryRepository.addListEntry(type, traktId, traktList))) }
    fun removeListEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventsChannel.send(Event.RemoveListEntryEvent(listEntryRepository.removeEntry(listTraktId, listEntryTraktId, type))) }

    val movieRatings = movieRatingsRepository.movieRatings

    fun addRating(newRating: Int, traktId: Int, tmdbId: Int?, movieTitle: String) = viewModelScope.launch { eventsChannel.send(Event.AddRatingEvent(movieRatingsRepository.addRating(traktId ?: -1, tmdbId, movieTitle, newRating), newRating)) }

    fun deleteRating(traktId: Int) = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingEvent(movieRatingsRepository.deleteRating(traktId ?: -1))) }

    fun checkin(traktId: Int, cancelActiveCheckins: Boolean) = viewModelScope.launch { eventsChannel.send(Event.CheckinEvent(movieDetailsActionButtonRepository.checkin(traktId, cancelActiveCheckins))) }
    fun cancelCheckin() = viewModelScope.launch { eventsChannel.send(Event.CancelCheckinEvent(movieDetailsActionButtonRepository.cancelCheckins())) }

    fun addToWatchedHistory(movie: TmMovie, watchedDate: OffsetDateTime) = viewModelScope.launch { eventsChannel.send(Event.AddToHistoryEvent(movieDetailsActionButtonRepository.addToWatchedHistory(movie, watchedDate))) }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)

            traktListsRepository.getListsAndEntries(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
            traktListsRepository.getListsAndEntries(true)
            statsWorkRefreshHelper.refreshMovieStats()

        }

    }

    fun resetRefreshState() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): Event()
        data class DeleteRatingEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class CheckinEvent(val movieCheckinResponse: Resource<MovieCheckinResponse>): Event()
        data class CancelCheckinEvent(val cancelCheckinResponse: Resource<Boolean>): Event()
        data class AddToHistoryEvent(val addHistoryResponse: Resource<SyncResponse>): Event()
        data class AddListEntryEvent(val addListEntryResponse: Resource<SyncResponse>): Event()
        data class RemoveListEntryEvent(val removeListEntryResponse: Resource<SyncResponse?>): Event()
    }
}