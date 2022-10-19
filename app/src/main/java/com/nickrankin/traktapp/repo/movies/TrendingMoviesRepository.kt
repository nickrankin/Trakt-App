package com.nickrankin.traktapp.repo.movies

import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.TrendingMovie
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "TrendingMoviesRepositor"
class TrendingMoviesRepository @Inject constructor(private val traktApi: TraktApi) {

    private val trendingMovies: MutableList<TrendingMovie> = mutableListOf()
    private val _trendingMoviesStateFlow = MutableStateFlow<Resource<List<TrendingMovie>>>(Resource.Loading())
    private val trandingMoviesStateFlow: StateFlow<Resource<List<TrendingMovie>>> = _trendingMoviesStateFlow.asStateFlow()

    suspend fun getTrendingMovies(shouldRefresh: Boolean): Flow<Resource<List<TrendingMovie>>> {

        _trendingMoviesStateFlow.update { Resource.Loading() }


        if(!shouldRefresh && trendingMovies.isNotEmpty()) {
            _trendingMoviesStateFlow.update { Resource.Success(trendingMovies) }
        } else {
            try {
                Log.d(TAG, "getTrendingMovies: Getting movies from Trakt")
                val response = traktApi.tmMovies().trending(0, 25, Extended.FULL)

                trendingMovies.clear()
                trendingMovies.addAll(response)

                _trendingMoviesStateFlow.update { Resource.Success(trendingMovies) }
            } catch(t: Throwable) {
                Log.e(TAG, "getTrendingMovies: Error ${t.message}", )
                _trendingMoviesStateFlow.update { Resource.Error(t, null) }
            }
        }

        return trandingMoviesStateFlow
    }
}