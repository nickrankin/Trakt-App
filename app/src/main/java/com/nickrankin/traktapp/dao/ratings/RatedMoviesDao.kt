package com.nickrankin.traktapp.dao.ratings

import androidx.room.*
import com.nickrankin.traktapp.dao.ratings.model.RatedMovie
import kotlinx.coroutines.flow.Flow

@Dao
interface RatedMoviesDao {

    @Transaction
    @Query("SELECT * FROM rated_movies")
    fun getRatings(): Flow<List<RatedMovie?>>

    @Transaction
    @Query("SELECT * FROM rated_movies WHERE trakt_id = :traktId")
    fun getRatingForMovie(traktId: Int): Flow<RatedMovie?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ratedMovies: List<RatedMovie>)

    @Update
    fun update(ratedMovie: RatedMovie)

    @Delete
    fun delete(ratedMovie: RatedMovie)

    @Transaction
    @Query("DELETE FROM rated_movies WHERE trakt_id = :movieTraktId")
    fun deleteRatingByTraktId(movieTraktId: Int)

    @Transaction
    @Query("DELETE FROM rated_movies")
    fun deleteRatedMovies()
}