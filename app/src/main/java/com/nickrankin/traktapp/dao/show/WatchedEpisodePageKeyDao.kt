package com.nickrankin.traktapp.dao.show

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodePageKey

@Dao
interface WatchedEpisodePageKeyDao {
    @Query("SELECT * FROM watched_episode_page_keys WHERE episodeTraktId = :episodeTraktId")
    suspend fun remoteKeyByPage(episodeTraktId: Int): WatchedEpisodePageKey

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keys: List<WatchedEpisodePageKey>)

    @Query("DELETE FROM watched_episode_page_keys WHERE episodeTraktId = :episodeTraktId")
    suspend fun deleteByPage(episodeTraktId: Int)

    @Query("DELETE FROM watched_episode_page_keys")
    fun deleteAll()
}