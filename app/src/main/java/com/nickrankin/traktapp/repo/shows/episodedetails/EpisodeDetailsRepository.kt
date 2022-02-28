package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "EpisodeDetailsRepositor"

class EpisodeDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showDataHelper: ShowDataHelper,
    private val sharedPreferences: SharedPreferences,
    private val showCreditsHelper: ShowCreditsHelper,
    private val showCreditsDatabase: CreditsDatabase,
    private val showsDatabase: ShowsDatabase,
    private val watchedHistoryDatabase: WatchedHistoryDatabase
): CreditsRepository(showCreditsHelper, showsDatabase, showCreditsDatabase) {
    private val episodesDao = showsDatabase.TmEpisodesDao()
    private val showsDbWatchedHistoryShowsDao = showsDatabase.watchedEpisodesDao()
    private val watchedHistoryDbWatchedHistoryDao = watchedHistoryDatabase.watchedHistoryShowsDao()
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


    suspend fun getWatchedEpisodes(shouldRefresh: Boolean, showTraktId: Int) = networkBoundResource(
        query = {
            watchedHistoryDbWatchedHistoryDao.getWatchedEpisodesPerShow(showTraktId)
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
            Log.d(TAG, "getWatchedEpisodes: Refreshing watched episodes")

            watchedHistoryDatabase.withTransaction {
                watchedHistoryDbWatchedHistoryDao.deleteAllWatchedEpisodesPerShow(showTraktId)

                watchedHistoryDbWatchedHistoryDao.insertEpisodes(getWatchedEpisodes(historyEntries))
            }
        }
    )

    suspend fun refreshWatchedEpisodes(showTraktId: Int): Boolean {
        return try {
            val response = traktApi.tmUsers().history(
                UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, null)),
                HistoryType.SHOWS,
                showTraktId,
                1,
                999,
                null,
                OffsetDateTime.now().minusYears(99),
                OffsetDateTime.now()
            )

                Log.d(TAG, "getWatchedEpisodes: Refreshing watched episodes")

                watchedHistoryDatabase.withTransaction {
                    watchedHistoryDbWatchedHistoryDao.deleteAllWatchedEpisodesPerShow(showTraktId)

                    watchedHistoryDbWatchedHistoryDao.insertEpisodes(getWatchedEpisodes(response))
                }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getWatchedEpisodes(historyEntries: List<HistoryEntry>): List<WatchedEpisode> {
        val watchedEpisodes: MutableList<WatchedEpisode> = mutableListOf()

        historyEntries.map { historyEntry ->
            watchedEpisodes.add(
                WatchedEpisode(
                    historyEntry.id,
                    historyEntry.episode?.ids?.trakt ?: 0,
                    historyEntry.episode?.ids?.tmdb ?: 0,
                    historyEntry.show?.language,
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

            val historyId = syncItems.ids?.first() ?: 0L

            /// Clean up the databases
            showsDatabase.withTransaction {
                showsDbWatchedHistoryShowsDao.deleteWatchedEpisodeById(historyId)
            }

            watchedHistoryDatabase.withTransaction {
                watchedHistoryDbWatchedHistoryDao.deleteWatchedEpisodeById(historyId)
            }

            // Ensure Watched History pager gets refreshed on next call if we remove a play
            sharedPreferences.edit()
                .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
                .apply()

            Resource.Success(response)
        } catch (e: Throwable) {
            Resource.Error(e, null)
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt"
        const val SHOW_TMDB_ID_KEY = "show_tmdb"
        const val SEASON_NUMBER_KEY = "season_number"
        const val EPISODE_NUMBER_KEY = "episode_number"
        const val LANGUAGE_KEY = "language"
        const val SHOULD_REFRESH_WATCHED_KEY = "should_refresh_watched"
    }
}