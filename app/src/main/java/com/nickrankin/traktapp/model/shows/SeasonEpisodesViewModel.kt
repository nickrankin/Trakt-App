package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SeasonEpisodesViewModel"

@HiltViewModel
class SeasonEpisodesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SeasonEpisodesRepository,
    private val statsWorkRefreshHelper: StatsWorkRefreshHelper
) : ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val seasonDataModel: SeasonDataModel =
        savedStateHandle.get<SeasonDataModel>(SeasonEpisodesFragment.SEASON_DATA_KEY)!!

    private val seasonSwitchedChannel = Channel<Int>()
    val seasonSwitched = seasonSwitchedChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, savedStateHandle.get<SeasonDataModel>(SeasonEpisodesFragment.SEASON_DATA_KEY)?.seasonNumber ?: 0)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShow(seasonDataModel.traktId, seasonDataModel.tmdbId, shouldRefresh)
    }

    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeasons(seasonDataModel.traktId, seasonDataModel.tmdbId, shouldRefresh)
    }

    val currentSeason = seasonSwitched.flatMapLatest { seasonNumber ->
            repository.getSeason(
                seasonDataModel.traktId,
                seasonDataModel.tmdbId,
                seasonNumber,
                false
            )
    }

    fun switchSeason(SeasonNumber: Int) {
        Log.e(TAG, "switchSeason: New Season number $SeasonNumber")
        viewModelScope.launch {
            seasonSwitchedChannel.send(SeasonNumber)
        }
    }

    @ExperimentalCoroutinesApi
    val episodes = seasonSwitched.flatMapLatest { seasonNumber ->
        refreshEvent.flatMapLatest { shouldRefresh ->
            repository.getSeasonEpisodes(
                seasonDataModel.traktId,
                seasonDataModel.tmdbId,
                seasonNumber,
                shouldRefresh
            )
        }
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            Log.e(TAG, "onRefresh: HERE", )
            refreshEventChannel.send(true)
//            repository.refreshShowStats(seasonDataModel?.traktId)
        }
    }
}