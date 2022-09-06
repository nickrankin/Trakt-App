package com.nickrankin.traktapp.repo.shows.showdetails

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "ShowDetailsOverviewRepo"
class ShowDetailsOverviewRepository @Inject constructor(
    private val personCreditsHelper: PersonCreditsHelper,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase,
    private val creditsHelper: PersonCreditsHelper
): CreditsRepository(personCreditsHelper, showsDatabase, creditsDatabase) {
    private val showsDao = showsDatabase.tmShowDao()

    private val showCastPeopleDao = creditsDatabase.showCastPeopleDao()
    private val castPersonDao = creditsDatabase.personDao()
    private val showCastDao = creditsDatabase.showCastPeopleDao()

    fun getShow(showDataModel: ShowDataModel?): Flow<TmShow?> {
        return showsDao.getShow(showDataModel?.traktId ?: 0)
    }

    fun getShowCast(traktId: Int, showGuestStars: Boolean) = showCastDao.getShowCast(traktId, showGuestStars)

    suspend fun getCredits(showDataModel: ShowDataModel?, shouldRefresh: Boolean): Flow<List<ShowCastPerson>> {
        val castMembers = showCastPeopleDao.getShowCast(showDataModel?.traktId ?: 0, false)

        if(shouldRefresh || castMembers.first().isEmpty()) {
            val castResponse = creditsHelper.getShowCredits(showDataModel?.traktId ?: -1, showDataModel?.tmdbId ?: -1)

            Log.d(TAG, "getCredits: Refreshing Credits")

            creditsDatabase.withTransaction {
                showCastPeopleDao.deleteShowCast(showDataModel?.traktId ?: 0)
            }

            showsDatabase.withTransaction {
                castResponse.map { castData ->
                    castPersonDao.insert(castData.person)
                    showCastPeopleDao.insert(castData.showCastPersonData)
                }
            }
        }
        return castMembers
    }

    fun getShow(traktId: Int) = showsDao.getShow(traktId)


}