package com.nickrankin.traktapp.repo.movies.watched

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.movies.WatchedMoviesMediatorDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.WatchedShowsMediatorDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "WatchedEpisodesReposito"
class WatchedMoviesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val moviesDatabase: WatchedMoviesMediatorDatabase) {

    private val watchedMoviesDao = moviesDatabase.watchedMoviesDao()

    private var latestWatchedMovies: List<HistoryEntry> = listOf()

    @OptIn(ExperimentalPagingApi::class)
    fun watchedMovies(shouldRefresh: Boolean) = Pager(
        config = PagingConfig(8),
        remoteMediator = WatchedMoviesRemoteMediator(traktApi, shouldRefresh, moviesDatabase, sharedPreferences)
    ) {
        watchedMoviesDao.getWatchedMovies()
    }.flow

    fun getLatestWatchedMovies(shouldRefresh: Boolean) = flow {
        emit(Resource.Loading())

        if(!shouldRefresh && latestWatchedMovies.isNotEmpty()) {
            Log.d(TAG, "getLatestWatchedMovies: Getting latest movies from cache")
            emit(Resource.Success(latestWatchedMovies))
        } else {
            Log.d(TAG, "getLatestWatchedMovies: Refreshing latest movies (Should Refresh Value: $shouldRefresh)")
            try {
                val response = traktApi.tmUsers().history(
                    UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                    HistoryType.MOVIES,
                    1,
                    5,
                    Extended.FULL,
                    OffsetDateTime.now().minusYears(99),
                    OffsetDateTime.now()
                )

                latestWatchedMovies = response

                emit(Resource.Success(latestWatchedMovies))

            } catch(t: Throwable) {
                emit(Resource.Error(t, null))
            }
        }
    }

    suspend fun deleteFromWatchedHistory(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            moviesDatabase.withTransaction {
                watchedMoviesDao.deleteMovieById(syncItems.ids?.first() ?: 0L)
            }
            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

}