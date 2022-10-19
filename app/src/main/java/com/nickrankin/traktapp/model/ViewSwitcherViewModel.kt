package com.nickrankin.traktapp.model

import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
open class ViewSwitcherViewModel @Inject constructor(): BaseViewModel() {
    private val viewTypeChannel = Channel<Int>()
    val viewType = viewTypeChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    suspend fun switchViewType() {
        if(viewType.value == MediaEntryBaseAdapter.VIEW_TYPE_CARD) {
            viewTypeChannel.send(MediaEntryBaseAdapter.VIEW_TYPE_POSTER)
        } else {
            viewTypeChannel.send(MediaEntryBaseAdapter.VIEW_TYPE_CARD)
        }
    }
}