package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.stats.model.*
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefresh
import com.nickrankin.traktapp.repo.IActionButtons
import com.nickrankin.traktapp.repo.lists.ListsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.*
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "ShowActionButtonsRepsit"

class ShowActionButtonsRepsitory @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val listsRepository: ListsRepository,
    private val showsDatabase: ShowsDatabase) : IActionButtons<EpisodeWatchedHistoryEntry> {

    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

    private val showRatingsDao = showsDatabase.ratedShowsStatsDao()

    private val collectedShowsDao = showsDatabase.collectedShowsDao()
    private val showCollectedStatsDao = showsDatabase.collectedShowsStatsDao()

    private val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()
    private val episodeWatchedHistoryEntryDao = showsDatabase.episodeWatchedHistoryEntryDao()

    private val lastRefreshedAtDao = showsDatabase.lastRefreshedAtDao()

    override suspend fun getRatings(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<RatingStats?>> = networkBoundResource(
        query = {
            showRatingsDao.getRatingsById(traktId)
        },
        fetch = {
            traktApi.tmUsers().ratingsShows(userSlug, RatingsFilter.ALL, null)
        },
        shouldFetch = { rating ->
            shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.RATED_SHOWS).first(), null)
        },
        saveFetchResult = { ratedShows ->

            showsDatabase.withTransaction {
                showRatingsDao.deleteRatingsStats()
                showRatingsDao.insert(getRatedShowsStats(ratedShows))
            }

            showsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.RATED_SHOWS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    private fun getRatedShowsStats(ratedShows: List<RatedShow>): List<RatingsShowsStats> {
        val ratedShowStats: MutableList<RatingsShowsStats> = mutableListOf()

        ratedShows.map { ratedShow ->
            ratedShowStats.add(
                RatingsShowsStats(
                    ratedShow.show?.ids?.trakt ?: 0,
                    ratedShow.rating?.value ?: 0,
                    ratedShow.rated_at ?: OffsetDateTime.now()
                )
            )
        }

        return ratedShowStats
    }

    suspend fun getCollectedStats(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<CollectedStats?>> = networkBoundResource(
        query = {
            showCollectedStatsDao.getCollectedShowById(traktId)
        },
        fetch = {
            traktApi.tmUsers().collectionShows(userSlug, Extended.FULL)
        },
        shouldFetch = { collectedShow ->
            shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.COLLECTED_SHOW_STATS).first(), null)
        },
        saveFetchResult = { collectedShows ->
            showsDatabase.withTransaction {
                showCollectedStatsDao.deleteCollectedStats()
                showCollectedStatsDao.insert(getCollectedStats(collectedShows))
            }

            showsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.COLLECTED_SHOW_STATS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    private fun getCollectedStats(baseShows: List<BaseShow>): List<ShowsCollectedStats> {
        val showCollectedStatsList: MutableList<ShowsCollectedStats> = mutableListOf()

        baseShows.map { baseShow ->
            showCollectedStatsDao.insert(
                ShowsCollectedStats(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb,
                    baseShow.last_collected_at,
                    baseShow.show?.title ?: "",
                    baseShow.listed_at,
                    baseShow.last_updated_at,
                    baseShow.reset_at,
                    baseShow.completed ?: 0
                )
            )
        }

        return showCollectedStatsList
    }

    override suspend fun getPlaybackHistory(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<List<EpisodeWatchedHistoryEntry>>> = networkBoundResource(
        query = {
            episodeWatchedHistoryEntryDao.getWatchedEpisodesPerShow(traktId)
        },
        fetch = {
            traktApi.tmUsers().history(
                userSlug,
                HistoryType.SHOWS,
                traktId,
                1,
                9999,
                null,
                null,
                null
            )
        },
        shouldFetch = { historyEntries ->
            shouldFetch || shouldRefresh(lastRefreshedAtDao.getLastRefreshed(RefreshType.PLAYBACK_HISORY_SHOWS).first(), null)
        },
        saveFetchResult = { historyEntries ->
            showsDatabase.withTransaction {
                episodeWatchedHistoryEntryDao.deleteWatchedHistoryPerShow(traktId)
                episodeWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
            }

            showsDatabase.withTransaction {
                lastRefreshedAtDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.PLAYBACK_HISORY_SHOWS,
                        OffsetDateTime.now()
                    )
                )
            }

        }
    )

    private fun convertHistoryEntries(historyEntries: List<com.uwetrottmann.trakt5.entities.HistoryEntry>): List<EpisodeWatchedHistoryEntry> {
        val episodeHistoryEntries: MutableList<EpisodeWatchedHistoryEntry> = mutableListOf()

        historyEntries.map { entry ->
            episodeHistoryEntries.add(
                EpisodeWatchedHistoryEntry(
                    entry.id ?: 0L,
                    entry.episode?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb,
                    entry.show?.title ?: "",
                    entry.watched_at ?: OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    entry.show?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb,
                    entry.episode?.season ?: -1,
                    entry.episode?.number ?: -1
                )
            )
        }

        return episodeHistoryEntries
    }


    override suspend fun checkin(
        traktId: Int,
        overrideActiveCheckins: Boolean
    ): Resource<BaseCheckinResponse> {
        return try {
            if (overrideActiveCheckins) {
                cancelCheckins()
            }

            val showCheckin = EpisodeCheckin.Builder(
                SyncEpisode().id(EpisodeIds.trakt(traktId)),
                AppConstants.APP_VERSION,
                AppConstants.APP_DATE
            ).build()
            val checkinResponse = traktApi.tmCheckin().checkin(
                showCheckin
            )

            Resource.Success(checkinResponse)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    override suspend fun cancelCheckins(): Resource<Boolean> {
        return try {

            traktApi.tmCheckin().deleteActiveCheckin()
            Resource.Success(true)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    override suspend fun addRating(
        traktId: Int,
        newRating: Int,
        ratedAt: OffsetDateTime
    ): Resource<SyncResponse> {
        val syncItems = SyncItems().apply {
            shows = listOf(
                SyncShow()
                    .id(ShowIds.trakt(traktId))
                    .rating(Rating.fromValue(newRating))
                    .ratedAt(OffsetDateTime.now())
            )
        }

        return try {
            val response = traktApi.tmSync().addRatings(syncItems)

            insertRating(RatingsShowsStats(traktId, newRating, OffsetDateTime.now()), response)

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertRating(ratingStats: RatingsShowsStats, syncResponse: SyncResponse) {
        if ((syncResponse.added?.shows ?: 0) <= 0) {
            Log.e(TAG, "insertRating: Error, sync returned 0, returning")

            return
        }
        showsDatabase.withTransaction {
            showRatingsDao.insert(ratingStats)
        }
    }

    override suspend fun deleteRating(traktId: Int): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteRatings(getSyncItems(traktId))

            deleteRating(traktId, response)

            Resource.Success(response)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteRating(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.shows ?: 0) <= 0) {
            Log.e(TAG, "deleteRating: Error, sync returned 0, returning")

            return
        }
        showsDatabase.withTransaction {
            showRatingsDao.deleteRatingsStatsByShowId(traktId)
        }
    }

    override suspend fun getTraktListsAndItems(shouldFetch: Boolean): Flow<Resource<out List<Pair<TraktList, List<TraktListEntry>>>>> {
        return listsRepository.getTraktListsAndItems(shouldFetch)
    }

    override suspend fun addToList(itemTraktId: Int, listTraktId: Int): Resource<SyncResponse> {
        return listsRepository.addToList(itemTraktId, listTraktId, Type.SHOW)
    }

    override suspend fun removeFromList(
        itemTraktId: Int,
        listTraktId: Int
    ): Resource<SyncResponse> {
        return listsRepository.removeFromList(itemTraktId, listTraktId, Type.SHOW)
    }

    override suspend fun addToHistory(
        traktId: Int,
        watchedAt: OffsetDateTime
    ): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                shows = listOf(
                    SyncShow().id(ShowIds.trakt(traktId))
                        .watchedAt(watchedAt)
                )
            }

            val syncResponse = traktApi.tmSync().addItemsToWatchedHistory(syncItems)

            insertHistoryEntries(traktId, syncResponse)

            Resource.Success(syncResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertHistoryEntries(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.added?.shows ?: 0) > 0) {
            val historyEntries = getShowWatchedHistoryEntries(traktId)

            showsDatabase.withTransaction {

                // We need to get the watched movies again for this movie, so the PagingData can be updated
                episodeWatchedHistoryEntryDao.insert(
                    convertHistoryEntries(
                        historyEntries
                    )
                )

                // Need to invalidate the PagingData due to db manipulations
                watchedEpisodesDao.getWatchedEpisodes().invalidate()
            }

            showsDatabase.withTransaction {
                // Now we update the history entry stats
                episodeWatchedHistoryEntryDao.deleteWatchedHistoryPerShow(traktId)
                episodeWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
            }
        }
    }

    private suspend fun getShowWatchedHistoryEntries(traktId: Int): List<HistoryEntry> {
        return try {
            val response = traktApi.tmUsers().history(
                userSlug,
                HistoryType.SHOWS,
                traktId,
                1,
                999,
                Extended.FULL,
                null,
                null
            )

            response
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removeFromHistory(id: Long): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                ids = listOf(id)
            }

            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            deleteHistoryEntries(id, response)

            Resource.Success(response)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteHistoryEntries(historyId: Long, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.shows ?: 0) <= 0) {
            Log.e(TAG, "deleteHistoryEntries: Error, sync returned 0, returning")

            return
        }

        showsDatabase.withTransaction {
            watchedEpisodesDao.deleteWatchedEpisodeById(historyId)
        }

        showsDatabase.withTransaction {
            episodeWatchedHistoryEntryDao.deleteWatchedHistoryEntry(historyId)
        }
    }

    override suspend fun addToCollection(traktId: Int): Resource<SyncResponse> {
        return try {

            val addCollectionReposnse =
                traktApi.tmSync().addItemsToCollection(getSyncItems(traktId))

            insertCollectedShow(traktId, addCollectionReposnse)

            Resource.Success(addCollectionReposnse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertCollectedShow(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.added?.shows ?: 0) <= 0) {
            return
        }

        val collectedShow = traktApi.tmShows().summary(traktId.toString(), Extended.FULL)

        showsDatabase.withTransaction {
            collectedShowsDao.insert(
                CollectedShow(
                    collectedShow.ids?.trakt ?: 0,
                    collectedShow.ids?.tmdb ?: 0,
                    collectedShow.language,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    0,
                    0,
                    collectedShow.overview,
                    collectedShow.first_aired,
                    collectedShow.runtime,
                    collectedShow.status,
                    collectedShow.title,
                    false
                )
            )

            showCollectedStatsDao.insert(
                ShowsCollectedStats(
                    collectedShow.ids?.trakt ?: 0,
                    collectedShow.ids?.tmdb ?: 0,
                    OffsetDateTime.now(),
                    collectedShow.title,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    null,
                    0

                )
            )
        }
    }

    suspend fun removeFromCollection(traktId: Int): Resource<SyncResponse> {
        return try {
            val removeFromCollectionResponse =
                traktApi.tmSync().deleteItemsFromCollection(getSyncItems(traktId))

            deleteCollectedShow(traktId, removeFromCollectionResponse)

            Resource.Success(removeFromCollectionResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun deleteCollectedShow(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.deleted?.shows ?: 0) <= 0) {
            Log.e(TAG, "deleteCollectedMovie: Error, sync returned 0, returning")
            return
        }
        showsDatabase.withTransaction {
            collectedShowsDao.deleteShowById(traktId)
            showCollectedStatsDao.deleteCollectedStatsById(traktId)
        }
    }

    private fun getSyncItems(traktId: Int): SyncItems {
        return SyncItems().apply {
            shows = listOf(
                SyncShow().id(ShowIds.trakt(traktId))
            )
        }
    }
}