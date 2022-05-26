package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsShowsStats
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingsShowsStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_ratings_shows")
    fun getRatingsStats(): Flow<List<RatingsShowsStats>>

    @Transaction
    @Query("SELECT * FROM stats_ratings_shows WHERE trakt_id = :traktId")
    fun getRatingsStatsById(traktId: Int): Flow<RatingsShowsStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsShowsStats: RatingsShowsStats)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsShowsStats: List<RatingsShowsStats>)

    @Update
    fun update(ratingsShowsStats: RatingsShowsStats)

    @Delete
    fun delete(ratingsShowsStats: RatingsShowsStats)

    @Transaction
    @Query("DELETE FROM stats_ratings_shows")
    fun deleteRatingsStats()
}