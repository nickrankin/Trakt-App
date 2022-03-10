package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.nickrankin.traktapp.repo.shows.collected.CollectedShowsRepository
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
    private val showsDatabase: ShowsDatabase
) {
    private val collectedShowDao = showsDatabase.collectedShowsDao()

    suspend fun getRatings(): Resource<List<RatedShow>> {
        return try {
            val ratingsResponse = traktApi.tmUsers().ratingsShows(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "null"
                    )
                ), RatingsFilter.ALL, null
            )
            Resource.Success(ratingsResponse)
        } catch (e: HttpException) {
            Log.e(
                TAG,
                "getRatings: Error getting Rating. HTTP Error code ${e.code()}. ${e.localizedMessage}",
            )
            e.printStackTrace()
            Resource.Error(e, null)
        } catch (e: Exception) {
            Log.e(TAG, "getRatings: Error getting rating! ${e.localizedMessage}")
            e.printStackTrace()
            Resource.Error(e, null)
        }
    }

    suspend fun refreshCollectedStatus(forceRefresh: Boolean) {
        // Refresh Collected every 24 hours, or force it
        if (shouldRefreshContents(
                sharedPreferences.getString(
                    CollectedShowsRepository.COLLECTED_SHOWS_LAST_REFRESHED_KEY,
                    ""
                ) ?: "", REFRESH_INTERVAL
            ) || forceRefresh
        ) {
            try {
                val baseShows = traktApi.tmUsers().collectionShows(
                    UserSlug(
                        sharedPreferences.getString(
                            AuthActivity.USER_SLUG_KEY,
                            "null"
                        )
                    ), Extended.FULL
                )

                showsDatabase.withTransaction {
                    collectedShowDao.deleteAllShows()
                    collectedShowDao.insert(convertToCollectedShows(baseShows))
                }

                sharedPreferences.edit()
                    .putString(
                        CollectedShowsRepository.COLLECTED_SHOWS_LAST_REFRESHED_KEY,
                        OffsetDateTime.now().toString()
                    )
                    .apply()

            } catch (e: HttpException) {
                Log.e(
                    TAG,
                    "refreshCollectedStatus: Error getting CollectedShows. Code ${e.code()}. ${e.localizedMessage}",
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun getCollectedShowFlow(traktId: Int): Flow<CollectedShow?> {
        return collectedShowDao.getCollectedShow(traktId)
    }

    private fun convertToCollectedShows(baseShows: List<BaseShow>): List<CollectedShow> {
        val collectedShows: MutableList<CollectedShow> = mutableListOf()

        baseShows.map { baseShow ->
            collectedShows.add(
                CollectedShow(
                    baseShow.show?.ids?.trakt ?: 0,
                    baseShow.show?.ids?.tmdb ?: 0,
                    baseShow.show?.language,
                    baseShow.last_collected_at,
                    baseShow.last_updated_at,
                    baseShow.last_watched_at,
                    baseShow.listed_at,
                    baseShow.seasons?.size ?: 0,
                    baseShow.plays ?: 0,
                    baseShow.show?.overview,
                    baseShow.show?.first_aired,
                    baseShow.show?.runtime,
                    baseShow.show?.status ?: Status.CANCELED,
                    baseShow.show?.title ?: "",
                    false
                )
            )
        }

        return collectedShows
    }


    suspend fun setRatings(
        traktId: Int,
        rating: Int,
        resetRatings: Boolean
    ): Resource<SyncResponse> {
        return try {
            val syncResponse = if (!resetRatings) {
                val syncItems = getSyncItems(traktId)

                // Apply new rating to syncitem
                syncItems.shows!!.first()
                    .rating = Rating.fromValue(rating)

                traktApi.tmSync().addRatings(syncItems)
            } else {
                traktApi.tmSync().deleteRatings(getSyncItems(traktId))
            }

            // Trigger call to getRatings() to notify all active observers or rating channel
            Resource.Success(syncResponse)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun addToCollection(traktId: Int): Resource<SyncResponse> {
        return try {
            val result = traktApi.tmSync().addItemsToCollection(getSyncItems(traktId))

            // Refresh the users collected Shows
            refreshCollectedStatus(true)

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
                collectedShowDao.deleteShowById(traktId)
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