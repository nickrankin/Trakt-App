package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.base_entity.EpisodeBaseEnity
import com.nickrankin.traktapp.dao.base_entity.MovieBaseEntity
import com.nickrankin.traktapp.dao.base_entity.PersonBaseEntity
import com.nickrankin.traktapp.dao.base_entity.ShowBaseEntity
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.lists.model.ListEntry
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedEpisode
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

private const val TAG = "EpisodeActionButtonsRep"

class EpisodeActionButtonsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val listsRepository: ListsRepository,
    private val showsDatabase: ShowsDatabase) : IActionButtons<EpisodeWatchedHistoryEntry> {
    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

    private val ratedEpisodesStatsDao = showsDatabase.ratedEpisodesStatsDao()

    private val episodeCollectedStats = showsDatabase.collectedEpisodesStatsDao()

    private val episodeWatchedHistoryEntryDao = showsDatabase.episodeWatchedHistoryEntryDao()
    private val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()

    private val collectedEpisodesStatsDao = showsDatabase.collectedEpisodesStatsDao()
    private val collectedEpisodesDao = showsDatabase.collectedEpisodeDao()

    private val lastRefreshedDao = showsDatabase.lastRefreshedAtDao()

    override suspend fun getRatings(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<RatingStats?>> =
        networkBoundResource(
            query = {
                ratedEpisodesStatsDao.getRatingsStatsPerEpisode(traktId)
            },
            fetch = {
                traktApi.tmUsers().ratingsEpisodes(userSlug, RatingsFilter.ALL, null, null, null)
            },
            shouldFetch = { rating ->
                shouldFetch || shouldRefresh(
                    lastRefreshedDao.getLastRefreshed(RefreshType.RATED_EPISODES).first()
                , null)
            },
            saveFetchResult = { ratedEpisodes ->
                showsDatabase.withTransaction {
                    ratedEpisodesStatsDao.deleteRatingsStats()
                }

                val ratedEpisodeStats: MutableList<RatingsEpisodesStats> = mutableListOf()

                ratedEpisodes.map { ratedEpisode ->
                    ratedEpisodeStats.add(
                        RatingsEpisodesStats(
                            ratedEpisode.episode?.ids?.trakt ?: 0,
                            ratedEpisode.rating?.value ?: 0,
                            ratedEpisode.rated_at ?: OffsetDateTime.now()
                        )
                    )
                }

                showsDatabase.withTransaction {
                    ratedEpisodesStatsDao.insert(
                        ratedEpisodeStats
                    )
                }

                showsDatabase.withTransaction {
                    lastRefreshedDao.insertLastRefreshStats(
                        LastRefreshedAt(
                            RefreshType.RATED_EPISODES,
                            OffsetDateTime.now()
                        )
                    )
                }
            }
        )

    override suspend fun getCollectedStats(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<CollectedStats?>> = networkBoundResource(
        query = {
            episodeCollectedStats.getCollectedStatsByEpisodeId(traktId)
        },
        fetch = {
            traktApi.tmUsers().collectionShows(userSlug, null)
        },
        shouldFetch = { collectionStats ->
            shouldFetch || shouldRefresh(
                lastRefreshedDao.getLastRefreshed(RefreshType.COLLECTED_EPISODE_STATS).first()
            , null)
        },
        saveFetchResult = { baseShows ->
            insertEpisodeStats(traktId, baseShows)

            showsDatabase.withTransaction {
                lastRefreshedDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.COLLECTED_EPISODE_STATS,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    private suspend fun insertEpisodeStats(episodeTraktId: Int, baseShows: List<BaseShow>) {
        baseShows.map { show ->
            show.seasons?.map { season ->
                season.episodes?.map { episode ->
                    showsDatabase.withTransaction {
                        episodeCollectedStats.insert(
                            EpisodesCollectedStats(
                                episodeTraktId,
                                episodeTraktId,
                                null,
                                episode.collected_at,
                                "",
                                null,
                                show.show?.ids?.trakt ?: 0,
                                season.number ?: -1,
                                episode.number ?: -1
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun getPlaybackHistory(
        traktId: Int,
        shouldFetch: Boolean
    ): Flow<Resource<List<EpisodeWatchedHistoryEntry>>> = networkBoundResource(
        query = {
            episodeWatchedHistoryEntryDao.getWatchedEpisodesPerEpisode(traktId)
        },
        fetch = {
            traktApi.tmUsers().history(
                userSlug,
                HistoryType.EPISODES,
                traktId,
                1,
                null,
                null,
                null,
                null
            )
        },
        shouldFetch = { historyEntries ->
            shouldFetch || shouldRefresh(
                LastRefreshedAt(
                    RefreshType.PLAYBACK_HISTORY_EPISODES,
                    OffsetDateTime.now()
                )
            , null)
        },
        saveFetchResult = { historyEntries ->
            showsDatabase.withTransaction {
                episodeWatchedHistoryEntryDao.deleteWatchedHistoryPerEpisode(traktId)
                episodeWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
            }

            showsDatabase.withTransaction {
                lastRefreshedDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.PLAYBACK_HISTORY_EPISODES,
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
                    entry.episode?.ids?.tmdb,
                    entry.episode?.title ?: "Episode",
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
            episodes = listOf(
                SyncEpisode()
                    .id(EpisodeIds.trakt(traktId))
                    .rating(Rating.fromValue(newRating))
                    .ratedAt(OffsetDateTime.now())
            )
        }

        return try {
            val response = traktApi.tmSync().addRatings(syncItems)

            val stats = RatingsEpisodesStats(traktId, newRating, OffsetDateTime.now())

            Log.d(TAG, "addRating: Stats updateRatingText $stats")

            insertRating(stats, response)

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertRating(
        ratingStats: RatingsEpisodesStats,
        syncResponse: SyncResponse
    ) {
        if ((syncResponse.added?.episodes ?: 0) <= 0) {
            Log.e(TAG, "insertRating: Error, sync returned 0, returning")

            return
        }
        showsDatabase.withTransaction {
            ratedEpisodesStatsDao.insert(ratingStats)
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
        if ((syncResponse.deleted?.episodes ?: 0) <= 0) {
            Log.e(TAG, "deleteRating: Error, sync returned 0, returning")

            return
        }
        showsDatabase.withTransaction {
            ratedEpisodesStatsDao.deleteRatingsStatByEpisodeId(traktId)
        }
    }

    override suspend fun getTraktListsAndItems(shouldFetch: Boolean): Flow<Resource<out List<Pair<TraktList, List<TraktListEntry>>>>> {
        return listsRepository.getTraktListsAndItems(shouldFetch)
    }

    override suspend fun addToList(itemTraktId: Int, listTraktId: Int): Resource<SyncResponse> {
        return listsRepository.addToList(itemTraktId, listTraktId, Type.EPISODE)
    }

    override suspend fun removeFromList(
        itemTraktId: Int,
        listTraktId: Int
    ): Resource<SyncResponse> {
        return listsRepository.removeFromList(itemTraktId, listTraktId, Type.EPISODE)
    }

    override suspend fun addToHistory(
        traktId: Int,
        watchedAt: OffsetDateTime
    ): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode().id(EpisodeIds.trakt(traktId))
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
        if ((syncResponse.added?.episodes ?: 0) > 0) {
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
                episodeWatchedHistoryEntryDao.deleteWatchedHistoryPerEpisode(traktId)
                episodeWatchedHistoryEntryDao.insert(convertHistoryEntries(historyEntries))
            }
        }
    }

    private suspend fun getShowWatchedHistoryEntries(traktId: Int): List<HistoryEntry> {
        return try {
            val response = traktApi.tmUsers().history(
                userSlug,
                HistoryType.EPISODES,
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
        if ((syncResponse.deleted?.episodes ?: 0) <= 0) {
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

            insertCollectedEpisode(traktId, addCollectionReposnse)

            Resource.Success(addCollectionReposnse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private suspend fun insertCollectedEpisode(traktId: Int, syncResponse: SyncResponse) {
        if ((syncResponse.added?.episodes ?: 0) <= 0) {
            return
        }

        val episodeResponse = traktApi.tmSearch()
            .idLookup(IdType.TRAKT, traktId.toString(), Type.EPISODE, Extended.FULL, null, null)

        if (episodeResponse.isNotEmpty()) {
            val result = episodeResponse.first()

            showsDatabase.withTransaction {
                collectedEpisodesDao.insert(
                    CollectedEpisode(
                        result.episode.ids?.trakt ?: 0,
                        result.episode.season,
                        result.episode.number,
                        result.show.title,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                    )
                )

                collectedEpisodesStatsDao.insert(
                    EpisodesCollectedStats(
                        result.episode.ids?.trakt ?: 0,
                        result.episode.ids?.trakt ?: 0,
                        result.episode.ids?.tmdb ?: 0,
                        OffsetDateTime.now(),
                        result.show.title,
                        OffsetDateTime.now(),
                        result.show.ids?.trakt ?: 0,
                        result.episode.season,
                        result.episode.number
                    )
                )
            }
        } else {
            Log.e(TAG, "insertCollectedEpisode: Error finding episode $traktId on Trakt!",)
        }
    }

    override suspend fun removeFromCollection(traktId: Int): Resource<SyncResponse> {
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
        if ((syncResponse.deleted?.episodes ?: 0) <= 0) {
            Log.e(TAG, "deleteCollectedMovie: Error, sync returned 0, returning")
            return
        }
        showsDatabase.withTransaction {
            collectedEpisodesDao.deleteCollectedEpisodeById(traktId)
            collectedEpisodesStatsDao.deleteEpisodeStatsByEpisodeId(traktId)
        }
    }

    private fun getSyncItems(traktId: Int): SyncItems {
        return SyncItems().apply {
            episodes = listOf(
                SyncEpisode().id(EpisodeIds.trakt(traktId))
            )
        }
    }
}