package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.repo.TrackedEpisodesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

private const val REFRESH_INTERVAL_HOURS = 48L
private const val TAG = "ShowDetailsRepository"

class ShowDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val showDataHelper: ShowDataHelper,
    private val sharedPreferences: SharedPreferences,
    private val showsDatabase: ShowsDatabase,
    private val watchedHistoryDatabase: WatchedHistoryDatabase
) : TrackedEpisodesRepository(traktApi, tmdbApi, showsDatabase) {
    private val tmShowDao = showsDatabase.tmShowDao()
    private val tmSeasonsDao = showsDatabase.TmSeasonsDao()
    private val trackedShowDao = showsDatabase.trackedShowDao()
    private val collectedShowDao = showsDatabase.collectedShowsDao()
    private val watchedEpisodesDao = watchedHistoryDatabase.watchedHistoryShowsDao()
    private val lastRefreshedShowDao = showsDatabase.lastRefreshedShowDao()

    val processChannel = Channel<BaseShow>()
    val trackingStatusChannel = Channel<Boolean>()


    fun getShowSummary(showTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            tmShowDao.getShow(showTraktId)
        },
        fetch = {
            showDataHelper.getShow(showTraktId)
        },
        shouldFetch = { tmShow ->
            val lastRefreshed =
                lastRefreshedShowDao.getShowLastRefreshDate(showTraktId).first()?.lastRefreshDate
            Log.d(TAG, "getShowSummary: Last Refreshed $lastRefreshed")

            if (lastRefreshed != null && !shouldRefresh) {
                val forceRefresh =
                    OffsetDateTime.now().minusHours(REFRESH_INTERVAL_HOURS).isAfter(lastRefreshed)
                Log.d(TAG, "getShowSummary: Should refresh: $forceRefresh")
                forceRefresh
            } else {
                tmShow == null || shouldRefresh
            }

        },
        saveFetchResult = { traktShow ->
            showsDatabase.withTransaction {
                lastRefreshedShowDao.insert(LastRefreshedShow(showTraktId, OffsetDateTime.now()))
                tmShowDao.insertShow(traktShow!!)
            }
        }
    )


    suspend fun getSeasons(traktId: Int, tmdbId: Int?, language: String?, shouldRefresh: Boolean) =
        networkBoundResource(
            query = {
                tmSeasonsDao.getSeasonsForShow(traktId)
            },
            fetch = {
                showDataHelper.getSeasons(traktId, tmdbId, language)
            },
            shouldFetch = { seasons ->
                seasons.isEmpty() || shouldRefresh
            },
            saveFetchResult = { seasons ->

                showsDatabase.withTransaction {
                    tmSeasonsDao.insertSeasons(
                        seasons
                    )
                }
            }
        )

    suspend fun getShowProgress(showTraktId: Int) {
        try {
            val progressCall = traktApi.tmShows().watchedProgress(
                showTraktId.toString(),
                false,
                false,
                false,
                ProgressLastActivity.WATCHED,
                null
            )

            processChannel.send(progressCall)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRatings(): List<RatedShow> {
        Log.e(TAG, "getRatings: Getting ratings ...")
        try {
            val ratings = traktApi.tmUsers().ratingsShows(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "null"
                    ) ?: "null"
                ), RatingsFilter.ALL, null
            )

            return ratings
            // ratingsChannel.send(ratings)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return emptyList()
    }

    suspend fun setRatings(syncItems: SyncItems, resetRatings: Boolean): Resource<SyncResponse> {
        return try {
            val syncResponse = if (!resetRatings) {
                traktApi.tmSync().addRatings(syncItems)
            } else {
                traktApi.tmSync().deleteRatings(syncItems)
            }

            // Trigger call to getRatings() to notify all active observers or rating channel
            Resource.Success(syncResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun getTrackingStatus(traktId: Int) {
        val trackedShows = trackedShowDao.getTrackedShows().first()

        val trackedShow = trackedShows.find { it.trakt_id == traktId }

        Log.e(TAG, "getTrackingStatus: State is now ${trackedShow != null}")

        trackingStatusChannel.send(trackedShow != null)
    }

    suspend fun setShowTracked(traktId: Int, tmdbId: Int) {
        Log.e(TAG, "setShowTracked: here")
        var newStatus = false
        val trackedShow = trackedShowDao.getTrackedShow(traktId).first()
        if (trackedShow != null) {
            cancelShowTracking(trackedShow)
        } else {
            newStatus = true
            val trackedShow = TrackedShow(traktId, tmdbId, OffsetDateTime.now())

            showsDatabase.withTransaction {
                trackedShowDao.insertTrackedShow(
                    trackedShow
                )
            }
            insertTrackedShow(trackedShow)
        }

        Log.e(TAG, "setShowTracked: New Status $newStatus")

        trackingStatusChannel.send(newStatus)
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

    suspend fun addToCollection(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().addItemsToCollection(syncItems)
            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeFromCollection(
        collectedShow: CollectedShow?,
        syncItems: SyncItems
    ): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().deleteItemsFromCollection(syncItems)

            if (collectedShow != null) {
                showsDatabase.withTransaction {
                    collectedShowDao.delete(collectedShow)
                }

            }

            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
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

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_LANGUAGE_KEY = "show_language"
    }
}