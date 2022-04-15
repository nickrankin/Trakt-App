package com.nickrankin.traktapp.dao.movies

import androidx.room.*
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodePageKey

@Dao
interface WatchedMoviePageKeyDao {
    @Query("SELECT * FROM watched_movie_page_keys WHERE historyId = :id")
    suspend fun remoteKeyByPage(id: Long): WatchedMoviePageKey

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keys: List<WatchedMoviePageKey>)

    @Query("DELETE FROM watched_movie_page_keys WHERE historyId = :id")
    suspend fun deleteByPage(id: Long)

    @Query("DELETE FROM watched_movie_page_keys")
    fun deleteAll()
}