package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.CollectedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedEpisodeDao {
    @Transaction
    @Query("SELECT * FROM collected_episodes")
    fun getCollectedEpisodes(): Flow<List<CollectedEpisode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedEpisodes: List<CollectedEpisode>)

    @Update
    fun update(collectedEpisode: CollectedEpisode)

    @Delete
    fun delete(collectedEpisode: CollectedEpisode)

    @Transaction
    @Query("DELETE FROM collected_episodes WHERE trakt_id = :episodeTraktId")
    fun deleteCollectedEpisodeById(episodeTraktId: Int)

    @Transaction
    @Query("DELETE FROM collected_episodes")
    fun deleteCollectedEpisodes()
}