package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.repo.shows.ShowsProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShowsProgressViewModel"
@HiltViewModel
class ShowsProgressViewModel @Inject constructor(private val repsitory: ShowsProgressRepository): BaseViewModel() {

    val showSeasonProgress = refreshEvent.flatMapLatest { shouldRefresh ->
        repsitory.getShowProgress(shouldRefresh)
    }
}