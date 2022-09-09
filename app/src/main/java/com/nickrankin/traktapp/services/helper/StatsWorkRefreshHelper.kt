package com.nickrankin.traktapp.services.helper

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.nickrankin.traktapp.services.ShowStatsRefreshWorker
import javax.inject.Inject

class StatsWorkRefreshHelper @Inject constructor(private val workManager: WorkManager) {
    fun refreshMovieStats() {

    }

    fun refreshShowStats() {
        workManager.enqueue(
            OneTimeWorkRequestBuilder<ShowStatsRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        )
    }
}