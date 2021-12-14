package com.nickrankin.traktapp.services.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nickrankin.traktapp.MainActivity
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.uwetrottmann.trakt5.entities.CalendarShowEntry
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val NOTIFICATION_HOURS_BEFORE = 24L
private const val TAG = "TrackedEpisodeAlarmSche"
class TrackedEpisodeAlarmScheduler(private val context: Context, private val alarmManager: AlarmManager, private val showsDatabase: ShowsDatabase) {
    private val trackedEpisodeDao = showsDatabase.trackedEpisodeDao()

    init {
//        alarmManager.set(
//            AlarmManager.RTC_WAKEUP,
//            OffsetDateTime.now().plusSeconds(30).toInstant().toEpochMilli(),
//            getPendingIntent(12456)
//        )
    }

    suspend fun scheduleAllAlarms() {
        val trackedEpisodes = trackedEpisodeDao.getAllEpisodesForNotification().first()

        if(trackedEpisodes.isEmpty()) {
            Log.d(TAG, "scheduleAllAlarms: No alarms to schedule")
            return
        }

        trackedEpisodes.map { trackedEpisode ->
            scheduleTrackedEpisodeAlarm(trackedEpisode)
        }
    }

    fun scheduleTrackedEpisodeAlarm(trackedEpisode: TrackedEpisode) {
        val notificationDateTime = trackedEpisode.airs_date.minusHours(NOTIFICATION_HOURS_BEFORE)

        if(notificationDateTime == null) {
            Log.e(TAG, "scheduleTrackedEpisodeAlarm: notification date and time must not be null", )
            return
        }

        val notificationTimeMillis = notificationDateTime.toInstant().toEpochMilli()

        Log.d(TAG, "scheduleTrackedEpisodeAlarm: Notification time (millis) $notificationTimeMillis")

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            notificationTimeMillis,
            getPendingIntent(trackedEpisode.trakt_id)
        )
    }

    fun cancelAlarm(episodeTraktId: Int) {
        Log.d(TAG, "cancelAlarm: Alarm for Episode $episodeTraktId cancelled!")
        alarmManager.cancel(getPendingIntent(episodeTraktId))
    }

    private fun getPendingIntent(episodeTraktId: Int): PendingIntent {
        val intent = Intent(context, EpisodeNotificationReceiver::class.java)
        intent.putExtra(EpisodeNotificationReceiver.TRAKT_ID_KEY, episodeTraktId)

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}