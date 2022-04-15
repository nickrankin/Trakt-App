package com.nickrankin.traktapp.repo.movies

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.helper.MovieDataHelper
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

private const val TAG = "MovieDetailsRepository"
class MovieDetailsRepository @Inject constructor(private val movieDataHelper: MovieDataHelper, private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase) {
    private val tmMovieDao = moviesDatabase.tmMovieDao()

    fun getMovieSummary(traktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
                tmMovieDao.getMovieById(traktId)
        },
        fetch = {
                movieDataHelper.getMovieSummary(traktId)
        },
        shouldFetch = { tmMovie ->
                      shouldRefresh || tmMovie == null
        },
        saveFetchResult = {tmMovie ->
            if(tmMovie != null) {
                moviesDatabase.withTransaction {
                    tmMovieDao.insert(tmMovie)
                }
            }
        }
    )

    suspend fun getTraktUserRatings(traktId: Int): Resource<Double> {
        Log.d(TAG, "getTraktUserRatings: Getting Trakt User Ratings")
        return try {
            val response = traktApi.tmMovies().ratings(traktId.toString())

            Resource.Success(response.rating ?: 0.0)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }

    }

    companion object {
        const val MOVIE_TRAKT_ID_KEY = "movie_trakt_id_key"
        const val MOVIE_TITLE_KEY = "movie_title_key"
        const val USER_RATINGS_KEY = "movie_trakt_user_ratings_key"
    }
}