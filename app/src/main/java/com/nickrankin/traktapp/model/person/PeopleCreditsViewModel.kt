package com.nickrankin.traktapp.model.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.repo.person.PersonRepository
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PeopleCreditsViewModel"
@HiltViewModel
class PeopleCreditsViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: PersonRepository): ViewModel() {

    private val personTraktId: Int = savedStateHandle.get<Int>(PersonActivity.PERSON_ID_KEY) ?: 0

    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)




    val movies = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getPersonMovies(personTraktId, shouldRefresh)
    }

    val shows = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getPersonShows(personTraktId, shouldRefresh)
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