package com.nickrankin.traktapp.repo.shows.collected

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val REFRESH_INTERVAL = 24L
private const val TAG = "CollectedShowsRepositor"
class CollectedShowsRepository @Inject constructor(private val traktApi: TraktApi, private val showsDatabase: ShowsDatabase, private val sharedPreferences: SharedPreferences) {
    private val collectedShowDao = showsDatabase.collectedShowsDao()

    suspend fun getCollectedShows(shouldRefresh: Boolean) = networkBoundResource(
        query = { collectedShowDao.getCollectedShows() },
        fetch = { traktApi.tmUsers().collectionShows(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null")), Extended.FULL) },
        shouldFetch = { collectedShows ->
            shouldRefreshContents(sharedPreferences.getString(COLLECTED_SHOWS_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL) || shouldRefresh || collectedShows.isEmpty()
        },
        saveFetchResult = { baseShows ->
            showsDatabase.withTransaction {
                collectedShowDao.deleteAllShows()
                collectedShowDao.insert(convertToCollectedShows(baseShows))
            }

            sharedPreferences.edit()
                .putString(COLLECTED_SHOWS_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                .apply()
        }
    )

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

    suspend fun removeFromCollection(collectedShow: CollectedShow): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                this. shows = arrayListOf(
                    SyncShow().id(ShowIds.trakt(collectedShow.show_trakt_id))
                )
            }

            val response = traktApi.tmSync().deleteItemsFromCollection(syncItems)

            showsDatabase.withTransaction {
                collectedShowDao.delete(collectedShow)
            }

            return Resource.Success(response)
        } catch(t: Throwable) {
            Resource.Error(t, null)
        }
    }

    companion object {
        const val COLLECTED_SHOWS_LAST_REFRESHED_KEY = "collected_shows_last_refresh"
    }
}