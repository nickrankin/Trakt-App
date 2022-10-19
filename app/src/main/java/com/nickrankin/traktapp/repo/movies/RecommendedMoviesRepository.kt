package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "RecommendedMoviesReposi"
class RecommendedMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences) {

    private val recommendedMovies: MutableList<Movie> = mutableListOf()
    private val _recommendedMoviesStateFlow: MutableStateFlow<Resource<List<Movie>>> = MutableStateFlow(Resource.Loading())
    private val recommendedMoviesStateFlow: StateFlow<Resource<List<Movie>>> = _recommendedMoviesStateFlow.asStateFlow()

    suspend fun getRecommendedMovies(shouldRefresh: Boolean): Flow<Resource<List<Movie>>> {

        _recommendedMoviesStateFlow.update { Resource.Loading() }

        if(!shouldRefresh && recommendedMovies.isNotEmpty()) {
            Log.d(TAG, "getRecommendedMovies: Returning movies from memory")

            _recommendedMoviesStateFlow.update { Resource.Success(recommendedMovies) }

        } else {
            try {
                Log.d(TAG, "getRecommendedMovies: Getting movies from Trakt")
                val response = traktApi.tmRecommendations().movies(0, 25, Extended.FULL)

                recommendedMovies.clear()
                recommendedMovies.addAll(response)

                _recommendedMoviesStateFlow.update { Resource.Success(recommendedMovies) }

            } catch(t: Throwable) {
                Log.e(TAG, "getRecommendedMovies: Error ${t.message}", )
                _recommendedMoviesStateFlow.update { Resource.Error(t, null) }
            }
        }

        return recommendedMoviesStateFlow


    }

    suspend fun deleteRecommendedMovie(movie: Movie): Resource<Boolean> {
        return try {
            traktApi.tmRecommendations().dismissMovie(movie.ids?.trakt.toString())

            recommendedMovies.remove(movie)

            _recommendedMoviesStateFlow.update { Resource.Success(recommendedMovies) }

            return Resource.Success(true)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}