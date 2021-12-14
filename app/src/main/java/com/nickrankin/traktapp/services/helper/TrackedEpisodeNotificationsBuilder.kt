package com.nickrankin.traktapp.services.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.repo.shows.EpisodeDetailsRepository
import com.nickrankin.traktapp.ui.shows.EpisodeDetailsActivity
import okhttp3.internal.notify
import javax.inject.Singleton

private const val TAG = "TrackedEpisodeNotificat"
@Singleton
class TrackedEpisodeNotificationsBuilder(private val context: Context) {
    init {
        createNotificationChannel()
    }

    fun buildNotification(trackedEpisode: TrackedEpisode) {
        var builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trakt_svgrepo_com)
            .setContentTitle("Upcoming episode ${trackedEpisode.title}")
            .setContentText("Show ${trackedEpisode.show_title}. Season ${trackedEpisode.season} Episode ${trackedEpisode.episode}")
            .setContentIntent(getPendingIntent(trackedEpisode))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Much longer text that cannot fit one line..."))
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
        val intent = Intent(context, EpisodeDetailsActivity::class.java)
        intent.putExtra(EpisodeDetailsRepository.SHOW_TMDB_ID_KEY, trackedEpisode.show_tmdb_id)
        intent.putExtra(EpisodeDetailsRepository.SEASON_NUMBER_KEY, trackedEpisode.season)
        intent.putExtra(EpisodeDetailsRepository.EPISODE_NUMBER_KEY, trackedEpisode.episode)
        intent.putExtra(EpisodeDetailsRepository.LANGUAGE_KEY, trackedEpisode.language)

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)

            getPendingIntent(trackedEpisode.trakt_id, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }


    companion object {
        const val CHANNEL_ID = "episodes_notifications_channel"
        const val CHANNEL_NAME = "Upcoming Episodes Notifications"
        const val CHANNEL_DESCRIPTION = "Upcoming Episodes Notifications"
    }
}