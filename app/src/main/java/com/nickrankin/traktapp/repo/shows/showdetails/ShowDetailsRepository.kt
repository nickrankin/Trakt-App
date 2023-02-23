package com.nickrankin.traktapp.repo.shows.showdetails

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.VideoService
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import java.util.*
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

    private val lastRefreshedShowDao = showsDatabase.lastRefreshedAtDao()

    fun getShowSummary(showTraktId: Int, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            tmShowDao.getShow(showTraktId)
        },
        fetch = {
            showDataHelper.getShow(showTraktId)
        },
        shouldFetch = { tmShow ->
            shouldRefresh || tmShow == null
        },
        saveFetchResult = { traktShow ->
            showsDatabase.withTransaction {
                if(traktShow != null) {
                    tmShowDao.insertShow(traktShow)
                }
            }
        }
    )

    suspend fun getShowStreamingServices(tmdbId: Int?, title: String?): List<VideoService> {

        if (tmdbId == null || title == null) {
            return emptyList()
        }

        val locale = Locale.getDefault().country
        val videos: MutableList<VideoService> = mutableListOf()

        try {
            val videoServiceResponse = tmdbApi.tmTvService().watchProviders(tmdbId)

            // Services available to this userw Locale setting
            val availableVideoServices = videoServiceResponse.results.filterKeys { it == locale }

            availableVideoServices.entries.map { entry ->

                entry.value.buy.map { paidEntry ->
                    videos.add(
                        VideoService(
                            tmdbId,
                            title,
                            paidEntry.provider_id,
                            paidEntry.provider_name,
                            paidEntry.display_priority,
                            VideoService.TYPE_BUY
                        )
                    )
                }

                entry.value.flatrate.map { prepaidEntry ->
                    videos.add(
                        VideoService(
                            tmdbId,
                            title,
                            prepaidEntry.provider_id,
                            prepaidEntry.provider_name,
                            prepaidEntry.display_priority,
                            VideoService.TYPE_STREAM
                        )
                    )
                }
            }

            return videos

        } catch (e: Exception) {
            Log.e(TAG, "getVideoStreamingServices: Error getting videos ${e.message}")
        }


        return emptyList()
    }
    companion object {
        const val SHOW_TRAKT_ID_KEY = "show_trakt_id"
        const val SHOW_TMDB_ID_KEY = "show_tmdb_id"
        const val SHOW_TITLE_KEY = "show_title_id"

    }
}