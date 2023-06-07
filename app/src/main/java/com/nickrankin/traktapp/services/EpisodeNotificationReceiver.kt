package com.nickrankin.traktapp.services.helper

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "EpisodeNotificationRece"
@AndroidEntryPoint
class EpisodeNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var traktApi: TraktApi
    
    @Inject
    lateinit var showsDatabase: ShowsDatabase

    @Inject
    lateinit var trackedEpisodeNotificationsBuilder: TrackedEpisodeNotificationsBuilder

    @OptIn(DelicateCoroutinesApi::class)
    val scope = GlobalScope


    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        showNotification(intent.getParcelableExtra(TRAKT_ID_KEY))

    }

    private fun showNotification(trackedEpisode: TrackedEpisode?) {
        if(trackedEpisode == null) {
            Log.e(TAG, "showNotification: Tracked Episode cannot be null", )
            return
        }

        val pendingResult: PendingResult = goAsync()

        @OptIn(DelicateCoroutinesApi::class)
        scope.launch {
            Log.d(TAG, "showNotification: Got episode for notification $trackedEpisode")

                    // If the user already taps this notification, we don't show again. If user dismiss notification (swipe by accident) show it again once more
                    if(!trackedEpisode.alreadyNotified && trackedEpisode.dismiss_count < 3) {
                        trackedEpisodeNotificationsBuilder.buildNotification(trackedEpisode)
                    } else {
                        Log.d(TAG, "showNotification: Episode $trackedEpisode dismissed 3 times, won't send again")
                    }

            pendingResult.finish()
        }
    }

    companion object {
        const val TRAKT_ID_KEY = "trakt_id"
    }
}