package com.nickrankin.traktapp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ScheduleTrackedEpisodes"
@AndroidEntryPoint
class ScheduleTrackedEpisodesBootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var alarmScheduler: TrackedEpisodeAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if(Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d(TAG, "onReceive: Boot process completed")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    alarmScheduler.scheduleAllAlarms()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}