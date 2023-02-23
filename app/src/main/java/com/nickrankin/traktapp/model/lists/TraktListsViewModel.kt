package com.nickrankin.traktapp.model.lists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.lists.ListsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject

private const val TAG = "TraktListsViewModel"
@HiltViewModel
class TraktListsViewModel @Inject constructor(private val repository: ListsRepository): ViewModel() {
    
    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val activeListChannel = Channel<Int?>()
    val activeList = activeListChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getLists(shouldRefresh)
    }

    val list = refreshEvent.flatMapLatest { shouldRefresh ->
        activeList.flatMapLatest { listId ->
                repository.getListById(listId, shouldRefresh)
        }
    }

    val listItems = refreshEvent.flatMapLatest { shouldRefresh ->
        activeList.flatMapLatest { listId ->
            Log.e(TAG, "Triggered with list id $listId: ", )
                repository.getListEntries(listId, shouldRefresh)
        }
    }

    fun addList(traktList: TraktList) = viewModelScope.launch { eventChannel.send(Event.AddListEvent(repository.addTraktList(traktList))) }
    fun editList(traktList: TraktList, listSlug: String) = viewModelScope.launch { eventChannel.send(Event.EditListEvent(repository.editTraktList(traktList, listSlug))) }
    fun deleteList(listTraktId: String) = viewModelScope.launch { eventChannel.send(Event.DeleteListEvent(repository.deleteTraktList(listTraktId))) }

    fun removeEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventChannel.send(
        Event.RemoveEntryEvent(repository.removeFromList(listEntryTraktId, listTraktId, type))) }


    fun switchList(listTraktId: Int?) {
        viewModelScope.launch {
            Log.e(TAG, "switchList: New list $listTraktId", )
            activeListChannel.send(listTraktId)
        }
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

    sealed class Event {
        data class AddListEvent(val addedListResource: Resource<com.nickrankin.traktapp.dao.lists.model.TraktList>): Event()
        data class EditListEvent(val editListResource: Resource<com.nickrankin.traktapp.dao.lists.model.TraktList>): Event()
        data class DeleteListEvent(val deleteListResource: Resource<Boolean>): Event()
        data class RemoveEntryEvent(val syncResponseResource: Resource<SyncResponse>): Event()

    }

    companion object {
        const val LIST_ID_KEY = "list_id_key"
        const val LIST_NAME_KEY = "list_name_key"
    }

}