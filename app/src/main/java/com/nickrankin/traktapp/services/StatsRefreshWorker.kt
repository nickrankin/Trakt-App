package com.nickrankin.traktapp.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nickrankin.traktapp.repo.movies.watched.WatchedMoviesRemoteMediator
import com.nickrankin.traktapp.repo.shows.watched.WatchedEpisodesRemoteMediator
import com.nickrankin.traktapp.repo.stats.EpisodesStatsRepository
import com.nickrankin.traktapp.repo.stats.MovieStatsRepository
import com.nickrankin.traktapp.repo.stats.ShowStatsRepository
import com.nickrankin.traktapp.repo.stats.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "StatsRefreshWorker"
@HiltWorker
class StatsRefreshWorker @AssistedInject constructor(@Assisted val context: Context, @Assisted params: WorkerParameters, val statsRepository: StatsRepository, val movieStatsRepository: MovieStatsRepository, val showStatsRepository: ShowStatsRepository, val episodesStatsRepository: EpisodesStatsRepository, val sharedPreferences: SharedPreferences): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "doWork: Refreshing user stats")
            statsRepository.refreshUserStats()
            movieStatsRepository.refreshAllMovieStats()
            showStatsRepository.refreshAllShowStats()
            episodesStatsRepository.refreshEpisodeStats()

            // Force refresh of Paging components
            sharedPreferences.edit()
                .putBoolean(WatchedMoviesRemoteMediator.WATCHED_MOVIES_FORCE_REFRESH_KEY, true)
                .putBoolean(WatchedEpisodesRemoteMediator.WATCHED_EPISODES_FORCE_REFRESH_KEY, true)
                .apply()

            Result.success()
        } catch(e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "doWork: Error refreshing user Stats. Error ${e.message}" )
            e.printStackTrace()
            Result.retry()
        }
    }
}