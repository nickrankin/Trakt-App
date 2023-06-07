package com.nickrankin.traktapp.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.EpisodeTrackingDataHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.threeten.bp.OffsetDateTime

private const val TAG = "DeleteExpiredEpisodesFr"
@HiltWorker
class DeleteExpiredEpisodesFromTrackingWorker @AssistedInject constructor(@Assisted val context: Context, @Assisted params: WorkerParameters, val episodeTrackingDataHelper: EpisodeTrackingDataHelper, val sharedPreferences: SharedPreferences): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val trackingEnabled = sharedPreferences.getBoolean(AppConstants.EPISODE_TRACKING_ENABLED, false)

        return if(trackingEnabled) {
            Log.d(TAG, "doWork: Removing expired episodes from tracking.")
            return try {
                // Refreshes all Episodes and schedule their alarms
                episodeTrackingDataHelper.removeExpiredEpisodesForTracking()

                Result.success()
            } catch(e: Exception) {
                Log.e(TAG, "doWork: Error removing expired episodes from tracking. ${e.localizedMessage}")
                e.printStackTrace()

                Result.retry()
            }
        } else {
            Log.d(TAG, "doWork: Episode Tracking not enabled, nothing to remove.")
            Result.success()
        }

    }
}