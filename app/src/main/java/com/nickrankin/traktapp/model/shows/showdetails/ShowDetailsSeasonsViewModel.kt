package com.nickrankin.traktapp.model.shows.showdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsRepository
import com.nickrankin.traktapp.repo.shows.showdetails.ShowDetailsSeasonsRepository
import com.nickrankin.traktapp.ui.shows.showdetails.ShowDetailsActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowDetailsSeasonsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: ShowDetailsSeasonsRepository) : ViewModel() {

       private val refreshEventChannel = Channel<Boolean>()
       private val refreshEvent = refreshEventChannel.receiveAsFlow()
              .shareIn(viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())

       private val showDataModel = savedStateHandle.get<ShowDataModel>(ShowDetailsActivity.SHOW_DATA_KEY)

       val seasons =  refreshEvent.flatMapLatest { shouldRefresh ->
              repository.getSeasons(showDataModel?.traktId ?: 0, showDataModel?.tmdbId, null, shouldRefresh)
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