package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.helper.TmdbToTraktIdHelper
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ICreditsPersons
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.*
import com.nickrankin.traktapp.repo.stats.SeasonStatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "ShowDetailsViewModel"

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val showActionButtonsRepository: ShowActionButtonsRepsitory,
    private val repository: ShowDetailsRepository,
    private val seasonEpisodesRepository: SeasonEpisodesRepository,
    private val showDetailsOverviewRepository: ShowDetailsOverviewRepository,
    private val statsWorkRefreshHelper: StatsWorkRefreshHelper,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val showDetailsProgressRepository: ShowDetailsProgressRepository,
    private val tmdbToTraktIdHelper: TmdbToTraktIdHelper
) : BaseViewModel(), ICreditsPersons {

    private val eventsChannel = Channel<ActionButtonEvent>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)


    private val showDataModelChangedChannel = Channel<ShowDataModel>()
    private val showDataModelChanded = showDataModelChangedChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            repository.getShowSummary(showDataModel.traktId, shouldRefresh)
        }
    }

    // Seasons & progress
    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            seasonEpisodesRepository.getSeasons(showDataModel.traktId, showDataModel.tmdbId ?: 0, shouldRefresh)
        }
    }

    val seasonWatchedStats = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            showDetailsProgressRepository.getSeasonStats(showDataModel.traktId, showDataModel.tmdbId, shouldRefresh)
        }
    }


    // Overview
    override val cast = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            castToggle.flatMapLatest { showGuestStars ->
                showDetailsOverviewRepository.getCast(showDataModel.traktId, showDataModel.tmdbId ?: 0, showGuestStars, shouldRefresh)
            }
        }

    }

    override fun filterCast(showGuestStars: Boolean) {
        viewModelScope.launch {
            castToggleChannel.send(showGuestStars)
        }
    }

    suspend fun getTraktPersonByTmdbId(tmdbId: Int) = tmdbToTraktIdHelper.getTraktPersonByTmdbId(tmdbId)

    // Action buttons
    val playCount = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            showActionButtonsRepository.getPlaybackHistory(showDataModel.traktId, shouldRefresh)
        }
    }

    val ratings = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            showActionButtonsRepository.getRatings(showDataModel.traktId, shouldRefresh)
        }
    }

    val collectionStatus = refreshEvent.flatMapLatest { shouldRefresh ->
        showDataModelChanded.flatMapLatest { showDataModel ->
            showActionButtonsRepository.getCollectedStats(showDataModel.traktId, shouldRefresh)
        }
    }

    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        showActionButtonsRepository.getTraktListsAndItems(shouldRefresh)
    }

//    suspend fun showStreamingServices() = repository.getShowStreamingServices(showDataModel?.tmdbId, showDataModel?.showTitle)

    fun addToCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddToCollectionEvent(showActionButtonsRepository.addToCollection(
                traktId
            ))
        )
    }

    fun deleteFromCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveFromCollectionEvent(
                showActionButtonsRepository.removeFromCollection(
                    traktId
                )
            )
        )
    }

    fun addListEntry(itemTraktId: Int, listTraktId: Int) = viewModelScope.launch {
        showActionButtonsRepository.addToList(
            itemTraktId,
            listTraktId
        )
    }

    fun removeListEntry(itemTraktId: Int, listTraktId: Int) =
        viewModelScope.launch {

            showActionButtonsRepository.removeFromList(
                itemTraktId,
                listTraktId
            )
        }

    fun addRating(newRating: Int, traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddRatingEvent(
                showActionButtonsRepository.addRating(traktId, newRating, OffsetDateTime.now()),
                newRating
            )
        )
    }

    fun deleteRating(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveRatingEvent(showActionButtonsRepository.deleteRating(traktId))
        )
    }

    fun checkin(traktId: Int, cancelActiveCheckins: Boolean) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.CheckinEvent(
                showActionButtonsRepository.checkin(
                    traktId,
                    cancelActiveCheckins
                )
            )
        )
    }

    fun addToWatchedHistory(traktId: Int, watchedDate: OffsetDateTime) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddHistoryEntryEvent(
                showActionButtonsRepository.addToHistory(
                    traktId,
                    watchedDate
                )
            )
        )
    }

    fun removeHistoryEntry(historyEntry: HistoryEntry) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveHistoryEntryEvent(
                showActionButtonsRepository.removeFromHistory(historyEntry.history_id)
            )
        )
    }

    fun switchShowDataModel(showDataModel: ShowDataModel) {
        viewModelScope.launch {
            showDataModelChangedChannel.send(showDataModel)
        }
    }
    
//
//    override fun onStart() {
//        viewModelScope.launch {
////            seasonStatsRepository.getWatchedSeasonStatsPerShow(showDataModel?.traktId ?: 0, false)
//        }
//    }
//
//    override fun onRefresh() {
//        viewModelScope.launch {
//
//            statsWorkRefreshHelper.refreshShowStats()
//        }
//
//    }

}