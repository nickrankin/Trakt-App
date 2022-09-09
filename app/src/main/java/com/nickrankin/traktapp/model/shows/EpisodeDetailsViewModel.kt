package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.ListWithEntries
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.ratings.EpisodeRatingsRepository
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.uwetrottmann.trakt5.entities.EpisodeCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import java.util.*
import javax.inject.Inject

private const val TAG = "EpisodeDetailsViewModel"
@HiltViewModel
class EpisodeDetailsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle,
                                                  private val showDetailsRepository: ShowDetailsRepository,
                                                  private val repository: EpisodeDetailsRepository,
                                                  private val listsRepository: TraktListsRepository,
                                                  private val statsRepository: StatsRepository,
                                                  private val statsWorkRefreshHelper: StatsWorkRefreshHelper,
private val episodeRatingsRepository: EpisodeRatingsRepository): ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val episodeDataModel = savedStateHandle.get<EpisodeDataModel>(EpisodeDetailsActivity.EPISODE_DATA_KEY)

    @ExperimentalCoroutinesApi
    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(episodeDataModel?.showTraktId ?: 0, shouldRefresh)
    }

    val episode = refreshEvent.flatMapLatest { shouldRefresh ->
        Log.d(TAG, "callStart Calling episode with status $shouldRefresh: ${UUID.randomUUID()} ")
        repository.getEpisodes(episodeDataModel?.showTraktId ?: 0, episodeDataModel?.tmdbId, episodeDataModel?.seasonNumber ?: -1, episodeDataModel?.episodeNumber ?: -1, shouldRefresh)
    }

    val watchedEpisodeStats = statsRepository.watchedEpisodeStats

//    fun removeWatchedHistoryItem(syncItems: SyncItems) = viewModelScope.launch { eventChannel.send(Event.DeleteWatchedHistoryItem(repository.removeWatchedEpisode(syncItems))) }


   fun onStart() {
        Log.d(TAG, "onStart: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(false)

            repository.getCast(episodeDataModel, false)
            listsRepository.getListsAndEntries(false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(true)

            repository.getCast(episodeDataModel, true)
            listsRepository.getListsAndEntries(true)

            statsWorkRefreshHelper.refreshShowStats()
        }
    }


}