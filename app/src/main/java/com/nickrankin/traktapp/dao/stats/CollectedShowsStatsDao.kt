package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.ShowsCollectedStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedShowsStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_collected_shows WHERE trakt_id = :traktId")
    fun getCollectedShowById(traktId: Int): Flow<ShowsCollectedStats?>

    @Transaction
    @Query("SELECT * FROM stats_collected_shows")
    fun getCollectedStats(): Flow<List<ShowsCollectedStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedShowsStat: ShowsCollectedStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(showsCollectedStats: List<ShowsCollectedStats>)

    @Update
    fun update(showsCollectedStats: ShowsCollectedStats)

    @Delete
    fun delete(showsCollectedStats: ShowsCollectedStats)

    @Transaction
    @Query("DELETE FROM stats_collected_shows")
    fun deleteCollectedStats()

    @Transaction
    @Query("DELETE FROM stats_collected_shows WHERE trakt_id = :traktId")
    fun deleteCollectedStatsById(traktId: Int)
}