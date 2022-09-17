package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "MovieDetailsActionButto"
class MovieDetailsActionButtonRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val movieStatsRepository: MovieStatsRepository, private val listsDatabase: TraktListsDatabase, private val moviesDatabase: MoviesDatabase) {
    private val watchedMoviesDao = moviesDatabase.watchedMoviesDao()
    private val watchedMoviesStatsDao = moviesDatabase.watchedMoviesStatsDao()
    private val listEntryDao = listsDatabase.listEntryDao()
    val listsWithEntries = listEntryDao.getAllListEntries()

    suspend fun checkin(movieTraktId: Int, cancelActiveCheckins: Boolean): Resource<MovieCheckinResponse> {
        return try {
            if(cancelActiveCheckins) {
                cancelCheckins()
            }

            val movieCheckin = MovieCheckin.Builder(SyncMovie().id(MovieIds.trakt(movieTraktId)), AppConstants.APP_VERSION, AppConstants.APP_DATE)
                .build()
            val checkinResponse = traktApi.tmCheckin().checkin(
                movieCheckin
            )

            Resource.Success(checkinResponse)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun cancelCheckins(): Resource<Boolean> {
        return try {

            traktApi.tmCheckin().deleteActiveCheckin()

            Resource.Success(true)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun addToWatchedHistory(movie: TmMovie, watchedDate: OffsetDateTime): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                movies = listOf(
                    SyncMovie().id(MovieIds.trakt(movie.trakt_id))
                        .watchedAt(watchedDate)
                )
            }
            val response = traktApi.tmSync().addItemsToWatchedHistory(syncItems)

            moviesDatabase.withTransaction {

                // We need to get the watched movies again for this movie, so the PagingData can be updated
                watchedMoviesDao.insert(WatchedMoviesRepository.convertHistoryEntries(getMovieWatchedHistoryEntries(movie.trakt_id)))

                // Need to invalidate the PagingData due to db manipulations
                watchedMoviesDao.getWatchedMovies().invalidate()

                // Refresh the WatchedStats
                movieStatsRepository.refreshWatchedMovies()
            }

            Resource.Success(response)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun getMovieWatchedHistoryEntries(traktId: Int): List<HistoryEntry> {
        return try {
            val response = traktApi.tmUsers().history(
                UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                HistoryType.MOVIES,
                traktId,
                1,
                999,
                Extended.FULL,
                null,
                null
            )

            response
        } catch(e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}