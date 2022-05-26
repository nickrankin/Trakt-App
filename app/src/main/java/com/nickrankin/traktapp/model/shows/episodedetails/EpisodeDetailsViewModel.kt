package com.nickrankin.traktapp.model.shows.episodedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EpisodeDetailsViewModel"
@HiltViewModel
class EpisodeDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val showDetailsRepository: ShowDetailsRepository,  private val repository: EpisodeDetailsRepository, private val statsRepository: StatsRepository): ViewModel() {

    private val initialRefreshEventChannel = Channel<Boolean>()
    private val initialRefreshEvent = initialRefreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    val episodeDataModel = savedStateHandle.get<EpisodeDataModel>(EpisodeDetailsActivity.EPISODE_DATA_KEY)

    @ExperimentalCoroutinesApi
    val show = initialRefreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(episodeDataModel?.traktId ?: 0, shouldRefresh)
    }

    val episode = initialRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getEpisodes(episodeDataModel?.traktId ?: 0, episodeDataModel?.tmdbId, episodeDataModel?.seasonNumber ?: -1, episodeDataModel?.episodeNumber ?: -1, shouldRefresh)
    }

    val watchedEpisodeStats = statsRepository.watchedEpisodeStats.map { watchedEpisodeStats ->
        watchedEpisodeStats.find { it.season == episodeDataModel?.seasonNumber && it.episode == episodeDataModel.episodeNumber }
    }

    fun removeWatchedHistoryItem(syncItems: SyncItems) = viewModelScope.launch { eventChannel.send(Event.DeleteWatchedHistoryItem(repository.removeWatchedEpisode(syncItems))) }

    fun onStart() {
        viewModelScope.launch {
            initialRefreshEventChannel.send(false)
            statsRepository.refreshShowStats( false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            initialRefreshEventChannel.send(true)

            statsRepository.refreshShowStats( false)

        }
    }

    sealed class Event {
        data class DeleteWatchedHistoryItem(val syncResponse: Resource<SyncResponse>): Event()
    }
}