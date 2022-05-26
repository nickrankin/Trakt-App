package com.nickrankin.traktapp.model.shows.showdetails

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsOverviewRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsSeasonsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowDetailsOverviewView"
@HiltViewModel
class ShowDetailsOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle,
                                                       private val repository: ShowDetailsOverviewRepository) : ViewModel() {
    private var showDataModel: ShowDataModel? = savedStateHandle.get<ShowDataModel>(ShowDetailsActivity.SHOW_DATA_KEY)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val castToggleChannel = Channel<Boolean>()
    private val castToggle = castToggleChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val show = repository.getShow(showDataModel?.traktId ?: 0)

    val cast = refreshEvent.flatMapLatest { shouldRefresh ->
        castToggle.flatMapLatest { showGuestStars ->
            repository.getCredits(showDataModel?.traktId ?: 0, showDataModel?.tmdbId, shouldRefresh, showGuestStars).map { castResource ->
                if(castResource is Resource.Success) {
                    castResource.data = castResource.data?.filter { it.showCastPersonData.isGuestStar == showGuestStars }
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
            refreshEventChannel.send(false)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
        }
    }
}