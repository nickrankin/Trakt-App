package com.nickrankin.traktapp.model.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.credits.model.CrewType
import com.nickrankin.traktapp.repo.person.PersonRepository
import com.nickrankin.traktapp.ui.person.PersonActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PersonViewModel"
@HiltViewModel
class PersonOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: PersonRepository): ViewModel() {
    private val personTraktId = savedStateHandle.get<Int>(PersonActivity.PERSON_ID_KEY)
    
    private val refreshEventChannel = Channel<Boolean>()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val filterChannel = Channel<String>()
    val filter = filterChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val crewTypeChannel = Channel<CrewType>()
    val crewType = crewTypeChannel.receiveAsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CrewType.PRODUCING)
    
    val person = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getPerson(personTraktId ?: 0, shouldRefresh)
    }

    val personCrew = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getCrewPersonCredits(personTraktId ?: 0, shouldRefresh)
    }

    val personCast = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getcastPersonCredits(personTraktId ?: 0, shouldRefresh)
    }

    fun changeCrewType(crewType: CrewType) {
        viewModelScope.launch {
            crewTypeChannel.send(crewType)
        }
    }

    fun changeFilter(newFilter: String) {
        viewModelScope.launch {
            filterChannel.send(newFilter)
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
}