package com.nickrankin.traktapp.repo.shows.showdetails

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
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

    suspend fun getCast(showTraktId: Int, showTmdbId: Int?, showGuestStars: Boolean, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            showCastPeopleDao.getShowCast(showTraktId, showGuestStars)
        },
        fetch = {
            creditsHelper.getShowCredits(showTraktId, showTmdbId)
        },
        shouldFetch = { castPersons ->
            shouldRefresh || castPersons.isEmpty()

        },
        saveFetchResult = { showCastPersons ->
            showsDatabase.withTransaction {
                showCastPeopleDao.insert(showCastPersons)
            }
        }
    )

    suspend fun getEpisodeCast(episodeDataModel: EpisodeDataModel?, showGuestStars: Boolean, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            showCastPeopleDao.getShowCast(episodeDataModel?.showTraktId ?: 0, showGuestStars)
        },
        fetch = {
            creditsHelper.getShowCredits(episodeDataModel?.showTraktId ?: 0 , episodeDataModel?.tmdbId)
        },
        shouldFetch = { castPersons ->
            shouldRefresh || castPersons.isEmpty()

        },
        saveFetchResult = { showCastPersons ->
            Log.d(TAG, "getEpisodeCast: Refreshing Episode Cast")
            showsDatabase.withTransaction {
                showCastPeopleDao.deleteShowCast(episodeDataModel?.showTraktId ?: 0)
                showCastPeopleDao.insert(showCastPersons)
            }

            val guestStars = creditsHelper.getEpisodeGuestCredits(episodeDataModel?.showTraktId ?: 0, episodeDataModel?.tmdbId ?: 0, episodeDataModel?.seasonNumber ?: 0, episodeDataModel?.episodeNumber ?: 0)
            showsDatabase.withTransaction {
                showCastDao.insert(guestStars)
            }

        }
    )

    fun getShow(traktId: Int) = showsDao.getShow(traktId)


}