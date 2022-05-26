package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsProgressRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsProgressView"
@HiltViewModel
class ShowDetailsProgressViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: ShowDetailsProgressRepository, private val seasonEpisodesRepository: SeasonEpisodesRepository, private val statsRepository: StatsRepository) : ViewModel() {

    private var showDataModel: ShowDataModel? = savedStateHandle.get<ShowDataModel>(
        ShowDetailsActivity.SHOW_DATA_KEY)


    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        seasonEpisodesRepository.getSeasons(showDataModel?.traktId ?: 0, showDataModel?.tmdbId, shouldRefresh)
    }


    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)

            statsRepository.getWatchedSeasonStats(showDataModel?.traktId ?: 0, false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Called")
        viewModelScope.launch {
            refreshEventChannel.send(true)
            statsRepository.getWatchedSeasonStats(showDataModel?.traktId ?: 0, true)

        }
    }

    sealed class Event {
        data class DeleteWatchedHistoryItemEvent(val syncResponse: Resource<SyncResponse>): Event()
    }

}