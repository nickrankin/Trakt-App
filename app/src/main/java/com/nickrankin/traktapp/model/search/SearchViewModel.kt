package com.nickrankin.traktapp.model.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.repo.search.SearchPagingSource
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "ShowSearchViewModel"

@HiltViewModel
open class SearchViewModel @Inject constructor(open val traktApi: TraktApi) : ViewModel() {

    fun doSearch(query: String, type: Type) = run {
        Pager(
            PagingConfig(15)
        ) {
            SearchPagingSource(traktApi, query, type)
        }.flow
            .cachedIn(viewModelScope)
    }
}