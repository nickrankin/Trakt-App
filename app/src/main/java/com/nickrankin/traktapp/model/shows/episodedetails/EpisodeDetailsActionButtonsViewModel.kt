package com.nickrankin.traktapp.model.shows.episodedetails

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.shows.showdetails.ShowDetailsActionButtonsViewModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsActionButtonsRepository
import com.uwetrottmann.trakt5.entities.EpisodeCheckin
import com.uwetrottmann.trakt5.entities.EpisodeCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActionBut"
@HiltViewModel
class EpisodeDetailsActionButtonsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: EpisodeDetailsActionButtonsRepository,private val listsRepository: TraktListsRepository,
                                                               private val listEntryRepository: ListEntryRepository) : ViewModel() {

    private var episodeTraktId = -1
    private var ratingInitialLoad = true

    val ratings: LiveData<Int> = savedStateHandle.getLiveData("episode_ratings")

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    val listsWithEntries = listsRepository.listsWithEntries



    private fun getRatings(shouldRefresh: Boolean) {
        if(shouldRefresh || ratingInitialLoad) {
            viewModelScope.launch {
                val response = repository.getRatings()

                if(response is Resource.Success) {
                    val ratedEpisode = response.data?.find { ratedEpisode ->
                        ratedEpisode.episode?.ids?.trakt == episodeTraktId
                    }

                    if(ratedEpisode != null) {
                        updateRatings(ratedEpisode.rating?.value ?: 0)
                    }
                }
            }

            ratingInitialLoad = false
        } else {
            Log.d(TAG, "getRatings: Not refreshing rating")
        }
    }

    fun updateRatings(rating: Int?) {
        if(rating != null) {
            savedStateHandle.set("episode_ratings", rating)
        } else {
            savedStateHandle.set("episode_ratings", -1)

        }

    }

    fun addListEntry(type: String, traktId: Int, traktList: TraktList) = viewModelScope.launch { eventsChannel.send(
        Event.AddListEntryEvent(listEntryRepository.addListEntry(type, traktId, traktList))) }
    fun removeListEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventsChannel.send(
        Event.RemoveListEntryEvent(listEntryRepository.removeEntry(listTraktId, listEntryTraktId, type))) }

    fun addRating(newRating: Int, episodeTraktId: Int) = viewModelScope.launch { eventsChannel.send(Event.AddRatingsEvent(repository.addRatings(newRating, episodeTraktId))) }

    fun resetRating(episodeTraktId: Int) = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingsEvent(repository.resetRating(episodeTraktId))) }

    fun checkin(episodeTraktId: Int) = viewModelScope.launch { eventsChannel.send(Event.AddCheckinEvent(repository.checkin(episodeTraktId))) }
    fun cancelCheckin(checkinCurrentEpisode: Boolean) = viewModelScope.launch { eventsChannel.send(Event.CancelCheckinEvent(checkinCurrentEpisode, repository.deleteActiveCheckin())) }

    fun addToWatchedHistory(episode: TmEpisode, watchedDate: OffsetDateTime) = viewModelScope.launch { eventsChannel.send(Event.AddToWatchedHistoryEvent(repository.addToWatchedHistory(episode, watchedDate))) }

    // Only trigger Data fetch once we have Episode Trakt Id
    fun setEpisodeTraktId(episodeTraktId: Int) {
        this.episodeTraktId = episodeTraktId
            getRatings(false)
    }

    fun onRefresh(): Boolean {
        return if(episodeTraktId != -1) {
            viewModelScope.launch {
                refreshEventChannel.send(true)
            }
            true
        } else {
            false
        }
    }



    sealed class Event {
        data class AddRatingsEvent(val syncResponse: Resource<Pair<SyncResponse, Int>>): Event()
        data class DeleteRatingsEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddCheckinEvent(val checkinResponse: Resource<EpisodeCheckinResponse?>): Event()
        data class CancelCheckinEvent(val checkinCurrentEpisode: Boolean, val cancelCheckinResult: Resource<Boolean>): Event()
        data class AddToWatchedHistoryEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddListEntryEvent(val addListEntryResponse: Resource<SyncResponse>): Event()
        data class RemoveListEntryEvent(val removeListEntryResponse: Resource<SyncResponse?>): Event()
    }
}