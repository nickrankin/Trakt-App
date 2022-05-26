package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
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
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    val state = savedStateHandle

    val showDataModel: ShowDataModel? = savedStateHandle.get(ShowDetailsActivity.SHOW_DATA_KEY)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
            statsRepository.getWatchedSeasonStats(showDataModel?.traktId ?: 0, false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)

            statsRepository.getWatchedSeasonStats(showDataModel?.traktId ?: 0, true)

        }
    }
}