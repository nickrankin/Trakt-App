package com.nickrankin.traktapp.model.shows.episodedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
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
class EpisodeDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val showDetailsRepository: ShowDetailsRepository,  private val repository: EpisodeDetailsRepository): ViewModel() {

    private val initialRefreshEventChannel = Channel<Boolean>()
    private val initialRefreshEvent = initialRefreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val afterEpisodeLoadRefreshEventChannel = Channel<Boolean>()
    private val afterEpisodeLoadRefreshEvent = afterEpisodeLoadRefreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    /**
     *
     * First: showGuestStars
     * Second: shouldRefresh
     * */
    private val castRefreshEventChannel = Channel<Pair<Boolean, Boolean>>()
    private val castRefreshEvent = castRefreshEventChannel.receiveAsFlow()

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    val ratings = savedStateHandle.getLiveData<Int>("ratings")

    private val showTraktId: Int = savedStateHandle.get(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val showTmdbId: Int = savedStateHandle.get(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY) ?: 0
    private val seasonNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.SEASON_NUMBER_KEY) ?: 0
    private val episodeNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.EPISODE_NUMBER_KEY) ?: 0
    private val language: String? = savedStateHandle.get(EpisodeDetailsRepository.LANGUAGE_KEY)
    private var episodeTraktId = 0

    @ExperimentalCoroutinesApi
    val show = initialRefreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(showTraktId, shouldRefresh)
    }

    val episode = initialRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getEpisodes(showTraktId, showTmdbId, seasonNumber, episodeNumber, shouldRefresh).map { episodeResource ->
            if(episodeResource is Resource.Success) {
                episodeTraktId = episodeResource.data?.episode_trakt_id ?: 0

                // For endpoints needing Trakt Episode ID to be available, refresh these now
                if(episodeTraktId != 0) {
                    afterEpisodeLoadRefreshEventChannel.send(shouldRefresh)
                }
            }

            episodeResource
        }
    }

    val watchedEpisodes = afterEpisodeLoadRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getWatchedEpisodes(shouldRefresh, showTraktId).map { watchedEpisodesResource ->
            if(watchedEpisodesResource is Resource.Success) {
                watchedEpisodesResource.data?.filter { watchedEpisode ->
                    watchedEpisode.episode_trakt_id == episodeTraktId
                }?.sortedBy { it.watched_at }?.reversed()
            } else {
                emptyList()
            }
        }
    }

    val cast = castRefreshEvent.flatMapLatest { updateCast ->
        repository.getCredits(showTraktId, showTmdbId, updateCast.first, updateCast.second)
    }

    fun filterCast(showGuestStars: Boolean) = viewModelScope.launch {

        castRefreshEventChannel.send(Pair(showGuestStars, false))
    }

    fun removeWatchedHistoryItem(syncItems: SyncItems) = viewModelScope.launch { eventChannel.send(Event.DeleteWatchedHistoryItem(repository.removeWatchedEpisode(syncItems))) }

    fun onStart() {
        viewModelScope.launch {
            launch {
                initialRefreshEventChannel.send(false)
            }
            launch {
                castRefreshEventChannel.send(Pair(false, false))
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            launch {
                initialRefreshEventChannel.send(true)
            }
            launch {
                castRefreshEventChannel.send(Pair(false, true))

            }
        }
    }

    sealed class Event {
        data class DeleteWatchedHistoryItem(val syncResponse: Resource<SyncResponse>): Event()
    }
}