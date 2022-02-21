package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.uwetrottmann.trakt5.entities.EpisodeCheckin
import com.uwetrottmann.trakt5.entities.EpisodeCheckinResponse
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

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    val ratings = savedStateHandle.getLiveData<Int>("ratings")

    private val showTraktId: Int = savedStateHandle.get(EpisodeDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val showTmdbId: Int = savedStateHandle.get(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY) ?: 0
    private val seasonNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.SEASON_NUMBER_KEY) ?: 0
    private val episodeNumber: Int = savedStateHandle.get(EpisodeDetailsRepository.EPISODE_NUMBER_KEY) ?: 0
    private val language: String? = savedStateHandle.get(EpisodeDetailsRepository.LANGUAGE_KEY)

    init {
        getRatings()
    }

    @ExperimentalCoroutinesApi
    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(showTraktId, shouldRefresh)
    }

    val episode = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getEpisodes(showTraktId, showTmdbId, seasonNumber, episodeNumber, shouldRefresh)
    }

    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getWatchedEpisodes(shouldRefresh, showTraktId)
    }

    private fun getRatings() = viewModelScope.launch {
        repository.getRatings().collectLatest { ratedEpisodesResource ->
            if(ratedEpisodesResource is Resource.Success) {
                val ratedEpisode = ratedEpisodesResource.data?.find { episode ->
                    episode.show?.ids?.trakt == showTraktId && episode.episode?.season ?: 0 == seasonNumber && episode.episode?.number ?: 0 == episodeNumber
                }

                if(ratedEpisode != null) {
                    savedStateHandle.set("ratings", ratedEpisode.rating?.value ?: -1)
                }
            }
        }
    }

    fun addRatings(syncItems: SyncItems) = viewModelScope.launch {
        eventsChannel.send(Event.AddRatingsEvent(repository.addRatings(syncItems)))
        // Refresh the ratings
        getRatings()
    }

    fun checkin(episodeName: String, episodeCheckin: EpisodeCheckin) = viewModelScope.launch { eventsChannel.send(Event.AddCheckinEvent(episodeName, episodeCheckin, repository.checkin(episodeCheckin))) }

    fun addItemsToWatchedHistory(syncItems: SyncItems) = viewModelScope.launch { eventsChannel.send(Event.AddToWatchedHistoryEvent(repository.addToWatchedHistory(syncItems))) }

    fun removeWatchedEpisode(syncItems: SyncItems) = viewModelScope.launch {
        eventsChannel.send(Event.DeleteWatchedEpisodeEvent(repository.removeWatchedEpisode(syncItems)))
    }

    suspend fun deleteCheckins() =  repository.deleteActiveCheckin()

    fun onStart() {
        viewModelScope.launch {
            launch {
                refreshEventChannel.send(false)
            }
            launch {
                repository.getRatings()
            }
        }
    }

    fun onRefresh() {
        savedStateHandle.set(EpisodeDetailsRepository.SHOULD_REFRESH_WATCHED_KEY, true)

        viewModelScope.launch {
            launch {
                refreshEventChannel.send(true)
            }
            launch {
                repository.getRatings()
            }
        }
    }

    sealed class Event {
        data class AddRatingsEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddCheckinEvent(val episodeName: String, val episodeCheckin: EpisodeCheckin, val checkinResponse: Resource<EpisodeCheckinResponse?>): Event()
        data class AddToWatchedHistoryEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class DeleteWatchedEpisodeEvent(val syncResponse: Resource<SyncResponse>): Event()
    }
}