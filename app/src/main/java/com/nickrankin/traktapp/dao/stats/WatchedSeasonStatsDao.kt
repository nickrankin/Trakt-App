package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedSeasonStats
import com.nickrankin.traktapp.dao.stats.model.WatchedShowsStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedSeasonStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_watched_season WHERE show_trakt_id = :traktId")
    fun getWatchedStatsByShow(traktId: Int): Flow<List<WatchedSeasonStats>>

    @Transaction
    @Query("SELECT * FROM stats_watched_season")
    fun getWatchedStats(): Flow<List<WatchedSeasonStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedSeasonStats: List<WatchedSeasonStats>)

    @Update
    fun update(watchedSeasonStats: WatchedSeasonStats)

    @Delete
    fun delete(watchedSeasonStats: WatchedSeasonStats)

    @Transaction
    @Query("DELETE FROM stats_watched_season")
    fun deleteWatchedStats()
}