package com.nickrankin.traktapp.repo.movies

import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.TrendingMovie
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "TrendingMoviesRepositor"
class TrendingMoviesRepository @Inject constructor(private val traktApi: TraktApi) {

    private var trendingMovies: List<TrendingMovie> = listOf()

    suspend fun getTrendingMovies(shouldRefresh: Boolean) = flow {

        emit(Resource.Loading(null))

        if(!shouldRefresh && trendingMovies.isNotEmpty()) {
            Log.d(TAG, "getTrendingMovies: Returning movies from memory")
            emit(Resource.Success(trendingMovies))
        } else {
            try {
                Log.d(TAG, "getTrendingMovies: Getting movies from Trakt")
                val response = traktApi.tmMovies().trending(0, 25, Extended.FULL)

                trendingMovies = response

                emit(Resource.Success(response))
            } catch(t: Throwable) {
                Log.e(TAG, "getTrendingMovies: Error ${t.message}", )
                emit(Resource.Error(t, null))
            }
        }
    }
}