package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsSeasonsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsOverviewView"
@HiltViewModel
class ShowDetailsOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle,
                                                       private val repository: ShowDetailsOverviewRepository) : ViewModel() {

    val traktId: Int = savedStateHandle.get(ShowDetailsRepository.SHOW_TRAKT_ID_KEY) ?: 0
    private var tmdbId: Int = 0

    /**
     *
     * First: showGuestStars
     * Second: shouldRefresh
     * */
    private val castRefreshEventChannel = Channel<Pair<Boolean, Boolean>>()
    private val castRefreshEvent = castRefreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val cast = castRefreshEvent.flatMapLatest { updateCast ->
        repository.getCredits(traktId, tmdbId, updateCast.first, updateCast.second)
    }

    fun filterCast(showGuestStars: Boolean) = viewModelScope.launch {

        castRefreshEventChannel.send(Pair(showGuestStars, false))
    }

    fun setTmdbId(tmdbId: Int) {
        this.tmdbId = tmdbId
    }

    fun onStart() {
        viewModelScope.launch {
            castRefreshEventChannel.send(Pair(false, false))
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            castRefreshEventChannel.send(Pair(false, true))
        }
    }
}