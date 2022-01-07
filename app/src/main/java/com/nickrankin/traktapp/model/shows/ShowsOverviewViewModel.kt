package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowsOverviewViewModel @Inject constructor(private val repository: ShowsOverviewRepository): ViewModel() {
    private val myShowsRefreshEventChannel = Channel<Boolean>()
    private val myShowsRefreshEvent = myShowsRefreshEventChannel.receiveAsFlow()

    private var showHiddenEntries = false

    @ExperimentalCoroutinesApi
    val myShows = myShowsRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMyShows(shouldRefresh)
    }.map { resource ->
        if(resource is Resource.Success) {
            repository.removeAlreadyAiredEpisodes(resource.data ?: emptyList())

        }
        resource
    }.map { resource ->
        repository.getHiddenStatus(showHiddenEntries, resource.data ?: emptyList())
    }

    fun showHiddenEntries(showHidden: Boolean) {
        this.showHiddenEntries = showHidden
    }

    fun setShowHiddenState(showTmdbId: Int, isHidden: Boolean) = viewModelScope.launch { repository.setShowHiddenState(showTmdbId, isHidden) }

    fun onReload() {
        viewModelScope.launch {
            launch {
                myShowsRefreshEventChannel.send(false)
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            launch {
                myShowsRefreshEventChannel.send(true)
            }
        }
    }
}