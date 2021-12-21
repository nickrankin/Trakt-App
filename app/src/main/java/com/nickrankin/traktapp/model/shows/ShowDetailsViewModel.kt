package com.nickrankin.traktapp.model.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.calculateProgress
import com.nickrankin.traktapp.repo.TrackedEpisodesRepository
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.ShowDetailsRepository
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsViewModel"

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ShowDetailsRepository,
    private val collectedShowsRepository: CollectedShowsRepository,
    private val episodesRepository: EpisodeDetailsRepository,
    private val episodeTrackingRepository: TrackedEpisodesRepository,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : ViewModel() {

    private val isLoggedIn = sharedPreferences.getBoolean(AuthActivity.IS_LOGGED_IN, false)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val trackingStatus = repository.trackingStatusChannel
    val progress = repository.processChannel.receiveAsFlow()

    val state = savedStateHandle

    val ratingsLiveData = savedStateHandle.getLiveData<Int?>("ratings")
    val progressLiveData = savedStateHandle.getLiveData<Int>("progress")
    val nextEpisodeLiveData = savedStateHandle.getLiveData<String>("next_episode")
    val trackingLiveData = savedStateHandle.getLiveData<Boolean>("tracking")

    val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private val tmdbId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TMDB_ID_KEY) ?: 0
    private val language: String? =
        savedStateHandle.get(ShowDetailsRepository.SHOW_LANGUAGE_KEY)

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    init {
        if (isLoggedIn) {
            getRatings()
            getTrackingStatus()
            getProgress()

            collectTrackingState()
            collectProgress()
        }

    }

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(traktId, tmdbId, language, shouldRefresh)
    }

    fun getProgress() = viewModelScope.launch { repository.getShowProgress(traktId) }

    val seasons = repository.getSeasons(tmdbId)

    val collectedShow = refreshEvent.flatMapLatest { shouldRefresh ->
        collectedShowsRepository.getCollectedShows(shouldRefresh)
    }.map { resource ->
        when (resource) {
            is Resource.Success -> {
                val foundShow = resource.data?.find { collectedShow ->
                    collectedShow.show_tmdb_id == tmdbId
                }
                (Resource.Success(listOf(foundShow)))
            }
            else -> {
                resource
            }
        }
    }

    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getWatchedEpisodes(true, traktId).map {
            if (it is Resource.Success) {
                it.data = it.data?.sortedBy { watchedEpisode ->
                    watchedEpisode.watched_at
                }?.reversed()
            }
            it
        }
    }

    fun getRatings() = viewModelScope.launch {
        if (ratingsLiveData.value == null) {
            val ratings = repository.getRatings()

            val foundShow = ratings.find {
                it.show?.ids?.trakt ?: 0 == traktId
            }
            savedStateHandle.set("rating", foundShow?.rating?.value ?: -1)
        }
    }

    fun setRatings(syncItems: SyncItems, resetRatings: Boolean) = viewModelScope.launch {
        eventChannel.send(
            Event.RatingSetEvent(
                repository.setRatings(
                    syncItems,
                    resetRatings
                ), syncItems.shows?.first()?.rating?.value ?: 0
            )
        )
    }

    fun refreshRating(newRating: Int) {
        savedStateHandle.set("rating", newRating)
    }

    suspend fun episode(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String) =
        episodesRepository.getEpisode(showTraktId, showTmdbId, seasonNumber, episodeNumber, language)

    fun getTrackingStatus() = viewModelScope.launch {
        Log.e(TAG, "getTrackingStatus: HERE")
        repository.getTrackingStatus(traktId)
    }

    fun setTracking(traktId: Int, tmdbId: Int) =
        viewModelScope.launch { repository.setShowTracked(traktId, tmdbId) }

    fun addToCollection(syncItems: SyncItems) = viewModelScope.launch {
        eventChannel.send(
            Event.AddToCollectionEvent(
                repository.addToCollection(syncItems)
            )
        )
    }

    fun removeFromCollection(collectedShow: CollectedShow?, syncItems: SyncItems) =
        viewModelScope.launch {
            eventChannel.send(
                Event.RemoveFromCollectionEvent(
                    repository.removeFromCollection(
                        collectedShow,
                        syncItems
                    )
                )
            )
        }

    fun removeWatchedEpisode(syncItems: SyncItems) = viewModelScope.launch {
        eventChannel.send(Event.DeleteWatchedEpisodeEvent(repository.removeWatchedEpisode(syncItems)))
    }

    private fun collectTrackingState() = viewModelScope.launch {
        trackingStatus.consumeAsFlow().collectLatest { isTracking ->
            Log.e(TAG, "getTrackingStatus: Got tracking state $isTracking")
            savedStateHandle.set("tracking", isTracking)
        }
    }

    private fun collectProgress() = viewModelScope.launch {
        progress.collectLatest { baseShow ->
            savedStateHandle.set(
                "progress",
                calculateProgress(
                    baseShow.completed?.toDouble() ?: 0.0,
                    baseShow.aired?.toDouble() ?: 0.0
                )
            )
            savedStateHandle.set("next_episode", gson.toJson(baseShow.next_episode))
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
            getRatings()
            getProgress()
            getTrackingStatus()
        }
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>) : Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>) : Event()
        data class RatingSetEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int) :
            Event()

        data class DeleteWatchedEpisodeEvent(val syncResponse: Resource<SyncResponse>) : Event()
    }
}