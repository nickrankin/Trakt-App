package com.nickrankin.traktapp.repo.shows

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import com.nickrankin.traktapp.helper.EpisodeTrackingDataHelper
import com.nickrankin.traktapp.services.helper.TrackedEpisodeAlarmScheduler
import javax.inject.Inject

private const val DAYS_TO_TRACK_UPCOMING = 30
private const val DEFAULT_TRAKT_DATE_FORMAT = "yyyy-MM-dd"

private const val TAG = "ShowsTrackingRepository"

class ShowsTrackingRepository @Inject constructor(
    private val showsDatabase: ShowsDatabase,
    private val episodeTrackingDataHelper: EpisodeTrackingDataHelper) {
    private val trackedShowDao = showsDatabase.trackedShowDao()

    fun getTrackedShows() = trackedShowDao.getTrackedShowsWithShow()

    suspend fun refreshAllShowsTrackingData() = episodeTrackingDataHelper.refreshUpComingEpisodesForAllShows()

    suspend fun refreshUpcomingEpisodes(showTraktId: Int) = episodeTrackingDataHelper.refreshUpcomingEpisodesforShow(showTraktId)

    suspend fun insertTrackedShow(trackedShow: TrackedShow) {
        showsDatabase.withTransaction {
            trackedShowDao.insertTrackedShow(trackedShow)
        }
    }

    suspend fun removeExpiredTrackedShows(showTraktId: Int) = episodeTrackingDataHelper.removeExpiredTrackedEpisodesPerShow(showTraktId)

    suspend fun deleteTrackedShow(trackedShow: TrackedShow) {
        Log.d(TAG, "deleteTrackedShow: Stop tracking Show ${trackedShow.title} // Trakt Id ${trackedShow.trakt_id}")
        // Delete the tracked episodes and cancel any pending alarms
        episodeTrackingDataHelper.cancelTrackingPerShow(trackedShow.trakt_id)

        showsDatabase.withTransaction {
            trackedShowDao.deleteTrackedShow(trackedShow)
        }
    }
}