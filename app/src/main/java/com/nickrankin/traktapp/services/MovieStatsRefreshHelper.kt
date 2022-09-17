package com.nickrankin.traktapp.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


private const val TAG = "MovieStatsRefreshHelper"
@HiltWorker
class MovieStatsRefreshWorker @AssistedInject constructor(@Assisted val context: Context, @Assisted params: WorkerParameters, val movieStatsRepository: MovieStatsRepository): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "doWork: Refreshing movie stats")
            movieStatsRepository.refreshAllMovieStats()

            Log.d(TAG, "doWork: Refreshing movie Stats completed ok")

            Result.success()
        } catch(e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "doWork: Error refreshing movie Stats. Error ${e.message}" )
            e.printStackTrace()
            Result.failure()
        }
    }
}