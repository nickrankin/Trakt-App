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
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
import com.nickrankin.traktapp.repo.stats.SeasonStatsRepository
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
import javax.inject.Inject
import kotlin.Exception

private const val TAG = "ShowDetailsProgressRepo"
class ShowDetailsProgressRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private val showDataHelper: ShowDataHelper,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val sharedPreferences: SharedPreferences
) {
    private val seasonStatsDao = showsDatabase.watchedSeasonStatsDao()
    private val seasonsDao = showsDatabase.TmSeasonsDao()



    fun getSeasonStats(traktId: Int, tmdbId: Int?, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            seasonStatsDao.getWatchedStatsByShow(traktId)
        },
        fetch = {
            showDataHelper.getSeasons(traktId, tmdbId, null)
        },
        shouldFetch = { watchedSeasonStats ->

            shouldRefresh || watchedSeasonStats.isEmpty()
        },
        saveFetchResult = { seasons ->
            Log.d(TAG, "getOverallSeasonStats: Reload Season data stats. Total seasons ${seasons.size}")
            showsDatabase.withTransaction {
                seasonsDao.insertSeasons(seasons)
            }

            seasonStatsRepository.getWatchedSeasonStatsPerShow(traktId, true)
        }
    )
}