package com.nickrankin.traktapp.repo.movies

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import com.uwetrottmann.trakt5.entities.MovieCheckin
import com.uwetrottmann.trakt5.entities.MovieCheckinResponse
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncMovie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "MovieDetailsRepository"
class MovieDetailsRepository @Inject constructor(private val movieDataHelper: MovieDataHelper,
                                                 private val traktApi: TraktApi,
                                                 private val moviesDatabase: MoviesDatabase,
                                                 private val personCreditsHelper: PersonCreditsHelper,
                                                 private val creditsDatabase: CreditsDatabase) {
    private val castPersonDao = creditsDatabase.personDao()
    private val movieCastPeopleDao = creditsDatabase.movieCastPeopleDao()
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

    suspend fun getCredits(movieDataModel: MovieDataModel?, shouldRefresh: Boolean) {

        if(movieDataModel == null) {
            Log.d(TAG, "refreshCredits: MovieDataModel cannot be null")
            return
        }

        val credits = movieCastPeopleDao.getMovieCast(movieDataModel.traktId)

        if(shouldRefresh || credits.first().isEmpty()) {
            val creditsResponse = personCreditsHelper.getMovieCredits(movieDataModel?.traktId ?: 0, movieDataModel?.tmdbId ?: 0)

            Log.d(TAG, "getCredits: Refreshing Credits")

            Log.d(TAG, "getCredits: Got ${creditsResponse.size} credits")

            creditsDatabase.withTransaction {
                creditsResponse.map { movieCastPerson ->
                    castPersonDao.insert(movieCastPerson.person)

                    movieCastPeopleDao.insert(
                        movieCastPerson.movieCastPersonData
                    )
                }
            }
        }

    }

    companion object {
        const val MOVIE_TRAKT_ID_KEY = "movie_trakt_id_key"
        const val MOVIE_TITLE_KEY = "movie_title_key"
        const val USER_RATINGS_KEY = "movie_trakt_user_ratings_key"
    }
}

