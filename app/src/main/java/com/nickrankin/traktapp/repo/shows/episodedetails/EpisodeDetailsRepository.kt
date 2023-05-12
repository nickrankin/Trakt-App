package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.stats.model.EpisodesCollectedStats
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import kotlinx.coroutines.flow.first
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "EpisodeDetailsRepositor"
class EpisodeDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showDataHelper: ShowDataHelper,
    private val sharedPreferences: SharedPreferences,
    private val creditsHelper: PersonCreditsHelper,
    private val creditsDatabase: CreditsDatabase,
    private val showsDatabase: ShowsDatabase): CreditsRepository(creditsHelper, showsDatabase, creditsDatabase) {

    private val episodesDao = showsDatabase.TmEpisodesDao()
//    private val collectedEpisodesStatsDao = showsDatabase.collectedEpisodesStatsDao()
//    private val showsDbWatchedHistoryShowsDao = showsDatabase.watchedEpisodesDao()

    suspend fun getEpisode(showTraktId: Int, seasonNumber: Int, episodeNumber: Int): TmEpisode? {
        return episodesDao.getEpisode(showTraktId, seasonNumber, episodeNumber).first()
    }

    suspend fun getEpisodes(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            episodesDao.getEpisode(showTraktId, seasonNumber, episodeNumber)
        },
        fetch = {
            showDataHelper.getSeasonEpisodesData(showTraktId, showTmdbId, seasonNumber, null)
        },
        shouldFetch = { episode ->
            shouldRefresh || episode == null
        },
        saveFetchResult = { episodes ->
            showsDatabase.withTransaction {
                episodesDao.deleteEpisodes(showTraktId, seasonNumber)

                episodesDao.insert(episodes)
            }
        }
    )

//    fun getEpisodeCollectionStatus(showTraktId: Int, season: Int, episode: Int, shouldRefresh: Boolean) = networkBoundResource(
//        query = {
//                collectedEpisodesStatsDao.getCollectedStatsByEpisode(showTraktId, season, episode)
//        },
//        fetch = {
//                traktApi.tmUsers().collectionShows(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")), null)
//        },
//        shouldFetch = { collectedStats ->
//            collectedStats.isEmpty() || shouldRefresh
//        },
//        saveFetchResult = { collectedShows ->
//            showsDatabase.withTransaction {
//                collectedEpisodesStatsDao.insert(getCollectedStats(collectedShows))
//            }
//        }
//    )
//
//    private fun getCollectedStats(baseShow: List<BaseShow>): List<EpisodesCollectedStats> {
//        val collectedEpisodes: MutableList<EpisodesCollectedStats> = mutableListOf()
//
//        baseShow.map { show ->
//            show.seasons?.map { season ->
//                season.episodes?.map { episode ->
//                    collectedEpisodes.add(
//                        EpisodesCollectedStats(
//                            0,
//                            show.show.ids.trakt,
//                            0,
//                            episode.collected_at,
//                            "",
//                            null,
//                            season.number,
//                            episode.number
//                        )
//                    )
//                }
//            }
//        }
//
//        return collectedEpisodes
//    }
//
//    suspend fun removeWatchedEpisode(syncItems: SyncItems): Resource<SyncResponse> {
//        return try {
//            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)
//
//            val historyId = syncItems.ids?.first() ?: 0L
//
//            /// Clean up the databases
//            showsDatabase.withTransaction {
//                showsDbWatchedHistoryShowsDao.deleteWatchedEpisodeById(historyId)
//            }
//
//            // Ensure Watched History pager gets refreshed on next call if we remove a play
//            sharedPreferences.edit()
//                .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
//                .apply()
//
//            Resource.Success(response)
//        } catch (e: Throwable) {
//            Resource.Error(e, null)
//        }
//    }

    companion object {
        const val SHOULD_REFRESH_WATCHED_KEY = "should_refresh_watched"
    }
}