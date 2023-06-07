package com.nickrankin.traktapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.databinding.ActivityDebugBinding
import com.nickrankin.traktapp.helper.EpisodeTrackingDataHelper
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DebugActvity: AppCompatActivity() {
    private lateinit var bindings: ActivityDebugBinding

    @Inject
    lateinit var episodeTrackingDataHelper: EpisodeTrackingDataHelper

    @Inject
    lateinit var traktAlarmScheduler: TrackedEpisodeAlarmScheduler

    @Inject
    lateinit var showsDatabase: ShowsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityDebugBinding.inflate(layoutInflater)

        setContentView(bindings.root)

        bindings.debugRefreshTrackedEpisodes.setOnClickListener {
            refreshTrackedcEpisodes()
        }

        bindings.debugRemoveExpiredEpisodes.setOnClickListener {
            deleteExpiredTrackedShows()
        }

        bindings.debugAddSampleEpisodes.setOnClickListener {
            TODO()
        }

        bindings.debugRegisterSampleNotifications.setOnClickListener {
            generateTestNotifications()
        }

        bindings.debugRegisterDeleteNotifications.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                showsDatabase.withTransaction {
                    showsDatabase.trackedEpisodeDao().deleteAllEpisodesForNotification()
                }
            }

        }

    }

    private fun refreshTrackedcEpisodes() {
        lifecycleScope.launchWhenStarted {
            episodeTrackingDataHelper.refreshUpComingEpisodesForAllShows()
        }
    }

    private fun deleteExpiredTrackedShows() {
        lifecycleScope.launchWhenStarted {
            episodeTrackingDataHelper.removeExpiredEpisodesForTracking()
        }
    }

    private fun generateTestNotifications() {
        lifecycleScope.launchWhenStarted {
            traktAlarmScheduler.generateTestNotifications()
        }
    }

}