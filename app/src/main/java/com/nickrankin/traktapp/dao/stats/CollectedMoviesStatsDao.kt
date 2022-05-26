package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedMoviesStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_collected_movies")
    fun getCollectedStats(): Flow<List<CollectedMoviesStats>>

    @Transaction
    @Query("SELECT * FROM stats_collected_movies WHERE trakt_id = :traktId")
    fun getCollectedMovieStatsById(traktId: Int): Flow<CollectedMoviesStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedMoviesStats: List<CollectedMoviesStats>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedMoviesStats: CollectedMoviesStats)

    @Update
    fun update(collectedMoviesStats: CollectedMoviesStats)

    @Delete
    fun delete(collectedMoviesStats: CollectedMoviesStats)

    @Transaction
    @Query("DELETE FROM stats_collected_movies WHERE trakt_id = :traktId")
    fun deleteCollectedMovieStatById(traktId: Int)

    @Transaction
    @Query("DELETE FROM stats_collected_movies")
    fun deleteCollectedStats()
}