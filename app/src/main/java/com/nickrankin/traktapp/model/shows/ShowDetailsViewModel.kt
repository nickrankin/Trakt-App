package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsViewModel"

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ShowDetailsRepository
) : ViewModel() {

    private var ratingsInitialRefresh = true

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    val state = savedStateHandle

    val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    val tmdbId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TMDB_ID_KEY) ?: -1
    
    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(traktId, shouldRefresh)
    }

    fun getAllUserRatings(shouldRefresh: Boolean) = viewModelScope.launch {

        if(ratingsInitialRefresh || shouldRefresh) {

            Log.d(TAG, "getAllUserRatings: Getting Trakt User Ratings")
            val ratings = repository.getAllUserRatings(traktId)

            if(ratings is Resource.Success) {
                ratingsInitialRefresh = false

                if(ratings.data != null) {
                    savedStateHandle.set("trakt_ratings", ratings.data!!.rating)
                }
            } else {
                Log.e(TAG, "Error getting Trakt Ratings ${ratings.error?.localizedMessage}: ", )
            }
        }
    }

    fun onStart() {
        getAllUserRatings(false)

        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
            getAllUserRatings(true)
        }
    }
}