package com.nickrankin.traktapp.dao.show

import androidx.paging.PagingSource
import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode

@Dao
interface WatchedEpisodesDao {
    @Transaction
    @Query("SELECT * FROM watched_episodes ORDER BY watched_at DESC")
    fun getWatchedEpisodes(): PagingSource<Int, WatchedEpisode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchedEpisodes: List<WatchedEpisode>)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAllCachedEpisodes()
}