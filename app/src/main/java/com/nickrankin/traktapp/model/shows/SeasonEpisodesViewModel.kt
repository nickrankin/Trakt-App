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
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val seasonDataModel: SeasonDataModel =
        savedStateHandle.get<SeasonDataModel>(SeasonEpisodesFragment.SEASON_DATA_KEY)!!

    private val seasonSwitchedChannel = Channel<Int>()
    private val seasonSwitched = seasonSwitchedChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShow(seasonDataModel.traktId, seasonDataModel.tmdbId, shouldRefresh)
    }

    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeasons(seasonDataModel.traktId, seasonDataModel.tmdbId, shouldRefresh)
    }

    val currentSeason = refreshEvent.flatMapLatest { shouldRefresh ->
        seasonSwitched.flatMapLatest { seasonNumber ->
            repository.getSeason(
                seasonDataModel.traktId,
                seasonDataModel.tmdbId,
                seasonNumber,
                shouldRefresh
            )
        }
    }

    fun switchSeason(SeasonNumber: Int) {
        Log.e(TAG, "switchSeason: New Season number $SeasonNumber")
        viewModelScope.launch {
            seasonSwitchedChannel.send(SeasonNumber)
        }
    }

    @ExperimentalCoroutinesApi
    val episodes = refreshEvent.flatMapLatest { shouldRefresh ->
        seasonSwitched.flatMapLatest { seasonNumber ->
            repository.getSeasonEpisodes(
                seasonDataModel.traktId ?: 0,
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
            refreshEventChannel.send(true)
//            repository.refreshShowStats(seasonDataModel?.traktId)
        }
    }
}