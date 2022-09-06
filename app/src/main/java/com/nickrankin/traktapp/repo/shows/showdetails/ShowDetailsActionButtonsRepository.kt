package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.lists.TraktListsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.stats.model.CollectedShowsStats
import com.nickrankin.traktapp.dao.stats.model.RatingsShowsStats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.nickrankin.traktapp.repo.lists.ListEntryRepository
import com.nickrankin.traktapp.repo.lists.TraktListsRepository
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.RatingsFilter
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import javax.inject.Inject
import kotlin.Exception

private const val REFRESH_INTERVAL = 24L
private const val TAG = "ShowDetailsActionButton"

class ShowDetailsActionButtonsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase,
    private val listsDatabase: TraktListsDatabase,
    private val statsRepository: StatsRepository
) {
    private val collectedShowStatsDao = showsDatabase.collectedShowsStatsDao()
    private val ratedShowsStatsDao = showsDatabase.ratedShowsStatsDao()


    private val listEntryDao = listsDatabase.listEntryDao()
    val listsWithEntries = listEntryDao.getAllListEntries()




    fun getRatings(traktId: Int): Flow<RatingsShowsStats?> {
        return ratedShowsStatsDao.getRatingsStatsById(traktId)
    }

    fun getCollectedShowFlow(traktId: Int): Flow<CollectedShowsStats?> {
        return collectedShowStatsDao.getCollectedShowById(traktId)
    }

    suspend fun setRatings(
        tmShow: TmShow,
        rating: Int,
        resetRatings: Boolean
    ): Resource<SyncResponse> {
        return try {
            val syncResponse = if (!resetRatings) {
                val syncItems = getSyncItems(tmShow.trakt_id)

                // Apply new rating to syncitem
                syncItems.shows!!.first()
                    .rating = Rating.fromValue(rating)

                showsDatabase.withTransaction {
                    ratedShowsStatsDao.insert(
                        RatingsShowsStats(
                            tmShow.trakt_id,
                            tmShow.tmdb_id,
                            rating,
                            tmShow.name,
                            OffsetDateTime.now()
                        )
                    )
                }

                traktApi.tmSync().addRatings(syncItems)
            } else {

                showsDatabase.withTransaction {
                    ratedShowsStatsDao.getRatingsStatsById(tmShow.trakt_id)
                }
                traktApi.tmSync().deleteRatings(getSyncItems(tmShow.trakt_id))
            }

            // Trigger call to getRatings() to notify all active observers or rating channel
            Resource.Success(syncResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun addToCollection(tmShow: TmShow): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().addItemsToCollection(getSyncItems(tmShow.trakt_id))

            statsRepository.refreshCollectedShows()

            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun removeFromCollection(
        traktId: Int
    ): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                shows = listOf(
                    SyncShow().id(ShowIds.trakt(traktId))
                )
            }

            val result = traktApi.tmSync().deleteItemsFromCollection(syncItems)

            showsDatabase.withTransaction {
                collectedShowStatsDao.deleteCollectedStatsById(traktId)
            }

            Resource.Success(result)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    private fun getSyncItems(traktId: Int): SyncItems {
        return SyncItems().apply {
            shows = listOf(
                SyncShow().id(ShowIds.trakt(traktId))
            )
        }
    }
}