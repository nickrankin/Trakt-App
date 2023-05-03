package com.nickrankin.traktapp.repo

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.history.model.MovieWatchedHistoryEntry
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefresh
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "MainActivityRepository"
class MainActivityRepository @Inject constructor(private val traktApi: TraktApi, private val moviesDatabase: MoviesDatabase, private val showsDatabase: ShowsDatabase, private val sharedPreferences: SharedPreferences) {
    private val watchedMoviesStatsDao = moviesDatabase.movieWatchedHistoryEntryDao()
    private val lastRefreshedMoviesDao = moviesDatabase.lastRefreshAtDao()

    private val watchedEpisodesStatsDao = showsDatabase.episodeWatchedHistoryEntryDao()
    private val lastRefreshedepisodeStatsDao = showsDatabase.lastRefreshedAtDao()

    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

    fun getLatestMovies(shouldRefresh: Boolean) = networkBoundResource(
        query = {
                watchedMoviesStatsDao.getLatestWatchedHistoryForMovie()
        },
        fetch = {
            val historyEntries: MutableList<HistoryEntry> = mutableListOf()

            var page = 1

            while(true) {
                val response = traktApi.tmUsers().history(
                    userSlug,
                    HistoryType.MOVIES,
                    page,
                    500,
                    null,
                    null,
                    null
                )

                page++

                historyEntries.addAll(response)

                if(response.isEmpty()) {
                    break
                }
            }

            Log.e(TAG, "getLatestMovies: Broken with ${historyEntries.size} ")

            historyEntries
        },
        shouldFetch = {
            shouldRefresh(lastRefreshedMoviesDao.getLastRefreshed(RefreshType.PLAYBACK_HISORY_MOVIES).first(), null) || shouldRefresh
        },
        saveFetchResult = { historyEntries ->
            Log.e(TAG, "getLatestMovies: Entries ${historyEntries.size}")
            moviesDatabase.withTransaction {
                watchedMoviesStatsDao.deleteAllMovieWatchedEntries()
                watchedMoviesStatsDao.insert(convertMovieHistoryEntries(historyEntries))
            }

            moviesDatabase.withTransaction {
                lastRefreshedMoviesDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.PLAYBACK_HISORY_MOVIES,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    fun getLatestEpisodes(shouldRefresh: Boolean) = networkBoundResource(
        query = {
            watchedEpisodesStatsDao.getLatestWatchedEpisodes()
        },
        fetch = {
            val historyEntries: MutableList<HistoryEntry> = mutableListOf()

            var page = 1

            while(true) {
                val response = traktApi.tmUsers().history(
                    userSlug,
                    HistoryType.EPISODES,
                    page,
                    500,
                    null,
                    null,
                    null
                )

                page++

                historyEntries.addAll(response)

                if(response.isEmpty()) {
                    break
                }
            }

            Log.e(TAG, "getLatestEpisodes: Broken with ${historyEntries.size} ")

            historyEntries
        },
        shouldFetch = {
            shouldRefresh(lastRefreshedepisodeStatsDao.getLastRefreshed(RefreshType.PLAYBACK_HISTORY_EPISODES).first(), null) || shouldRefresh
        },
        saveFetchResult = { historyEntries ->
            Log.e(TAG, "getLatestEpisodes: Entries ${historyEntries.size}")
            moviesDatabase.withTransaction {
                watchedEpisodesStatsDao.deleteAllWatchedHistory()
                watchedEpisodesStatsDao.insert(convertEpisodeHistoryEntries(historyEntries))
            }

            showsDatabase.withTransaction {
                lastRefreshedepisodeStatsDao.insertLastRefreshStats(
                    LastRefreshedAt(
                        RefreshType.PLAYBACK_HISTORY_EPISODES,
                        OffsetDateTime.now()
                    )
                )
            }
        }
    )

    private fun convertMovieHistoryEntries(historyEntries: List<com.uwetrottmann.trakt5.entities.HistoryEntry>): List<MovieWatchedHistoryEntry> {
        val movieHistoryEntries: MutableList<MovieWatchedHistoryEntry> = mutableListOf()

        historyEntries.map { entry ->
            movieHistoryEntries.add(
                MovieWatchedHistoryEntry(
                    entry.id ?: 0L,
                    entry.movie?.ids?.trakt ?: 0,
                    entry.movie?.ids?.tmdb,
                    entry.movie?.title ?: "",
                    entry.watched_at ?: OffsetDateTime.now(),
                    OffsetDateTime.now()
                )
            )
        }

        return movieHistoryEntries
    }

    private fun convertEpisodeHistoryEntries(historyEntries: List<com.uwetrottmann.trakt5.entities.HistoryEntry>): List<EpisodeWatchedHistoryEntry> {
        val episodeHistoryEntries: MutableList<EpisodeWatchedHistoryEntry> = mutableListOf()

        historyEntries.map { entry ->
            episodeHistoryEntries.add(
                EpisodeWatchedHistoryEntry(
                    entry.id ?: 0L,
                    entry.episode?.ids?.trakt ?: 0,
                    entry.episode?.ids?.tmdb,
                    entry.episode?.title ?: "Episode ",
                    entry.watched_at ?: OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    entry.show?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb,
                    entry.episode?.season ?: -1,
                    entry.episode?.number ?: -1,
                )
            )
        }

        return episodeHistoryEntries
    }
}