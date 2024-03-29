package com.nickrankin.traktapp.model.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.credits.model.CrewType
import com.nickrankin.traktapp.repo.person.PersonRepository
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PersonViewModel"
@HiltViewModel
class PersonOverviewViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: PersonRepository): ViewModel() {
    private val personTraktId = savedStateHandle.get<Int>(PersonOverviewFragment.PERSON_ID_KEY)

    private val personChangedChannel = Channel<Int>()
    private val personChanged = personChangedChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)
    
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
        personChanged.flatMapLatest { personTraktId ->
            repository.getPerson(personTraktId, shouldRefresh)
        }
    }

    val personCrew = refreshEvent.flatMapLatest { shouldRefresh ->
        personChanged.flatMapLatest { personTraktId ->
            repository.getCrewPersonCredits(personTraktId, shouldRefresh)
        }
    }

    val personCast = refreshEvent.flatMapLatest { shouldRefresh ->
        personChanged.flatMapLatest { personTraktId ->

            repository.getcastPersonCredits(personTraktId, shouldRefresh)
        }
    }

    fun switchPerson(newPersonTraktId: Int) {
        viewModelScope.launch {
            personChangedChannel.send(newPersonTraktId)
        }
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