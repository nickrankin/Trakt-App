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
import com.nickrankin.traktapp.ui.shows.ShowsMainActivity
import com.nickrankin.traktapp.ui.shows.episodedetails.EpisodeDetailsFragment
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import javax.inject.Singleton

private const val TAG = "TrackedEpisodeNotificat"
@Singleton
class TrackedEpisodeNotificationsBuilder(private val context: Context) {
    private var sharedPreferences: SharedPreferences

    init {
        Log.d(TAG, "Creating instance: ")

        // Create the notification channel
        createNotificationChannel()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun buildNotification(trackedEpisode: TrackedEpisode) {

        if(trackedEpisode.alreadyNotified || OffsetDateTime.now().isAfter(trackedEpisode.airs_date)) {
            // No notification for already dismissed notifications one ones expired ...
            Log.d(TAG, "buildNotification: Notfication not shown as alrwady notified or it has already aired $trackedEpisode")
            return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trakt_white_svgrepo_com)
            .setContentTitle("Upcoming Episode - ${trackedEpisode.show_title}")
            .setContentText("Season: ${ trackedEpisode.season } Episode: ${ trackedEpisode.episode } (Airing in: ${convertToHumanReadableTime(trackedEpisode.airs_date!!)})")
            .setContentIntent(getPendingIntent(trackedEpisode))
            .setDeleteIntent(getCancelPendingIntent(trackedEpisode))
//            .setStyle(NotificationCompat.BigTextStyle()
//                .bigText("Airing ${trackedEpisode.show_title} - ${trackedEpisode.title} - ${convertToHumanReadableTime(
//                    trackedEpisode.airs_date
//                )}"))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(trackedEpisode.trakt_id, builder.build())
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
        val intent = Intent(context, ShowsMainActivity::class.java)

        intent.putExtra(EpisodeDetailsFragment.EPISODE_DATA_KEY,
            EpisodeDataModel(
                trackedEpisode.show_trakt_id,
                trackedEpisode.show_tmdb_id,
                trackedEpisode.season,
                trackedEpisode.episode,
                trackedEpisode.show_title
            )
        )

        intent.putExtra(EpisodeDetailsFragment.CLICK_FROM_NOTIFICATION_KEY, true)

        // Extra params to handle Notification dismissal in EpisodeDetailsActivity
        intent.putExtra(FROM_NOTIFICATION_TAP, true)
        intent.putExtra(EPISODE_TRAKT_ID, trackedEpisode.trakt_id)

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(trackedEpisode.trakt_id, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun getCancelPendingIntent(trackedEpisode: TrackedEpisode): PendingIntent {
        Log.d(TAG, "getCancelPendingIntent: Notification dismissed")

        val intent = Intent(context, CancelShowTrackingNotificationReceiver::class.java)
        intent.putExtra(DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID, trackedEpisode.trakt_id)

        return PendingIntent.getBroadcast(context, trackedEpisode.trakt_id, intent, PendingIntent.FLAG_IMMUTABLE)
    }


    companion object {
        const val CHANNEL_ID = "episodes_notifications_channel"
        const val FROM_NOTIFICATION_TAP = "notification_tapped_episode"
        const val DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID = "dismissed_trakt_episode_id"
        const val EPISODE_TRAKT_ID = "notification_trakt_episode_id"
        const val CHANNEL_NAME = "Upcoming Episodes Notifications"
        const val CHANNEL_DESCRIPTION = "Upcoming Episodes Notifications"
        const val GROUP_KEY = "com.nickrankin.traktapp.notifications.shows.upcoming"
    }
}