package com.nickrankin.traktapp.model.person

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.dao.credits.model.CreditPerson
import com.nickrankin.traktapp.repo.person.PersonRepository
import com.nickrankin.traktapp.ui.person.PersonOverviewFragment
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

    private val personTraktId: Int = savedStateHandle.get<Int>(PersonOverviewFragment.PERSON_ID_KEY) ?: 0
    private val _creditsList: MutableLiveData<List<CreditPerson>> = MutableLiveData()
    val creditsList: LiveData<List<CreditPerson>> = _creditsList

    fun submitCreditsData(credits: List<CreditPerson>) {
        _creditsList.value = credits
    }
}