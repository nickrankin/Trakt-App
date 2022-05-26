package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.CollectedShowsStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedShowsStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_collected_shows WHERE trakt_id = :traktId")
    fun getCollectedShowById(traktId: Int): Flow<CollectedShowsStats?>

    @Transaction
    @Query("SELECT * FROM stats_collected_shows")
    fun getCollectedStats(): Flow<List<CollectedShowsStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedShowsStat: CollectedShowsStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedShowsStats: List<CollectedShowsStats>)

    @Update
    fun update(collectedShowsStats: CollectedShowsStats)

    @Delete
    fun delete(collectedShowsStats: CollectedShowsStats)

    @Transaction
    @Query("DELETE FROM stats_collected_shows")
    fun deleteCollectedStats()

    @Transaction
    @Query("DELETE FROM stats_collected_shows WHERE trakt_id = :traktId")
    fun deleteCollectedStatsById(traktId: Int)
}