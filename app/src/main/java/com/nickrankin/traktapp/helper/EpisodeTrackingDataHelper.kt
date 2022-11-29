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

    suspend fun refreshUpComingEpisodesForAllShows(): List<TrackedEpisode> {
        Log.d(TAG, "refreshUpComingEpisodesForAllShows: Refreshing all shows")
        val trackedShows = trackedShowDao.getTrackedShows().first()
        val trackedEpisodes: MutableList<TrackedEpisode> = mutableListOf()

            trackedShows.map { trackedShow ->
                trackedEpisodes.addAll(getTrackedEpisodesPerShow(trackedShow.trakt_id))
            }

        return trackedEpisodes
    }

    suspend fun getTrackedEpisodesPerShow(showTraktId: Int): List<TrackedEpisode> {
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
                "storeUpcomingEpisodes: Got ${upcomingEpisodesResponse.size} Upcoming Episodes for ${show?.name}"
            )
            val trackedEpisodes = buildTrackedEpisodes(show, upcomingEpisodesResponse)

            // Schedule alarms for these episodes
            scheduleAlarms(trackedEpisodes)
            return trackedEpisodes
    }

    suspend fun refreshUpcomingEpisodesPerShow(showTraktId: Int): Resource<TmShow?> {
        return try {
            val show = showDataHelper.getShow(showTraktId)

            val showEpisodes = getTrackedEpisodesPerShow(showTraktId)

            showsDatabase.withTransaction {
                trackedEpisodeDao.insert(showEpisodes)
            }

            Resource.Success(show)
        } catch(t: Throwable) {
            Resource.Error(t, null)
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

    suspend fun cancelTrackingForAllShows(keepShows: Boolean, keepEpisodes: Boolean) {
        Log.d(TAG, "cancelTrackingForAllShows: Turning off Show Tracking")

        //Get all the TrackedEpisodes
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForNotification().first()

        // Cancel any pending alarms
        trackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)
        }

        // Clean up
        showsDatabase.withTransaction {
            // If user switches off Tracking (e.g: in settings) keep Tracked Shows so if enabled again tracked show data will be present
            if(!keepShows) {
                trackedShowDao.deleteAllTrackedShows()
            }
            // In situation we refresh TrackedShows, don't delete already tracked to avoid nuisance alerts (ones already dismissed)
            if(!keepEpisodes) {
                trackedEpisodeDao.deleteAllEpisodesForNotification()
            }
        }

    }

    suspend fun removeExpiredTrackedEpisodesPerShow(showTraktId: Int) {
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForShow(showTraktId).first()
        val expiredTrackedEpisodes = trackedEpisodes.filter { it?.airs_date?.isBefore(OffsetDateTime.now()) ?: false}

        // Cancel any remaining alarms
        expiredTrackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)
        }

        // Remove the tracked episodes
        showsDatabase.withTransaction {
            trackedEpisodeDao.deleteAll(expiredTrackedEpisodes)
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
                    show?.network,
                    episode.title,
                    show?.name ?: "",
                    episode.season ?: 0,
                    episode.number ?: 0,
                    OffsetDateTime.now(),
                    0,
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