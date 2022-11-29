package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.WatchedSeasonStats
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

private const val TAG = "SeasonStatsRepository"
class SeasonStatsRepository @Inject constructor(private val traktApi: TraktApi, private val showsDatabase: ShowsDatabase, private val sharedPreferences: SharedPreferences) {


    private val watchedSeasonStatsDao = showsDatabase.watchedSeasonStatsDao()
    private val watchedSeasonStats = watchedSeasonStatsDao.getWatchedStats()

    suspend fun getWatchedSeasonStatsPerShow(showTraktId: Int, shouldRefresh: Boolean) {
        val showSeasonStats = watchedSeasonStatsDao.getWatchedStatsByShow(showTraktId)

        if(shouldRefresh || showSeasonStats.first().isEmpty()) {
            Log.d(
                TAG,
                "getWatchedSeasonStatsPerShow: Getting watched season Stats for show $showTraktId"
            )

            val response = traktApi.tmShows().watchedProgress(
                showTraktId.toString(),
                true,
                true,
                false,
                ProgressLastActivity.WATCHED,
                null
            )

            insertWatchedSeasonsStats(showTraktId, response)
        }
    }

    private suspend fun insertWatchedSeasonsStats(showTraktId: Int, baseShow: BaseShow) {
        val watchedSeasonsStats: MutableList<WatchedSeasonStats> = mutableListOf()

        baseShow.seasons?.map { baseSeason ->
            watchedSeasonsStats.add(
                WatchedSeasonStats(
                    "${showTraktId}S${baseSeason.number}",
                    showTraktId,
                    baseSeason.number ?: 0,
                    baseSeason?.aired ?: 0,
                    baseSeason?.completed ?: 0
                )
            )
        }

        showsDatabase.withTransaction {
            watchedSeasonStatsDao.insert(watchedSeasonsStats)
        }
    }

}