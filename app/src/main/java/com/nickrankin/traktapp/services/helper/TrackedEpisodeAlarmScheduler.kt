package com.nickrankin.traktapp.services.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import kotlinx.coroutines.flow.first
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime

private const val NOTIFICATION_HOURS_BEFORE = 24L
private const val TAG = "TrackedEpisodeAlarmSche"
class TrackedEpisodeAlarmScheduler(private val context: Context, private val alarmManager: AlarmManager, private val showsDatabase: ShowsDatabase) {
    private val trackedEpisodeDao = showsDatabase.trackedEpisodeDao()
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

    suspend fun scheduleTrackedEpisodeAlarm(trackedEpisode: TrackedEpisode) {
        val notificationDateTime = trackedEpisode.airs_date?.minusHours(NOTIFICATION_HOURS_BEFORE)

        val trackedEpisodeFromDb = trackedEpisodeDao.getTrackedEpisode(trackedEpisode.trakt_id).first()

        if(trackedEpisodeFromDb == null) {
            showsDatabase.withTransaction {
                trackedEpisodeDao.insert(trackedEpisode)
            }
        }

        if(notificationDateTime == null) {
            Log.e(TAG, "scheduleTrackedEpisodeAlarm: notification date and time must not be null")
            return
        }

        // Already notified we won't reschedule alarm
        if(trackedEpisodeFromDb != null && trackedEpisodeFromDb.alreadyNotified) {
            Log.d(TAG, "scheduleTrackedEpisodeAlarm: Episode ${trackedEpisode.trakt_id} already had notification dismissed")
            return
        }

        val notificationTime = calculateNotificationTime(notificationDateTime)
        Log.d(TAG, "scheduleTrackedEpisodeAlarm: Notification time (millis ${notificationTime.toInstant().toEpochMilli()}) ${notificationTime} for ${trackedEpisode.trakt_id}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime.toInstant().toEpochMilli(),
                getPendingIntent(trackedEpisode)
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime.toInstant().toEpochMilli(),
                getPendingIntent(trackedEpisode)
            )
        }
    }

    private fun calculateNotificationTime(airsDate: OffsetDateTime): OffsetDateTime {

        // We notify our Alarm the day before show is airing
        val notificationDate = airsDate.minusDays(1)

        return when(notificationDate.hour) {

            // For shows airing earlier in the day, notifications set to 4PM
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 -> {
                notificationDate.withHour(16)
            }
            // For shows airing later in the day, notifications set to 7PM
            13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 -> {
                notificationDate.withHour(19)
            }
            else -> {
                notificationDate.withHour(16)
            }
        }

    }

    suspend fun cancelAlarm(episodeTraktId: Int) {
        val trackedEpisode = trackedEpisodeDao.getTrackedEpisode(episodeTraktId).first()

        if(trackedEpisode != null) {
            // Cancel any potential remaining alarms
            alarmManager.cancel(getPendingIntent(trackedEpisode))

        } else {
            Log.e(TAG, "cancelAlarm: Cannot cancel alarm when Traked Episode is null! (Episode Trakt Id ($episodeTraktId))", )
        }
    }

    fun cancelAlarm(trackedEpisode: TrackedEpisode) {
        Log.d(TAG, "cancelAlarm: Alarm for Episode $trackedEpisode cancelled!")
        alarmManager.cancel(getPendingIntent(trackedEpisode))
    }

    suspend fun dismissNotification(episodeTraktId: Int, notificationTapped: Boolean) {
        Log.d(TAG, "dismissNotification: Dismissed alarm for $episodeTraktId")

        val trackedEpisode = trackedEpisodeDao.getTrackedEpisode(episodeTraktId).first()

        if(trackedEpisode != null) {
            // Cancel any potential remaining alarms
            alarmManager.cancel(getPendingIntent(trackedEpisode))

            if(notificationTapped) {
                showsDatabase.withTransaction {
                    trackedEpisodeDao.setNotificationStatus(trackedEpisode.trakt_id, true)
                }
            } else {
                showsDatabase.withTransaction {
                    trackedEpisodeDao.setDismissCount(trackedEpisode.trakt_id)
                }
            }
        } else {
            Log.e(TAG, "dismissNotification: Cannot cancel alarm when Traked Episode is null! (Episode Trakt Id ($episodeTraktId))", )
        }

    }

    private fun getPendingIntent(trackedEpisode: TrackedEpisode): PendingIntent {
        val intent = Intent(context, EpisodeNotificationReceiver::class.java)
        intent.putExtra(EpisodeNotificationReceiver.TRAKT_ID_KEY, trackedEpisode)

        return PendingIntent.getBroadcast(context, trackedEpisode.trakt_id, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    suspend fun generateTestNotifications() {

        val collectedEpisodes = showsDatabase.collectedShowsDao().getCollectedShows().first()

        val rand1 = collectedEpisodes.random()
        Log.e(TAG, "generateTestNotifications: all ${collectedEpisodes.size} $rand1", )
        val rand2 = collectedEpisodes.random()
        Log.e(TAG, "generateTestNotifications: $rand2", )

        val rand3 = collectedEpisodes.random()
        val rand4 = collectedEpisodes.random()
        val rand5 = collectedEpisodes.random()

        val testTrackingEpisodes = listOf(
//                    TrackedEpisode(
//                        rand1.show_trakt_id, 0,0,0, OffsetDateTime.now().plusMinutes(2),
//                        null,"Episode 6",rand1.show_title ?: "empty",4,6, OffsetDateTime.now(),0,false
//                    ),
//                    TrackedEpisode(
//                        rand2.show_trakt_id, 0,rand2.show_trakt_id,0, OffsetDateTime.now().plusDays(4).plusHours(6L),
//                        null,"Episode 6",rand2.show_title ?: "empty",4,6, OffsetDateTime.now(),0,false
//                    ),
//                    TrackedEpisode(
//                        rand3.show_trakt_id, 0, rand3.show_trakt_id,0,
//                        OffsetDateTime.now().plusDays(5).plusHours(9L),null,"Episode 7",rand3.show_title ?: "empty",4,7,
//                        OffsetDateTime.now(),0,false
//                    ),
//                    TrackedEpisode(
//                        rand4.show_trakt_id, 0,rand4.show_trakt_id,0,
//                        OffsetDateTime.now().plusDays(6).plusHours(5L),null,"Episode 8",rand4.show_title ?: "empty",4,8,
//                        OffsetDateTime.now(),0,false),
                    TrackedEpisode(
                        887343,3501517,42221,42445, OffsetDateTime.now().plusMinutes(2),
                        null,"Episode 1","Borgen",3,1,OffsetDateTime.now(),0,false
                    ),
                    TrackedEpisode(
                        887346,3501519,42221,42445, OffsetDateTime.now().plusDays(4).plusHours(6L),null,"Episode 2","Borgen",3,4,OffsetDateTime.now(),0,false
                    ),
                    TrackedEpisode(
                        887349,3501520,42221,42445,OffsetDateTime.now().plusDays(2L),null,"Episode 7","Borgen",3,7,OffsetDateTime.now(),0,false
                    )
                )

            testTrackingEpisodes.map { ep ->
                scheduleTrackedEpisodeAlarm(ep)
            }
    }
}