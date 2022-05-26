package com.nickrankin.traktapp.services.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.helper.convertToHumanReadableTime
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.episodedetails.EpisodeDetailsRepository
import com.nickrankin.traktapp.services.CancelShowTrackingNotificationReceiver
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsActivity
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import javax.inject.Singleton

private const val TAG = "TrackedEpisodeNotificat"
@Singleton
class TrackedEpisodeNotificationsBuilder(private val context: Context) {
    private var sharedPreferences: SharedPreferences
    private var groupSummaryNotification: Notification

    private var notificationSummaryDisplay = false

    init {
        Log.d(TAG, "Creating instance: ")

        // Create the notification channel
        createNotificationChannel()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        groupSummaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trakt_white_svgrepo_com)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("Upcoming Episodes"))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()
    }

    fun buildNotification(trackedEpisode: TrackedEpisode) {

        if(trackedEpisode.alreadyNotified || OffsetDateTime.now().isAfter(trackedEpisode.airs_date)) {
            // No notification for already dismissed notifications one ones expired ...
            return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trakt_white_svgrepo_com)
            .setContentTitle("${trackedEpisode.show_title}")
            .setContentText("Airing ${trackedEpisode.show_title} - Upcoming episode: ${trackedEpisode.title} (${convertToHumanReadableTime(trackedEpisode.airs_date!!)})")
            .setContentIntent(getPendingIntent(trackedEpisode))
            .setDeleteIntent(getCancelPendingIntent(trackedEpisode))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Airing ${trackedEpisode.show_title} - ${trackedEpisode.title} - ${convertToHumanReadableTime(trackedEpisode.airs_date!!)}"))
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        displayAsSummaryGroup()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(trackedEpisode.trakt_id, builder.build())
        }
    }

    private fun displayAsSummaryGroup() {
        synchronized(this) {
            // Create one group
            if(!notificationSummaryDisplay) {
                Log.d(TAG, "buildNotification: Creating Summary Group")
                with(NotificationManagerCompat.from(context)) {
                    notify(SUMMARY_ID, groupSummaryNotification)
                }

                notificationSummaryDisplay = true
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPendingIntent(trackedEpisode: TrackedEpisode): PendingIntent {
        val intent = Intent(context, EpisodeDetailsActivity::class.java)

        intent.putExtra(EpisodeDetailsActivity.EPISODE_DATA_KEY,
            EpisodeDataModel(
                trackedEpisode.show_trakt_id,
                trackedEpisode.show_tmdb_id,
                trackedEpisode.season,
                trackedEpisode.episode,
                trackedEpisode.show_title
            )
        )

        // Extra params to handle Notification dismissal in EpisodeDetailsActivity
        intent.putExtra(FROM_NOTIFICATION_TAP, true)
        intent.putExtra(EPISODE_TRAKT_ID, trackedEpisode.trakt_id)

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(trackedEpisode.trakt_id, PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }

    private fun getCancelPendingIntent(trackedEpisode: TrackedEpisode): PendingIntent {
        Log.d(TAG, "getCancelPendingIntent: Notification dismissed")

        val intent = Intent(context, CancelShowTrackingNotificationReceiver::class.java)
        intent.putExtra(DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID, trackedEpisode.trakt_id)

        return PendingIntent.getBroadcast(context, trackedEpisode.trakt_id, intent, 0)
    }


    companion object {
        const val CHANNEL_ID = "episodes_notifications_channel"
        const val FROM_NOTIFICATION_TAP = "notification_tapped_episode"
        const val DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID = "dismissed_trakt_episode_id"
        const val EPISODE_TRAKT_ID = "notification_trakt_episode_id"
        const val CHANNEL_NAME = "Upcoming Episodes Notifications"
        const val CHANNEL_DESCRIPTION = "Upcoming Episodes Notifications"
        const val GROUP_KEY = "com.nickrankin.traktapp.notifications.shows.upcoming"
        const val SUMMARY_ID = 0
    }
}