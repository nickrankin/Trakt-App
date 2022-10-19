package com.nickrankin.traktapp.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor(): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)


    open fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }

    open fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
        }
    }
}