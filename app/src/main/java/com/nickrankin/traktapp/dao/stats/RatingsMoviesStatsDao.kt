package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingsMoviesStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_ratings_movies")
    fun getRatingsStats(): Flow<List<RatingsMoviesStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsMoviesStats: RatingsMoviesStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsMoviesStats: List<RatingsMoviesStats>)

    @Update
    fun update(ratingsMoviesStats: RatingsMoviesStats)

    @Delete
    fun delete(ratingsMoviesStats: RatingsMoviesStats)

    @Transaction
    @Query("DELETE FROM stats_ratings_movies WHERE trakt_id = :traktId")
    fun deleteRatingsStatsById(traktId: Int)

    @Transaction
    @Query("DELETE FROM stats_ratings_movies")
    fun deleteRatingsStats()
}