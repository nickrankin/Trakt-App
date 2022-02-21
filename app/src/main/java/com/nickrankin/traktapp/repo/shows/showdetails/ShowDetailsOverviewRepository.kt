package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.ShowCreditsHelper
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

class ShowDetailsOverviewRepository @Inject constructor(
    private val creditsHelper: ShowCreditsHelper,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase,
) {

    val tmShowDao = showsDatabase.tmShowDao()

    private val showCastPeopleDao = creditsDatabase.showCastPeopleDao()
    private val castPersonDao = creditsDatabase.castPersonDao()

    suspend fun getCredits(traktId: Int, tmdbId: Int?, showGuestStars: Boolean, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            showCastPeopleDao.getShowCast(traktId, showGuestStars)
        },
        fetch = {
            creditsHelper.getShowCredits(traktId, tmdbId)
        },
        shouldFetch = { showCastPersonList ->
            showCastPersonList.isEmpty() || shouldRefresh
        },
        saveFetchResult = { castPersons ->
            creditsDatabase.withTransaction {
                showCastPeopleDao.deleteShowCast(traktId)
            }

            showsDatabase.withTransaction {
                castPersons.map { castPersonPair ->
                    castPersonDao.insert(castPersonPair.second)
                    showCastPeopleDao.insert(castPersonPair.first)
                }

            }
        }
    )
}