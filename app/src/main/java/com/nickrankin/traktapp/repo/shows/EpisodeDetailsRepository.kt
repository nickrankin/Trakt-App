package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.nickrankin.traktapp.helper.networkBoundResource
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
    private val showsDatabase: ShowsDatabase,
    private val watchedHistoryDatabase: WatchedHistoryDatabase
) {
    private val episodesDao = showsDatabase.TmEpisodesDao()
    private val watchedHistoryShowsDao = watchedHistoryDatabase.watchedHistoryShowsDao()

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
            watchedHistoryShowsDao.getWatchedEpisodesPerShow(showTraktId)
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
                watchedHistoryShowsDao.deleteAllWatchedEpisodesPerShow(showTraktId)

                watchedHistoryShowsDao.insertEpisodes(getWatchedEpisodes(historyEntries))
            }
        }
    )

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

    suspend fun getRatings() = flow {
        try {
            val response = traktApi.tmUsers().ratingsEpisodes(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), RatingsFilter.ALL, null
            )

            emit(Resource.Success(response))

        } catch (e: Throwable) {
            e.printStackTrace()
            emit(Resource.Error(e, null))
        }
    }

    suspend fun addRatings(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().addRatings(syncItems)

            // Notify active Rating channel observers
            getRatings()

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun checkin(episodeCheckin: EpisodeCheckin): Resource<EpisodeCheckinResponse?> {

        return try {
            val response = traktApi.tmCheckin().checkin(episodeCheckin)

            Resource.Success(response)

        } catch (e: HttpException) {
            if (e.code() == 409) {
                // User is already checked in so not really an error as such. Null EpisodeCheckinResponse = need to delete active checkin first
                Resource.Success(null)
            } else {
                Resource.Error(e, null)
            }
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun deleteActiveCheckin(): Resource<Boolean> {

        return try {
            traktApi.tmCheckin().deleteActiveCheckin()

            Resource.Success(true)

        } catch (t: Throwable) {
            Resource.Error(t, false)
        }
    }

    suspend fun addToWatchedHistory(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().addItemsToWatchedHistory(syncItems)

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeWatchedEpisode(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            showsDatabase.withTransaction {
                watchedHistoryShowsDao.deleteWatchedEpisodeById(syncItems.ids?.first() ?: 0L)
            }

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