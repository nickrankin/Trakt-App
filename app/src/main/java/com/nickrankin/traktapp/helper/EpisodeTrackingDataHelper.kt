package com.nickrankin.traktapp.helper

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TmShow
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.uwetrottmann.trakt5.entities.Episode
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

private const val REFRESH_INTERVAL = 24L
private const val TAG = "EpisodeTrackingDataHelp"
@Singleton
class EpisodeTrackingDataHelper @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private val showDataHelper: ShowDataHelper,
    private val trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler
) {
    private val trackedShowDao = showsDatabase.trackedShowDao()
    private val trackedEpisodeDao = showsDatabase.trackedEpisodeDao()

    suspend fun refreshUpComingEpisodesForAllShows() {
        Log.d(TAG, "refreshUpComingEpisodesForAllShows: Refreshing all shows")
        val trackedShows = trackedShowDao.getTrackedShows().first()

        trackedShows.map { trackedShow ->
            Log.d(TAG, "refreshUpComingEpisodesForAllShows: Refresh called for Show ${trackedShow.title}")
                Log.d(TAG, "refreshUpComingEpisodesForAllShows: Refreshing Show episodes ${trackedShow.title}")
                val response = refreshUpcomingEpisodesforShow(trackedShow.trakt_id)

                if(response is Resource.Success) {
                    Log.d(TAG, "refreshUpComingEpisodesForAllShows: Successfully refreshed episodes for Show ${trackedShow.title}")
                } else if(response is Resource.Error) {
                    Log.e(TAG, "refreshUpComingEpisodesForAllShows: Error refreshing episodes for Show ${trackedShow.title}. Error code ${response.error?.message}", )
                    response.error?.printStackTrace()
                } else {

                }

        }
    }

    suspend fun refreshUpcomingEpisodesforShow(showTraktId: Int): Resource<List<TrackedEpisode?>> {
        return try {
            // Get show info
            val show = showDataHelper.getShow(showTraktId)

            Log.d(TAG, "storeUpcomingEpisodes: Got show ${show?.name}")

            // Get all of shows episodes and filter those upcoming after current date and time
            val upcomingEpisodesResponse =
                traktApi.tmSeasons().summary(showTraktId.toString(), Extended.FULLEPISODES)
                    .map { season ->
                        season.episodes
                    }.flatMap { episodes ->
                        episodes.filter { episode ->
                            episode?.first_aired?.isAfter(OffsetDateTime.now()) ?: false
                        }
                    }

            Log.d(
                TAG,
                "storeUpcomingEpisodes: Got ${upcomingEpisodesResponse.size} Upcoming Episodes"
            )
            val trackedEpisodes = buildTrackedEpisodes(show, upcomingEpisodesResponse)

            // Update Tracked Episodes db
            showsDatabase.withTransaction {
                trackedEpisodeDao.insert(trackedEpisodes)
            }

            // Schedule alarms for these episodes
            scheduleAlarms(trackedEpisodes)

            Resource.Success(trackedEpisodes)
        } catch (e: Throwable) {
            Resource.Error(e, null)
        }
    }

    suspend fun cancelTrackingPerShow(traktShowId: Int) {
        Log.d(TAG, "cancelTrackingPerShow: Cancelling tracking for show $traktShowId")
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForShow(traktShowId).first()
        Log.d(TAG, "cancelTrackingPerShow: Cancelling $trackedEpisodes alarms and tracking")

        // Cancel the alarms
        trackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)
        }

        // Delete the tracked episodes
        showsDatabase.withTransaction {
            trackedEpisodeDao.deleteAllPerShow(traktShowId)
        }
    }

    suspend fun cancelTrackingForAllShows(keepShows: Boolean) {
        Log.d(TAG, "cancelTrackingForAllShows: Turning off Show Tracking")

        //Get all the TrackedEpisodes
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForNotification().first()

        // Cancel any pending alarms
        trackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)
        }

        // Clean up
        showsDatabase.withTransaction {
            if(!keepShows) {
                trackedShowDao.deleteAllTrackedShows()
            }
            trackedEpisodeDao.deleteAllEpisodesForNotification()
        }

    }

    private fun buildTrackedEpisodes(show: TmShow?, episodes: List<Episode>): List<TrackedEpisode> {
        val trackedEpisodes: MutableList<TrackedEpisode> = mutableListOf()

        episodes.map { episode ->
            trackedEpisodes.add(
                TrackedEpisode(
                    episode.ids?.trakt ?: 0,
                    episode.ids?.tmdb ?: 0,
                    show?.trakt_id ?: -1,
                    show?.tmdb_id,
                    episode.first_aired,
                    show?.networks?.map { network -> network?.name } ?: emptyList(),
                    episode.title,
                    show?.name ?: "",
                    episode.season ?: 0,
                    episode.number ?: 0,
                    OffsetDateTime.now(),
                false)
            )
        }

        return trackedEpisodes
    }

    private fun scheduleAlarms(trackedEpisodes: List<TrackedEpisode>) {
        trackedEpisodes.map { trackedEpisode ->
            Log.d(TAG, "scheduleAlarms: Scheduling alarm for ${trackedEpisode.title} S${trackedEpisode.season}E${trackedEpisode.episode}. Airing ${trackedEpisode.airs_date}")
            trackedEpisodeAlarmScheduler.scheduleTrackedEpisodeAlarm(trackedEpisode)
        }
    }
}