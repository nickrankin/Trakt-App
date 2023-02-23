package com.nickrankin.traktapp.dao.movies

import androidx.paging.PagingSource
import androidx.room.*
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedMoviesDao {
    @Transaction
    @Query("SELECT * FROM watched_movies ORDER BY watched_at DESC")
    fun getWatchedMovies(): PagingSource<Int, WatchedMovieAndStats>

    @Transaction
    @Query("SELECT * FROM watched_movies ORDER BY watched_at DESC LIMIT 5")
    fun getLastWatchedMovies(): Flow<List<WatchedMovie>>

    @Transaction
    @Query("SELECT * FROM watched_movies WHERE trakt_id = :traktId")
    fun getWatchedMovie(traktId: Int): Flow<WatchedMovie?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedMovies: List<WatchedMovie>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedMovie: WatchedMovie)

    @Update
    fun update(watchedMovie: WatchedMovie)

    @Delete
    fun delete(watchedMovie: WatchedMovie)

    @Query("DELETE FROM watched_movies WHERE id = :id")
    fun deleteMovieById(id: Long)

    @Transaction
    @Query("DELETE FROM watched_movies")
    fun deleteAllMovies()
}