package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchedEpisodesViewModel @Inject constructor(private val repository: WatchedEpisodesRepository): ViewModel() {

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    val watchedEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.watchedEpisodes(shouldRefresh)
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