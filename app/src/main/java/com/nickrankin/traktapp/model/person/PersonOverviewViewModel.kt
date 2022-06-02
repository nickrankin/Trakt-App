package com.nickrankin.traktapp.model.person

import android.util.Log
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

private const val TAG = "PersonViewModel"
@HiltViewModel
class PersonOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: PersonRepository): ViewModel() {
    private val personTraktId = savedStateHandle.get<Int>(PersonActivity.PERSON_ID_KEY)
    
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)
    
    val person = refreshEvent.flatMapLatest { shouldRefresh ->
        Log.d(TAG, "Getting person with $personTraktId. Should Refresh: $shouldRefresh: ")
        repository.getPerson(personTraktId ?: 0, shouldRefresh)
    }

    val personMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getPersonMovies(personTraktId ?: 0, shouldRefresh)
    }

    val personShows = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getPersonShows(personTraktId ?: 0, shouldRefresh)
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