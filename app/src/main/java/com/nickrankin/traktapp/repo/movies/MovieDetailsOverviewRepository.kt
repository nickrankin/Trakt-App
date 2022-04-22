package com.nickrankin.traktapp.repo.movies

import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.MovieCastPeopleDao
import com.nickrankin.traktapp.helper.MovieCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

class MovieDetailsOverviewRepository @Inject constructor(
    private val movieCreditsHelper: MovieCreditsHelper,
    private val creditsDatabase: CreditsDatabase
) {
    private val castPersonDao = creditsDatabase.castPersonDao()
    private val movieCastPeopleDao = creditsDatabase.movieCastPeopleDao()

    fun getMovieCredits(traktId: Int, tmdbId: Int?, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            movieCastPeopleDao.getMovieCast(traktId)
        },
        fetch = {
            movieCreditsHelper.getMovieCredits(traktId, tmdbId)
        },
        shouldFetch = { credits ->
            shouldRefresh || credits.isEmpty()
        },
        saveFetchResult = { credits ->
            creditsDatabase.withTransaction {
                credits.map { castPair ->
                    val castPersonData = castPair.first
                    val castPerson = castPair.second

                    castPersonDao.insert(castPerson)

                    movieCastPeopleDao.insert(
                        castPersonData
                    )

                }

            }
        }
    )
}