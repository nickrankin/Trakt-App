package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.WatchedEpisodesDao
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.TmSeason
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.getTmdbLanguage
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.RatedEpisode
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.RatingsFilter
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import java.util.*
import javax.inject.Inject

private const val TAG = "SeasonEpisodesRepositor"

class SeasonEpisodesRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val showDataHelper: ShowDataHelper,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences) {

    private val tmShowDao = showsDatabase.tmShowDao()
    private val watchedEpisodesDao = showsDatabase.watchedEpisodesDao()
    private val seasonDao = showsDatabase.TmSeasonsDao()
    private val episodesDao = showsDatabase.TmEpisodesDao()

    private val episodeWatchedHistoryEntryDao = showsDatabase.episodeWatchedHistoryEntryDao()
    private val ratingEpisodesStatsDao = showsDatabase.ratedEpisodesStatsDao()

    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null"))

    fun getShow(showTraktId: Int, showTmdbId: Int?, shouldRefresh: Boolean) = networkBoundResource(
        query = {
                tmShowDao.getShow(showTraktId)
        },
        fetch = {
                showDataHelper.getShow(showTraktId)
        },
        shouldFetch = { show ->
            show == null || shouldRefresh
        },
        saveFetchResult = { show ->
            showsDatabase.withTransaction {
                tmShowDao.insertShow(show!!)
            }
        }
    )

