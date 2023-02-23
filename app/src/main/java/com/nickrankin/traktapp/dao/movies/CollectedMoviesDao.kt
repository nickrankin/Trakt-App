package com.nickrankin.traktapp.dao.movies

import androidx.room.*
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.uwetrottmann.trakt5.enums.SortBy
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedMoviesDao {
    @Transaction
    @Query("SELECT * FROM collected_movies")
    fun getCollectedMovies(): Flow<List<CollectedMovie>>

    @Transaction
    @Query("SELECT * FROM collected_movies WHERE trakt_id = :traktId")
    fun getCollectedMovie(traktId: Int): Flow<CollectedMovie?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedMovie: CollectedMovie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectedMovies: List<CollectedMovie>)

    @Update
    fun update(collectedMovie: CollectedMovie)

    @Delete
    fun delete(collectedMovie: CollectedMovie)

    @Query("DELETE FROM collected_movies WHERE trakt_id = :traktId")
    fun deleteMovieById(traktId: Int)

    @Transaction
    @Query("DELETE FROM collected_movies")
    fun deleteAllMovies()
}