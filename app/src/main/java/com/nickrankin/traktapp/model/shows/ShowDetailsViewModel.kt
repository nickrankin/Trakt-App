package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.helper.TmdbToTraktIdHelper
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.ICreditsPersons
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.*
import com.nickrankin.traktapp.repo.stats.SeasonStatsRepository
import com.nickrankin.traktapp.services.helper.StatsWorkRefreshHelper
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
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
) : ViewModel(), ICreditsPersons {

    private val eventsChannel = Channel<ActionButtonEvent>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val showDataModel: ShowDataModel? = savedStateHandle.get(ShowDetailsActivity.SHOW_DATA_KEY)

    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getShowSummary(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    // Seasons & progress
    val seasons = refreshEvent.flatMapLatest { shouldRefresh ->
            seasonEpisodesRepository.getSeasons(showDataModel?.traktId ?: 0, showDataModel?.tmdbId ?: 0, shouldRefresh)
    }

    val seasonWatchedStats = refreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsProgressRepository.getSeasonStats(showDataModel?.traktId ?: 0, showDataModel?.tmdbId, shouldRefresh)
    }


    // Overview
    override val cast = refreshEvent.flatMapLatest { shouldRefresh ->
        castToggle.flatMapLatest { showGuestStars ->
            showDetailsOverviewRepository.getCast(showDataModel?.traktId ?: 0, showDataModel?.tmdbId ?: 0, showGuestStars, shouldRefresh)
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
        showActionButtonsRepository.getPlaybackHistory(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    val ratings = refreshEvent.flatMapLatest { shouldRefresh ->
        showActionButtonsRepository.getRatings(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    val collectionStatus = refreshEvent.flatMapLatest { shouldRefresh ->
        showActionButtonsRepository.getCollectedStats(showDataModel?.traktId ?: 0, shouldRefresh)
    }

    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        showActionButtonsRepository.getTraktListsAndItems(shouldRefresh)
    }

    suspend fun showStreamingServices() = repository.getShowStreamingServices(showDataModel?.tmdbId, showDataModel?.showTitle)

    fun addToCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.AddToCollectionEvent(showActionButtonsRepository.addToCollection(traktId ?: 0))
        )
    }

    fun deleteFromCollection(traktId: Int) = viewModelScope.launch {
        eventsChannel.send(
            ActionButtonEvent.RemoveFromCollectionEvent(
                showActionButtonsRepository.removeFromCollection(
                    traktId ?: -1
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
            ActionButtonEvent.RemoveRatingEvent(showActionButtonsRepository.deleteRating(traktId ?: -1))
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

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
            seasonStatsRepository.getWatchedSeasonStatsPerShow(showDataModel?.traktId ?: 0, false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)

            statsWorkRefreshHelper.refreshShowStats()
        }

    }

    fun resetRefreshState() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }
}