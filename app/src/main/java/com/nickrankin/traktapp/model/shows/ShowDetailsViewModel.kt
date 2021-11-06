package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: ShowDetailsRepository, private val episodesRepository: EpisodeDetailsRepository): ViewModel() {
    private val showRefreshEventChannel = Channel<Boolean>()
    private val showRefreshEvent = showRefreshEventChannel.receiveAsFlow()

    private val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val tmdbId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TMDB_ID_KEY) ?: 0
    private val language: String = savedStateHandle.get(ShowDetailsRepository.SHOW_LANGUAGE_KEY) ?: "en"

    @ExperimentalCoroutinesApi
    val show = showRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(tmdbId, language, shouldRefresh)
    }

    val progress = repository.getShowProgress(traktId)

    val seasons = repository.getSeasons(tmdbId)

    suspend fun episode(showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String) = episodesRepository.getEpisode(showTmdbId, seasonNumber, episodeNumber, language, false)


    fun onStart() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(false)
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            launch {
                showRefreshEventChannel.send(true)
            }
        }
    }
}