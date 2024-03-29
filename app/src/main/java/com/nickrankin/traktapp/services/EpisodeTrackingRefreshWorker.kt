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
import javax.inject.Inject

private const val TAG = "EpisodeTrackingRefreshW"
@HiltWorker
class EpisodeTrackingRefreshWorker @AssistedInject constructor(@Assisted val context: Context, @Assisted params: WorkerParameters, val episodeTrackingDataHelper: EpisodeTrackingDataHelper, val sharedPreferences: SharedPreferences): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val trackingEnabled = sharedPreferences.getBoolean(AppConstants.EPISODE_TRACKING_ENABLED, false)
        
        return if(trackingEnabled) {
            Log.d(TAG, "doWork: Refreshing Tracked Episodes now. Date ${OffsetDateTime.now()}")
            return try {
                // Refreshes all Episodes and schedule their alarms
                episodeTrackingDataHelper.refreshUpComingEpisodesForAllShows()

                Result.success()
            } catch(e: Exception) {
                Log.e(TAG, "doWork: Error refreshing Tracked Episodes. ${e.localizedMessage}")
                e.printStackTrace()

                Result.retry()
            }
        } else {
            Log.d(TAG, "doWork: Episode Tracking not enabled, nothing to refresh.")
            Result.success()
        }

    }
}