package com.nickrankin.traktapp.model.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.TrackedEpisodesRepository
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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


    val state = savedStateHandle

    val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    val tmdbId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TMDB_ID_KEY) ?: -1

    init {
            getAllUserRatings()
    }

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(traktId, shouldRefresh)
    }

    fun getAllUserRatings() = viewModelScope.launch {
        val ratings = repository.getAllUserRatings(traktId)

        if(ratings is Resource.Success) {
            if(ratings.data != null) {
                savedStateHandle.set("trakt_ratings", ratings.data!!.rating)
            }
        } else {
            Log.e(TAG, "Error getting Trakt Ratings ${ratings.error?.localizedMessage}: ", )
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
            getAllUserRatings()
        }
    }
}