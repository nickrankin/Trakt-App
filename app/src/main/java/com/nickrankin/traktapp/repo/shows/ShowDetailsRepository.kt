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
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.shows.ShowDetailsViewModel
import com.nickrankin.traktapp.repo.TrackedEpisodesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "ShowDetailsRepository"
class ShowDetailsRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase, private val watchedHistoryDatabase: WatchedHistoryDatabase): TrackedEpisodesRepository(traktApi, tmdbApi, showsDatabase) {
    private val tmShowDao = showsDatabase.tmShowDao()
    private val tmSeasonsDao = showsDatabase.TmSeasonsDao()
    private val trackedShowDao = showsDatabase.trackedShowDao()
    private val collectedShowDao = showsDatabase.collectedShowsDao()
    private val watchedEpisodesDao = watchedHistoryDatabase.watchedHistoryShowsDao()

    val processChannel = Channel<BaseShow>()
    val trackingStatusChannel = Channel<Boolean>()

    fun getShowSummary(showTraktId: Int, showTmdbId: Int, language: String, shouldRefresh: Boolean) = networkBoundResource(
        query = {
                tmShowDao.getShow(showTmdbId)
        },
        fetch = {
                tmdbApi.tmTvService().tv(showTmdbId, language+",null", AppendToResponse(AppendToResponseItem.CREDITS, AppendToResponseItem.TV_CREDITS, AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.VIDEOS))
        },
        shouldFetch = { tmShow ->
            tmShow == null || shouldRefresh
        },
        saveFetchResult = { tvShow ->
            showsDatabase.withTransaction {
                tmSeasonsDao.deleteAllSeasonsForShow(showTmdbId)

                tmShowDao.insertShow(convertShow(showTraktId, tvShow))
                tmSeasonsDao.insertSeasons(convertSeasons(showTraktId, showTmdbId, tvShow.seasons ?: emptyList()))
            }
        }
    )

    fun getSeasons(showTmdbId: Int): Flow<List<TmSeason>> {
        return tmSeasonsDao.getSeasonsForShow(showTmdbId)
    }

    private fun convertShow(traktId: Int, tvShow: TvShow): TmShow {
        return TmShow(
            tvShow.id ?: 0,
            traktId,
            tvShow.name ?: "",
            tvShow.overview ?: "",
            tvShow.origin_country ?: emptyList(),
            tvShow.created_by ?: emptyList(),
            tvShow.credits,
            tvShow.external_ids,
            tvShow.genres,
            tvShow.homepage,
            tvShow.images,
            tvShow.in_production,
            tvShow.languages ?: emptyList(),
            tvShow.first_air_date,
            tvShow.last_air_date,
            tvShow.last_episode_to_air,
            tvShow.networks,
            tvShow.next_episode_to_air,
            tvShow.number_of_episodes ?: 0,
            tvShow.number_of_seasons ?: 0,
            tvShow.status ?: "",
            tvShow.poster_path,
            tvShow.backdrop_path,
            tvShow.type,
            tvShow.videos,
            false
        )
    }

    private fun convertSeasons(showTraktId: Int, showTmdbId: Int, seasons: List<TvSeason>): List<TmSeason> {
        val tmSeasons: MutableList<TmSeason> = mutableListOf()

        seasons.map { tvSeason ->
            tmSeasons.add(
                TmSeason(
                    tvSeason.id ?: 0,
                    showTmdbId,
                    showTraktId,
                    tvSeason.name ?: "",
                    tvSeason.overview ?: "",
                    tvSeason.credits,
                    tvSeason.external_ids,
                    tvSeason.images,
                    tvSeason.videos,
                    tvSeason.air_date,
                    tvSeason.episode_count ?: 0,
                    tvSeason.season_number ?: 0,
                    tvSeason.poster_path
                )
            )
        }

        return tmSeasons
    }

    suspend fun getShowProgress(showTraktId: Int) {
        try {
            val progressCall = traktApi.tmShows().watchedProgress(showTraktId.toString(), false, false, false, ProgressLastActivity.WATCHED, null)

            processChannel.send(progressCall)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRatings(): List<RatedShow> {
        Log.e(TAG, "getRatings: Getting ratings ...", )
        try {
            val ratings = traktApi.tmUsers().ratingsShows(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")?: "null"), RatingsFilter.ALL, null)

            return ratings
           // ratingsChannel.send(ratings)
        } catch(t: Throwable) {
            t.printStackTrace()
        }

        return emptyList()
    }

    suspend fun setRatings(syncItems: SyncItems, resetRatings: Boolean): Resource<SyncResponse> {
        return try {
            val syncResponse = if (!resetRatings) {traktApi.tmSync().addRatings(syncItems)} else {traktApi.tmSync().deleteRatings(syncItems)}

            // Trigger call to getRatings() to notify all active observers or rating channel
            Resource.Success(syncResponse)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun getTrackingStatus(traktId: Int) {
        val trackedShows = trackedShowDao.getTrackedShows().first()

        val trackedShow = trackedShows.find { it.trakt_id == traktId }

        Log.e(TAG, "getTrackingStatus: State is now ${trackedShow != null}", )

        trackingStatusChannel.send(trackedShow != null)
    }

    suspend fun setShowTracked(traktId: Int, tmdbId: Int) {
        Log.e(TAG, "setShowTracked: here", )
        var newStatus = false
        val trackedShow = trackedShowDao.getTrackedShow(traktId).first()
        if(trackedShow != null) {
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

        Log.e(TAG, "setShowTracked: New Status $newStatus", )

        trackingStatusChannel.send(newStatus)
    }

    suspend fun getWatchedEpisodes(shouldRefresh: Boolean, showTraktId: Int) = networkBoundResource(
        query = {
                watchedEpisodesDao.getWatchedEpisodesPerShow(showTraktId)
        },
        fetch = {
                traktApi.tmUsers().history(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, null)), HistoryType.SHOWS, showTraktId, 1,999, null,
                    OffsetDateTime.now().minusYears(99), OffsetDateTime.now())
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

    suspend fun removeFromCollection(collectedShow: CollectedShow?, syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().deleteItemsFromCollection(syncItems)

            if(collectedShow != null) {
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
            val response =  traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            showsDatabase.withTransaction {
                watchedEpisodesDao.deleteWatchedEpisodeById(syncItems.ids?.first() ?: 0L)
            }

            Resource.Success(response)
        }catch (e: Throwable) {
            Resource.Error(e, null)
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_LANGUAGE_KEY = "show_language"
    }
}