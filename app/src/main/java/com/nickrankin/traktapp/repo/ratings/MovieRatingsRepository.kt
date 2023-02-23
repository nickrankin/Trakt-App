package com.nickrankin.traktapp.repo.ratings

import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class MovieRatingsRepository @Inject constructor(private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase) {
    private val movieRatingDao = moviesDatabase.ratedMoviesStatsDao()
    val movieRatings = movieRatingDao.getRatingsStats()

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

            val ratingStats = RatingsMoviesStats(traktId, newRating, OffsetDateTime.now())

            moviesDatabase.withTransaction {
                movieRatingDao.insert(ratingStats)
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
                movieRatingDao.deleteRatingsStatsById(traktId)
            }
            Resource.Success(response)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}