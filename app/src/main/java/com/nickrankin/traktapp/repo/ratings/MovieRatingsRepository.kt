package com.nickrankin.traktapp.repo.ratings

import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.ratings.model.RatedMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.RatingsFilter
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

class MovieRatingsRepository @Inject constructor(private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase) {
    private val movieRatingDao = moviesDatabase.ratedMovieDao()

    fun getRatings(shouldRefresh: Boolean, traktId: Int) = networkBoundResource(
        query = {
                movieRatingDao.getRatingForMovie(traktId)
        },
        fetch = {
            traktApi.tmSync().ratingsMovies(RatingsFilter.ALL, Extended.NOSEASONS, null, null)
        },
        shouldFetch = { ratedMovie ->
                      shouldRefresh || ratedMovie == null
        },
        saveFetchResult = { ratedMovies ->
            moviesDatabase.withTransaction {
                movieRatingDao.insert(getRatedMovies(ratedMovies))
            }
        }
    )

    private fun getRatedMovies(ratedMovies: List<com.uwetrottmann.trakt5.entities.RatedMovie>): List<RatedMovie> {
        val movies: MutableList<RatedMovie> = mutableListOf()

        ratedMovies.map {
            movies.add(
                RatedMovie(
                    it.movie?.ids?.trakt ?: -1,
                    it.rating?.value ?: -1,
                    it.rated_at
                )
            )
        }

        return movies
    }


    suspend fun addRating(traktId: Int, newRating: Int): Resource<SyncResponse> {

        val syncItems = SyncItems().apply {
            movies = listOf(
                SyncMovie()
                    .id(MovieIds.trakt(traktId))
                    .rating(Rating.fromValue(newRating))
                    .ratedAt(OffsetDateTime.now())
            )
        }

        return try {
            val response = traktApi.tmSync().addRatings(syncItems)

            moviesDatabase.withTransaction {
                movieRatingDao.insert(listOf(RatedMovie(traktId, newRating, OffsetDateTime.now())))
            }

            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun deleteRating(traktId: Int): Resource<SyncResponse> {
        val syncItems = SyncItems().apply {
            movies = listOf(
                SyncMovie()
                    .id(MovieIds.trakt(traktId))
            )
        }

        return try {
            val response = traktApi.tmSync().deleteRatings(syncItems)

            moviesDatabase.withTransaction {
                movieRatingDao.deleteRatingByTraktId(traktId)
            }

            Resource.Success(response)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}