package com.nickrankin.traktapp.dao.show

import androidx.paging.PagingSource
import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodesDao {
    @Transaction
    @Query("SELECT * FROM watched_episodes ORDER BY watched_at DESC")
    fun getWatchedEpisodes(): PagingSource<Int, WatchedEpisode>

    @Transaction
    @Query("SELECT * FROM watched_episodes WHERE show_trakt_id = :showTraktId")
    fun getWatchedEpisodesByShowId(showTraktId: Int): Flow<List<WatchedEpisode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchedEpisodes: List<WatchedEpisode>)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAllCachedEpisodes()

    @Query("DELETE FROM watched_episodes WHERE id = :traktId")
    suspend fun deleteWatchedEpisodeById(traktId: Long)
}