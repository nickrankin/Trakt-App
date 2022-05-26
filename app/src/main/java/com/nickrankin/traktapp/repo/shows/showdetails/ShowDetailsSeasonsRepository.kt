package com.nickrankin.traktapp.repo.shows.showdetails

import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

class ShowDetailsSeasonsRepository @Inject constructor(
    private val showDataHelper: ShowDataHelper,
    private val showsDatabase: ShowsDatabase
) {
    private val tmSeasonsDao = showsDatabase.TmSeasonsDao()

    suspend fun getSeasons(traktId: Int, tmdbId: Int?, language: String?, shouldRefresh: Boolean) =
        networkBoundResource(
            query = {
                tmSeasonsDao.getSeasonsForShow(traktId)
            },
            fetch = {
                showDataHelper.getSeasons(traktId, tmdbId, language)
            },
            shouldFetch = { seasons ->
                seasons.isEmpty() || shouldRefresh
            },
            saveFetchResult = { seasons ->

                showsDatabase.withTransaction {
                    tmSeasonsDao.insertSeasons(
                        seasons
                    )
                }
            }
        )
}