package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ActionButtonEvent
import com.nickrankin.traktapp.model.ICreditsPersons
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeActionButtonsRepository
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
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
                                                  private val showDetailsActionButtonsRepository: EpisodeActionButtonsRepository,
                                                  private val episodeActionButtonsRepository: EpisodeActionButtonsRepository,
                                                  private val repository: EpisodeDetailsRepository,
                                                  private val seasonEpisodesRepository: SeasonEpisodesRepository,
                                                  private val episodesStatsRepository: EpisodesStatsRepository,
                                                  private val showDetailsOverviewRepository: ShowDetailsOverviewRepository): ViewModel(), ICreditsPersons {

    val episodeDataModel = savedStateHandle.get<EpisodeDataModel>(EpisodeDetailsActivity.EPISODE_DATA_KEY)

    private val eventChannel = Channel<ActionButtonEvent>()
    val events = eventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily)

    private val episodeIdChannel = Channel<Int>()
    private val episodeId = episodeIdChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val episodeNumberChannel = Channel<Int>()
    private val episodeNumber = episodeNumberChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, episodeDataModel?.episodeNumber ?: 0)



    @ExperimentalCoroutinesApi
    val show = refreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsRepository.getShowSummary(episodeDataModel?.showTraktId ?: 0, shouldRefresh)
    }

    val episode = refreshEvent.flatMapLatest { shouldRefresh ->
        episodeNumber.flatMapLatest { episodeNumber ->
            repository.getEpisodes(episodeDataModel?.showTraktId ?: 0, episodeDataModel?.tmdbId, episodeDataModel?.seasonNumber ?: -1, episodeNumber, shouldRefresh).map {
                if(it is Resource.Success) {
                    viewModelScope.launch {
                        episodeIdChannel.send(it.data?.episode_trakt_id ?: 0)
                    }
                }
                it
            }
        }
    }

    val seasonEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        combine(episodeNumber, seasonEpisodesRepository.getSeasonEpisodes(episodeDataModel?.showTraktId ?: 0, episodeDataModel?.tmdbId, episodeDataModel?.seasonNumber ?: 0, shouldRefresh)) { e, w ->
            Pair(e, w)
        }
    }

    val watchedEpisodes = episodeId.flatMapLatest { traktId ->
        refreshEvent.flatMapLatest { shouldRefresh ->
            episodeActionButtonsRepository.getPlaybackHistory(traktId, shouldRefresh)
        }
    }

    val collectionStatus = episodeId.flatMapLatest { episodeId ->
        refreshEvent.flatMapLatest { shouldRefresh ->
            episodeActionButtonsRepository.getCollectedStats(episodeId, shouldRefresh)
        }
    }

    val rating = episodeId.flatMapLatest { episodeId ->
        refreshEvent.flatMapLatest { shouldRefresh ->
            episodeActionButtonsRepository.getRatings(episodeId, shouldRefresh)
        }
    }

    // Overview Fragment
    override val cast = refreshEvent.flatMapLatest { shouldRefresh ->
        castToggle.flatMapLatest { showGuestStars ->
            showDetailsOverviewRepository.getEpisodeCast(episodeDataModel, showGuestStars, shouldRefresh)
        }
    }

    val watchedEpisodeStats = episodesStatsRepository.watchedEpisodeStats



    override fun filterCast(showGuestStars: Boolean) {
        viewModelScope.launch {
            castToggleChannel.send(showGuestStars)
        }
    }

    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        showDetailsActionButtonsRepository.getTraktListsAndItems(shouldRefresh)
    }

    fun addListEntry(itemTraktId: Int, listTraktId: Int) = viewModelScope.launch {
        showDetailsActionButtonsRepository.addToList(
            itemTraktId,
            listTraktId
        )
    }

    fun removeListEntry(itemTraktId: Int, listTraktId: Int) =
        viewModelScope.launch {

            showDetailsActionButtonsRepository.removeFromList(
                itemTraktId,
                listTraktId
            )
        }


    fun checkin(traktId: Int, overrideCheckins: Boolean) = viewModelScope.launch {
        eventChannel.send(ActionButtonEvent.CheckinEvent(episodeActionButtonsRepository.checkin(traktId, overrideCheckins)))
    }

    fun addRating(traktId: Int, newRating: Int, ratedAt: OffsetDateTime) = viewModelScope.launch {
        eventChannel.send(
            ActionButtonEvent.AddRatingEvent(episodeActionButtonsRepository.addRating(traktId, newRating, ratedAt), newRating)
        )
    }

    fun deleteRating(traktId: Int) = viewModelScope.launch {
        eventChannel.send(
            ActionButtonEvent.RemoveRatingEvent(episodeActionButtonsRepository.deleteRating(traktId))
        )
    }

    fun addToHistory(traktId: Int, whenWatched: OffsetDateTime) = viewModelScope.launch {
        eventChannel.send(
            ActionButtonEvent.AddHistoryEntryEvent(episodeActionButtonsRepository.addToHistory(traktId,whenWatched))
        )
    }

    fun deleteFromHistory(historyId: Long) = viewModelScope.launch {
        eventChannel.send(
            ActionButtonEvent.RemoveHistoryEntryEvent(episodeActionButtonsRepository.removeFromHistory(historyId))
        )
    }

    fun addToCollection(traktId: Int) = viewModelScope.launch {
        eventChannel.send(
            ActionButtonEvent.AddToCollectionEvent(episodeActionButtonsRepository.addToCollection(traktId))
        )
    }

    fun removeFromCollection(traktId: Int) = viewModelScope.launch {
        eventChannel.send(ActionButtonEvent.RemoveFromCollectionEvent(episodeActionButtonsRepository.removeFromCollection(traktId)))
    }

    fun switchEpisode(episodeNumber: Int?) {
        if(episodeNumber == null) {
            Log.e(TAG, "switchEpisode: Episode number cannot be null. ", )
            return
        }
        viewModelScope.launch {
            episodeNumberChannel.send(episodeNumber)
//            refreshEventChannel.send(false)
        }
    }

   fun onStart() {
        Log.d(TAG, "onStart: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        Log.d(TAG, "onRefresh: Called callStart")
        viewModelScope.launch {
            refreshEventChannel.send(true)

            //statsWorkRefreshHelper.refreshShowStats()
        }
    }

    fun resetRefreshStatus() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }
}