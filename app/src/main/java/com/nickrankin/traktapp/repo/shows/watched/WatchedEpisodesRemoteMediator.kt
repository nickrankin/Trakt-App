package com.nickrankin.traktapp.repo.shows.watched

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.dao.show.ShowsDatabase
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
class WatchedEpisodesRemoteMediator(
    private val traktApi: TraktApi,
    private val shouldRefresh: Boolean,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences
) : RemoteMediator<Int, WatchedEpisode>() {
    val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()
    private val remoteKeyDao = showsDatabase.watchedEpisodePageKeyDao()
    override suspend fun initialize(): InitializeAction {
        return when {
            shouldRefresh -> {
                Log.d(TAG, "initialize: Refresh forcing")
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
            else -> {
                Log.d(TAG, "initialize: Initialize first refresh")

                return if(shouldRefreshContents(sharedPreferences.getString(
                        WATCHED_EPISODES_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL)) {

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
        state: PagingState<Int, WatchedEpisode>
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
                showsDatabase.withTransaction {
                    remoteKeyDao.deleteAll()
                    watchedEpisodesDao.deleteAllCachedEpisodes()
                }

                // Save last refresh date
                sharedPreferences.edit()
                    .putString(WATCHED_EPISODES_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                    .apply()
            }

            val data = convertHistoryEntries(
                traktApi.tmUsers().history(
                    UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                    HistoryType.EPISODES,
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
                WatchedEpisodePageKey(it.episode_trakt_id, prevKey, nextKey)
            }
            remoteKeyDao.insert(keys)
            watchedEpisodesDao.insert(data)


            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        }


    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, WatchedEpisode>
    ): WatchedEpisodePageKey? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.episode_trakt_id?.let { episodeId ->
                remoteKeyDao.remoteKeyByPage(episodeId)
            }
        }
    }


    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, WatchedEpisode>): WatchedEpisodePageKey? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { episode ->
                // Get the remote keys of the first items retrieved
                remoteKeyDao.remoteKeyByPage(episode.episode_trakt_id)
            }
    }


    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, WatchedEpisode>): WatchedEpisodePageKey? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { episode ->
                // Get the remote keys of the last item retrieved
                remoteKeyDao.remoteKeyByPage(episode.episode_trakt_id)
            }
    }


    private fun convertHistoryEntries(historyEntries: List<HistoryEntry>): List<WatchedEpisode> {
        val watchedEpisodes: MutableList<WatchedEpisode> = mutableListOf()

        historyEntries.map { entry ->
            watchedEpisodes.add(
                WatchedEpisode(
                    entry.id,
                    entry.episode?.ids?.trakt ?: 0,
                    entry.episode?.ids?.tmdb ?: 0,
                    entry.show?.language,
                    entry.show?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb ?: 0,
                    entry.watched_at,
                    entry.episode?.season ?: 0,
                    entry.episode?.number ?: 0,
                    entry.episode?.number_abs ?: 0,
                    entry.episode?.overview,
                    entry.episode?.runtime,
                    entry.episode?.title,
                    entry.show?.status ?: Status.CANCELED,
                    entry.show?.title ?: ""
                )
            )
        }
        return watchedEpisodes
    }
    
    companion object {
        const val WATCHED_EPISODES_LAST_REFRESHED_KEY = "watched_episodes_last_refreshed"
    }
}