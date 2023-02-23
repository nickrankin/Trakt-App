package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.MoviesCollectedStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedMoviesStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_collected_movies")
    fun getCollectedStats(): Flow<List<MoviesCollectedStats>>

    @Transaction
    @Query("SELECT * FROM stats_collected_movies WHERE trakt_id = :traktId")
    fun getCollectedMovieStatsById(traktId: Int): Flow<MoviesCollectedStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(moviesCollectedStats: List<MoviesCollectedStats>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(moviesCollectedStats: MoviesCollectedStats)

    @Update
    fun update(moviesCollectedStats: MoviesCollectedStats)

    @Delete
    fun delete(moviesCollectedStats: MoviesCollectedStats)

    @Transaction
    @Query("DELETE FROM stats_collected_movies WHERE trakt_id = :traktId")
    fun deleteCollectedMovieStatById(traktId: Int)

    @Transaction
    @Query("DELETE FROM stats_collected_movies")
    fun deleteCollectedStats()
}