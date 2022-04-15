package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "RecommendedMoviesReposi"
class RecommendedMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences) {

    private var recommendedMovies: List<Movie> = listOf()

    suspend fun getRecommendedMovies(shouldRefresh: Boolean) = flow {

        emit(Resource.Loading(null))

        if(!shouldRefresh && recommendedMovies.isNotEmpty()) {
            Log.d(TAG, "getRecommendedMovies: Returning movies from memory")
            emit(Resource.Success(recommendedMovies))
        } else {
            try {
                Log.d(TAG, "getRecommendedMovies: Getting movies from Trakt")
                val response = traktApi.tmRecommendations().movies(0, 25, Extended.FULL)

                recommendedMovies = response

                emit(Resource.Success(response))
            } catch(t: Throwable) {
                Log.e(TAG, "getRecommendedMovies: Error ${t.message}", )
                emit(Resource.Error(t, null))
            }
        }


    }

    suspend fun deleteRecommendedMovie(traktId: Int): Resource<Boolean> {
        return try {
            val response = traktApi.tmRecommendations().dismissMovie(traktId.toString())
            return Resource.Success(true)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}