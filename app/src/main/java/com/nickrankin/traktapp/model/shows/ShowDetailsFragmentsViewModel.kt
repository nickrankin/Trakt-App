package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsProgressRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowDetailsFragmentsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle,
                                                        private val repository: ShowDetailsRepository,
                                                        private val listsRepository: TraktListsRepository,
                                                        private val listEntryRepository: ListEntryRepository,
                                                        private val showDetailsProgressRepository: ShowDetailsProgressRepository,
                                                        private val showDetailsActionButtonsRepository: ShowDetailsActionButtonsRepository,
                                                        private val showDetailsOverviewRepository: ShowDetailsOverviewRepository,
                                                        private val statsRepository: StatsRepository): ViewModel() {
    val showDataModel: ShowDataModel? = savedStateHandle.get(ShowDetailsActivity.SHOW_DATA_KEY)

    private val eventsChannel = Channel<Event>()
    val events = eventsChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    // Overview
    val cast = castToggle.flatMapLatest { showGuestStars ->
           showDetailsOverviewRepository.getShowCast(showDataModel?.traktId ?: 0, showGuestStars)
        }

    val show = showDetailsOverviewRepository.getShow(showDataModel)

    val seasons = showDetailsProgressRepository.getSeasons(showDataModel?.traktId ?: 0)

    fun filterCast(showGuestStars: Boolean) = viewModelScope.launch {
        castToggleChannel.send(showGuestStars)
    }

    val overallSeasonStats = showDetailsProgressRepository.getOverallSeasonStats(showDataModel?.traktId ?: 0)

    fun getSeasonWatchedStats(seasonNumber: Int) = showDetailsProgressRepository.getSeasonWatchedStats(showDataModel?.traktId ?: 0, seasonNumber)

    // Action Buttons
    val listsWithEntries =  listEntryRepository.listEntries

    fun getRatings(traktId: Int) = showDetailsActionButtonsRepository.getRatings(traktId)

    fun addRating(tmShow: TmShow, newRating: Int) = viewModelScope.launch {
        eventsChannel.send(Event.AddRatingEvent(showDetailsActionButtonsRepository.setRatings(tmShow, newRating, false), newRating))
    }

    fun deleteRating(tmShow: TmShow) = viewModelScope.launch { eventsChannel.send(Event.DeleteRatingEvent(showDetailsActionButtonsRepository.setRatings(tmShow, -1, true))) }

    fun addToCollection(tmShow: TmShow) = viewModelScope.launch { eventsChannel.send(Event.AddToCollectionEvent(showDetailsActionButtonsRepository.addToCollection(tmShow))) }

    val showsCollectedStatus = showDetailsActionButtonsRepository.getCollectedShowFlow(showDataModel?.traktId ?: -1).map { collectedShow ->
        collectedShow != null
    }

    fun addListEntry(type: String, traktId: Int, traktList: TraktList) = viewModelScope.launch { eventsChannel.send(
        Event.AddListEntryEvent(listEntryRepository.addListEntry(type, traktId, traktList))) }
    fun removeListEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventsChannel.send(
        Event.RemoveListEntryEvent(listEntryRepository.removeEntry(listTraktId, listEntryTraktId, type))) }

    fun removeFromCollection() = viewModelScope.launch { eventsChannel.send(Event.RemoveFromCollectionEvent(showDetailsActionButtonsRepository.removeFromCollection(showDataModel?.traktId ?: -1))) }

    sealed class Event {
        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddRatingEvent(val syncResponse: Resource<SyncResponse>, val newRating: Int): Event()
        data class DeleteRatingEvent(val syncResponse: Resource<SyncResponse>): Event()
        data class AddListEntryEvent(val addListEntryResponse: Resource<SyncResponse>): Event()
        data class RemoveListEntryEvent(val removeListEntryResponse: Resource<SyncResponse?>): Event()
    }
}