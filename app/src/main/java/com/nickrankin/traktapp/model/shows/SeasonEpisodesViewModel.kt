package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeasonEpisodesViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: SeasonEpisodesRepository): ViewModel() {

    private val seasonEpisodesRefreshEventChannel = Channel<Boolean>()
    private val seasonEpisodesRefreshEvent = seasonEpisodesRefreshEventChannel.receiveAsFlow()

    private val showTraktId: Int = savedStateHandle.get(SeasonEpisodesRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val showTmdbtId: Int = savedStateHandle.get(SeasonEpisodesRepository.SHOW_TMDB_ID_KEY) ?: 0

    private val seasonNumber: Int = savedStateHandle.get(SeasonEpisodesRepository.SEASON_NUMBER_KEY) ?: 0

    suspend fun season() = repository.getSeason(showTraktId, seasonNumber)

    @ExperimentalCoroutinesApi
    val episodes = seasonEpisodesRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeasonEpisodes(showTraktId, showTmdbtId, seasonNumber, shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            seasonEpisodesRefreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            seasonEpisodesRefreshEventChannel.send(true)

        }
    }
}