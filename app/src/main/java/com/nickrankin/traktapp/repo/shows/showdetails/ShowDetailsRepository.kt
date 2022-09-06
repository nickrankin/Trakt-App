package com.nickrankin.traktapp.repo.shows.showdetails

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.uwetrottmann.trakt5.entities.*
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val REFRESH_INTERVAL_HOURS = 48L
private const val TAG = "ShowDetailsRepository"

class ShowDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val showDataHelper: ShowDataHelper,
    private val showsDatabase: ShowsDatabase
) {
    private val tmShowDao = showsDatabase.tmShowDao()
    private val seasonDao = showsDatabase.TmSeasonsDao()

    private val lastRefreshedShowDao = showsDatabase.lastRefreshedShowDao()

    fun getShowSummary(showTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            tmShowDao.getShow(showTraktId)
        },
        fetch = {
            showDataHelper.getShow(showTraktId)
        },
        shouldFetch = { tmShow ->
            val lastRefreshed =
                lastRefreshedShowDao.getShowLastRefreshDate(showTraktId).first()?.lastRefreshDate
            Log.d(TAG, "getShowSummary: Last Refreshed $lastRefreshed")

            if (lastRefreshed != null && !shouldRefresh) {
                val forceRefresh =
                    OffsetDateTime.now().minusHours(REFRESH_INTERVAL_HOURS).isAfter(lastRefreshed)
                Log.d(TAG, "getShowSummary: Should refresh: $forceRefresh")
                forceRefresh
            } else {
                tmShow == null || shouldRefresh
            }

        },
        saveFetchResult = { traktShow ->
            showsDatabase.withTransaction {
                lastRefreshedShowDao.insert(LastRefreshedShow(showTraktId, OffsetDateTime.now()))
                tmShowDao.insertShow(traktShow!!)
            }
        }
    )

    suspend fun refreshSeasons(showDataModel: ShowDataModel?, shouldRefresh: Boolean) {
        if(showDataModel == null) {
            return
        }

        val seasons = seasonDao.getSeasonsForShow(showDataModel.traktId)

        if(shouldRefresh || seasons.first().isEmpty()) {
            Log.d(
                TAG,
                "refreshSeasons: Refreshing Season Data for show ${showDataModel.traktId} - ${showDataModel.showTitle}")

            val seasonsResponse = showDataHelper.getSeasons(showDataModel.traktId, showDataModel.tmdbId, null)

            showsDatabase.withTransaction {
                seasonDao.deleteAllSeasonsForShow(showDataModel.traktId)
                seasonDao.insertSeasons(seasonsResponse)
            }
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_TITLE_KEY = "show_title_id"

    }
}