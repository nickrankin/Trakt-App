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
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.WatchedMoviePageKey
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
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
    private val moviesDatabase: MoviesDatabase,
    private val sharedPreferences: SharedPreferences
) : RemoteMediator<Int, WatchedMovieAndStats>() {
    val watchedMoviesDao = moviesDatabase.watchedMoviesDao()
    private val remoteKeyDao = moviesDatabase.watchedMoviePageKeyDao()
    private val watchedMovies: MutableList<WatchedMovie> = mutableListOf()

    override suspend fun initialize(): InitializeAction {
        return when {
            shouldRefresh -> {
                Log.d(TAG, "initialize: Refresh invoked by user")
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
            else -> {
                return if(shouldRefreshContents(sharedPreferences.getString(
                        WATCHED_MOVIES_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL)) {
                    Log.d(TAG, "initialize: Performing scheduled refresh")
                    InitializeAction.LAUNCH_INITIAL_REFRESH
                } else if(sharedPreferences.getBoolean(
                        WATCHED_MOVIES_FORCE_REFRESH_KEY, false)) {
                    Log.d(TAG, "initialize: Performing Forced Refresh")
                    InitializeAction.LAUNCH_INITIAL_REFRESH

                }
                else {
                    Log.d(TAG, "initialize: Skipping refresh, load from cache")
                    InitializeAction.SKIP_INITIAL_REFRESH
                }
            }
        }
    }



    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WatchedMovieAndStats>
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
                moviesDatabase.withTransaction {
                    watchedMoviesDao.deleteAllMovies()
                }
                // Save last refresh date and reset the FORCED refresh flag
                sharedPreferences.edit()
                    .putString(WATCHED_MOVIES_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                    .putBoolean(WATCHED_MOVIES_FORCE_REFRESH_KEY, false)
                    .apply()
            }

            var data = convertHistoryEntries(
                traktApi.tmUsers().history(
                    UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                    HistoryType.MOVIES,
                    page,
                    PAGE_LIMIT,
                    null,
                    null,
                    null
                )
            )

            val endOfPaginationReached = data.isEmpty()


            val prevKey = if (page == START_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1
            val keys = data.map {
                WatchedMoviePageKey(it.id, prevKey, nextKey)
            }

            moviesDatabase.withTransaction {
                Log.e(TAG, "load: Begin Insrt", )
                remoteKeyDao.insert(keys)
                watchedMoviesDao.insert(data)

                Log.e(TAG, "load: End Insrt", )

                data = emptyList()

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
        state: PagingState<Int, WatchedMovieAndStats>
    ): WatchedMoviePageKey? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.watchedMovie?.id?.let { id ->
                remoteKeyDao.remoteKeyByPage(id)
            }
        }
    }


    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, WatchedMovieAndStats>): WatchedMoviePageKey? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { movie ->
                // Get the remote keys of the first items retrieved
                remoteKeyDao.remoteKeyByPage(movie.watchedMovie.id)
            }
    }


    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, WatchedMovieAndStats>): WatchedMoviePageKey? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { movie ->
                // Get the remote keys of the last item retrieved
                remoteKeyDao.remoteKeyByPage(movie.watchedMovie.id)
            }
    }


    private fun convertHistoryEntries(historyEntries: List<HistoryEntry>): List<WatchedMovie> {
        watchedMovies.clear()
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
        const val PAGE_LIMIT = 25
        const val WATCHED_MOVIES_LAST_REFRESHED_KEY = "watched_movies_last_refreshed"
        const val WATCHED_MOVIES_FORCE_REFRESH_KEY = "force_refresh_watched_movies"
    }
}