package com.nickrankin.traktapp.model.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListEntryViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: ListEntryRepository): ViewModel() {
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val eventsChannel = Channel<Event>()
    val event = eventsChannel.receiveAsFlow()

    private val listId: Int = savedStateHandle.get<Int>(LIST_ID_KEY) ?: 0

    val listItems = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getListEntries(listId, shouldRefresh)
    }

    fun removeEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventsChannel.send(Event.RemoveEntryEvent(repository.removeEntry(listTraktId, listEntryTraktId, type))) }


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

    companion object {
        const val LIST_ID_KEY = "list_id_key"
        const val LIST_NAME_KEY = "list_name_key"
    }

    sealed class Event {
        data class RemoveEntryEvent(val syncResponseResource: Resource<SyncResponse?>): Event()
    }
}