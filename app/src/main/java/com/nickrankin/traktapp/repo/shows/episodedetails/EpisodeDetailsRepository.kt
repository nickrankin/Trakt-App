package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.uwetrottmann.trakt5.entities.*
import kotlinx.coroutines.flow.first
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "EpisodeDetailsRepositor"
class EpisodeDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showDataHelper: ShowDataHelper,
    private val sharedPreferences: SharedPreferences,
    private val creditsHelper: PersonCreditsHelper,
    private val creditsDatabase: CreditsDatabase,
    private val showsDatabase: ShowsDatabase): CreditsRepository(creditsHelper, showsDatabase, creditsDatabase) {

    private val episodesDao = showsDatabase.TmEpisodesDao()
    private val showsDbWatchedHistoryShowsDao = showsDatabase.watchedEpisodesDao()
    private val personDao = creditsDatabase.personDao()
    private val showCastPeopleDao = creditsDatabase.showCastPeopleDao()

    suspend fun getCast(episodeDataModel: EpisodeDataModel?, shouldRefresh: Boolean) {
        if(episodeDataModel ==  null) {
            return
        }
        val credits = showCastPeopleDao.getShowCast(episodeDataModel.showTraktId, false).first()

        if(shouldRefresh || credits.isEmpty()) {
            val creditsResponse = creditsHelper.getShowCredits(episodeDataModel.showTraktId , episodeDataModel.tmdbId)

            Log.d(TAG, "refreshCast: Refreshing Credits")

            creditsDatabase.withTransaction {
                showCastPeopleDao.deleteShowCast(episodeDataModel?.showTraktId ?: 0)
            }

            showsDatabase.withTransaction {
                creditsResponse.map { castData ->
                    personDao.insert(castData.person)
                    showCastPeopleDao.insert(castData.showCastPersonData)
                }
            }

        }

    }

    suspend fun getEpisodes(
        showTraktId: Int,
        showTmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
        shouldRefresh: Boolean
    ) = networkBoundResource(
        query = {
            episodesDao.getEpisode(showTraktId, seasonNumber, episodeNumber)
        },
        fetch = {
            showDataHelper.getSeasonEpisodesData(showTraktId, showTmdbId, seasonNumber, null)
        },
        shouldFetch = { episode ->
            shouldRefresh || episode == null
        },
        saveFetchResult = { episodes ->
            showsDatabase.withTransaction {
                episodesDao.deleteEpisodes(showTraktId, seasonNumber)

                episodesDao.insert(episodes)
            }
        }
    )

    suspend fun removeWatchedEpisode(syncItems: SyncItems): Resource<SyncResponse> {
        return try {
            val response = traktApi.tmSync().deleteItemsFromWatchedHistory(syncItems)

            val historyId = syncItems.ids?.first() ?: 0L

            /// Clean up the databases
            showsDatabase.withTransaction {
                showsDbWatchedHistoryShowsDao.deleteWatchedEpisodeById(historyId)
            }

            // Ensure Watched History pager gets refreshed on next call if we remove a play
            sharedPreferences.edit()
                .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
                .apply()

            Resource.Success(response)
        } catch (e: Throwable) {
            Resource.Error(e, null)
        }
    }

    companion object {
        const val SHOULD_REFRESH_WATCHED_KEY = "should_refresh_watched"
    }
}