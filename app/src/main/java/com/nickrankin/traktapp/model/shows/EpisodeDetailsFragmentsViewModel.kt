package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.movies.MovieDetailsOverviewRepository
import com.nickrankin.traktapp.repo.ratings.EpisodeRatingsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsOverviewFragment
import com.uwetrottmann.trakt5.entities.EpisodeCheckinResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class EpisodeDetailsFragmentsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val listsRepository: TraktListsRepository,
    private val episodeRatingsRepository: EpisodeRatingsRepository,
    private val showDetailsOverviewRepository: ShowDetailsOverviewRepository,
    private val listEntryRepository: ListEntryRepository,
    private val episodeDetailsActionButtonsRepository: EpisodeDetailsActionButtonsRepository
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val episodeDataModel =
        savedStateHandle.get<EpisodeDataModel>(EpisodeDetailsActivity.EPISODE_DATA_KEY)


    // Overview Fragment
    val cast = castToggle.flatMapLatest { showGuestStars ->
        showDetailsOverviewRepository.getShowCast(
            episodeDataModel?.showTraktId ?: 0,
            showGuestStars
        )
    }

    val episode = episodeDetailsActionButtonsRepository.getEpisodeDetails(
        episodeDataModel?.showTraktId ?: 0,
        episodeDataModel?.seasonNumber ?: 0,
        episodeDataModel?.episodeNumber ?: 0
    )

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