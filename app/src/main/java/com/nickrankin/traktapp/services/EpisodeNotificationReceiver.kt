package com.nickrankin.traktapp.services.helper

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "EpisodeNotificationRece"
@AndroidEntryPoint
class EpisodeNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var showsDatabase: ShowsDatabase

    @Inject
    lateinit var trackedEpisodeNotificationsBuilder: TrackedEpisodeNotificationsBuilder

    @OptIn(DelicateCoroutinesApi::class)
    val scope = GlobalScope


    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.e(TAG, "onReceive: Receive episode ${intent.getIntExtra(TRAKT_ID_KEY, -1)}", )

        showNotification(intent.getIntExtra(TRAKT_ID_KEY, -1))

    }

    private fun showNotification(episodeTraktId: Int) {
        if(episodeTraktId == -1) {
            Log.e(TAG, "showNotification: Valid episode ID not supplied ($episodeTraktId)", )
            return
        }

        val pendingResult: PendingResult = goAsync()

        @OptIn(DelicateCoroutinesApi::class)
        scope.launch {
            val episode = getTrackedEpisode(episodeTraktId)
            Log.d(TAG, "showNotification: Got episode for notification $episode")

                if(episode != null) {
                    // If the user already taps this notification, we don't show again. If user dismiss notification (swipe by accident) show it again once more
                    if(!episode.alreadyNotified && episode.dismiss_count < 3) {
                        trackedEpisodeNotificationsBuilder.buildNotification(episode)
                    } else {
                        Log.d(TAG, "showNotification: Episode $episodeTraktId dismissed 3 times, won't send again")
                    }
                } else {
                    Log.e(TAG, "showNotification: Episode $episodeTraktId not found in DB", )
                }

            pendingResult.finish()
        }
    }

    private suspend fun getTrackedEpisode(traktId: Int): TrackedEpisode? {
        val trackedEpisodeDao = showsDatabase.trackedEpisodeDao()

        return trackedEpisodeDao.getTrackedEpisode(traktId).first()
    }

    companion object {
        const val TRAKT_ID_KEY = "trakt_id"
    }
}