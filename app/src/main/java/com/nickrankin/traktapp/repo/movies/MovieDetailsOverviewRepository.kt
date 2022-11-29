package com.nickrankin.traktapp.repo.movies

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewRep"
class MovieDetailsOverviewRepository @Inject constructor(
    private val personCreditsHelper: PersonCreditsHelper,
    private val moviesDatabase: MoviesDatabase,
    private val creditsDatabase: CreditsDatabase
) {
    private val tmMovieDao = moviesDatabase.tmMovieDao()
    private val movieCastPeopleDao = creditsDatabase.movieCastPeopleDao()
    private val movieCrewDao = creditsDatabase.crewPersonDao()

    fun getCrew(traktId: Int, tmdbId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            movieCrewDao.getCrewPersonCredits(traktId)
        },
        fetch = {
            personCreditsHelper.getMovieCast(traktId, tmdbId)
        },
        shouldFetch = { movieCastPeople ->
            movieCastPeople.isEmpty() || shouldRefresh

        },
        saveFetchResult = { movieCastPeople ->

            Log.d(TAG, "getCredits: Refreshing Credits")

            Log.d(TAG, "getCredits: Got ${movieCastPeople.size} credits")

            creditsDatabase.withTransaction {
                movieCastPeople.map { movieCastPerson ->
                    movieCastPeopleDao.insert(movieCastPerson)
                }
            }
        }
    )

    fun getCast(traktId: Int, tmdbId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
           movieCastPeopleDao.getMovieCast(traktId)
        },
        fetch = {
            personCreditsHelper.getMovieCast(traktId, tmdbId)
        },
        shouldFetch = { movieCastPeople ->
            movieCastPeople.isEmpty() || shouldRefresh

        },
        saveFetchResult = { movieCastPeople ->

            Log.d(TAG, "getCredits: Refreshing Credits")

            Log.d(TAG, "getCredits: Got ${movieCastPeople.size} credits")

            creditsDatabase.withTransaction {
                movieCastPeople.map { movieCastPerson ->
                    movieCastPeopleDao.insert(movieCastPerson)
                }
            }
        }
    )


    fun getMovie(movieDataModel: MovieDataModel?): Flow<TmMovie?> {
        return tmMovieDao.getMovieById(movieDataModel?.traktId ?: 0)
    }

}