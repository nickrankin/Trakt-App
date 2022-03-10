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
    fun setNotificationStatus(episodeTraktId: Int, status: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(trackedEpisodes: List<TrackedEpisode>)

    @Update
    fun update(trackedEpisode: TrackedEpisode)

    @Delete
    fun delete(trackedEpisode: TrackedEpisode)

    @Query("DELETE FROM notifications_episode WHERE show_trakt_id = :episodeTraktId")
    fun deleteAllPerShow(episodeTraktId: Int)

    @Transaction
    @Query("DELETE FROM notifications_episode")
    fun deleteAllEpisodesForNotification()
}