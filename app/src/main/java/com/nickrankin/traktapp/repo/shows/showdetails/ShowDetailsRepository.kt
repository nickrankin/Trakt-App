package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.repo.TrackedEpisodesRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

private const val REFRESH_INTERVAL_HOURS = 48L
private const val TAG = "ShowDetailsRepository"

class ShowDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val tmdbApi: TmdbApi,
    private val showDataHelper: ShowDataHelper,
    private val showsDatabase: ShowsDatabase,
) : TrackedEpisodesRepository(traktApi, tmdbApi, showsDatabase) {
    private val tmShowDao = showsDatabase.tmShowDao()
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

    suspend fun getAllUserRatings(traktId: Int): Resource<Ratings> {
        return try {
            val showRatingsResponse = traktApi.tmShows().ratings(traktId.toString())
            Resource.Success(showRatingsResponse)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_TITLE_KEY = "show_title"
        const val SHOW_LANGUAGE_KEY = "show_language"
    }
}