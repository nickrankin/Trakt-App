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
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
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
                                                  private val episodeDetailsActionButtonsRepository: EpisodeDetailsActionButtonsRepository,
                                                  private val repository: EpisodeDetailsRepository,
                                                  private val listsRepository: TraktListsRepository,
                                                  private val episodesStatsRepository: EpisodesStatsRepository,
                                                  private val statsWorkRefreshHelper: StatsWorkRefreshHelper,
                                                  private val showDetailsOverviewRepository: ShowDetailsOverviewRepository,
                                                  private val listEntryRepository: ListEntryRepository,
                                                  private val episodeRatingsRepository: EpisodeRatingsRepository): ViewModel() {


    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

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

    // Overview Fragment
    val cast = refreshEvent.flatMapLatest { shouldRefresh ->
        castToggle.flatMapLatest { showGuestStars ->
            if(showGuestStars) {
                showDetailsOverviewRepository.getEpisodeGuestStars(
                    episodeDataModel,
                    episodeDataModel?.seasonNumber ?: 0,
                    episodeDataModel?.episodeNumber ?: 0,
                    shouldRefresh
                )
            } else {
                showDetailsOverviewRepository.getEpisodeCast(
                    episodeDataModel,
                    shouldRefresh
                )
            }
        }
    }

    val watchedEpisodeStats = episodesStatsRepository.watchedEpisodeStats

//    fun removeWatchedHistoryItem(syncItems: SyncItems) = viewModelScope.launch { eventChannel.send(Event.DeleteWatchedHistoryItem(repository.removeWatchedEpisode(syncItems))) }

    val collectedEpisodes = episodeDetailsActionButtonsRepository.collectedEpisodes

    fun filterCast(showGuestStars: Boolean) = viewModelScope.launch {
        castToggleChannel.send(showGuestStars)
    }

    // Action Buttons Fragment
    val listsWithEntries = listEntryRepository.listEntries

    val ratings = episodeRatingsRepository.episodeRatings

    fun addListEntry(type: String, traktId: Int, traktList: TraktList) = viewModelScope.launch {
        eventChannel.send(
            Event.AddListEntryEvent(listEntryRepository.addListEntry(type, traktId, traktList))
        )
    }

    fun removeListEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) =
        viewModelScope.launch {
            eventChannel.send(
                Event.RemoveListEntryEvent(
                    listEntryRepository.removeEntry(
                        listTraktId,
                        listEntryTraktId,
                        type
                    )
                )
            )
        }


    fun addRating(newRating: Int, episodeTraktId: Int) = viewModelScope.launch {
        eventChannel.send(
            Event.AddRatingsEvent(
                episodeRatingsRepository.addRatings(
                    newRating,
                    episodeDataModel,
                    episodeTraktId
                ), newRating
            )
        )
    }

    fun resetRating(episodeTraktId: Int) = viewModelScope.launch {
        eventChannel.send(
            Event.DeleteRatingsEvent(episodeRatingsRepository.resetRating(episodeTraktId))
        )
    }

    fun checkin(episodeTraktId: Int) = viewModelScope.launch {
        eventChannel.send(
            Event.AddCheckinEvent(episodeDetailsActionButtonsRepository.checkin(episodeTraktId))
        )
    }

    fun cancelCheckin(checkinCurrentEpisode: Boolean) = viewModelScope.launch {
        eventChannel.send(
            Event.CancelCheckinEvent(
                checkinCurrentEpisode,
                episodeDetailsActionButtonsRepository.deleteActiveCheckin()
            )
        )
    }

    fun addToWatchedHistory(episode: TmEpisode, watchedDate: OffsetDateTime) =
        viewModelScope.launch {
            eventChannel.send(
                Event.AddToWatchedHistoryEvent(
                    episodeDetailsActionButtonsRepository.addToWatchedHistory(
                        episode,
                        watchedDate
                    )
                )
            )
        }

    fun addToCollection(episode: TmEpisode) = viewModelScope.launch {
        eventChannel.send(
            Event.AddToCollectionEvent(
                episodeDetailsActionButtonsRepository.addToCollection(
                    episode
                )
            )
        )
    }

    fun removeFromCollection(episodeTraktId: Int) = viewModelScope.launch {
        eventChannel.send(
            Event.RemoveFromCollectionEvent(
                episodeDetailsActionButtonsRepository.removeFromCollection(
                    episodeTraktId
                )
            )
        )
    }


   fun onStart() {
        Log.d(TAG, "onStart: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(false)

            episodeDetailsActionButtonsRepository.refreshCollectedEpisodes(false)

            listsRepository.getListsAndEntries(false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(true)
            listsRepository.getListsAndEntries(true)

            statsWorkRefreshHelper.refreshShowStats()
            episodeDetailsActionButtonsRepository.refreshCollectedEpisodes(true)
        }
    }

    fun resetRefreshStatus() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    sealed class Event {
        data class AddRatingsEvent(
            val syncResponse: Resource<Pair<SyncResponse, Int>>,
            val newRating: Int
        ) : Event()

        data class DeleteRatingsEvent(val syncResponse: Resource<SyncResponse>) : Event()
        data class DeleteWatchedHistoryItem(val syncResponse: Resource<SyncResponse>) : Event()
        data class AddCheckinEvent(val checkinResponse: Resource<EpisodeCheckinResponse?>) : Event()
        data class CancelCheckinEvent(
            val checkinCurrentEpisode: Boolean,
            val cancelCheckinResult: Resource<Boolean>
        ) : Event()

        data class AddToWatchedHistoryEvent(val syncResponse: Resource<SyncResponse>) : Event()
        data class AddListEntryEvent(val addListEntryResponse: Resource<SyncResponse>) : Event()
        data class RemoveListEntryEvent(val removeListEntryResponse: Resource<SyncResponse?>) :
            Event()

        data class AddToCollectionEvent(val syncResponse: Resource<SyncResponse>) : Event()
        data class RemoveFromCollectionEvent(val syncResponse: Resource<SyncResponse>) : Event()
    }

}