package com.nickrankin.traktapp.model.shows.episodedetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EpisodeDetailsOverviewV"

@HiltViewModel
class EpisodeDetailsOverviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val episodeDetailsRepository: EpisodeDetailsRepository
) : ViewModel() {
    private val initialRefreshEventChannel = Channel<Boolean>()
    private val initialRefreshEvent = initialRefreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val episodeDataModel =
        savedStateHandle.get<EpisodeDataModel>(EpisodeDetailsActivity.EPISODE_DATA_KEY)

    val cast = initialRefreshEvent.flatMapLatest { shouldRefresh ->
        castToggle.flatMapLatest { showGuestStars ->
            episodeDetailsRepository.getCredits(
                episodeDataModel?.traktId ?: 0,
                episodeDataModel?.tmdbId,
                shouldRefresh,
                showGuestStars
            )
                .map { castResource ->
                    if (castResource is com.nickrankin.traktapp.helper.Resource.Success) {
                        castResource.data =
                            castResource.data?.filter { it.showCastPersonData.isGuestStar == showGuestStars }
                        castResource
                    } else {
                        castResource
                    }
                }
        }
    }

    fun filterCast(showGuestStars: Boolean) = viewModelScope.launch {
        castToggleChannel.send(showGuestStars)
    }

    fun onStart() {
        viewModelScope.launch {
            initialRefreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            initialRefreshEventChannel.send(true)
        }
    }

}