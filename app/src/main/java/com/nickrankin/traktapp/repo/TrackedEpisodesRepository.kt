package com.nickrankin.traktapp.repo

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import com.uwetrottmann.trakt5.entities.CalendarShowEntry
import com.uwetrottmann.trakt5.enums.Extended
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val DAYS_TO_TRACK_UPCOMING = 3
private const val DEFAULT_TRAKT_DATE_FORMAT = "yyyy-MM-dd"
private const val TAG = "TrackedEpisodesReposito"
open class TrackedEpisodesRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi, private val showsDatabase: ShowsDatabase) {
    private val trackedShowsDao = showsDatabase.trackedShowDao()
    private val trackedEpisodeDao = showsDatabase.trackedEpisodeDao()

    @Inject
    lateinit var trackedEpisodeAlarmScheduler: TrackedEpisodeAlarmScheduler

    suspend fun insertTrackedShow(trackedShow: TrackedShow) {
        val upcomingEpisodes = traktApi.tmCalendars().shows(OffsetDateTime.now().format(
            DateTimeFormatter.ofPattern(DEFAULT_TRAKT_DATE_FORMAT)), DAYS_TO_TRACK_UPCOMING)

        upcomingEpisodes.map { calendarShowEntry ->
            if(trackedShow.trakt_id == calendarShowEntry.show?.ids?.trakt ?: 0) {
                Log.d(TAG, "insertTrackedEpisode: Show ${calendarShowEntry.show?.title} is tracked!")
                val trackedEpisode = buildTrackedEpisode(calendarShowEntry)
                    showsDatabase.withTransaction {
                    trackedEpisodeDao.insert(trackedEpisode)
                }

                trackedEpisodeAlarmScheduler.scheduleTrackedEpisodeAlarm(trackedEpisode)
            }
        }
    }

    suspend fun cancelShowTracking(trackedShow: TrackedShow) {
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForShow(trackedShow.trakt_id).first()

            showsDatabase.withTransaction {
                trackedShowsDao.deleteTrackedShow(trackedShow)
            }

        // Now cancel the existing alarms and delete from db
        trackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)

            showsDatabase.withTransaction {
                trackedEpisodeDao.delete(trackedEpisode)
            }
        }
    }

    suspend fun refreshTrackedShows() {
        val upcomingEpisodes = traktApi.tmCalendars().shows(OffsetDateTime.now().format(
            DateTimeFormatter.ofPattern(DEFAULT_TRAKT_DATE_FORMAT)), DAYS_TO_TRACK_UPCOMING)

        val trackedEpisodes =  trackedEpisodeDao.getAllEpisodesForNotification().first()
        val trackedShowTraktIds = trackedShowsDao.getTrackedShowIds().first()

        // Cleanup the current notifications and cancel any alarms to avoid repetitions
        trackedEpisodes.map { trackedEpisode ->
            trackedEpisodeAlarmScheduler.cancelAlarm(trackedEpisode.trakt_id)
        }

        showsDatabase.withTransaction {
            trackedEpisodeDao.deleteAllEpisodesForNotification()
        }

        // update tracked episodes db and reschedule all alarms
        upcomingEpisodes.map { upcomingEpisodeEntry ->
            if(trackedShowTraktIds.contains(upcomingEpisodeEntry.show?.ids?.trakt ?: 0)) {
                Log.d(TAG, "refreshTrackedEpisodes: Show ${upcomingEpisodeEntry.show?.title} is tracked!")
                showsDatabase.withTransaction {
                    trackedEpisodeDao.insert(buildTrackedEpisode(upcomingEpisodeEntry))
                }
            }
        }

        trackedEpisodeAlarmScheduler.scheduleAllAlarms()
    }

    private fun buildTrackedEpisode(calendarShowEntry: CalendarShowEntry): TrackedEpisode {
        return TrackedEpisode(
            calendarShowEntry.episode?.ids?.trakt ?: 0,
            calendarShowEntry.episode?.ids?.tmdb ?: 0,
            calendarShowEntry.show?.ids?.trakt ?: 0,
            calendarShowEntry.show?.ids?.tmdb ?: 0,
            calendarShowEntry.show?.language,
            calendarShowEntry.first_aired!!,
            calendarShowEntry.episode?.title,
            calendarShowEntry.show?.title ?: "",
            calendarShowEntry.episode?.season ?: 0,
            calendarShowEntry.episode?.number ?: 0,
            false
        )
    }

}