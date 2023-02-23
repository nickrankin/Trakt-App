package com.nickrankin.traktapp.repo.shows.showdetails

import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import javax.inject.Inject

class SimilarShowsRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi) {
    suspend fun getRecommendedMovies(tmdbId: Int, language: String?): Resource<TvShowResultsPage> {
        return try {
            Resource.Success(tmdbApi.tmTvService().recommendations(tmdbId, 1, getTmdbLanguage(language)))
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun getTraktIdFromTmdbId(tmdbId: Int): Int? {
        try {
            val traktIdResponse = traktApi.tmSearch().idLookup(IdType.TMDB, tmdbId.toString(), Type.SHOW, null, 1, 1)

            if(traktIdResponse.isNotEmpty()) {
                return traktIdResponse.first().show?.ids?.trakt
            }

        } catch(e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}