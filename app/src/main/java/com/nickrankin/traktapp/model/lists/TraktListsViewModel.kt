package com.nickrankin.traktapp.model.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.uwetrottmann.trakt5.entities.TraktList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TraktListsViewModel @Inject constructor(private val repository: TraktListsRepository): ViewModel() {

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)


    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getLists(shouldRefresh)
    }

    fun addList(traktList: TraktList) = viewModelScope.launch { eventChannel.send(Event.AddListEvent(repository.addTraktList(traktList))) }
    fun editList(traktList: TraktList, listSlug: String) = viewModelScope.launch { eventChannel.send(Event.EditListEvent(repository.editTraktList(traktList, listSlug))) }
    fun deleteList(listTraktId: String) = viewModelScope.launch { eventChannel.send(Event.DeleteListEvent(repository.deleteTraktList(listTraktId))) }


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
    }
}