package com.nickrankin.traktapp

import android.app.Application
import android.content.Intent
import android.content.Intent.getIntent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.services.DeleteExpiredEpisodesFromTrackingWorker
import com.nickrankin.traktapp.services.EpisodeTrackingRefreshWorker
import com.nickrankin.traktapp.services.StatsRefreshWorker
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.UserSlug
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private const val REFRESH_INTERVAL = 24L
private const val REFRESH_INTERVAL_REMOVE_EXPIRED_EPISODES = 4L
private const val RETRY_INTERVAL = 30

private const val EPISODES_DELETE_EXPIRED_TRACKING_ITEMS_KEY = "delete_expired_tracked_episodes"
private const val EPISODES_NOTIFICATION_SERVICE_WORKER_KEY = "refresh_trakt_episodes_worker"
private const val STATS_REFRESH_WORK_KEY = "refresh_user_stats_worker"

private const val TAG = "TmApplication"
@HiltAndroidApp
class TmApplication : Application(), Configuration.Provider {
    private lateinit var workManager: WorkManager

    @Inject
    lateinit var traktApi: TraktApi

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var userSlug: UserSlug

    private var isLoggedIn = false



    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        workManager = WorkManager.getInstance(this)

        authenticate()
        
        if(isLoggedIn) {
            // Setup Stats Refresh worker
            setupStatisticsRefreshService()
        }
        

        // Uncomment to cancel all work
//        WorkManager.getInstance(this).cancelAllWork()

        // If user has enabled Upcoming Episode notifications, schedule service to refresh every 24 hours (REFRESH_INTERVAL)
        if (sharedPreferences.getBoolean("enable_traked_show_notification", false)) {
            setupEpisodeNotificationService()
        }
    }

    fun authenticate() {
        val accessToken = sharedPreferences.getString(AuthActivity.ACCESS_TOKEN_KEY, "")
        val refreshToken = sharedPreferences.getString(AuthActivity.REFRESH_TOKEN_KEY, "")
        val slug = sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "NULL")

        if (accessToken?.isNotEmpty() == true) {
            Log.d(TAG, "authenticate: User is authenticated (access token ${accessToken})")

            updateLoginState(true)

            traktApi.accessToken(accessToken)
            traktApi.refreshToken(refreshToken)

            isLoggedIn = true
        } else {

            updateLoginState(false)

            sharedPreferences.edit()
                .putBoolean(AuthActivity.IS_LOGGED_IN, false)
                .apply()
        }

        userSlug = UserSlug(slug)
    }

    /***
     *
     * Function called to log user out of Trakt account.
     * @param keepSlug The slug will be kept during logout (e.g during Token Refresh)
     *
     * **/
    fun logout(keepSlug: Boolean): Boolean {
        Log.d(TAG, "logout: Logout called")

        sharedPreferences.edit()
            .remove(AuthActivity.ACCESS_TOKEN_KEY)
            .remove(AuthActivity.REFRESH_TOKEN_KEY)
            .remove(AuthActivity.REFRESH_TOKEN_AT_KEY)
            .apply()

        if(!keepSlug) {
            Log.d(TAG, "logout: Removing UserSlug")
            sharedPreferences.edit()
                .remove(AuthActivity.USER_SLUG_KEY)
                .apply()
        }

        traktApi.accessToken("")
        traktApi.refreshToken("")

        updateLoginState(false)

        return true
    }

    private fun updateLoginState(isLoggedIn: Boolean) {
        Log.d(TAG, "updateLoginState: Logon state updated. New state: ${isLoggedIn}")
        sharedPreferences.edit()
            .putBoolean(AuthActivity.IS_LOGGED_IN, isLoggedIn)
            .apply()
    }

    private fun setupEpisodeNotificationService() {

        // Refresh the users Tracked Shows to get any new Episodes
        val notificationsRefreshWork = PeriodicWorkRequestBuilder<EpisodeTrackingRefreshWorker>(
            REFRESH_INTERVAL, TimeUnit.HOURS
        ).addTag(EPISODES_NOTIFICATION_SERVICE_WORKER_KEY)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MINUTES
            ).build()

        workManager.enqueueUniquePeriodicWork(
            EPISODES_NOTIFICATION_SERVICE_WORKER_KEY,
            ExistingPeriodicWorkPolicy.KEEP,
            notificationsRefreshWork
        )

        // Check every 4 hours if there are any Expired episodes being tracked and delete them
        val expiredEpisodesDeletionWorker = PeriodicWorkRequestBuilder<DeleteExpiredEpisodesFromTrackingWorker>(
            REFRESH_INTERVAL_REMOVE_EXPIRED_EPISODES, TimeUnit.HOURS
        ).addTag(EPISODES_DELETE_EXPIRED_TRACKING_ITEMS_KEY)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MINUTES
            ).build()

        workManager.enqueueUniquePeriodicWork(
            EPISODES_DELETE_EXPIRED_TRACKING_ITEMS_KEY,
            ExistingPeriodicWorkPolicy.KEEP,
            expiredEpisodesDeletionWorker
        )
    }

    private fun setupStatisticsRefreshService() {
        Log.d(TAG, "setupStatisticsRefreshService: Setting up StatsRefreshWorker")
        val statsWork = PeriodicWorkRequestBuilder<StatsRefreshWorker>(
            REFRESH_INTERVAL, TimeUnit.HOURS
        ).addTag(STATS_REFRESH_WORK_KEY)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MINUTES
            ).build()

        workManager.enqueueUniquePeriodicWork(
            STATS_REFRESH_WORK_KEY,
            ExistingPeriodicWorkPolicy.KEEP,
            statsWork
        )
    }
}