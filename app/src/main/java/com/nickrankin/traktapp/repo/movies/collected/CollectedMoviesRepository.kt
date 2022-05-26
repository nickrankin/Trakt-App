package com.nickrankin.traktapp.repo.movies.collected

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.SortBy
import kotlinx.coroutines.flow.first
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import java.lang.RuntimeException
import javax.inject.Inject

private const val TAG = "CollectedMoviesReposito"
class CollectedMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase, private val sharedPreferences: SharedPreferences, private val statsRepository: StatsRepository) {
    private val tmMovieDao = moviesDatabase.tmMovieDao()
    private val collectedMoviesDao = moviesDatabase.collectedMovieDao()

    private val collectedMoviesStatsDao = moviesDatabase.collectedMoviesStatsDao()

    suspend fun getCollectedMovies(shouldRefresh: Boolean) = networkBoundResource(
        query = { collectedMoviesDao.getCollectedMovies() },
        fetch = { traktApi.tmUsers().collectionMovies(
            UserSlug(sharedPreferences.getString(
                AuthActivity.USER_SLUG_KEY, "null")), Extended.FULL) },
        shouldFetch = { movies ->
            movies.isEmpty() || shouldRefresh
        },
        saveFetchResult = { baseMovies ->
            Log.d(TAG, "getCollectedMovies: Inserting ${baseMovies.size} movies")
            moviesDatabase.withTransaction {
                collectedMoviesDao.insert(convertBaseMovies(baseMovies))
            }

        }
    )

    private fun convertBaseMovies(baseMovies: List<BaseMovie>): List<CollectedMovie> {
        val collectedMovies: MutableList<CollectedMovie> = mutableListOf()

        baseMovies.forEach { baseMovie ->
            collectedMovies.add(
                CollectedMovie(
                    baseMovie.movie?.ids?.trakt ?: 0,
                    baseMovie.movie?.ids?.tmdb ?: 0,
                    baseMovie.movie?.language,
                    baseMovie.collected_at,
                    baseMovie.last_updated_at,
                    baseMovie.listed_at,
                    baseMovie.plays,
                    baseMovie.movie?.overview,
                    baseMovie.movie?.released,
                    baseMovie.movie?.runtime,
                    baseMovie.movie!!.title)
            )
        }

        Log.d(TAG, "convertBaseMovies: Returning ${collectedMovies.size} movies")

        return collectedMovies
    }

    /**
     *     val trakt_id: Int,
    val tmdb_id: Int,
    val language: String?,
    val collected_at: OffsetDateTime?,
    val last_updated_at: OffsetDateTime?,
    val listed_at: OffsetDateTime?,
    val plays: Int,
    val movie_overview: String?,
    val release_date: LocalDate?,
    val runtime: Int?,
    val title: String)
     *
     *
     * */

    suspend fun addCollectedMovie(traktId: Int): Resource<SyncResponse> {
        Log.d(TAG, "addCollectedMovie: Adding movie to collection")
        val movie = tmMovieDao.getMovieById(traktId).first()
            ?: return Resource.Error(RuntimeException("Movie Cannot be NULL"), null)

        Log.d(TAG, "addCollectedMovie: Got TmMovie object $movie")

        val syncItems = SyncItems().apply {
            movies = listOf(
                SyncMovie()
                    .id(MovieIds.trakt(movie.trakt_id))
            )
        }
        return try {
            val response = traktApi.tmSync().addItemsToCollection(syncItems)
            val releaseDate = movie.release_date

            var releaseDateLocalDate: LocalDate? = null

            if(releaseDate != null) {
                releaseDateLocalDate = Instant.ofEpochMilli(releaseDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            moviesDatabase.withTransaction {
                collectedMoviesDao.insert(
                    listOf(
                        CollectedMovie(
                            movie.trakt_id,
                            movie.tmdb_id,
                            movie.original_language,
                            OffsetDateTime.now(),
                            null,
                            null,
                            0,
                            movie.overview,
                            releaseDateLocalDate,
                            movie.runtime,
                            movie.title
                        )
                    )
                )

                collectedMoviesStatsDao.insert(
                    CollectedMoviesStats(
                        movie.trakt_id,
                        movie.tmdb_id,
                        OffsetDateTime.now(),
                        movie.title,
                        null
                    )
                )
            }

            Resource.Success(response)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeCollectedMovie(traktId: Int): Resource<SyncResponse> {
        val syncItems = SyncItems().apply {
            movies = listOf(
                SyncMovie()
                    .id(MovieIds.trakt(traktId))
            )
        }

        return try {
            val response = traktApi.tmSync().deleteItemsFromCollection(syncItems)

            moviesDatabase.withTransaction {
                collectedMoviesDao.deleteMovieById(traktId)
                collectedMoviesStatsDao.deleteCollectedMovieStatById(traktId)
            }

            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }
}