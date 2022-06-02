package com.nickrankin.traktapp.dao.credits

import androidx.room.*
import com.nickrankin.traktapp.dao.credits.model.MovieCastPersonData
import com.nickrankin.traktapp.dao.credits.model.ShowCastPersonData
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieCastPeopleDao {
    @Transaction
    @Query("SELECT * FROM movie_cast WHERE movie_trakt_id = :traktId ORDER BY ordering ASC")
    fun getMovieCast(traktId: Int): Flow<List<MovieCastPerson>>

    @Transaction
    @Query("SELECT * FROM movie_cast WHERE person_trakt_id = :traktId")
    fun getPersonMovies(traktId: Int): Flow<List<MovieCastPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movieCastPersonData: MovieCastPersonData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movieCastPersonData: List<MovieCastPersonData>)


    @Update
    fun update(movieCastPersonData: MovieCastPersonData)

    @Delete
    fun delete(movieCastPersonData: MovieCastPersonData)

    @Transaction
    @Query("DELETE FROM movie_cast WHERE movie_trakt_id = :traktId")
    fun deleteMovieCast(traktId: Int)
}