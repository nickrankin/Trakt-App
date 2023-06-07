package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedEpisodeDao {
    @Transaction
    @Query("SELECT * FROM notifications_episode")
    fun getAllEpisodesForNotification(): Flow<List<TrackedEpisode>>

    @Transaction
    @Query("SELECT * FROM notifications_episode WHERE show_trakt_id = :showTraktId")
    fun getAllEpisodesForShow(showTraktId: Int): Flow<List<TrackedEpisode>>

    @Transaction
    @Query("SELECT * FROM notifications_episode WHERE trakt_id = :traktId")
    fun getTrackedEpisode(traktId: Int): Flow<TrackedEpisode?>

    @Transaction
    @Query("UPDATE notifications_episode SET alreadyNotified = :status WHERE trakt_id = :episodeTraktId")
    fun setNotificationStatus(episodeTraktId: Int, status: Boolean)

    @Transaction
    @Query("UPDATE notifications_episode SET dismiss_count = dismiss_count + 1 WHERE trakt_id = :episodeTraktId")
    fun setDismissCount(episodeTraktId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trackedEpisodes: List<TrackedEpisode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trackedEpisode: TrackedEpisode)

    @Update
    fun update(trackedEpisode: TrackedEpisode)

    @Delete
    fun delete(trackedEpisode: TrackedEpisode)

    @Delete
    fun deleteAll(trackedEpisodes: List<TrackedEpisode>)

    @Query("DELETE FROM notifications_episode WHERE show_trakt_id = :episodeTraktId")
    fun deleteAllPerShow(episodeTraktId: Int)

    @Transaction
    @Query("DELETE FROM notifications_episode")
    fun deleteAllEpisodesForNotification()
}