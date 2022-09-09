package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.SeasonDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.shows.SeasonEpisodesActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SeasonEpisodesViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: SeasonEpisodesRepository, private val statsWorkRefreshHelper: StatsWorkRefreshHelper): ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val seasonDataModel = savedStateHandle.get<SeasonDataModel>(SeasonEpisodesActivity.SEASON_DATA_KEY)

    private var seasonNumber = seasonDataModel?.seasonNumber ?: 0

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShow(seasonDataModel?.traktId ?: 0, seasonDataModel?.tmdbId, shouldRefresh)
    }

    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeasons(seasonDataModel?.traktId ?: 0, seasonDataModel?.tmdbId, shouldRefresh)
    }

    val currentSeason = seasons.map { seasonResource ->
        if(seasonResource is Resource.Success) {
            seasonResource.data = seasonResource.data?.filter { it.season.season_number == seasonNumber }
            seasonResource
        } else {
            seasonResource
        }
    }

    fun switchSeason(seasonNumber: Int) {
                this.seasonNumber = seasonNumber
                viewModelScope.launch {
                    refreshEventChannel.send(false)
                }
    }

    @ExperimentalCoroutinesApi
    val episodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getSeasonEpisodes(seasonDataModel?.traktId ?: 0, seasonDataModel?.tmdbId, seasonNumber, shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
            statsWorkRefreshHelper.refreshShowStats()
        }
    }
}