package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.WatchedEpisodeStats
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.WatchedShowsStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodesStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_watched_episode")
    fun getWatchedStats(): Flow<List<WatchedEpisodeStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedEpisodeStats: List<WatchedEpisodeStats>)

    @Update
    fun update(watchedEpisodeStats: WatchedEpisodeStats)

    @Delete
    fun delete(watchedEpisodeStats: WatchedEpisodeStats)

    @Transaction
    @Query("DELETE FROM stats_watched_episode")
    fun deleteWatchedStats()

    @Transaction
    @Query("DELETE FROM stats_watched_episode WHERE show_trakt_id = :showTraktId")
    fun deleteWatchedStatsPerShow(showTraktId: Int)
}