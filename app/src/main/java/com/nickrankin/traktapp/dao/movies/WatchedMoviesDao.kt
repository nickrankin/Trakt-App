package com.nickrankin.traktapp.dao.movies

import androidx.paging.PagingSource
import androidx.room.*
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedMoviesDao {
    @Transaction
    @Query("SELECT * FROM watched_movies ORDER BY watched_at DESC")
    fun getWatchedMovies(): PagingSource<Int, WatchedMovie>

    @Transaction
    @Query("SELECT * FROM watched_movies WHERE trakt_id = :traktId")
    fun getWatchedMovie(traktId: Int): Flow<WatchedMovie?>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(watchedMovies: List<WatchedMovie>)

    @Update
    fun update(watchedMovie: WatchedMovie)

    @Delete
    fun delete(watchedMovie: WatchedMovie)

    @Query("DELETE FROM watched_movies WHERE trakt_id = :traktId")
    fun deleteMovieById(traktId: Long)

    @Transaction
    @Query("DELETE FROM watched_movies")
    fun deleteAllMovies()
}