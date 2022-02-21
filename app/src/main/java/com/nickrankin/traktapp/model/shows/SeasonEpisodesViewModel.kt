package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeasonEpisodesViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: SeasonEpisodesRepository): ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val showTraktId: Int = savedStateHandle.get(SeasonEpisodesRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val showTmdbId: Int = savedStateHandle.get(SeasonEpisodesRepository.SHOW_TMDB_ID_KEY) ?: 0

    private var seasonNumber: Int = savedStateHandle.get(SeasonEpisodesRepository.SEASON_NUMBER_KEY) ?: 0

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShow(showTraktId, showTmdbId, shouldRefresh)
    }

    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeason(showTraktId, showTmdbId, shouldRefresh)
    }

    fun switchSeason(seasonNumber: Int) {
            if(this.seasonNumber != seasonNumber) {
                this.seasonNumber = seasonNumber
                viewModelScope.launch {
                    refreshEventChannel.send(false)
                }

            }

    }

    @ExperimentalCoroutinesApi
    val episodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getWatchedEpisodes(showTraktId, seasonNumber, shouldRefresh).flatMapLatest { watchedEpisodesResource ->
                repository.getSeasonEpisodes(showTraktId, showTmdbId, seasonNumber, shouldRefresh, watchedEpisodesResource.data ?: emptyList())
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

        }
    }
}