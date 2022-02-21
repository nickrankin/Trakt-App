package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import kotlinx.coroutines.channels.Channel
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

class ShowDetailsProgressRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private val watchedHistoryDatabase: WatchedHistoryDatabase,
    private val sharedPreferences: SharedPreferences
) {
    private val watchedEpisodesDao = watchedHistoryDatabase.watchedHistoryShowsDao()

    val progressChannel = Channel<Resource<BaseShow>>()

    suspend fun refreshShowProgress(showTraktId: Int) {
        try {
            progressChannel.send(Resource.Loading(null))
            val progressCall = traktApi.tmShows().watchedProgress(
                showTraktId.toString(),
                false,
                false,
                false,
                ProgressLastActivity.WATCHED,
                Extended.FULL
            )
            progressChannel.send(Resource.Success(progressCall))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e, null)
        }
    }

    suspend fun getWatchedEpisodes(shouldRefresh: Boolean, showTraktId: Int) = networkBoundResource(
        query = {
            watchedEpisodesDao.getWatchedEpisodesPerShow(showTraktId)
        },
        fetch = {
            traktApi.tmUsers().history(
                UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, null)),
                HistoryType.SHOWS,
                showTraktId,
                1,
                999,
                null,
                OffsetDateTime.now().minusYears(99),
                OffsetDateTime.now()
            )
        },
        shouldFetch = { episodes ->
            shouldRefresh || episodes.isEmpty()
        },
        saveFetchResult = { historyEntries ->
            watchedHistoryDatabase.withTransaction {
                watchedEpisodesDao.deleteAllWatchedEpisodesPerShow(showTraktId)

                watchedEpisodesDao.insertEpisodes(getWatchedEpisodes(historyEntries))
            }
        }
    )

    private fun getWatchedEpisodes(historyEntries: List<HistoryEntry>): List<WatchedEpisode> {
        val watchedEpisodes: MutableList<WatchedEpisode> = mutableListOf()

        historyEntries.map { historyEntry ->
            watchedEpisodes.add(
                WatchedEpisode(
                    historyEntry.id ?: 0,
                    historyEntry.episode?.ids?.trakt ?: 0,
                    historyEntry.episode?.ids?.tmdb ?: 0,
                    historyEntry.show?.language ?: "en",
                    historyEntry.show?.ids?.trakt ?: 0,
                    null,
                    historyEntry.watched_at,
                    historyEntry.episode?.season,
                    historyEntry.episode?.number,
                    historyEntry.episode?.number_abs,
                    null,
                    null,
                    historyEntry.episode?.title,
                    null,
                    null
                )
            )
        }


        return watchedEpisodes
    }

    suspend fun removeWatchedEpisode(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            showsDatabase.withTransaction {
                watchedEpisodesDao.deleteWatchedEpisodeById(syncItems.ids?.first() ?: 0L)
            }

            Resource.Success(response)
        } catch (e: Throwable) {
            Resource.Error(e, null)
        }
    }

}