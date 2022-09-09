package com.nickrankin.traktapp.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nickrankin.traktapp.repo.stats.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "ShowStatsRefreshWorker"
@HiltWorker
class ShowStatsRefreshWorker @AssistedInject constructor(@Assisted val context: Context, @Assisted params: WorkerParameters, val statsRepository: StatsRepository): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "doWork: Refreshing show stats")
            statsRepository.refreshAllShowStats()

            Log.d(TAG, "doWork: Refreshing Show Stats completed ok")

            Result.success()
        } catch(e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "doWork: Error refreshing show Stats. Error ${e.message}" )
            e.printStackTrace()
            Result.retry()
        }
    }
}