package com.nickrankin.traktapp.model.lists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.lists.ListsRepository
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.TraktList
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
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
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private val menuSortingChannel = Channel<String>()
    private val menuSorting = menuSortingChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SORT_MENU_NUM_ITEMS)

    private val menuSortHowChannel = Channel<String>()
    private val menuSortHow = menuSortHowChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortHow.DESC.name)


    val lists = refreshEvent.flatMapLatest { shouldRefresh ->
        combine(menuSorting, menuSortHow) { sortBy, sortHow ->
            repository.getLists(sortBy, sortHow, shouldRefresh)
        }.flattenMerge()
    }

    val list = refreshEvent.flatMapLatest { shouldRefresh ->
        activeList.flatMapLatest { listId ->
                repository.getListById(listId, shouldRefresh)
        }
    }

    val listItems = refreshEvent.flatMapLatest { shouldRefresh ->
        activeList.flatMapLatest { listId ->
            combine(repository.getListById(listId, shouldRefresh), repository.getListEntries(listId, shouldRefresh)) { list, entries ->
                Pair(list, entries)
            }
        }
    }


    fun addList(traktList: TraktList) = viewModelScope.launch { eventChannel.send(Event.AddListEvent(repository.addTraktList(traktList))) }
    fun editList(traktList: TraktList, listSlug: String) = viewModelScope.launch { eventChannel.send(Event.EditListEvent(repository.editTraktList(traktList, listSlug))) }
    fun deleteList(listTraktId: String) = viewModelScope.launch { eventChannel.send(Event.DeleteListEvent(repository.deleteTraktList(listTraktId))) }

    fun changeListOrdering(traktList: com.nickrankin.traktapp.dao.lists.model.TraktList?, newSortBy: SortBy) = viewModelScope.launch { eventChannel.send(Event.ErrorEvent(repository.reorderList(traktList, newSortBy))) }

    fun removeEntry(listTraktId: Int, listEntryTraktId: Int, type: Type) = viewModelScope.launch { eventChannel.send(
        Event.RemoveEntryEvent(repository.removeFromList(listEntryTraktId, listTraktId, type))) }


    fun switchList(listTraktId: Int?) {
        viewModelScope.launch {
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

    fun changeOrdering(newOrdering: String) {
        viewModelScope.launch {
            if(menuSorting.value == newOrdering) {
                if(menuSortHow.value == SortHow.DESC.name) {
                    menuSortHowChannel.send(SortHow.ASC.name)
                } else {
                    menuSortHowChannel.send(SortHow.DESC.name)
                }
            }
            menuSortingChannel.send(newOrdering)
        }
    }

    sealed class Event {
        data class AddListEvent(val addedListResource: Resource<com.nickrankin.traktapp.dao.lists.model.TraktList>): Event()
        data class EditListEvent(val editListResource: Resource<com.nickrankin.traktapp.dao.lists.model.TraktList>): Event()
        data class DeleteListEvent(val deleteListResource: Resource<Boolean>): Event()
        data class RemoveEntryEvent(val syncResponseResource: Resource<SyncResponse>): Event()

        data class ErrorEvent(val error: Throwable?): Event()

    }

    companion object {
        const val LIST_ID_KEY = "list_id_key"
        const val LIST_NAME_KEY = "list_name_key"

        const val SORT_MENU_TITLE = "name"
        const val SORT_MENU_CREATED = "created_at"
        const val SORT_MENU_NUM_ITEMS = "item_count"
    }

}