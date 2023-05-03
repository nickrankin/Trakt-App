package com.nickrankin.traktapp.services.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import kotlinx.coroutines.flow.first

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

        Log.d(TAG, "scheduleAllAlarms: Scheduling ${trackedEpisodes.size} alarms")

        trackedEpisodes.sortedBy { it.airs_date }.reversed().map { trackedEpisode ->
            scheduleTrackedEpisodeAlarm(trackedEpisode)
        }
    }

    fun scheduleTrackedEpisodeAlarm(trackedEpisode: TrackedEpisode) {
        val notificationDateTime = trackedEpisode.airs_date?.minusHours(NOTIFICATION_HOURS_BEFORE)

        if(notificationDateTime == null) {
            Log.e(TAG, "scheduleTrackedEpisodeAlarm: notification date and time must not be null")
            return
        }

        // Already notified we won't reschedule alarm
        if(trackedEpisode.alreadyNotified) {
            Log.d(TAG, "scheduleTrackedEpisodeAlarm: Episode ${trackedEpisode.trakt_id} already had notification dismissed")
            return
        }

        val notificationTimeMillis = notificationDateTime.toInstant().toEpochMilli()
        Log.d(TAG, "scheduleTrackedEpisodeAlarm: Notification time (millis) $notificationTimeMillis for ${trackedEpisode.trakt_id}")

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            notificationTimeMillis,
            getPendingIntent(trackedEpisode.trakt_id)
        )
    }

    fun cancelAlarm(episodeTraktId: Int) {
        Log.d(TAG, "cancelAlarm: Alarm for Episode $episodeTraktId cancelled!")
        alarmManager.cancel(getPendingIntent(episodeTraktId))
    }
    
    suspend fun dismissNotification(episodeTraktId: Int, notificationTapped: Boolean) {
        Log.d(TAG, "dismissNotification: Dismissed alarm for $episodeTraktId")
        // Cancel any potential remaining alarms
        alarmManager.cancel(getPendingIntent(episodeTraktId))
        
        if(notificationTapped) {
            showsDatabase.withTransaction {
                trackedEpisodeDao.setNotificationStatus(episodeTraktId, true)
            }
        } else {
            showsDatabase.withTransaction {
                trackedEpisodeDao.setDismissCount(episodeTraktId)
            }
        }

    }

    private fun getPendingIntent(episodeTraktId: Int): PendingIntent {
        val intent = Intent(context, EpisodeNotificationReceiver::class.java)
        intent.putExtra(EpisodeNotificationReceiver.TRAKT_ID_KEY, episodeTraktId)

        return PendingIntent.getBroadcast(context, episodeTraktId, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}