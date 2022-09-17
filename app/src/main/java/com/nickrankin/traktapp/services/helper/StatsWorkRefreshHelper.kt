package com.nickrankin.traktapp.services.helper

import androidx.lifecycle.LiveData
import androidx.work.*
import com.nickrankin.traktapp.services.MovieStatsRefreshWorker
import com.nickrankin.traktapp.services.ShowStatsRefreshWorker
import javax.inject.Inject

private const val TAG_MOVIES_REFRESH = "refresh_movies"
private const val TAG_SHOWS_REFRESH = "refresh_shows"
class StatsWorkRefreshHelper @Inject constructor(private val workManager: WorkManager) {
    fun refreshMovieStats(): LiveData<WorkInfo> {

        workManager.cancelAllWorkByTag(TAG_MOVIES_REFRESH)

        val workRequest = OneTimeWorkRequestBuilder<MovieStatsRefreshWorker>()
            .addTag(TAG_MOVIES_REFRESH)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

         workManager.enqueue(workRequest)

        return workManager.getWorkInfoByIdLiveData(workRequest.id)
    }

    fun refreshShowStats(): LiveData<WorkInfo> {

        workManager.cancelAllWorkByTag(TAG_SHOWS_REFRESH)

        val workRequest = OneTimeWorkRequestBuilder<ShowStatsRefreshWorker>()
            .addTag(TAG_SHOWS_REFRESH)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueue(workRequest)

        return workManager.getWorkInfoByIdLiveData(workRequest.id)
    }
}