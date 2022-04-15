package com.nickrankin.traktapp.dao.movies

import androidx.room.*
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface TmMovieDao {
    @Transaction
    @Query("SELECT * FROM movies WHERE trakt_id = :traktId")
    fun getMovieById(traktId: Int): Flow<TmMovie?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tmMovie: TmMovie)

    @Update
    fun update(movie: TmMovie)

    @Delete
    fun delete(tmMovie: TmMovie)

    @Transaction
    @Query("DELETE FROM movies")
    fun deleteAllMovies()
}