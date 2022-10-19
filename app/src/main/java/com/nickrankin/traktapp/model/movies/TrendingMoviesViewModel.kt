package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.model.ViewSwitcherViewModel
import com.nickrankin.traktapp.repo.movies.RecommendedMoviesRepository
import com.nickrankin.traktapp.repo.movies.TrendingMoviesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendingMoviesViewModel @Inject constructor(private val repository: TrendingMoviesRepository): ViewSwitcherViewModel() {

    val trendingMovies = refreshEvent.flatMapLatest { shouldRefresh ->
        repository.getTrendingMovies(shouldRefresh)
    }

}