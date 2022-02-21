package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsActionButton"

@HiltViewModel
class ShowDetailsActionButtonsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ShowDetailsActionButtonsRepository
) : ViewModel() {

    val traktId = savedStateHandle.get<Int>(ShowDetailsRepository.SHOW_TRAKT_ID_KEY)
    val ratings = savedStateHandle.getLiveData<Int>(RATINGS_KEY)

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    init {
        getRatings()
    }

    fun getRatings() {
        viewModelScope.launch {
            val result = repository.getRatings()

            if (result is Resource.Success) {
                Log.d(TAG, "getRatings: Got ratings successfully from Trakt")
                val currentShowRating =
                    result.data?.find { it.show?.ids?.trakt ?: 0 == traktId ?: -1 }

                if (currentShowRating != null) {
                    Log.d(TAG, "getRatings: Found rating for show $traktId")
                    // This show is rated
                    updateRatingDisplay(currentShowRating.rating?.value?.inc())
                } else {
                    Log.d(TAG, "getRatings: Show $traktId not rated yet")
                }
            } else if(result is Resource.Error) {
                Log.e(TAG, "getRatings: Error getting ratings", )
            }
        }
    }

    fun updateRatingDisplay(newRating: Int?) {
        ratings.value = newRating
    }

    fun addRating(newRating: Int) = viewModelScope.launch {
        eventsChannel.send(Event.AddRatingEvent(repository.setRatings(traktId ?: -1, newRating, false), newRating))
    }

    fun deleteRating() = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingEvent(repository.setRatings(traktId ?: -1, -1, true))) }



    fun addToCollection() = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(repository.addToCollection(traktId ?: -1))) }

    val showsCollectedStatus = repository.getCollectedShowFlow(traktId ?: -1).map { collectedShow ->
        collectedShow != null
    }

    fun removeFromCollection() = viewModelScope.launch { eventsChannel.send(Event.RemoveFromCollectionEvent(repository.removeFromCollection(traktId ?: -1))) }


    fun onStart() {
        viewModelScope.launch {
            repository.refreshCollectedStatus(false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Refreshing ActionButtons Data")
        getRatings()
        viewModelScope.launch {
            repository.refreshCollectedStatus(true)
        }
    }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): Event()
        data class DeleteRatingEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

    companion object {
        const val RATINGS_KEY = "ratings_key"
    }
}