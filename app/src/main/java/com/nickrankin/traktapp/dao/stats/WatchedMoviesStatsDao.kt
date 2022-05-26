package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.WatchedMoviesStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedMoviesStatsDao {

    @Transaction
    @Query("SELECT * FROM stats_watched_movies WHERE trakt_id = :traktId")
    fun getWatchedStatById(traktId: Int): Flow<WatchedMoviesStats?>

    @Transaction
    @Query("SELECT * FROM stats_watched_movies")
    fun getWatchedStats(): Flow<List<WatchedMoviesStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedMoviesStats: List<WatchedMoviesStats>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedMoviesStats: WatchedMoviesStats)

    @Update
    fun update(watchedMoviesStats: WatchedMoviesStats)

    @Delete
    fun delete(watchedMoviesStats: WatchedMoviesStats)

    @Transaction
    @Query("DELETE FROM stats_watched_movies WHERE trakt_id = :traktId")
    fun deleteWatchedStatsById(traktId: Int)

    @Transaction
    @Query("DELETE FROM stats_watched_movies")
    fun deleteWatchedStats()
}