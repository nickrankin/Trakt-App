package com.nickrankin.traktapp.repo.movies.watched

import android.content.SharedPreferences
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.uwetrottmann.trakt5.entities.*
import javax.inject.Inject

private const val TAG = "WatchedEpisodesReposito"
class WatchedMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val moviesDatabase: MoviesDatabase, private val movieStatsRepository: MovieStatsRepository) {

    private val watchedMoviesDao = moviesDatabase.watchedMoviesDao()

    @OptIn(ExperimentalPagingApi::class)
    fun watchedMovies(shouldRefresh: Boolean) = Pager(
        config = PagingConfig(8),
        remoteMediator = WatchedMoviesRemoteMediator(traktApi, shouldRefresh, moviesDatabase, sharedPreferences)
    ) {
        watchedMoviesDao.getWatchedMovies()
    }.flow

    suspend fun deleteFromWatchedHistory(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            moviesDatabase.withTransaction {
                watchedMoviesDao.deleteMovieById(syncItems.ids?.first()!!)

                // Need to invalidate the PagingData due to db manipulations
                watchedMoviesDao.getWatchedMovies().invalidate()

                sharedPreferences.edit()
                    .putBoolean(WatchedMoviesRemoteMediator.WATCHED_MOVIES_FORCE_REFRESH_KEY, true)
                    .apply()
            }

            // Refresh watched stats
            movieStatsRepository.refreshWatchedMovies()

            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    companion object {
        fun convertHistoryEntries(historyEntries: List<HistoryEntry>): List<WatchedMovie> {
            val watchedMovies: MutableList<WatchedMovie> = mutableListOf()

            historyEntries.map { entry ->
                watchedMovies.add(
                    WatchedMovie(
                        entry.id,
                        entry.movie?.ids?.trakt ?: 0,
                        entry.movie?.ids?.tmdb ?: 0,
                        entry.movie?.language,
                        entry.watched_at,
                        entry.movie?.overview,
                        entry.movie?.runtime,
                        entry.movie?.title)
                )
            }
            return watchedMovies
        }
    }

}