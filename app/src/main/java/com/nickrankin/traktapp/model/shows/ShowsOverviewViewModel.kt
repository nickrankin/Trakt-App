package com.nickrankin.traktapp.model.auth.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.auth.shows.ShowsOverviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowsOverviewViewModel @Inject constructor(private val repository: ShowsOverviewRepository): ViewSwitcherViewModel() {


    private var showHiddenEntries = false

    @ExperimentalCoroutinesApi
    val myShows = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getMyShows(shouldRefresh)
    }.map { result ->
        repository.getHiddenStatus(showHiddenEntries, result.data ?: emptyList())
    }

    fun showHiddenEntries(showHidden: Boolean) {
        this.showHiddenEntries = showHidden
    }

    fun setShowHiddenState(showTmdbId: Int, isHidden: Boolean) = viewModelScope.launch { repository.setShowHiddenState(showTmdbId, isHidden) }
}