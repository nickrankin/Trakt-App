package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.BaseMovie
import com.uwetrottmann.trakt5.entities.RatedMovie
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

private const val TAG = "MovieStatsRepository"
class MovieStatsRepository @Inject constructor(private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase, private val sharedPreferences: SharedPreferences) {
    // Movies
    private val collectedMoviesStatsDao = moviesDatabase.collectedMoviesStatsDao()
    private val watchedMoviesStatsDao = moviesDatabase.watchedMoviesStatsDao()
    private val ratingsMoviesStatsDao = moviesDatabase.ratedMoviesStatsDao()

    // Flows Movies
    val collectedMoviesStats = collectedMoviesStatsDao.getCollectedStats()
    val watchedMoviesStats = watchedMoviesStatsDao.getWatchedStats()
    val ratedMoviesStats = ratingsMoviesStatsDao.getRatingsStats()

    suspend fun getCollectedMovieStatsById(traktId: Int): Flow<CollectedMoviesStats?> {
        return collectedMoviesStatsDao.getCollectedMovieStatsById(traktId)
    }

    suspend fun refreshAllMovieStats() {
        Log.d(TAG, "refreshAllMovieStats: Refreshing All Movie Stats")
        refreshWatchedMovies()
        refreshCollectedMovieStats()
        refreshMovieRatingsStats()

    }

    suspend fun refreshWatchedMovies() {
        Log.d(TAG, "refreshWatchedMovies: Refresh watched movies 1")
        Log.d(TAG, "refreshWatchedMovies: Refresh watched movies 2")

            Log.d(TAG, "refreshWatchedMovies: Refresh watched movies 3")
            val watchedMoviesStats = traktApi.tmUsers().watchedMovies(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), null
            )
            insertWatchedMoviesStats(watchedMoviesStats)


    }

    suspend fun refreshCollectedMovieStats() {

            val collectedMoviesStats = traktApi.tmUsers().collectionMovies(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), null
            )

            insertCollectedMoviesStats(collectedMoviesStats)

    }

    private suspend fun refreshMovieRatingsStats() {

            val ratedMoviesStats = traktApi.tmUsers().ratingsMovies(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), RatingsFilter.ALL, null
            )

            insertRatedMoviesStats(ratedMoviesStats)
    }

    private suspend fun insertCollectedMoviesStats(movies: List<BaseMovie>) {
        val collectedMovieStatsList: MutableList<CollectedMoviesStats> = mutableListOf()

        movies.map { baseMovie ->
            collectedMovieStatsList.add(
                CollectedMoviesStats(
                    baseMovie.movie?.ids?.trakt ?: 0,
                    baseMovie.movie?.ids?.tmdb,
                    baseMovie.collected_at,
                    baseMovie.movie?.title ?: "",
                    baseMovie.listed_at
                )
            )
        }

        moviesDatabase.withTransaction {
            collectedMoviesStatsDao.deleteCollectedStats()
            collectedMoviesStatsDao.insert(collectedMovieStatsList)
        }
    }

    private suspend fun insertWatchedMoviesStats(movies: List<BaseMovie>) {
        val watchedMoviesStatsList: MutableList<WatchedMoviesStats> = mutableListOf()

        movies.map { baseMovie ->
            watchedMoviesStatsList.add(
                WatchedMoviesStats(
                    baseMovie.movie?.ids?.trakt ?: 0,
                    baseMovie.movie?.ids?.tmdb,
                    baseMovie.last_watched_at,
                    baseMovie.movie?.title ?: "",
                    baseMovie.listed_at,
                    baseMovie.plays
                )
            )
        }

        moviesDatabase.withTransaction {
            watchedMoviesStatsDao.deleteWatchedStats()
            watchedMoviesStatsDao.insert(watchedMoviesStatsList)
        }
    }

    private suspend fun insertRatedMoviesStats(ratedMovies: List<RatedMovie>) {
        val ratedMoviesStatsList: MutableList<RatingsMoviesStats> = mutableListOf()

        ratedMovies.forEach { ratedMovie ->
            ratedMoviesStatsList.add(
                RatingsMoviesStats(
                    ratedMovie.movie?.ids?.trakt ?: 0,
                    ratedMovie.movie?.ids?.tmdb ?: 0,
                    ratedMovie.rating?.value ?: 0,
                    ratedMovie.movie?.title ?: "",
                    ratedMovie.rated_at
                )
            )
        }

        moviesDatabase.withTransaction {
            ratingsMoviesStatsDao.deleteRatingsStats()
            ratingsMoviesStatsDao.insert(ratedMoviesStatsList)
        }
    }
}