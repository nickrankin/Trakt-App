package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.TrackedShow
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedShowDao {
    @Transaction
    @Query("SELECT trakt_id FROM tracked_shows")
    fun getTrackedShowIds(): Flow<List<Int>>

    @Transaction
    @Query("SELECT * FROM tracked_shows")
    fun getTrackedShows(): Flow<List<TrackedShow>>

    @Transaction
    @Query("SELECT * FROM tracked_shows WHERE trakt_id = :traktId")
    fun getTrackedShow(traktId: Int): Flow<TrackedShow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrackedShow(trackedShow: TrackedShow)

    @Update
    fun updateTrackedShow(trackedShow: TrackedShow)

    @Delete
    fun deleteTrackedShow(trackedShow: TrackedShow)

    @Query("DELETE FROM tracked_shows")
    fun deleteAllTrackedShows()
}