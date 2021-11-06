package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpisodeDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val showDetailsRepository: ShowDetailsRepository,  private val repository: EpisodeDetailsRepository): ViewModel() {

    private val showRefreshEventChannel = Channel<Boolean>()
    private val showRefreshEvent = showRefreshEventChannel.receiveAsFlow()

    private val episodeRefreshEventChannel = Channel<Boolean>()
    private val episodeRefreshEvent = episodeRefreshEventChannel.receiveAsFlow()

    private val showTmdbId: Int = savedStateHandle.get(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY) ?: 0
    private val seasonNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.SEASON_NUMBER_KEY) ?: 0
    private val episodeNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.EPISODE_NUMBER_KEY) ?: 0
    private val language: String = savedStateHandle.get(EpisodeDetailsRepository.LANGUAGE_KEY) ?: "en"

    @ExperimentalCoroutinesApi
    val show = showRefreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(showTmdbId, language, shouldRefresh)
    }

    val episode = episodeRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getEpisode(showTmdbId, seasonNumber, episodeNumber, language, shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(false)
            }
            launch {
                episodeRefreshEventChannel.send(false)
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(true)
            }
            launch {
                episodeRefreshEventChannel.send(true)
            }
        }
    }
}