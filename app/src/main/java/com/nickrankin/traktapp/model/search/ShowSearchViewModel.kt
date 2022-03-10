package com.nickrankin.traktapp.model.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.repo.search.ShowSearchPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "ShowSearchViewModel"
@HiltViewModel
open class ShowSearchViewModel @Inject constructor(open val traktApi: TraktApi): ViewModel() {

    fun doSearch(query: String) = run {
        Pager(
            PagingConfig(15)
        ) {
            ShowSearchPagingSource(traktApi, query)
        }.flow
            .cachedIn(viewModelScope)
    }
}