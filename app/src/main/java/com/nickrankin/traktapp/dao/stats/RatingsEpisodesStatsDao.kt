package com.nickrankin.traktapp.dao.stats

import androidx.room.*
import com.nickrankin.traktapp.dao.stats.model.RatingStats
import com.nickrankin.traktapp.dao.stats.model.RatingsEpisodesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingsEpisodesStatsDao {
    @Transaction
    @Query("SELECT * FROM stats_ratings_episodes")
    fun getRatingsStats(): Flow<List<RatingsEpisodesStats>>

    @Transaction
    @Query("SELECT * FROM stats_ratings_episodes WHERE trakt_id = :episodeTraktId")
    fun getRatingsStatsPerEpisode(episodeTraktId: Int): Flow<RatingsEpisodesStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsEpisodesStat: RatingsEpisodesStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratingsEpisodesStats: List<RatingsEpisodesStats>)

    @Update
    fun update(ratingsEpisodesStats: RatingsEpisodesStats)

    @Delete
    fun delete(ratingsEpisodesStats: RatingsEpisodesStats)

    @Transaction
    @Query("DELETE FROM stats_ratings_episodes WHERE trakt_id = :episodeTraktId")
    fun deleteRatingsStatByEpisodeId(episodeTraktId: Int)

    @Transaction
    @Query("DELETE FROM stats_ratings_episodes")
    fun deleteRatingsStats()
}