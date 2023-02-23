package com.nickrankin.traktapp.dao.stats

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nickrankin.traktapp.dao.stats.model.EpisodesCollectedStats
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodesCollectedStatsDao {

    @Transaction
    @Query("SELECT * FROM collected_episodes_stats WHERE show_trakt_id = :showTraktId AND season = :season AND episode = :episode")
    fun getCollectedStatsByEpisode(showTraktId: Int, season: Int, episode: Int): Flow<List<EpisodesCollectedStats?>>

    @Transaction
    @Query("SELECT * FROM collected_episodes_stats WHERE trakt_id = :episodeTraktId")
    fun getCollectedStatsByEpisodeId(episodeTraktId: Int): Flow<EpisodesCollectedStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(epsideCollectedStats: List<EpisodesCollectedStats>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(epsideCollectedStat: EpisodesCollectedStats)

    @Delete
    fun delete(episodesCollectedStats: EpisodesCollectedStats)

    @Transaction
    @Query("DELETE FROM collected_episodes_stats WHERE show_trakt_id = :showTraktId AND season = :season AND episode = :episode")
    fun deleteEpisodeStatsByEpisode(showTraktId: Int, season: Int, episode: Int)

    @Transaction
    @Query("DELETE FROM collected_episodes_stats WHERE trakt_id = :episodeTraktId")
    fun deleteEpisodeStatsByEpisodeId(episodeTraktId: Int)


    @Transaction
    @Query("DELETE FROM collected_episodes_stats WHERE show_trakt_id = :showTraktId")
    fun deleteEpisodeStatsByShowId(showTraktId: Int)
}