package com.nickrankin.traktapp.repo.shows.watched

import android.content.SharedPreferences
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
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
class WatchedEpisodesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase) {

    private val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()

    @OptIn(ExperimentalPagingApi::class)
    fun watchedEpisodes(shouldRefresh: Boolean) = Pager(
        config = PagingConfig(8),
        remoteMediator = WatchedEpisodesRemoteMediator(traktApi, shouldRefresh, showsDatabase, sharedPreferences)
    ) {
        showsDatabase.watchedEpisodesDao().getWatchedEpisodes()
    }.flow

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