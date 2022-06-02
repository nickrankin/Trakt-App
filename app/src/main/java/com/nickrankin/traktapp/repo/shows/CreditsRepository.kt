package com.nickrankin.traktapp.repo.shows

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import javax.inject.Inject

private const val TAG = "CreditsRepository"
open class CreditsRepository @Inject constructor(
    private val creditsHelper: PersonCreditsHelper,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase
) {
    private val showCastPeopleDao = creditsDatabase.showCastPeopleDao()
    private val castPersonDao = creditsDatabase.personDao()

    suspend fun getCredits(traktId: Int, tmdbId: Int?, shouldRefresh: Boolean, showGuestStars: Boolean) = networkBoundResource(
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

            try {
                Log.d(TAG, "getCredits: Refreshing Credits")
                Log.d(TAG, "getCredits: Got $castPersons")

                creditsDatabase.withTransaction {
                    showCastPeopleDao.deleteShowCast(traktId)
                }

                showsDatabase.withTransaction {
                    castPersons.map { castData ->
                        castPersonDao.insert(castData.person)
                        showCastPeopleDao.insert(castData.showCastPersonData)
                    }
                }
            } catch(e: Exception) {
                Log.e(TAG, "getCredits: Error ${e.message}", )
                e.printStackTrace()
            }


        }
    )
}