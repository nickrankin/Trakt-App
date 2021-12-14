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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trackedEpisode: TrackedEpisode)

    @Update
    fun update(trackedEpisode: TrackedEpisode)

    @Delete
    fun delete(trackedEpisode: TrackedEpisode)

    @Transaction
    @Query("DELETE FROM notifications_episode")
    fun deleteAllEpisodesForNotification()
}