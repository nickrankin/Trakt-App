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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EpisodeNotificationRece"
@AndroidEntryPoint
class EpisodeNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var showsDatabase: ShowsDatabase

    @Inject
    lateinit var trackedEpisodeNotificationsBuilder: TrackedEpisodeNotificationsBuilder


    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.e(TAG, "onReceive: Receive epiasode ${intent.getIntExtra(TRAKT_ID_KEY, -1)}", )

        showNotification(intent.getIntExtra(TRAKT_ID_KEY, -1))

    }

    private fun showNotification(episodeTraktId: Int) {
        if(episodeTraktId == -1) {
            Log.e(TAG, "showNotification: Valid episode ID not supplied ($episodeTraktId)", )
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val episode = getTrackedEpisode(episodeTraktId)

            if(episode != null) {
                trackedEpisodeNotificationsBuilder.buildNotification(episode)
            } else {
                Log.e(TAG, "showNotification: Episode $episodeTraktId not found in DB", )
            }
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