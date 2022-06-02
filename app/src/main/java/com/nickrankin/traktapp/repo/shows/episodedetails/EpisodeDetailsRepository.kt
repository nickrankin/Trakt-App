package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.helper.*
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "EpisodeDetailsRepositor"

class EpisodeDetailsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showDataHelper: ShowDataHelper,
    private val sharedPreferences: SharedPreferences,
    private val creditsHelper: PersonCreditsHelper,
    private val showCreditsDatabase: CreditsDatabase,
    private val showsDatabase: ShowsDatabase): CreditsRepository(creditsHelper, showsDatabase, showCreditsDatabase) {

    private val episodesDao = showsDatabase.TmEpisodesDao()
    private val showsDbWatchedHistoryShowsDao = showsDatabase.watchedEpisodesDao()
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