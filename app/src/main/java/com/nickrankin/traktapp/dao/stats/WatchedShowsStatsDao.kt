package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedShowsStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedShowsStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_watched_shows")
    fun getWatchedStats(): Flow<List<WatchedShowsStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedShowsStats: List<WatchedShowsStats>)

    @Update
    fun update(watchedShowsStats: WatchedShowsStats)

    @Delete
    fun delete(watchedShowsStats: WatchedShowsStats)

    @Transaction
    @Query("DELETE FROM stats_watched_shows")
    fun deleteWatchedStats()
}