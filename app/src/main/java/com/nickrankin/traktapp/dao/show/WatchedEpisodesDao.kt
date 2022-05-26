package com.nickrankin.traktapp.dao.show

import androidx.paging.PagingSource
import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodeAndStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodesDao {
    @Transaction
    @Query("SELECT * FROM watched_episodes ORDER BY watched_at DESC")
    fun getWatchedEpisodes(): PagingSource<Int, WatchedEpisodeAndStats>

    @Transaction
    @Query("SELECT * FROM watched_episodes WHERE show_trakt_id = :showTraktId")
    fun getWatchedEpisodesByShowId(showTraktId: Int): Flow<List<WatchedEpisode>>

    @Transaction
    @Query("SELECT * FROM watched_episodes WHERE show_trakt_id = :showTraktId AND episode_season = :seasonNumber")
    fun getWatchedEpisodesByShowIdSeasonNumber(showTraktId: Int, seasonNumber: Int): Flow<List<WatchedEpisode>>

    @Transaction
    @Query("DELETE FROM watched_episodes WHERE show_trakt_id = :showTraktId AND episode_season = :seasonNumber")
    fun deleteWatchedEpisodesByShowIdSeasonNumber(showTraktId: Int, seasonNumber: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchedEpisodes: List<WatchedEpisode>)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAllCachedEpisodes()

    @Query("DELETE FROM watched_episodes WHERE id = :traktId")
    suspend fun deleteWatchedEpisodeById(traktId: Long)
}