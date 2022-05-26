package com.nickrankin.traktapp.repo.shows.episodedetails

import android.content.SharedPreferences

import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.model.TmEpisode
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.RatingsFilter
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "EpisodeDetailsActionBut"
class EpisodeDetailsActionButtonsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val episodeDetailsRepository: EpisodeDetailsRepository,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun getRatings(): Resource<List<RatedEpisode>> {
        return try {
            val response = traktApi.tmUsers().ratingsEpisodes(
                UserSlug(
                    sharedPreferences.getString(
                        AuthActivity.USER_SLUG_KEY,
                        "NULL"
                    )
                ), RatingsFilter.ALL, null
            )

            Resource.Success(response)

        } catch (e: Throwable) {
            e.printStackTrace()
            Resource.Error(e, null)
        }
    }

    suspend fun addRatings(newRating: Int, episodeTraktId: Int): Resource<Pair<SyncResponse, Int>> {
        return try {
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode().rating(
                        Rating.fromValue(newRating)
                    )
                        .id(EpisodeIds.trakt(episodeTraktId))
                )
            }

            val response = traktApi.tmSync().addRatings(syncItems)

            Resource.Success(Pair(response, newRating))

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun resetRating(episodeTraktId: Int): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                    episodes = listOf(
                        SyncEpisode().id(EpisodeIds.trakt(episodeTraktId))
                    )
                }

            val response = traktApi.tmSync().deleteRatings(syncItems)


            Resource.Success(response)
        } catch(t: Throwable) {

            Resource.Error(t, null)
        }
    }

    suspend fun checkin(episodeTraktId: Int): Resource<EpisodeCheckinResponse?> {

        return try {
            val episodeCheckin = EpisodeCheckin.Builder(
                SyncEpisode().id(EpisodeIds.trakt(episodeTraktId)),
                AppConstants.APP_VERSION,
                AppConstants.APP_DATE
            ).build()

            val response = traktApi.tmCheckin().checkin(episodeCheckin)

            Resource.Success(response)

        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }

    suspend fun deleteActiveCheckin(): Resource<Boolean> {

        return try {
            traktApi.tmCheckin().deleteActiveCheckin()

            Resource.Success(true)

        } catch (t: Throwable) {
            Resource.Error(t, false)
        }
    }

    suspend fun addToWatchedHistory(episode:TmEpisode, watchedDate: OffsetDateTime): Resource<SyncResponse> {
        return try {
            val syncItems = SyncItems().apply {
                episodes = listOf(
                    SyncEpisode()
                        .id(EpisodeIds.trakt(episode.episode_trakt_id))
                        .watchedAt(watchedDate)
                )
            }

            val response = traktApi.tmSync().addItemsToWatchedHistory(syncItems)

            // TODO update watched history in DB

            // Force the update of the last watched pager
            sharedPreferences.edit()
                .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
                .apply()

            Resource.Success(response)
        } catch (t: Throwable) {
            Resource.Error(t, null)
        }
    }


}