package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowsOverviewViewModel @Inject constructor(private val repository: ShowsOverviewRepository): ViewModel() {
    private val myShowsRefreshEventChannel = Channel<Boolean>()
    private val myShowsRefreshEvent = myShowsRefreshEventChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    val myShows = myShowsRefreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMyShows(shouldRefresh)
    }

    fun onStart() {
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