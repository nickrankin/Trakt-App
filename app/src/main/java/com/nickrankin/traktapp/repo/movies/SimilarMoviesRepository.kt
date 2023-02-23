package com.nickrankin.traktapp.repo.movies

import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject

class SimilarMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi) {
    suspend fun getRecommendedMovies(movieTmdbId: Int, movieLanguage: String?): Resource<MovieResultsPage> {
        return try {
            Resource.Success(tmdbApi.tmMovieService().recommendations(movieTmdbId, 1, getTmdbLanguage(movieLanguage)))
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun getTraktIdFromTmdbId(tmdbId: Int): Int? {
        try {
            val traktIdResponse = traktApi.tmSearch().idLookup(IdType.TMDB, tmdbId.toString(), Type.MOVIE, null, 1, 1)

            if(traktIdResponse.isNotEmpty()) {
                return traktIdResponse.first().movie?.ids?.trakt
            }

        } catch(e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}