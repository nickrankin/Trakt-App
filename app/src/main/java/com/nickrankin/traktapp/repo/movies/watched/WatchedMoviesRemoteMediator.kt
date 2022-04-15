package com.nickrankin.traktapp.repo.movies.watched

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.dao.movies.WatchedMoviePageKey
import com.nickrankin.traktapp.dao.movies.WatchedMoviesMediatorDatabase
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.WatchedShowsMediatorDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodePageKey
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.HttpException
import java.io.IOException

private const val START_INDEX = 1
private const val REFRESH_INTERVAL = 24L
private const val TAG = "WatchedEpisodesRemoteMe"
@OptIn(ExperimentalPagingApi::class)
class WatchedMoviesRemoteMediator(
    private val traktApi: TraktApi,
    private val shouldRefresh: Boolean,
    private val watchedMoviesMediatorDatabase: WatchedMoviesMediatorDatabase,
    private val sharedPreferences: SharedPreferences
) : RemoteMediator<Int, WatchedMovie>() {
    val watchedMoviesDao = watchedMoviesMediatorDatabase.watchedMoviesDao()
    private val remoteKeyDao = watchedMoviesMediatorDatabase.watchedMoviePageKeyDao()
    override suspend fun initialize(): InitializeAction {
        return when {
            shouldRefresh -> {
                Log.d(TAG, "initialize: Refresh forcing")
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
            else -> {
                Log.d(TAG, "initialize: Initialize first refresh")

                return if(shouldRefreshContents(sharedPreferences.getString(
                        WATCHED_MOVIES_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL) || sharedPreferences.getBoolean(
                        WATCHED_MOVIES_FORCE_REFRESH_KEY, false)) {

                    Log.d(TAG, "initialize: Performing scheduled refresh")
                    InitializeAction.LAUNCH_INITIAL_REFRESH
                } else {
                    Log.d(TAG, "initialize: Skipping refresh, load from cache")
                    InitializeAction.SKIP_INITIAL_REFRESH
                }
            }
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WatchedMovie>
    ): MediatorResult {
        try {
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: START_INDEX
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    // If remoteKeys is null, that means the refresh result is not in the database yet.
                    val prevKey = remoteKeys?.prevPage
                    if (prevKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    }
                    prevKey
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    // If remoteKeys is null, that means the refresh result is not in the database yet.
                    // We can return Success with endOfPaginationReached = false because Paging
                    // will call this method again if RemoteKeys becomes non-null.
                    // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                    // the end of pagination for append.
                    val nextKey = remoteKeys?.nextPage
                    if (nextKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    }
                    nextKey
                }
            }
            
            if(loadType == LoadType.REFRESH) {
                Log.d(TAG, "load: Refreshing Watched Shows. Deleting already cached")
                watchedMoviesMediatorDatabase.withTransaction {
                    remoteKeyDao.deleteAll()
                    watchedMoviesDao.deleteAllMovies()
                }

                // Save last refresh date
                sharedPreferences.edit()
                    .putString(WATCHED_MOVIES_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                    .putBoolean(WATCHED_MOVIES_FORCE_REFRESH_KEY, false)
                    .apply()
            }

            val data = convertHistoryEntries(
                traktApi.tmUsers().history(
                    UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                    HistoryType.MOVIES,
                    page,
                    15,
                    Extended.FULL,
                    OffsetDateTime.now().minusYears(99),
                    OffsetDateTime.now()
                )
            )

            val endOfPaginationReached = data.isEmpty()


            val prevKey = if (page == START_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1
            val keys = data.map {
                WatchedMoviePageKey(it.id, prevKey, nextKey)
            }

            watchedMoviesMediatorDatabase.withTransaction {
                remoteKeyDao.insert(keys)
                watchedMoviesDao.insert(data)
            }



            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            Log.e(TAG, "load: IO Exception ${e.message}", )
            e.printStackTrace()
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "load: HTTPException (code ${e.code()}). ${e.message()}", )
            e.printStackTrace()
            return MediatorResult.Error(e)
        }


    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, WatchedMovie>
    ): WatchedMoviePageKey? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                remoteKeyDao.remoteKeyByPage(id)
            }
        }
    }


    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, WatchedMovie>): WatchedMoviePageKey? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { movie ->
                // Get the remote keys of the first items retrieved
                remoteKeyDao.remoteKeyByPage(movie.id)
            }
    }


    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, WatchedMovie>): WatchedMoviePageKey? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { movie ->
                // Get the remote keys of the last item retrieved
                remoteKeyDao.remoteKeyByPage(movie.id)
            }
    }


    private fun convertHistoryEntries(historyEntries: List<HistoryEntry>): List<WatchedMovie> {
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
    
    companion object {
        const val WATCHED_MOVIES_LAST_REFRESHED_KEY = "watched_movies_last_refreshed"
        const val WATCHED_MOVIES_FORCE_REFRESH_KEY = "force_refresh_watched_movies"
    }
}