    fun getSeasons(
        showTraktId: Int,
        showTmdbId: Int?,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
                seasonDao.getSeasonsForShow(showTraktId)
        },
        fetch = {
                showDataHelper.getSeasons(showTraktId, showTmdbId, null)
        },
        shouldFetch = { seasons ->
            seasons.isEmpty() || shouldRefresh
        },
        saveFetchResult = { seasons ->
            Log.d(TAG, "getSeasons: Refreshing Seasons")
            showsDatabase.withTransaction {
                seasonDao.deleteAllSeasonsForShow(showTraktId)
                seasonDao.insertSeasons(seasons)
            }
        }
    )

    fun getSeason(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            seasonDao.getSeasonForShow(showTraktId, seasonNumber)
        },
        fetch = {
            showDataHelper.getSeasons(showTraktId, showTmdbId, null)
        },
        shouldFetch = { season ->
            season == null || shouldRefresh
        },
        saveFetchResult = { seasons ->
            Log.d(TAG, "getSeasons: Refreshing Seasons")
            showsDatabase.withTransaction {
                seasonDao.deleteAllSeasonsForShow(showTraktId)
                seasonDao.insertSeasons(seasons)
            }
        }
    )

    suspend fun getSeasonEpisodes(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        shouldRefresh: Boolean) = networkBoundResource(
        query = {
            episodesDao.getEpisodes(showTraktId, seasonNumber)
        },
        fetch = {
            showDataHelper.getSeasonEpisodesData(showTraktId, showTmdbId, seasonNumber, null)
        },
        shouldFetch = { episodes ->
            shouldRefresh || episodes.isEmpty()
        },
        saveFetchResult = { episodes ->

            showsDatabase.withTransaction {
                episodesDao.deleteEpisodes(showTraktId, seasonNumber)

                episodesDao.insert(
                    episodes
                )
            }

        }
    )

    suspend fun getWatchedEpisodes(
        showTraktId: Int,
        seasonNumber: Int,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            watchedEpisodesDao.getWatchedEpisodesByShowIdSeasonNumber(showTraktId, seasonNumber)
        },
        fetch = {
            traktApi.tmUsers().history(
                UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")),
                HistoryType.SHOWS,
                showTraktId,
                1,
                999,
                Extended.FULLEPISODES,
                OffsetDateTime.now().minusYears(99),
                OffsetDateTime.now()
            )
        },
        shouldFetch = { shouldRefresh || it.isEmpty() },
        saveFetchResult = { historyEntries ->
            showsDatabase.withTransaction {

                watchedEpisodesDao.deleteWatchedEpisodesByShowIdSeasonNumber(showTraktId, seasonNumber)

                watchedEpisodesDao.insert(getWatchedEpisodes(historyEntries))
            }
        }
    )

    private fun getWatchedEpisodes(episodes: List<HistoryEntry>): List<WatchedEpisode> {
        val watchedEpisodes: MutableList<WatchedEpisode> = mutableListOf()

        episodes.map { entry ->
            watchedEpisodes.add(
                WatchedEpisode(
                    entry.id ?: 0L,
                    entry.episode?.ids?.trakt ?: 0,
                    entry.episode?.ids?.tmdb ?: 0,
                    entry.show?.language,
                    entry.show?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb ?: 0,
                    entry.watched_at,
                    entry.episode?.season ?: 0,
                    entry.episode?.number ?: 0,
                    entry.episode?.number_abs ?: 0,
                    entry.episode?.overview,
                    entry.episode?.runtime,
                    entry.episode?.title,
                    entry.show?.status ?: Status.CANCELED,
                    entry.show?.title ?: ""
                )
            )


        }
        return watchedEpisodes
    }

    suspend fun refreshShowStats(traktShowId: Int?) {
        if(traktShowId == null) {
            Log.e(TAG, "refreshShowStats: Trakt Show ID must not be null", )
            return
        }
        Log.d(TAG, "refreshEpisodeStats: Refreshing Watched and Rating Stats")

        try {
            val episodeWatchedStatsResponse = traktApi.tmUsers().history(
                userSlug,
                HistoryType.SHOWS,
                traktShowId,
                1,
                9999,
                null,
                null,
                null
            )

            val episodeRatingsResponse = traktApi.tmUsers().ratingsEpisodes(userSlug, RatingsFilter.ALL, null, null, null)

            updateEpisodeWatchedStats(episodeWatchedStatsResponse)
            updateEpisodeRatings(episodeRatingsResponse)
        } catch(t: Throwable) {
            t.printStackTrace()
        }
    }

    private suspend fun updateEpisodeRatings(ratedEpisodes: List<RatedEpisode>) {
        Log.d(TAG, "updateEpisodeRatings: Refreshing ${ratedEpisodes.size} episodes")
        val ratedEpisodeStats: MutableList<RatingsEpisodesStats> = mutableListOf()
        ratedEpisodes.map { ratedEpisode ->
            ratedEpisodeStats.add(
                RatingsEpisodesStats(
                    ratedEpisode.episode?.ids?.trakt ?: -1,
                    ratedEpisode.rating?.value ?: 0,
                    ratedEpisode.rated_at ?: OffsetDateTime.now()
                )
            )
        }

        showsDatabase.withTransaction {
            ratingEpisodesStatsDao.insert(
                ratedEpisodeStats
            )
        }
    }

    private suspend fun updateEpisodeWatchedStats(historyEntries: List<HistoryEntry>) {
        Log.d(TAG, "updateEpisodeWatchedStats: Refreshing ${historyEntries.size} watched episodes")
        val watchedHistoryEntries: MutableList<EpisodeWatchedHistoryEntry> = mutableListOf()
        historyEntries.map { watchedEpisode ->
            watchedHistoryEntries.add(
                EpisodeWatchedHistoryEntry(
                    watchedEpisode.id ?: 0L,
                    watchedEpisode.episode?.ids?.trakt ?: -1,
                    watchedEpisode.episode?.ids?.tmdb,
                    watchedEpisode.episode?.title ?: "",
                    watchedEpisode.watched_at ?: OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    watchedEpisode.show?.ids?.trakt ?: -1,
                    watchedEpisode.show?.ids?.tmdb,
                    watchedEpisode.episode?.season ?: -1,
                    watchedEpisode.episode?.number ?: -1

                    )
            )
        }

        showsDatabase.withTransaction {
            episodeWatchedHistoryEntryDao.insert(
                watchedHistoryEntries
            )
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SEASON_NUMBER_KEY = "season_number"
        const val LANGUAGE_KEY = "language"

        const val LAST_FULL_REFRESH_KEY = "last_full_episodes_refresh"
    }
}