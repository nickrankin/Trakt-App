package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.CollectedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedEpisodeDao {
    @Transaction
    @Query("SELECT * FROM collected_episodes")
    fun getCollectedEpisodes(): Flow<List<CollectedEpisode>>

    @Transaction
    @Query("SELECT * FROM collected_episodes WHERE trakt_id = :traktId")
    fun getCollectedEpisodeById(traktId: Int): Flow<CollectedEpisode?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedEpisodes: List<CollectedEpisode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedEpisode: CollectedEpisode)

    @Update
    fun update(collectedEpisode: CollectedEpisode)

    @Delete
    fun delete(collectedEpisode: CollectedEpisode)

    @Transaction
    @Query("DELETE FROM collected_episodes WHERE show_trakt_id = :showTraktId AND season_number = :seasonNumber AND episode_number = :episodeNumber")
    fun deleteCollectedEpisodeById(showTraktId: Int, seasonNumber: Int, episodeNumber: Int)

    @Transaction
    @Query("DELETE FROM collected_episodes")
    fun deleteCollectedEpisodes()
}