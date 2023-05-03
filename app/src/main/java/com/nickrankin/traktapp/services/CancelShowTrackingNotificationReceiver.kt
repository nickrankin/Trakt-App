package com.nickrankin.traktapp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.TrackedEpisodeDao
import com.nickrankin.traktapp.services.helper.TrackedEpisodeNotificationsBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CancelShowTrackingNotif"
@AndroidEntryPoint
class CancelShowTrackingNotificationReceiver: BroadcastReceiver() {
    @Inject
    lateinit var showsDatabase: ShowsDatabase

    private lateinit var trackedEpisodeDao: TrackedEpisodeDao

    @OptIn(DelicateCoroutinesApi::class)
    private val scope = GlobalScope

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.hasExtra(TrackedEpisodeNotificationsBuilder.DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID)) {
            Log.d(TAG, "onReceive: Notification deleted")
            trackedEpisodeDao = showsDatabase.trackedEpisodeDao()

            val episodeTraktId = intent.getIntExtra(TrackedEpisodeNotificationsBuilder.DISMISSED_TRAKT_EPISODE_NOTIFICATION_ID, -1)
            val pendingResult: PendingResult = goAsync()

            if(episodeTraktId != -1) {
                Log.d(TAG, "onReceive: User dismissed notification for $episodeTraktId without tapping")

                @OptIn(DelicateCoroutinesApi::class)
                scope.launch {
                    showsDatabase.withTransaction {
                        Log.d(TAG, "onReceive: Incrementing notifications dismiss count")
                        trackedEpisodeDao.setDismissCount(episodeTraktId)
                        Log.d(TAG, "onReceive: Successfully incremented dismiss count")
                    }
                    pendingResult.finish()
                }

            } else {
                Log.e(TAG, "onReceive: Incorrect Episode Traky ID value received (-1)")
            }
        }
    }
}