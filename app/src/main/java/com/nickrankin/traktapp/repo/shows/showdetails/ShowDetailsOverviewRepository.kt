package com.nickrankin.traktapp.repo.shows.showdetails

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.model.datamodel.ShowDataModel
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import kotlinx.coroutines.flow.Flow
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
                showCastPeopleDao.deleteShowCast(showTraktId)
                showCastPeopleDao.insert(showCastPersons)
            }
        }
    )

    suspend fun getEpisodeCast(episodeDataModel: EpisodeDataModel?, showGuestStars: Boolean, shouldRefresh: Boolean) = networkBoundResource(
        query = {
            showCastPeopleDao.getShowCast(episodeDataModel?.traktId ?: 0, showGuestStars)
        },
        fetch = {
            Log.e(TAG, "getEpisodeCast: edm $episodeDataModel")
            creditsHelper.getShowCredits(episodeDataModel?.traktId ?: 0 , episodeDataModel?.tmdbId)
        },
        shouldFetch = { castPersons ->
            shouldRefresh || (castPersons.isEmpty() && !showGuestStars) || showGuestStars
        },
        saveFetchResult = { showCastPersons ->
            Log.d(TAG, "getEpisodeCast: Refreshing Episode Cast for episode ${episodeDataModel?.traktId}")

            val guestStars = creditsHelper.getEpisodeCredits(episodeDataModel?.traktId ?: 0, episodeDataModel?.tmdbId ?: 0, episodeDataModel?.seasonNumber ?: 0, episodeDataModel?.episodeNumber ?: 0)
            showsDatabase.withTransaction {
                showCastDao.insert(showCastPersons)
                showCastDao.insert(guestStars)
            }
        }
    )

    fun getShow(traktId: Int) = showsDao.getShow(traktId)


}