package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.SeasonStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.services.ShowStatsRefreshWorker
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
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
    private val seasonEpisodesRepository: SeasonEpisodesRepository,
    private val showDetailsOverviewRepository: ShowDetailsOverviewRepository,
    private val listsRepository: TraktListsRepository,
    private val statsWorkRefreshHelper: StatsWorkRefreshHelper,
    private val seasonStatsRepository: SeasonStatsRepository
) : ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val state = savedStateHandle

    val showDataModel: ShowDataModel? = savedStateHandle.get(ShowDetailsActivity.SHOW_DATA_KEY)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    // Seasons & progress
    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        seasonEpisodesRepository.getSeasons(showDataModel?.traktId ?: 0, showDataModel?.tmdbId ?: 0, shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)

            seasonStatsRepository.getWatchedSeasonStatsPerShow(showDataModel?.traktId ?: 0, false)

            showDetailsOverviewRepository.getCredits(showDataModel, false)
            listsRepository.getListsAndEntries(false)

        }
    }

    fun onRefresh(showStatsRefreshWorkInfo: (workInfo: LiveData<WorkInfo>) -> Unit) {
        viewModelScope.launch {
            refreshEventChannel.send(true)

            showDetailsOverviewRepository.getCredits(showDataModel, true)
            listsRepository.getListsAndEntries(true)

            seasonStatsRepository.getWatchedSeasonStatsPerShow(showDataModel?.traktId ?: 0, true)

            showStatsRefreshWorkInfo(statsWorkRefreshHelper.refreshShowStats())
        }

    }


}