package com.nickrankin.traktapp.repo.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.api.services.trakt.model.BaseShow
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.RefreshType
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.SeasonProgress
import com.nickrankin.traktapp.dao.show.model.ShowsProgress
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefresh
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.BaseSeason
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "ShowsProgressRepository"
class ShowsProgressRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences
) {

    private val shoeSeasonProgressDao = showsDatabase.showSeasonProgressDao()
    private val lastRefreshedAtDao = showsDatabase.lastRefreshedAtDao()
    private val userSlug = UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL"))

    fun getShowProgress(shouldRefresh: Boolean) = networkBoundResource(
        query = {
            shoeSeasonProgressDao.getShowSeasonProgress()
        },
        fetch = {
            traktApi.tmUsers().watchedShows(userSlug, Extended.FULL)
        },
        shouldFetch = { showAndSeasonProgress ->
            val lastRefreshedAt = lastRefreshedAtDao.getLastRefreshed(RefreshType.PROGRESS_SHOWS).first()
            shouldRefresh || showAndSeasonProgress.isEmpty() || shouldRefresh(lastRefreshedAt, null)
        },
        saveFetchResult = { baseShows ->

            Log.d(TAG, "getShowProgress: Refreshing Show progress")
            val showSeasonProgress = getShowProgress(baseShows)

            showsDatabase.withTransaction {
                showSeasonProgress.map { showSeasonProgress ->
                    shoeSeasonProgressDao.insertShowProgress(showSeasonProgress.key)
                    shoeSeasonProgressDao.insertSeasonProgress(showSeasonProgress.value)
                }

                lastRefreshedAtDao.insertLastRefreshStats(LastRefreshedAt(RefreshType.PROGRESS_SHOWS, OffsetDateTime.now()))
            }

        }
    )

    private fun getShowProgress(shows: List<BaseShow>): Map<ShowsProgress, List<SeasonProgress>> {
        val showProgressList: MutableMap<ShowsProgress, List<SeasonProgress>> = mutableMapOf()

        shows.map { baseShow ->
            showProgressList.put(
                ShowsProgress(
                    baseShow.show?.ids?.trakt!!,
                    baseShow.show?.ids?.tmdb,
                    baseShow.show?.aired_episodes ?: 0,
                    baseShow.show?.title!!,
                    baseShow.show?.overview,
                    baseShow.last_watched_at,
                    baseShow.last_updated_at
                ),
                getShowSeasonProgress(
                    baseShow.show?.ids?.trakt!!,
                    baseShow.seasons ?: emptyList()
                )
            )
        }

        return showProgressList

    }

    private fun getShowSeasonProgress(
        showTraktId: Int,
        seasons: List<BaseSeason>
    ): List<SeasonProgress> {
        val seasonProgressList: MutableList<SeasonProgress> = mutableListOf()

        seasons.map { baseSeason ->
            seasonProgressList.add(
                SeasonProgress(
                    (showTraktId + baseSeason.number) ?: 0,
                    showTraktId,
                    baseSeason.number ?: 0,
                    baseSeason.episodes?.size ?: 0
                )
            )
        }


        return seasonProgressList
    }
}