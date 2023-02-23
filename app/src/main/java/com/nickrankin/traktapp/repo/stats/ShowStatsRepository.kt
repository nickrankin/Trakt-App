package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.ShowsCollectedStats
import com.nickrankin.traktapp.dao.stats.model.RatingsShowsStats
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedShowsStats
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.RatedShow
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.RatingsFilter
import javax.inject.Inject

private const val TAG = "ShowStatsRepository"
class ShowStatsRepository @Inject constructor(private val traktApi: TraktApi, private val showsDatabase: ShowsDatabase, private val sharedPreferences: SharedPreferences) {
    //Shows
    private val collectedShowsStatsDao = showsDatabase.collectedShowsStatsDao()
    private val ratingsShowsStatsDao = showsDatabase.ratedShowsStatsDao()
    private val watchedShowsStatsDao = showsDatabase.watchedShowsStatsDao()
    private val watchedEpisodesStatsDao = showsDatabase.watchedEpisodesStatsDao()

    // Flows Shows
    val collectedShowsStats = collectedShowsStatsDao.getCollectedStats()
    val watchedShowsStats = watchedShowsStatsDao.getWatchedStats()
    val ratedShowsStats = ratingsShowsStatsDao.getRatingsStats()


    suspend fun refreshAllShowStats() {
        Log.d(TAG, "refreshAllShowStatsTest: Refreshing All Show stats")
        refreshWatchedShows()

        refreshCollectedShows()
        refreshShowsRatings()

        // Will also refresh Episodes and Seasons watched stats

    }

    suspend fun refreshWatchedShows() {

            val watchedShowStatsResponse = traktApi.tmUsers().watchedShows(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), null
            )

            insertWatchedShowsStats(watchedShowStatsResponse)
    }

    suspend fun refreshCollectedShows() {

            val collectedShowStatsResponse = traktApi.tmUsers().collectionShows(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), null
            )

            insertCollectedShowsStats(collectedShowStatsResponse)
    }



    private suspend fun refreshShowsRatings() {

            val showRatingsResponse = traktApi.tmUsers().ratingsShows(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), RatingsFilter.ALL, null
            )

            insertRatedShowsStats(showRatingsResponse)
    }

    private suspend fun insertCollectedShowsStats(shows: List<BaseShow>) {
        val showsCollectedStatsList: MutableList<ShowsCollectedStats> = mutableListOf()

        shows.map { baseShow ->
            showsCollectedStatsList.add(
                ShowsCollectedStats(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb,
                    baseShow.last_collected_at,
                    baseShow.show?.title ?: "",
                    baseShow.listed_at,
                    baseShow.last_updated_at,
                    baseShow.reset_at,
                    baseShow.completed ?: 0,
                )
            )
        }

        showsDatabase.withTransaction {
            collectedShowsStatsDao.deleteCollectedStats()
            collectedShowsStatsDao.insert(showsCollectedStatsList)
        }

    }

    private suspend fun insertWatchedShowsStats(watchedShows: List<BaseShow>) {
        val watchedShowsStatsList: MutableList<WatchedShowsStats> = mutableListOf()

        watchedShows.map { baseShow ->
            watchedShowsStatsList.add(
                WatchedShowsStats(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb,
                    baseShow.last_watched_at,
                    baseShow.reset_at,
                    baseShow.completed ?: 0,
                    baseShow.show?.title ?: "",
                    baseShow.listed_at,
                    baseShow.plays ?: 0
                )
            )

            insertWatchedEpisodesStats(baseShow)
        }

        showsDatabase.withTransaction {
            watchedShowsStatsDao.insert(watchedShowsStatsList)
        }
    }

    private suspend fun insertRatedShowsStats(ratedShows: List<RatedShow>) {
        val ratedShowsStatsList: MutableList<RatingsShowsStats> = mutableListOf()

        ratedShows.forEach { ratedShow ->
            ratedShowsStatsList.add(
                RatingsShowsStats(
                    ratedShow.show?.ids?.trakt ?: 0,
                    ratedShow.rating?.value ?: 0,
                    ratedShow.rated_at
                )
            )
        }

        showsDatabase.withTransaction {
            ratingsShowsStatsDao.deleteRatingsStats()
            ratingsShowsStatsDao.insert(ratedShowsStatsList)
        }
    }

    private suspend fun insertWatchedEpisodesStats(baseShow: BaseShow) {
        val watchedEpisodeStats: MutableList<WatchedEpisodeStats> = mutableListOf()

        baseShow.seasons?.map { baseSeason ->
            val seasonNumber = baseSeason.number

            baseSeason.episodes?.map { baseEpisode ->
                watchedEpisodeStats.add(
                    WatchedEpisodeStats(
                        "${baseShow.show?.ids?.trakt}S${baseSeason.number}E${baseEpisode.number}",
                        baseShow.show?.ids?.tmdb,
                        baseShow.show?.ids?.trakt ?: 0,
                        baseShow.show?.title ?: "",
                        baseSeason.number ?: 0,
                        baseEpisode?.number ?: 0,
                        baseEpisode.last_watched_at,
                        baseEpisode.plays ?: 0
                    )
                )
            }
        }

        showsDatabase.withTransaction {
            watchedEpisodesStatsDao.insert(watchedEpisodeStats)
        }


    }
}