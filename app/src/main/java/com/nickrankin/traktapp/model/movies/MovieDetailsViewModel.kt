package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsActionButtonRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsRepository
import com.nickrankin.traktapp.repo.movies.collected.CollectedMoviesRepository
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.ratings.MovieRatingsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
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
    private val statsRepository: StatsRepository,
    private val statsWorkRefreshHelper: StatsWorkRefreshHelper

) : ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val movieDataModel: MovieDataModel? =
        savedStateHandle.get(MovieDetailsActivity.MOVIE_DATA_KEY)

    val movie = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMovieSummary(movieDataModel?.traktId ?: 0, shouldRefresh)
    }

    val watchedMovieStats = statsRepository.watchedMoviesStats.map { watchedMovies ->
        watchedMovies.find { it.trakt_id == movieDataModel?.traktId ?: 0 }
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)

            repository.getCredits(movieDataModel, false)
            traktListsRepository.getListsAndEntries(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
            repository.getCredits(movieDataModel, true)
            traktListsRepository.getListsAndEntries(true)
            statsWorkRefreshHelper.refreshMovieStats()

        }

    }



}