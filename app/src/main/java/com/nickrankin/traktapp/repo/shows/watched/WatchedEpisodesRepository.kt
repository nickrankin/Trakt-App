package com.nickrankin.traktapp.repo.shows.watched

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.WatchedShowsMediatorDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "WatchedEpisodesReposito"
class WatchedEpisodesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: WatchedShowsMediatorDatabase) {

    private val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()
    private var latestWatchedEpisodes: List<HistoryEntry> = listOf()


    @OptIn(ExperimentalPagingApi::class)
    fun watchedEpisodes(shouldRefresh: Boolean) = Pager(
        config = PagingConfig(8),
        remoteMediator = WatchedEpisodesRemoteMediator(traktApi, shouldRefresh, showsDatabase, sharedPreferences)
    ) {
        showsDatabase.watchedEpisodesDao().getWatchedEpisodes()
    }.flow

    fun getLatestWatchedEpisodes(shouldRefresh: Boolean) = flow {
        emit(Resource.Loading())

        if(!shouldRefresh && latestWatchedEpisodes.isNotEmpty()) {
            Log.d(TAG, "getLatestWatchedEpisodes: Getting latest movies from cache")
            emit(Resource.Success(latestWatchedEpisodes))
        } else {
            Log.d(TAG, "getLatestWatchedEpisodes: Refreshing latest movies (Should Refresh Value: $shouldRefresh)")
            try {
                val response = traktApi.tmUsers().history(
                    UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                    HistoryType.EPISODES,
                    1,
                    5,
                    Extended.FULL,
                    OffsetDateTime.now().minusYears(99),
                    OffsetDateTime.now()
                )

                latestWatchedEpisodes = response

                emit(Resource.Success(latestWatchedEpisodes))

            } catch(t: Throwable) {
                emit(Resource.Error(t, null))
            }
        }
    }

    suspend fun deleteFromWatchedHistory(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            showsDatabase.withTransaction {
                watchedEpisodesDao.deleteWatchedEpisodeById(syncItems.ids?.first() ?: 0L)
            }
            Resource.Success(response)

        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

}