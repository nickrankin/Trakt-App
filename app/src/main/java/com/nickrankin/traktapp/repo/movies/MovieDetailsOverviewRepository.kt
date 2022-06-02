package com.nickrankin.traktapp.repo.movies

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.MovieCastPeopleDao
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewRep"
class MovieDetailsOverviewRepository @Inject constructor(
    private val personCreditsHelper: PersonCreditsHelper,
    private val creditsDatabase: CreditsDatabase
) {
    private val castPersonDao = creditsDatabase.personDao()
    private val movieCastPeopleDao = creditsDatabase.movieCastPeopleDao()

    fun getMovieCredits(movieTraktId: Int, movieTmdbId: Int?, movieTitle: String?, movieYear: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            movieCastPeopleDao.getMovieCast(movieTraktId)
        },
        fetch = {
            personCreditsHelper.getMovieCredits(movieTraktId, movieTmdbId)
        },
        shouldFetch = { credits ->
            shouldRefresh || credits.isEmpty()
        },
        saveFetchResult = { credits ->
            Log.d(TAG, "getMovieCredits: Refreshing Credits")

            Log.d(TAG, "getMovieCredits: Got ${credits.size} credits")

            creditsDatabase.withTransaction {
                credits.map { movieCastPerson ->

                    castPersonDao.insert(movieCastPerson.person)

                    movieCastPeopleDao.insert(
                        movieCastPerson.movieCastPersonData
                    )
                }
            }
        }
    )
}