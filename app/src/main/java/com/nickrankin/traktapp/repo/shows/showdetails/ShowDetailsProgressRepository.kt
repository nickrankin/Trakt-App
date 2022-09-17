package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.stats.model.WatchedSeasonStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import com.nickrankin.traktapp.ui.IOnStatistcsRefreshListener
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "ShowDetailsProgressRepo"
class ShowDetailsProgressRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private  val showDataHelper: ShowDataHelper
) {
    private val seasonStatsDao = showsDatabase.watchedSeasonStatsDao()
    private val seasonsDao = showsDatabase.TmSeasonsDao()



    fun getSeasons(traktId: Int) = seasonsDao.getSeasonsForShow(traktId)

    suspend fun getOverallSeasonStats(traktId: Int, tmdbId: Int?): Flow<List<WatchedSeasonStats>> {
        val overallSeasonStats = seasonStatsDao.getWatchedStatsByShow(traktId)

        // The user either has not watched any of this shows episodes yet, we need to populate the tables so he sees a list of all seasons progress
        // For season watched stats otherwise, we use "users/{username}/watchlist/seasons" via the StatsRepository. If at least one episode is watched, we don't need to force refresh overall season stats!
        if(overallSeasonStats.first().isNullOrEmpty()) {

            val seasonsInfo = showDataHelper.getSeasons(traktId, tmdbId, null)

            Log.d(TAG, "getOverallSeasonStats: Reload Season data stats. Total seasons ${seasonsInfo.size}")

            showsDatabase.withTransaction {
                seasonsDao.insertSeasons(seasonsInfo)
            }

            seasonsInfo.map { seasonData ->

                showsDatabase.withTransaction {
                    seasonStatsDao.insert(
                        listOf(
                            WatchedSeasonStats(
                                "${traktId}S${seasonData.season_number}",
                                traktId, seasonData.season_number ?: 0,
                                seasonData.episode_count ?: 0,
                                0
                            )
                        )
                    )
                }
            }
        }

        return overallSeasonStats
    }

    fun getSeasonWatchedStats(showTraktId: Int, seasonNumber: Int) = seasonStatsDao.getSeasonWatchedStats(showTraktId, seasonNumber)
}